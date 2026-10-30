package com.library.books;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies - 1 " +
           "WHERE b.id = :id AND b.availableCopies > 0")
    int decrementAvailable(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1 " +
           "WHERE b.id = :id AND b.availableCopies < b.totalCopies")
    int incrementAvailable(@Param("id") UUID id);
}
