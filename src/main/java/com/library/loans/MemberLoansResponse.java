package com.library.loans;

import com.library.members.MemberResponse;

import java.util.List;
import java.util.UUID;

public record MemberLoansResponse(
        UUID memberId,
        String memberName,
        String memberEmail,
        List<LoanResponse> loans
) {
}
