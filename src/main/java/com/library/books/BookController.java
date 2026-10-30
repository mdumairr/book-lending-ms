package com.library.books;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
class BookController {

    private final BookService service;

    BookController(BookService service) {
        this.service = service;
    }

    @Operation(summary = "Create a book",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: creates a new book in the catalog.\n\n" +
                    "Business logic:\n" +
                    "- ISBN must be unique; duplicate ISBN returns 409.\n" +
                    "- `availableCopies` is initialized to `totalCopies`.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return BookResponse.from(service.create(request));
    }

    @Operation(summary = "Get a book by id",
            description = "Who can access: Librarian or Member.\n\n" +
                    "What it does:\n" +
                    "- Librarian/Member: returns the book details including copy counts.")
    @GetMapping("/{bookId}")
    public BookResponse get(@PathVariable UUID bookId) {
        return BookResponse.from(service.get(bookId));
    }

    @Operation(summary = "List all books",
            description = "Who can access: Librarian or Member.\n\n" +
                    "What it does:\n" +
                    "- Librarian/Member: returns the full catalog.")
    @GetMapping
    public List<BookResponse> list() {
        return service.list().stream().map(BookResponse::from).toList();
    }

    @Operation(summary = "Update a book (full)",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: replaces all mutable fields of a book.\n\n" +
                    "Business logic:\n" +
                    "- ISBN uniqueness is re-validated.")
    @PutMapping("/{bookId}")
    public BookResponse update(@PathVariable UUID bookId, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(service.update(bookId, request));
    }

    @Operation(summary = "Patch a book (partial)",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: updates only the fields supplied in the payload.\n\n" +
                    "Business logic:\n" +
                    "- Only non-null fields are applied; others stay untouched.\n" +
                    "- `totalCopies` cannot be reduced below `availableCopies`.\n" +
                    "- ISBN uniqueness is re-validated when supplied.")
    @PatchMapping("/{bookId}")
    public BookResponse patch(@PathVariable UUID bookId, @Valid @RequestBody BookPatchRequest request) {
        return BookResponse.from(service.patch(bookId, request));
    }

    @Operation(summary = "Delete a book",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: deletes a book from the catalog.\n\n" +
                    "Business logic:\n" +
                    "- Deletion is blocked (409) when not all copies are available " +
                    "(i.e. some copies are on loan).\n" +
                    "- Associated loan history is removed before deletion.")
    @DeleteMapping("/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID bookId) {
        service.delete(bookId);
    }
}
