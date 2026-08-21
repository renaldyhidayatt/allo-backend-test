package com.renaldyhidayatt.split_bill_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.renaldyhidayatt.split_bill_api.model.ExpenseCategory;
import com.renaldyhidayatt.split_bill_api.model.SplitType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    /**
     * @param participantIds who the expense is split between when splitType == EQUAL.
     *                       Ignored for PERCENTAGE/EXACT (use {@code shares} instead).
     *                       If null/empty for EQUAL, defaults to every participant in the group.
     * @param shares         per-participant percentage or exact amount, required for
     *                       PERCENTAGE and EXACT split types. Ignored for EQUAL.
     */
    public record AddExpenseRequest(
            @NotBlank(message = "description must not be blank") String description,
            @NotNull @Positive(message = "amount must be positive") BigDecimal amount,
            @NotNull(message = "paidByParticipantId is required") Long paidByParticipantId,
            ExpenseCategory category,
            @NotNull(message = "splitType is required") SplitType splitType,
            List<Long> participantIds,
            @Valid List<ShareInput> shares
    ) {
    }

    /**
     * @param value for PERCENTAGE this is a percentage (0-100), for EXACT this is a currency amount.
     */
    public record ShareInput(
            @NotNull Long participantId,
            @NotNull @Positive BigDecimal value
    ) {
    }

    public record ExpenseShareResponse(Long participantId, String participantName, BigDecimal amountOwed) {
    }

    public record ExpenseResponse(
            Long id,
            String description,
            BigDecimal amount,
            Long paidByParticipantId,
            String paidByName,
            ExpenseCategory category,
            SplitType splitType,
            Instant createdAt,
            List<ExpenseShareResponse> shares
    ) {
    }

    public record CategorySummaryResponse(ExpenseCategory category, BigDecimal totalAmount) {
    }
}