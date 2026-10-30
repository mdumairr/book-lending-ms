package com.library.members;

public record MemberPatchRequest(
        String name,
        String email
) {
}
