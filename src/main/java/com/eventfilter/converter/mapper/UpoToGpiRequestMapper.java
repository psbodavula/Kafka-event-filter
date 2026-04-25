package com.eventfilter.converter.mapper;

import com.eventfilter.model.upo.UniversalPaymentObject;
import com.eventfilter.swift.model.GpiStatusUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;

/**
 * MapStruct auto-generates the implementation at compile time.
 * Only field renames and nested object wrapping need annotations — everything else is automatic.
 */
@Mapper(componentModel = "spring")
public interface UpoToGpiRequestMapper {

    @Mapping(source = "debtorAgentBic", target = "from")
    @Mapping(source = "creditorAgentBic", target = "to")
    @Mapping(source = "debtorName", target = "originator")
    @Mapping(source = "transactionReference", target = "instructionIdentification")
    @Mapping(target = "trackerInformingParty", ignore = true)
    @Mapping(source = "valueDate", target = "interbankSettlementDate")
    @Mapping(source = "chargeBearer", target = "chargeType")
    @Mapping(source = "instructedAmount", target = "instructedAmount", qualifiedByName = "toInstructedAmount")
    @Mapping(source = "chargesAmount", target = "chargeAmount", qualifiedByName = "toChargeAmount")
    @Mapping(source = "statusReasonCode", target = "transactionStatusReason", qualifiedByName = "toStatusReason")
    @Mapping(target = "lastUpdateTime", expression = "java(java.time.Instant.now())")
    GpiStatusUpdateRequest toGpiRequest(UniversalPaymentObject upo);

    @Named("toInstructedAmount")
    default GpiStatusUpdateRequest.InstructedAmount toInstructedAmount(java.math.BigDecimal amount) {
        if (amount == null) return null;
        return GpiStatusUpdateRequest.InstructedAmount.builder()
                .amount(amount)
                .build();
    }

    @Named("toChargeAmount")
    default GpiStatusUpdateRequest.ChargeAmount toChargeAmount(java.math.BigDecimal amount) {
        if (amount == null) return null;
        return GpiStatusUpdateRequest.ChargeAmount.builder()
                .amount(amount)
                .build();
    }

    @Named("toStatusReason")
    default GpiStatusUpdateRequest.TransactionStatusReason toStatusReason(String reasonCode) {
        if (reasonCode == null) return null;
        return GpiStatusUpdateRequest.TransactionStatusReason.builder()
                .reasonCode(reasonCode)
                .build();
    }
}
