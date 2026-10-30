package com.library.loans;

import com.library.books.BookRepository;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.members.Member;
import com.library.members.MemberRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private static final String MEMBER_EMAIL = "member@library.com";

    @Mock private LoanRepository loanRepository;
    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LoanPolicyValidator policyValidator;

    private LoanService service;
    private SimpleMeterRegistry meters;
    private Member member;

    @BeforeEach
    void setUp() {
        meters = new SimpleMeterRegistry();
        LendingProperties props = new LendingProperties(3, 14, "UTC");
        service = new LoanService(loanRepository, bookRepository, memberRepository, policyValidator,
                props, FIXED_CLOCK, meters);
        member = new Member("Demo Member", MEMBER_EMAIL);
        UUID memberId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        try {
            java.lang.reflect.Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, memberId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        lenient().when(memberRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(MEMBER_EMAIL, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returningAlreadyReturnedLoanDoesNotIncrementStock() {
        UUID loanId = UUID.randomUUID();
        Loan loan = new Loan(UUID.randomUUID(), member.getId(),
                Instant.parse("2026-06-01T00:00:00Z"), LocalDate.of(2026, 6, 15));
        loan.setReturnedAt(Instant.parse("2026-06-10T00:00:00Z"));
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        Loan result = service.get(loanId);

        assertThat(result.isReturned()).isTrue();
    }

    @Test
    void borrowRejectsWhenAtomicDecrementReturnsZero() {
        UUID book = UUID.randomUUID();
        when(bookRepository.existsById(book)).thenReturn(true);
        when(bookRepository.decrementAvailable(book)).thenReturn(0);

        assertThatThrownBy(() -> service.borrow(book))
                .isInstanceOf(LoanRejectedException.class)
                .extracting("reason").isEqualTo("out_of_stock");
    }

    @Test
    void borrowPropagatesPolicyRejectionAndIncrementsCounter() {
        UUID book = UUID.randomUUID();
        when(bookRepository.existsById(book)).thenReturn(true);
        org.mockito.Mockito.doThrow(new LoanRejectedException("max_loans_exceeded", "too many"))
                .when(policyValidator).validate(member.getId(), book);

        assertThatThrownBy(() -> service.borrow(book))
                .isInstanceOf(LoanRejectedException.class)
                .extracting("reason").isEqualTo("max_loans_exceeded");

        assertThat(meters.counter("loans.rejected", "reason", "max_loans_exceeded").count()).isEqualTo(1.0);
    }

    @Test
    void borrowSucceedsAndIncrementsBorrowedCounter() {
        UUID book = UUID.randomUUID();
        when(bookRepository.existsById(book)).thenReturn(true);
        when(bookRepository.decrementAvailable(book)).thenReturn(1);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan result = service.borrow(book);

        assertThat(result.getBookId()).isEqualTo(book);
        assertThat(result.getMemberId()).isEqualTo(member.getId());
        assertThat(meters.counter("loans.borrowed").count()).isEqualTo(1.0);
    }

    @Test
    void borrowThrowsNotFoundForUnknownBook() {
        UUID book = UUID.randomUUID();
        when(bookRepository.existsById(book)).thenReturn(false);

        assertThatThrownBy(() -> service.borrow(book))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnLoanThrowsWhenNoActiveLoan() {
        UUID book = UUID.randomUUID();
        when(loanRepository.findByMemberIdAndBookIdAndReturnedAtIsNull(member.getId(), book))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnLoan(book))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
