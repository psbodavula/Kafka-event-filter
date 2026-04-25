package com.eventfilter.swift.model;

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
 *
 * Mapping from UPO is handled by MapStruct (UpoToGpiRequestMapper).
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
