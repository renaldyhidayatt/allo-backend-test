package com.renaldyhidayatt.split_bill_api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record BalanceEntry(Long participantId, String name, BigDecimal netBalance) {
    }

    /** A single suggested payment that helps settle the group's debts. */
    public record SettlementTransaction(
            Long fromParticipantId, String fromName,
            Long toParticipantId, String toName,
            BigDecimal amount
    ) {
    }

    public record SettlementResponse(
            Long groupId,
            String groupName,
            BigDecimal totalExpenses,
            @JsonProperty("service_charge_pct") int serviceChargePct,
            @JsonProperty("service_charge_amount") BigDecimal serviceChargeAmount,
            List<BalanceEntry> balances,
            List<SettlementTransaction> transactions
    ) {
    }
}
