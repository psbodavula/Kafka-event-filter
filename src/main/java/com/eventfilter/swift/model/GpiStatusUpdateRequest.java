package com.eventfilter.swift.model;

import com.eventfilter.model.upo.UniversalPaymentObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body for SWIFT GPI Tracker Status Update API.
 * PUT /swift-apitracker/v5/payments/{uetr}/status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpiStatusUpdateRequest {

    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("originator")
    private String originator;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("transaction_status_reason")
    private TransactionStatusReason transactionStatusReason;

    @JsonProperty("tracker_informing_party")
    private String trackerInformingParty;

    @JsonProperty("instruction_identification")
    private String instructionIdentification;

    @JsonProperty("interbank_settlement_amount")
    private BigDecimal interbankSettlementAmount;

    @JsonProperty("interbank_settlement_date")
    private String interbankSettlementDate;

    @JsonProperty("interbank_settlement_currency")
    private String interbankSettlementCurrency;

    @JsonProperty("instructed_amount")
    private InstructedAmount instructedAmount;

    @JsonProperty("charge_amount")
    private ChargeAmount chargeAmount;

    @JsonProperty("charge_type")
    private String chargeType;

    @JsonProperty("last_update_time")
    private Instant lastUpdateTime;

    /**
     * Build a GPI status update request directly from a UPO — no manual mapping needed.
     */
    public static GpiStatusUpdateRequest fromUpo(UniversalPaymentObject upo, String institutionBic) {
        GpiStatusUpdateRequestBuilder builder = GpiStatusUpdateRequest.builder()
                .from(upo.getDebtorAgentBic())
                .to(upo.getCreditorAgentBic())
                .originator(upo.getDebtorName())
                .transactionStatus(upo.getTransactionStatus() != null ? upo.getTransactionStatus() : "ACSP")
                .trackerInformingParty(institutionBic)
                .instructionIdentification(upo.getTransactionReference())
                .lastUpdateTime(Instant.now());

        if (upo.getInterbankSettlementAmount() != null) {
            builder.interbankSettlementAmount(upo.getInterbankSettlementAmount())
                   .interbankSettlementCurrency(upo.getInterbankSettlementCurrency());
        }

        if (upo.getValueDate() != null) {
            builder.interbankSettlementDate(upo.getValueDate().toString());
        }

        if (upo.getInstructedAmount() != null) {
            builder.instructedAmount(InstructedAmount.builder()
                    .amount(upo.getInstructedAmount())
                    .currency(upo.getInstructedCurrency())
                    .build());
        }

        if (upo.getChargesAmount() != null) {
            builder.chargeAmount(ChargeAmount.builder()
                    .amount(upo.getChargesAmount())
                    .currency(upo.getChargesCurrency())
                    .build())
                   .chargeType(upo.getChargeBearer());
        }

        if (upo.getStatusReasonCode() != null) {
            builder.transactionStatusReason(TransactionStatusReason.builder()
                    .reasonCode(upo.getStatusReasonCode())
                    .build());
        }

        return builder.build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionStatusReason {
        @JsonProperty("reason_code")
        private String reasonCode;

        @JsonProperty("additional_information")
        private String additionalInformation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructedAmount {
        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("currency")
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeAmount {
        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("currency")
        private String currency;
    }
}
