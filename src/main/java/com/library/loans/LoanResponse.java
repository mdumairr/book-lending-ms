package com.library.loans;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanResponse(
        UUID loanId,
        UUID bookId,
        UUID memberId,
        Instant borrowedAt,
        LocalDate dueDate,
        Instant returnedAt
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getBookId(), loan.getMemberId(),
                loan.getBorrowedAt(), loan.getDueDate(), loan.getReturnedAt());
    }
}
