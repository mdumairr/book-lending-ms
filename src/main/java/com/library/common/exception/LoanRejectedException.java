package com.library.common.exception;

public class LoanRejectedException extends RuntimeException {
    private final String reason;

    public LoanRejectedException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
