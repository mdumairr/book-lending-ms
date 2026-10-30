package com.library.members;

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
class MemberServiceTest {

    @Mock private MemberRepository repository;
    @Mock private LoanRepository loanRepository;

    private MemberService service;

    @BeforeEach
    void setUp() {
        service = new MemberService(repository, loanRepository);
    }

    @Test
    void createThrowsOnDuplicateEmail() {
        MemberRequest req = new MemberRequest("N", "e@e.com");
        when(repository.existsByEmail("e@e.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createSavesMemberWhenEmailUnique() {
        MemberRequest req = new MemberRequest("N", "e@e.com");
        when(repository.existsByEmail("e@e.com")).thenReturn(false);
        Member saved = new Member("N", "e@e.com");
        when(repository.save(any(Member.class))).thenReturn(saved);
        Member result = service.create(req);
        assertThat(result.getName()).isEqualTo("N");
    }

    @Test
    void getThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesFields() {
        UUID id = UUID.randomUUID();
        Member member = new Member("Old", "old@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(repository.existsByEmail("new@e.com")).thenReturn(false);
        when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        MemberRequest req = new MemberRequest("New", "new@e.com");
        Member result = service.update(id, req);
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getEmail()).isEqualTo("new@e.com");
    }

    @Test
    void updateThrowsOnDuplicateEmail() {
        UUID id = UUID.randomUUID();
        Member member = new Member("Old", "old@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(repository.existsByEmail("dup@e.com")).thenReturn(true);
        MemberRequest req = new MemberRequest("New", "dup@e.com");
        assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void patchUpdatesOnlySuppliedFields() {
        UUID id = UUID.randomUUID();
        Member member = new Member("Old", "old@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        MemberPatchRequest req = new MemberPatchRequest("NewName", null);
        Member result = service.patch(id, req);
        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getEmail()).isEqualTo("old@e.com");
    }

    @Test
    void patchRejectsDuplicateEmail() {
        UUID id = UUID.randomUUID();
        Member member = new Member("Old", "old@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(repository.existsByEmail("dup@e.com")).thenReturn(true);
        MemberPatchRequest req = new MemberPatchRequest(null, "dup@e.com");
        assertThatThrownBy(() -> service.patch(id, req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteThrowsWhenActiveLoans() {
        UUID id = UUID.randomUUID();
        Member member = new Member("M", "m@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(id)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(LoanRejectedException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteSucceedsWhenNoActiveLoans() {
        UUID id = UUID.randomUUID();
        Member member = new Member("M", "m@e.com");
        when(repository.findById(id)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(id)).thenReturn(0L);
        service.delete(id);
        verify(loanRepository).deleteByMemberId(id);
        verify(repository).delete(member);
    }

    @Test
    void listReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(new Member("M", "m@e.com")));
        assertThat(service.list()).hasSize(1);
    }
}
