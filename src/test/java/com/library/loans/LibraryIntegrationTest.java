package com.library.loans;

import com.library.books.Book;
import com.library.books.BookRepository;
import com.library.members.Member;
import com.library.members.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class LibraryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booklending")
            .withUsername("booklending")
            .withPassword("booklending");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private LoanService loanService;

    private static final String MEMBER_EMAIL = "member@library.com";

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void asMember() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(MEMBER_EMAIL, null));
    }

    @Test
    void endToEndBorrowAndReturnRestoresStock() {
        Book book = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780000000003", 2));
        asMember();

        Loan loan = loanService.borrow(book.getId());
        assertThat(loan.getReturnedAt()).isNull();
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies()).isEqualTo(1);

        loanService.returnLoan(book.getId());
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies()).isEqualTo(2);

        // idempotent second return does not change stock
        loanService.returnLoan(book.getId());
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies()).isEqualTo(2);
    }

    @Test
    void concurrentBorrowOfLastCopyAllowsOnlyOne() throws InterruptedException {
        Book book = bookRepository.save(new Book("Last Copy", "Author", "9780000000002", 1));
        asMember();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        java.util.List<Exception> errors = new java.util.ArrayList<>();

        Runnable task = () -> {
            try {
                start.await();
                // each thread needs its own security context
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(MEMBER_EMAIL, null));
                loanService.borrow(book.getId());
                successes.incrementAndGet();
            } catch (com.library.common.exception.LoanRejectedException e) {
                rejections.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        pool.submit(task);
        pool.submit(task);
        start.countDown();
        pool.shutdown();
        while (!pool.isTerminated()) {
            Thread.sleep(50);
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections.get()).isEqualTo(1);
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableCopies()).isEqualTo(0);
        assertThat(errors).isEmpty();
    }
}
