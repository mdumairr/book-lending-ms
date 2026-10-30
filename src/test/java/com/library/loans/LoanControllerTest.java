package com.library.loans;

import com.library.common.exception.GlobalExceptionHandler;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock LoanService loanService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoanController(loanService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Loan stubLoan(UUID bookId, UUID memberId) {
        return new Loan(bookId, memberId, Instant.parse("2026-06-15T00:00:00Z"), LocalDate.of(2026, 6, 29));
    }

    // --- POST /api/loans/borrow/{bookId} ---

    @Test
    void borrowReturns201WithLoanResponse() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.borrow(bookId)).thenReturn(stubLoan(bookId, UUID.randomUUID()));

        mockMvc.perform(post("/api/loans/borrow/" + bookId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.returnedAt").doesNotExist());
    }

    @Test
    void borrowReturns409WhenOutOfStock() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.borrow(bookId))
                .thenThrow(new LoanRejectedException("out_of_stock", "No available copies"));

        mockMvc.perform(post("/api/loans/borrow/" + bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("out_of_stock"));
    }

    @Test
    void borrowReturns409WhenMaxLoansExceeded() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.borrow(bookId))
                .thenThrow(new LoanRejectedException("max_loans_exceeded", "Too many active loans"));

        mockMvc.perform(post("/api/loans/borrow/" + bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("max_loans_exceeded"));
    }

    @Test
    void borrowReturns409WhenOverdueLoanExists() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.borrow(bookId))
                .thenThrow(new LoanRejectedException("overdue_loan_exists", "Has overdue loan"));

        mockMvc.perform(post("/api/loans/borrow/" + bookId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("overdue_loan_exists"));
    }

    @Test
    void borrowReturns404WhenBookNotFound() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.borrow(bookId))
                .thenThrow(new ResourceNotFoundException("Book not found: " + bookId));

        mockMvc.perform(post("/api/loans/borrow/" + bookId))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/loans/return/{bookId} ---

    @Test
    void returnReturns200WithReturnedLoan() throws Exception {
        UUID bookId = UUID.randomUUID();
        Loan returned = stubLoan(bookId, UUID.randomUUID());
        returned.setReturnedAt(Instant.parse("2026-06-20T00:00:00Z"));
        when(loanService.returnLoan(bookId)).thenReturn(returned);

        mockMvc.perform(post("/api/loans/return/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").value("2026-06-20T00:00:00Z"));
    }

    @Test
    void returnReturns404WhenNoActiveLoan() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(loanService.returnLoan(bookId))
                .thenThrow(new ResourceNotFoundException("No active loan for book " + bookId));

        mockMvc.perform(post("/api/loans/return/" + bookId))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/loans ---

    @Test
    void listLoansReturnsGroupedResponse() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(loanService.listLoansForCurrentUser()).thenReturn(
                List.of(new MemberLoansResponse(memberId, "Demo Member", "member@library.com", List.of()))
        );

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].memberEmail").value("member@library.com"));
    }

    // --- GET /api/loans/{loanId} ---

    @Test
    void getReturns404WhenLoanNotFound() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.get(loanId))
                .thenThrow(new ResourceNotFoundException("Loan not found: " + loanId));

        mockMvc.perform(get("/api/loans/" + loanId))
                .andExpect(status().isNotFound());
    }
}
