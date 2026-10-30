package com.library.books;

import java.util.UUID;

public record BookResponse(
        UUID bookId,
        String title,
        String author,
        String isbn,
        int totalCopies,
        int availableCopies
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getTotalCopies(), book.getAvailableCopies());
    }
}
