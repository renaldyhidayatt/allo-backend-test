package com.renaldyhidayatt.split_bill_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record RecordPaymentRequest(
            @NotNull Long fromParticipantId,
            @NotNull Long toParticipantId,
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record PaymentResponse(
            Long id,
            Long fromParticipantId,
            String fromName,
            Long toParticipantId,
            String toName,
            BigDecimal amount,
            Instant createdAt
    ) {
    }
}
