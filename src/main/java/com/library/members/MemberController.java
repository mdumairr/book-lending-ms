package com.library.members;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
class MemberController {

    private final MemberService service;

    MemberController(MemberService service) {
        this.service = service;
    }

    @Operation(summary = "Create a member",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: registers a new library member.\n\n" +
                    "Business logic:\n" +
                    "- Email must be unique; duplicate email returns 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(service.create(request));
    }

    @Operation(summary = "Get a member by id",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: returns member details.")
    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable UUID memberId) {
        return MemberResponse.from(service.get(memberId));
    }

    @Operation(summary = "List all members",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: returns all registered members.")
    @GetMapping
    public List<MemberResponse> list() {
        return service.list().stream().map(MemberResponse::from).toList();
    }

    @Operation(summary = "Update a member (full)",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: replaces name and email of a member.\n\n" +
                    "Business logic:\n" +
                    "- Email uniqueness is re-validated.")
    @PutMapping("/{memberId}")
    public MemberResponse update(@PathVariable UUID memberId, @Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(service.update(memberId, request));
    }

    @Operation(summary = "Patch a member (partial)",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: updates only the fields supplied in the payload.\n\n" +
                    "Business logic:\n" +
                    "- Only non-null fields are applied; others stay untouched.\n" +
                    "- Email uniqueness is re-validated when supplied.")
    @PatchMapping("/{memberId}")
    public MemberResponse patch(@PathVariable UUID memberId, @Valid @RequestBody MemberPatchRequest request) {
        return MemberResponse.from(service.patch(memberId, request));
    }

    @Operation(summary = "Delete a member",
            description = "Who can access: Librarian only.\n\n" +
                    "What it does:\n" +
                    "- Librarian: deletes a member.\n\n" +
                    "Business logic:\n" +
                    "- Deletion is blocked (409) when the member has active loans.\n" +
                    "- All loan history for the member is removed before deletion.")
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID memberId) {
        service.delete(memberId);
    }
}
