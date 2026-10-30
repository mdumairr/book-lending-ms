package com.library.loans;

import com.library.common.exception.LoanRejectedException;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Component
class LoanPolicyValidator {

    private final LendingProperties properties;
    private final Clock clock;
    private final LoanRepository loanRepository;

    LoanPolicyValidator(LendingProperties properties, Clock clock, LoanRepository loanRepository) {
        this.properties = properties;
        this.clock = clock;
        this.loanRepository = loanRepository;
    }

    void validate(UUID memberId) {
        validate(memberId, null);
    }

    void validate(UUID memberId, UUID bookId) {
        long active = loanRepository.countByMemberIdAndReturnedAtIsNull(memberId);
        if (active >= properties.maxActiveLoans()) {
            throw new LoanRejectedException("max_loans_exceeded",
                    "Member " + memberId + " already has " + active + " active loans");
        }
        LocalDate today = LocalDate.now(clock);
        if (loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(memberId, today)) {
            throw new LoanRejectedException("overdue_loan_exists",
                    "Member " + memberId + " has an overdue active loan");
        }
        if (bookId != null && loanRepository.findByMemberIdAndBookIdAndReturnedAtIsNull(memberId, bookId).isPresent()) {
            throw new LoanRejectedException("book_already_borrowed",
                    "Member " + memberId + " already has an active loan for book " + bookId);
        }
    }
}
