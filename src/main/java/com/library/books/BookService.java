package com.library.books;

import com.library.common.exception.DuplicateResourceException;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.loans.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository repository;
    private final LoanRepository loanRepository;

    BookService(BookRepository repository, LoanRepository loanRepository) {
        this.repository = repository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public Book create(BookRequest request) {
        if (repository.existsByIsbn(request.isbn())) {
            throw new DuplicateResourceException("A book with ISBN " + request.isbn() + " already exists");
        }
        Book saved = repository.save(new Book(request.title(), request.author(), request.isbn(), request.totalCopies()));
        log.info("Book created bookId={} isbn={} title=\"{}\" copies={}", saved.getId(), saved.getIsbn(), saved.getTitle(), saved.getTotalCopies());
        return saved;
    }

    @Transactional(readOnly = true)
    public Book get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Book> list() {
        return repository.findAll();
    }

    @Transactional
    public Book update(UUID id, BookRequest request) {
        Book book = get(id);
        if (!book.getIsbn().equals(request.isbn()) && repository.existsByIsbn(request.isbn())) {
            throw new DuplicateResourceException("A book with ISBN " + request.isbn() + " already exists");
        }
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setTotalCopies(request.totalCopies());
        Book saved = repository.save(book);
        log.info("Book updated bookId={} isbn={} title=\"{}\"", saved.getId(), saved.getIsbn(), saved.getTitle());
        return saved;
    }

    @Transactional
    public Book patch(UUID id, BookPatchRequest request) {
        Book book = get(id);
        if (request.title() != null) {
            if (request.title().isBlank()) throw new IllegalArgumentException("title must not be blank");
            book.setTitle(request.title());
        }
        if (request.author() != null) {
            if (request.author().isBlank()) throw new IllegalArgumentException("author must not be blank");
            book.setAuthor(request.author());
        }
        if (request.isbn() != null) {
            if (request.isbn().isBlank()) throw new IllegalArgumentException("isbn must not be blank");
            if (!book.getIsbn().equals(request.isbn()) && repository.existsByIsbn(request.isbn())) {
                throw new DuplicateResourceException("A book with ISBN " + request.isbn() + " already exists");
            }
            book.setIsbn(request.isbn());
        }
        if (request.totalCopies() != null) {
            if (request.totalCopies() < 1) throw new IllegalArgumentException("totalCopies must be >= 1");
            if (request.totalCopies() < book.getAvailableCopies()) {
                throw new LoanRejectedException("invalid_total_copies",
                        "totalCopies cannot be less than availableCopies (" + book.getAvailableCopies() + ")");
            }
            book.setTotalCopies(request.totalCopies());
        }
        Book saved = repository.save(book);
        log.info("Book patched bookId={}", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Book book = get(id);
        if (book.getAvailableCopies() != book.getTotalCopies()) {
            throw new LoanRejectedException("book_has_active_loans",
                    "Cannot delete book with copies currently on loan: " + id);
        }
        loanRepository.deleteByBookId(id);
        repository.delete(book);
        log.info("Book deleted bookId={} isbn={}", id, book.getIsbn());
    }
}
