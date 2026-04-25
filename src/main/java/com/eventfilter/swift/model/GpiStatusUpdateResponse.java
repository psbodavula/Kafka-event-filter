package com.eventfilter.swift.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpiStatusUpdateResponse {

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("confirmation_number")
    private String confirmationNumber;

    private boolean success;
    private String errorMessage;
    private int httpStatusCode;
}
