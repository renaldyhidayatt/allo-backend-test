package com.renaldyhidayatt.split_bill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.renaldyhidayatt.split_bill_api.dto.SettlementDtos.SettlementTransaction;
import com.renaldyhidayatt.split_bill_api.model.Expense;
import com.renaldyhidayatt.split_bill_api.model.ExpenseShare;
import com.renaldyhidayatt.split_bill_api.model.Participant;
import com.renaldyhidayatt.split_bill_api.model.Payment;

/**
 * Pure settlement math: no Spring, no persistence, just domain objects in and
 * numbers/DTOs out. Kept separate from {@link SettlementService} so the logic
 * that actually matters for correctness can be unit tested without a database
 * or application context.
 */
final class SettlementCalculator {

    private SettlementCalculator() {
    }

    /**
     * Net balance per participant: positive means the group owes them money
     * (they're a creditor), negative means they owe the group (a debtor).
     * Computed as (amount they paid for expenses) - (amount they were charged
     * across expense shares) + (payments they made) - (payments they received).
     */
    static Map<Long, BigDecimal> computeNetBalances(
            List<Participant> participants, List<Expense> expenses, List<Payment> payments) {

        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (Participant participant : participants) {
            balances.put(participant.getId(), BigDecimal.ZERO);
        }

        for (Expense expense : expenses) {
            balances.merge(expense.getPaidBy().getId(), expense.getAmount(), BigDecimal::add);
            for (ExpenseShare share : expense.getShares()) {
                balances.merge(share.getParticipant().getId(), share.getAmountOwed().negate(), BigDecimal::add);
            }
        }

        for (Payment payment : payments) {
            // Paying down a debt moves the payer's balance toward zero (less negative);
            // receiving a payment moves the receiver's balance toward zero (less positive).
            balances.merge(payment.getFrom().getId(), payment.getAmount(), BigDecimal::add);
            balances.merge(payment.getTo().getId(), payment.getAmount().negate(), BigDecimal::add);
        }

        balances.replaceAll((id, amount) -> amount.setScale(2, RoundingMode.HALF_UP));
        return balances;
    }

    /**
     * Greedily matches the largest creditor against the largest debtor, repeatedly,
     * until every balance is settled. This is not guaranteed to produce the
     * mathematically-minimal number of transactions in every case (that variant of
     * the problem is NP-hard in general), but it's a well-known, fast approximation
     * that in practice gets very close, and is easy to reason about and test.
     */
    static List<SettlementTransaction> optimizeTransactions(
            Map<Long, BigDecimal> balances, Map<Long, String> namesById) {

        List<Ledger> creditors = new ArrayList<>();
        List<Ledger> debtors = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            int cmp = entry.getValue().compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                creditors.add(new Ledger(entry.getKey(), entry.getValue()));
            } else if (cmp < 0) {
                debtors.add(new Ledger(entry.getKey(), entry.getValue().abs()));
            }
        }

        PriorityQueue<Ledger> creditorQueue = new PriorityQueue<>(Comparator.comparing(Ledger::amount).reversed());
        PriorityQueue<Ledger> debtorQueue = new PriorityQueue<>(Comparator.comparing(Ledger::amount).reversed());
        creditorQueue.addAll(creditors);
        debtorQueue.addAll(debtors);

        List<SettlementTransaction> transactions = new ArrayList<>();
        while (!creditorQueue.isEmpty() && !debtorQueue.isEmpty()) {
            Ledger creditor = creditorQueue.poll();
            Ledger debtor = debtorQueue.poll();

            BigDecimal settleAmount = creditor.amount().min(debtor.amount());
            transactions.add(new SettlementTransaction(
                    debtor.participantId(), namesById.get(debtor.participantId()),
                    creditor.participantId(), namesById.get(creditor.participantId()),
                    settleAmount));

            BigDecimal remainingCredit = creditor.amount().subtract(settleAmount);
            BigDecimal remainingDebt = debtor.amount().subtract(settleAmount);

            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                creditorQueue.add(new Ledger(creditor.participantId(), remainingCredit));
            }
            if (remainingDebt.compareTo(BigDecimal.ZERO) > 0) {
                debtorQueue.add(new Ledger(debtor.participantId(), remainingDebt));
            }
        }
        return transactions;
    }

    private record Ledger(Long participantId, BigDecimal amount) {
    }
}
