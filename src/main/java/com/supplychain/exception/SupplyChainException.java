package com.supplychain.exception;

/**
 * Exception thrown when a supply chain operation fails.
 */
public class SupplyChainException extends RuntimeException {

    private final String errorCode;

    public SupplyChainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SupplyChainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
