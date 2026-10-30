package com.library.loans;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.lending")
public record LendingProperties(
        @Min(1) int maxActiveLoans,
        @Min(1) int loanDurationDays,
        @NotBlank String zoneId
) {
}
