package com.eventfilter.model.upo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalPaymentObject {

    /**
     * Unique End-to-End Transaction Reference (UUID format).
     * Core identifier for SWIFT GPI tracking.
     */
    private String uetr;

    /**
     * Transaction reference number (e.g., MT103 field 20).
     */
    private String transactionReference;

    /**
     * Sender's reference (field 20 of the original message).
     */
    private String senderReference;

    /**
     * Message type: MT103, MT202, pacs.008, pacs.009, etc.
     */
    private String messageType;

    // --- Financial Details ---

    /**
     * Instructed amount in the original currency.
     */
    private BigDecimal instructedAmount;

    /**
     * Currency of the instructed amount (ISO 4217).
     */
    private String instructedCurrency;

    /**
     * Interbank settlement amount.
     */
    private BigDecimal interbankSettlementAmount;

    /**
     * Currency of the interbank settlement (ISO 4217).
     */
    private String interbankSettlementCurrency;

    /**
     * Exchange rate applied (if currency conversion occurred).
     */
    private BigDecimal exchangeRate;

    // --- Parties ---

    /**
     * Ordering institution BIC (sender bank).
     */
    private String debtorAgentBic;

    /**
     * Ordering institution name.
     */
    private String debtorAgentName;

    /**
     * Debtor (originator) name.
     */
    private String debtorName;

    /**
     * Debtor account (IBAN or account number).
     */
    private String debtorAccount;

    /**
     * Beneficiary institution BIC (receiver bank).
     */
    private String creditorAgentBic;

    /**
     * Beneficiary institution name.
     */
    private String creditorAgentName;

    /**
     * Creditor (beneficiary) name.
     */
    private String creditorName;

    /**
     * Creditor account (IBAN or account number).
     */
    private String creditorAccount;

    // --- Intermediaries ---

    /**
     * Intermediary agent BIC (correspondent bank).
     */
    private String intermediaryAgentBic;

    /**
     * Instructing agent BIC.
     */
    private String instructingAgentBic;

    // --- Dates ---

    /**
     * Value date / settlement date.
     */
    private LocalDate valueDate;

    /**
     * Creation date/time of the payment instruction.
     */
    private Instant creationDateTime;

    // --- Payment Details ---

    /**
     * Charge bearer: SHA, BEN, OUR.
     */
    private String chargeBearer;

    /**
     * Total charges amount.
     */
    private BigDecimal chargesAmount;

    /**
     * Charges currency.
     */
    private String chargesCurrency;

    /**
     * Remittance information / payment details.
     */
    private String remittanceInformation;

    /**
     * Purpose of payment code.
     */
    private String purposeCode;

    // --- GPI Tracking ---

    /**
     * Current transaction status for GPI tracking.
     * Values: ACCC, ACSP, ACSC, RJCT, PDNG, etc.
     */
    private String transactionStatus;

    /**
     * Status reason code (if rejected or returned).
     */
    private String statusReasonCode;

    /**
     * Timestamp of the last status update.
     */
    private Instant lastUpdateTime;

    /**
     * Tracker confirmation number from SWIFT.
     */
    private String confirmationNumber;
}
