package com.library.books;

import com.library.common.exception.DuplicateResourceException;
import com.library.common.exception.GlobalExceptionHandler;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock BookService bookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BookController(bookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- POST /api/books ---

    @Test
    void createReturns201WithBookResponse() throws Exception {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 3);
        when(bookService.create(any())).thenReturn(book);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Clean Code","author":"Robert C. Martin",
                                 "isbn":"9780132350884","totalCopies":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.totalCopies").value(3));
    }

    @Test
    void createReturns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","author":"A","isbn":"123","totalCopies":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void createReturns400WhenTotalCopiesIsZero() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","author":"A","isbn":"123","totalCopies":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns409OnDuplicateIsbn() throws Exception {
        when(bookService.create(any())).thenThrow(new DuplicateResourceException("ISBN already exists"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","author":"A","isbn":"dup","totalCopies":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate resource"));
    }

    // --- GET /api/books ---

    @Test
    void listReturnsAllBooks() throws Exception {
        when(bookService.list()).thenReturn(List.of(
                new Book("Book A", "Author A", "111", 2),
                new Book("Book B", "Author B", "222", 1)
        ));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getReturns404WhenBookNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(bookService.get(id)).thenThrow(new ResourceNotFoundException("Book not found: " + id));

        mockMvc.perform(get("/api/books/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    // --- DELETE /api/books ---

    @Test
    void deleteReturns204OnSuccess() throws Exception {
        mockMvc.perform(delete("/api/books/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns409WhenCopiesOnLoan() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new LoanRejectedException("copies_on_loan", "Cannot delete: copies are on loan"))
                .when(bookService).delete(id);

        mockMvc.perform(delete("/api/books/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("copies_on_loan"));
    }

    // --- PUT /api/books ---

    @Test
    void updateReturns200WithUpdatedBook() throws Exception {
        UUID id = UUID.randomUUID();
        Book updated = new Book("New Title", "New Author", "999", 5);
        when(bookService.update(eq(id), any())).thenReturn(updated);

        mockMvc.perform(put("/api/books/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"New Title","author":"New Author","isbn":"999","totalCopies":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.totalCopies").value(5));
    }
}
