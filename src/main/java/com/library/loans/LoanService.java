package com.library.loans;

import com.library.books.BookRepository;
import com.library.common.exception.LoanRejectedException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.members.Member;
import com.library.members.MemberRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LoanPolicyValidator policyValidator;
    private final LendingProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    LoanService(LoanRepository loanRepository, BookRepository bookRepository, MemberRepository memberRepository,
                LoanPolicyValidator policyValidator, LendingProperties properties, Clock clock,
                MeterRegistry meterRegistry) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.policyValidator = policyValidator;
        this.properties = properties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    private Member currentMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No member profile for user: " + email));
    }

    @Transactional
    public Loan borrow(UUID bookId) {
        Member member = currentMember();

        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found: " + bookId);
        }

        try {
            policyValidator.validate(member.getId(), bookId);
        } catch (LoanRejectedException ex) {
            log.warn("Loan rejected for member={} book={} reason={}", member.getId(), bookId, ex.getReason());
            meterRegistry.counter("loans.rejected", "reason", ex.getReason()).increment();
            throw ex;
        }

        int updated = bookRepository.decrementAvailable(bookId);
        if (updated == 0) {
            log.warn("Loan rejected for member={} book={} reason=out_of_stock", member.getId(), bookId);
            meterRegistry.counter("loans.rejected", "reason", "out_of_stock").increment();
            throw new LoanRejectedException("out_of_stock", "No available copies for book " + bookId);
        }

        Loan loan = new Loan(bookId, member.getId(), clock.instant(),
                LocalDate.now(clock).plusDays(properties.loanDurationDays()));
        Loan saved = loanRepository.save(loan);
        meterRegistry.counter("loans.borrowed").increment();
        log.info("Book borrowed loanId={} bookId={} memberId={} dueDate={}", saved.getId(), bookId, member.getId(), saved.getDueDate());
        return saved;
    }

    @Transactional
    public Loan returnLoan(UUID bookId) {
        Member member = currentMember();
        Loan loan = loanRepository.findByMemberIdAndBookIdAndReturnedAtIsNull(member.getId(), bookId)
                .orElseGet(() -> {
                    List<Loan> recent = loanRepository.findByMemberIdAndBookIdOrderByBorrowedAtDesc(member.getId(), bookId);
                    if (!recent.isEmpty() && recent.getFirst().isReturned()) {
                        return recent.getFirst(); // idempotent: already returned
                    }
                    throw new ResourceNotFoundException(
                            "No loan found for book " + bookId + " by member " + member.getId());
                });

        if (!loan.isReturned()) {
            loan.setReturnedAt(clock.instant());
            loanRepository.save(loan);
            bookRepository.incrementAvailable(bookId);
            meterRegistry.counter("loans.returned").increment();
            log.info("Book returned loanId={} bookId={} memberId={}", loan.getId(), bookId, member.getId());
        } else {
            log.debug("Idempotent return ignored loanId={} bookId={} memberId={}", loan.getId(), bookId, member.getId());
        }
        return loan;
    }

    @Transactional(readOnly = true)
    public Loan get(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Loan> listByMember(UUID memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<MemberLoansResponse> listLoansForCurrentUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isLibrarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        if (isLibrarian) {
            return listAllGroupedByMember();
        }
        Member member = currentMember();
        return List.of(new MemberLoansResponse(member.getId(), member.getName(), member.getEmail(),
                listByMember(member.getId()).stream().map(LoanResponse::from).toList()));
    }

    @Transactional(readOnly = true)
    public List<MemberLoansResponse> listAllGroupedByMember() {
        // group all loans by member
        java.util.Map<UUID, List<Loan>> grouped = new java.util.HashMap<>();
        for (Loan loan : loanRepository.findAll()) {
            grouped.computeIfAbsent(loan.getMemberId(), k -> new java.util.ArrayList<>()).add(loan);
        }
        List<MemberLoansResponse> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<UUID, List<Loan>> e : grouped.entrySet()) {
            Member member = memberRepository.findById(e.getKey()).orElse(null);
            String name = member != null ? member.getName() : "Unknown";
            String email = member != null ? member.getEmail() : "";
            result.add(new MemberLoansResponse(e.getKey(), name, email,
                    e.getValue().stream().map(LoanResponse::from).toList()));
        }
        return result;
    }
}
