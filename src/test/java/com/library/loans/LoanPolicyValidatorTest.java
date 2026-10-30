package com.library.loans;

import com.library.common.exception.LoanRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanPolicyValidatorTest {

    private static final int MAX_ACTIVE_LOANS = 3;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @Mock
    private LoanRepository loanRepository;

    private LoanPolicyValidator validator;

    @BeforeEach
    void setUp() {
        LendingProperties properties = new LendingProperties(MAX_ACTIVE_LOANS, 14, "UTC");
        validator = new LoanPolicyValidator(properties, FIXED_CLOCK, loanRepository);
    }

    @Test
    void allowsBorrowWhenActiveLoansAreOneBelowLimit() {
        UUID member = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn((long) (MAX_ACTIVE_LOANS - 1));
        when(loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member, TODAY)).thenReturn(false);

        validator.validate(member); // no exception
    }

    @Test
    void rejectsBorrowWhenActiveLoansReachLimit() {
        UUID member = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn((long) MAX_ACTIVE_LOANS);

        assertThatThrownBy(() -> validator.validate(member))
                .isInstanceOf(LoanRejectedException.class)
                .extracting("reason").isEqualTo("max_loans_exceeded");
    }

    @Test
    void allowsBorrowWhenNoOverdueLoan() {
        UUID member = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member, TODAY)).thenReturn(false);

        validator.validate(member); // no exception
    }

    @Test
    void rejectsBorrowWhenOverdueByOneDay() {
        UUID member = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member, TODAY)).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(member))
                .isInstanceOf(LoanRejectedException.class)
                .extracting("reason").isEqualTo("overdue_loan_exists");
    }

    @Test
    void doesNotTreatDueTodayAsOverdue() {
        UUID member = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member, TODAY)).thenReturn(false);

        validator.validate(member); // no exception
    }

    @Test
    void rejectsBorrowWhenMemberAlreadyHasActiveLoanForSameBook() {
        UUID member = UUID.randomUUID();
        UUID book = UUID.randomUUID();
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(member)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member, TODAY)).thenReturn(false);
        when(loanRepository.findByMemberIdAndBookIdAndReturnedAtIsNull(member, book))
                .thenReturn(java.util.Optional.of(new Loan(book, member,
                        java.time.Instant.parse("2026-06-01T00:00:00Z"), TODAY.plusDays(14))));

        assertThatThrownBy(() -> validator.validate(member, book))
                .isInstanceOf(LoanRejectedException.class)
                .extracting("reason").isEqualTo("book_already_borrowed");
    }
}
