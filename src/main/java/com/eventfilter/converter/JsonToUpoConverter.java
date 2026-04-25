package com.eventfilter.converter;

import com.eventfilter.model.upo.UniversalPaymentObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@Component
public class JsonToUpoConverter {

    /**
     * Convert a raw JSON event payload (Map) to a UniversalPaymentObject.
     * Supports flat and nested field structures.
     */
    public UniversalPaymentObject convert(Map<String, Object> payload) {
        log.debug("Converting JSON payload to UPO: {}", payload);

        return UniversalPaymentObject.builder()
                // Core identifiers
                .uetr(getString(payload, "uetr"))
                .transactionReference(getString(payload, "transactionReference", "txnRef", "field20"))
                .senderReference(getString(payload, "senderReference", "sndRef"))
                .messageType(getString(payload, "messageType", "msgType"))

                // Financial details
                .instructedAmount(getBigDecimal(payload, "instructedAmount", "amount"))
                .instructedCurrency(getString(payload, "instructedCurrency", "currency", "ccy"))
                .interbankSettlementAmount(getBigDecimal(payload, "interbankSettlementAmount", "settlementAmount"))
                .interbankSettlementCurrency(getString(payload, "interbankSettlementCurrency", "settlementCurrency"))
                .exchangeRate(getBigDecimal(payload, "exchangeRate", "xchgRate"))

                // Debtor (ordering) party
                .debtorAgentBic(getString(payload, "debtorAgentBic", "senderBic", "orderingInstitution"))
                .debtorAgentName(getString(payload, "debtorAgentName", "senderName"))
                .debtorName(getString(payload, "debtorName", "originatorName"))
                .debtorAccount(getString(payload, "debtorAccount", "originatorAccount"))

                // Creditor (beneficiary) party
                .creditorAgentBic(getString(payload, "creditorAgentBic", "receiverBic", "beneficiaryInstitution"))
                .creditorAgentName(getString(payload, "creditorAgentName", "receiverName"))
                .creditorName(getString(payload, "creditorName", "beneficiaryName"))
                .creditorAccount(getString(payload, "creditorAccount", "beneficiaryAccount"))

                // Intermediaries
                .intermediaryAgentBic(getString(payload, "intermediaryAgentBic", "correspondentBic"))
                .instructingAgentBic(getString(payload, "instructingAgentBic"))

                // Dates
                .valueDate(getLocalDate(payload, "valueDate", "valueDt"))
                .creationDateTime(getInstant(payload, "creationDateTime", "createdAt"))

                // Payment details
                .chargeBearer(getString(payload, "chargeBearer", "chargeBrr"))
                .chargesAmount(getBigDecimal(payload, "chargesAmount", "charges"))
                .chargesCurrency(getString(payload, "chargesCurrency"))
                .remittanceInformation(getString(payload, "remittanceInformation", "remittanceInfo", "paymentDetails"))
                .purposeCode(getString(payload, "purposeCode"))

                // GPI tracking
                .transactionStatus(getString(payload, "transactionStatus", "status"))
                .statusReasonCode(getString(payload, "statusReasonCode", "reasonCode"))
                .lastUpdateTime(getInstant(payload, "lastUpdateTime", "updatedAt"))
                .confirmationNumber(getString(payload, "confirmationNumber"))

                .build();
    }

    /**
     * Try multiple field name aliases and return the first non-null string value.
     */
    @SuppressWarnings("unchecked")
    private String getString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            // Support dot notation for nested fields
            if (key.contains(".")) {
                Object value = resolveNestedField(payload, key);
                if (value != null) {
                    return value.toString();
                }
            } else {
                Object value = payload.get(key);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = key.contains(".") ? resolveNestedField(payload, key) : payload.get(key);
            if (value != null) {
                try {
                    return new BigDecimal(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse '{}' as BigDecimal for field '{}'", value, key);
                }
            }
        }
        return null;
    }

    private LocalDate getLocalDate(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = key.contains(".") ? resolveNestedField(payload, key) : payload.get(key);
            if (value != null) {
                try {
                    return LocalDate.parse(value.toString());
                } catch (DateTimeParseException e) {
                    log.warn("Cannot parse '{}' as LocalDate for field '{}'", value, key);
                }
            }
        }
        return null;
    }

    private Instant getInstant(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = key.contains(".") ? resolveNestedField(payload, key) : payload.get(key);
            if (value != null) {
                try {
                    return Instant.parse(value.toString());
                } catch (DateTimeParseException e) {
                    log.warn("Cannot parse '{}' as Instant for field '{}'", value, key);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object resolveNestedField(Map<String, Object> payload, String dottedKey) {
        String[] parts = dottedKey.split("\\.");
        Object current = payload;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
