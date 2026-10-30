package com.library.books;

import com.library.common.exception.DuplicateResourceException;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.loans.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository repository;
    @Mock private LoanRepository loanRepository;

    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(repository, loanRepository);
    }

    @Test
    void createThrowsOnDuplicateIsbn() {
        BookRequest req = new BookRequest("T", "A", "111", 2);
        when(repository.existsByIsbn("111")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createSavesBookWhenIsbnUnique() {
        BookRequest req = new BookRequest("T", "A", "111", 2);
        when(repository.existsByIsbn("111")).thenReturn(false);
        Book saved = new Book("T", "A", "111", 2);
        when(repository.save(any(Book.class))).thenReturn(saved);
        Book result = service.create(req);
        assertThat(result.getTitle()).isEqualTo("T");
        verify(repository).save(any(Book.class));
    }

    @Test
    void getThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesAllFields() {
        UUID id = UUID.randomUUID();
        Book book = new Book("Old", "OldA", "111", 2);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        when(repository.existsByIsbn("222")).thenReturn(false);
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        BookRequest req = new BookRequest("New", "NewA", "222", 5);
        Book result = service.update(id, req);
        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getAuthor()).isEqualTo("NewA");
        assertThat(result.getIsbn()).isEqualTo("222");
        assertThat(result.getTotalCopies()).isEqualTo(5);
    }

    @Test
    void updateThrowsOnDuplicateIsbn() {
        UUID id = UUID.randomUUID();
        Book book = new Book("Old", "OldA", "111", 2);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        when(repository.existsByIsbn("222")).thenReturn(true);
        BookRequest req = new BookRequest("New", "NewA", "222", 5);
        assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void patchUpdatesOnlySuppliedFields() {
        UUID id = UUID.randomUUID();
        Book book = new Book("Old", "OldA", "111", 3);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        BookPatchRequest req = new BookPatchRequest("NewTitle", null, null, null);
        Book result = service.patch(id, req);
        assertThat(result.getTitle()).isEqualTo("NewTitle");
        assertThat(result.getAuthor()).isEqualTo("OldA");
        assertThat(result.getIsbn()).isEqualTo("111");
        assertThat(result.getTotalCopies()).isEqualTo(3);
    }

    @Test
    void patchRejectsTotalCopiesBelowAvailable() {
        UUID id = UUID.randomUUID();
        Book book = new Book("T", "A", "111", 3);
        // simulate 1 copy on loan
        book.setAvailableCopies(2);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        BookPatchRequest req = new BookPatchRequest(null, null, null, 1);
        assertThatThrownBy(() -> service.patch(id, req)).isInstanceOf(LoanRejectedException.class);
    }

    @Test
    void patchRejectsDuplicateIsbn() {
        UUID id = UUID.randomUUID();
        Book book = new Book("T", "A", "111", 3);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        when(repository.existsByIsbn("222")).thenReturn(true);
        BookPatchRequest req = new BookPatchRequest(null, null, "222", null);
        assertThatThrownBy(() -> service.patch(id, req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteThrowsWhenCopiesOnLoan() {
        UUID id = UUID.randomUUID();
        Book book = new Book("T", "A", "111", 3);
        book.setAvailableCopies(2);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(LoanRejectedException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenAllCopiesAvailable() {
        UUID id = UUID.randomUUID();
        Book book = new Book("T", "A", "111", 3);
        when(repository.findById(id)).thenReturn(Optional.of(book));
        service.delete(id);
        verify(loanRepository).deleteByBookId(id);
        verify(repository).delete(book);
    }

    @Test
    void listReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(new Book("T", "A", "1", 1)));
        assertThat(service.list()).hasSize(1);
    }
}
