package com.library.books;

public record BookPatchRequest(
        String title,
        String author,
        String isbn,
        Integer totalCopies
) {
}
