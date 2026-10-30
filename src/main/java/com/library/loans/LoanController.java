package com.library.loans;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
class LoanController {

    private final LoanService service;

    LoanController(LoanService service) {
        this.service = service;
    }

    @Operation(summary = "Borrow a book",
            description = "Who can access: Member only.\n\n" +
                    "What it does:\n" +
                    "- Member: borrows a book by book id; the member is resolved from the logged-in user.\n\n" +
                    "Business logic:\n" +
                    "- Max active loans per member must not be exceeded.\n" +
                    "- Member must not have any overdue active loan.\n" +
                    "- Member cannot borrow a book they already have on loan.\n" +
                    "- Stock is decremented atomically; out-of-stock returns 409.\n" +
                    "- Due date = borrow date + configured loan duration.")
    @PostMapping("/borrow/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrow(@PathVariable UUID bookId) {
        return LoanResponse.from(service.borrow(bookId));
    }

    @Operation(summary = "Return a borrowed book",
            description = "Who can access: Member only.\n\n" +
                    "What it does:\n" +
                    "- Member: returns the active loan for the given book; member resolved from principal.\n\n" +
                    "Business logic:\n" +
                    "- Idempotent: returning the same book twice is a no-op.\n" +
                    "- Stock is incremented once on first return.")
    @PostMapping("/return/{bookId}")
    public LoanResponse returnLoan(@PathVariable UUID bookId) {
        return LoanResponse.from(service.returnLoan(bookId));
    }

    @Operation(summary = "Get a loan by id",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: returns a single loan record.")
    @GetMapping("/{loanId}")
    public LoanResponse get(@PathVariable UUID loanId) {
        return LoanResponse.from(service.get(loanId));
    }

    @Operation(summary = "List loans for a member",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: returns all loans (active and returned) for the given member.")
    @GetMapping("/members/{memberId}")
    public List<LoanResponse> listByMember(@PathVariable UUID memberId) {
        return service.listByMember(memberId).stream().map(LoanResponse::from).toList();
    }

    @Operation(summary = "List loans",
            description = "Who can access: Librarian or Member.\n\n" +
                    "What it does:\n" +
                    "- Librarian: returns all loans grouped by member.\n" +
                    "- Member: returns only their own loans (same structure, single entry).\n\n" +
                    "Business logic:\n" +
                    "- Member identity is resolved from the logged-in user.")
    @GetMapping
    public List<MemberLoansResponse> list() {
        return service.listLoansForCurrentUser();
    }
}
