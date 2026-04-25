package com.eventfilter.model.upo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniversalPaymentObject {

    private String uetr;

    @JsonAlias({"txnRef", "field20"})
    private String transactionReference;

    @JsonAlias("sndRef")
    private String senderReference;

    @JsonAlias("msgType")
    private String messageType;

    @JsonAlias("amount")
    private BigDecimal instructedAmount;

    @JsonAlias({"currency", "ccy"})
    private String instructedCurrency;

    @JsonAlias("settlementAmount")
    private BigDecimal interbankSettlementAmount;

    @JsonAlias("settlementCurrency")
    private String interbankSettlementCurrency;

    @JsonAlias("xchgRate")
    private BigDecimal exchangeRate;

    @JsonAlias({"senderBic", "orderingInstitution"})
    private String debtorAgentBic;

    @JsonAlias("senderName")
    private String debtorAgentName;

    @JsonAlias("originatorName")
    private String debtorName;

    @JsonAlias("originatorAccount")
    private String debtorAccount;

    @JsonAlias({"receiverBic", "beneficiaryInstitution"})
    private String creditorAgentBic;

    @JsonAlias("receiverName")
    private String creditorAgentName;

    @JsonAlias("beneficiaryName")
    private String creditorName;

    @JsonAlias("beneficiaryAccount")
    private String creditorAccount;

    @JsonAlias("correspondentBic")
    private String intermediaryAgentBic;

    private String instructingAgentBic;

    @JsonAlias("valueDt")
    private LocalDate valueDate;

    @JsonAlias("createdAt")
    private Instant creationDateTime;

    @JsonAlias("chargeBrr")
    private String chargeBearer;

    @JsonAlias("charges")
    private BigDecimal chargesAmount;

    private String chargesCurrency;

    @JsonAlias({"remittanceInfo", "paymentDetails"})
    private String remittanceInformation;

    private String purposeCode;

    @JsonAlias("status")
    private String transactionStatus;

    @JsonAlias("reasonCode")
    private String statusReasonCode;

    @JsonAlias("updatedAt")
    private Instant lastUpdateTime;

    private String confirmationNumber;
}
