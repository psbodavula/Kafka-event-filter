package com.eventfilter.model.upo;

/**
 * SWIFT GPI transaction status codes (ISO 20022 based).
 */
public enum TransactionStatus {

    /** Accepted and credits posted — final. */
    ACCC("AcceptedSettlementCompleted"),

    /** Accepted and settlement in progress. */
    ACSP("AcceptedSettlementInProcess"),

    /** Accepted and settlement completed. */
    ACSC("AcceptedSettlementCompletedCreditorAccount"),

    /** Pending — awaiting further processing. */
    PDNG("Pending"),

    /** Rejected. */
    RJCT("Rejected"),

    /** Returned. */
    RTRN("Returned"),

    /** Accepted — technical validation successful. */
    ACTC("AcceptedTechnicalValidation");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
