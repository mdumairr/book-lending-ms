package com.library.loans;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    long countByMemberIdAndReturnedAtIsNull(UUID memberId);

    boolean existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(UUID memberId, LocalDate date);

    List<Loan> findByMemberId(UUID memberId);

    List<Loan> findByMemberIdAndReturnedAtIsNull(UUID memberId);

    Optional<Loan> findByMemberIdAndBookIdAndReturnedAtIsNull(UUID memberId, UUID bookId);

    List<Loan> findByMemberIdAndBookIdOrderByBorrowedAtDesc(UUID memberId, UUID bookId);

    void deleteByMemberId(UUID memberId);

    void deleteByBookId(UUID bookId);
}
