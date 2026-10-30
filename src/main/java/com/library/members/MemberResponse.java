package com.library.members;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID memberId,
        String name,
        String email,
        Instant createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getCreatedAt());
    }
}
