package com.library.members;

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
class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository repository;
    private final LoanRepository loanRepository;

    MemberService(MemberRepository repository, LoanRepository loanRepository) {
        this.repository = repository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public Member create(MemberRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A member with email " + request.email() + " already exists");
        }
        Member saved = repository.save(new Member(request.name(), request.email()));
        log.info("Member created memberId={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public Member get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Member> list() {
        return repository.findAll();
    }

    @Transactional
    public Member update(UUID id, MemberRequest request) {
        Member member = get(id);
        if (!member.getEmail().equals(request.email()) && repository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A member with email " + request.email() + " already exists");
        }
        member.setName(request.name());
        member.setEmail(request.email());
        Member saved = repository.save(member);
        log.info("Member updated memberId={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Transactional
    public Member patch(UUID id, MemberPatchRequest request) {
        Member member = get(id);
        if (request.name() != null) {
            if (request.name().isBlank()) throw new IllegalArgumentException("name must not be blank");
            member.setName(request.name());
        }
        if (request.email() != null) {
            if (request.email().isBlank()) throw new IllegalArgumentException("email must not be blank");
            if (!member.getEmail().equals(request.email()) && repository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("A member with email " + request.email() + " already exists");
            }
            member.setEmail(request.email());
        }
        Member saved = repository.save(member);
        log.info("Member patched memberId={}", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Member member = get(id);
        if (loanRepository.countByMemberIdAndReturnedAtIsNull(id) > 0) {
            throw new LoanRejectedException("member_has_active_loans",
                    "Cannot delete member with active loans: " + id);
        }
        loanRepository.deleteByMemberId(id);
        repository.delete(member);
        log.info("Member deleted memberId={} email={}", id, member.getEmail());
    }
}
