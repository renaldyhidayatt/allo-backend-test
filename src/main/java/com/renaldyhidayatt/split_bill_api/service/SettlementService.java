package com.renaldyhidayatt.split_bill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaldyhidayatt.split_bill_api.dto.SettlementDtos.BalanceEntry;
import com.renaldyhidayatt.split_bill_api.dto.SettlementDtos.SettlementResponse;
import com.renaldyhidayatt.split_bill_api.dto.SettlementDtos.SettlementTransaction;
import com.renaldyhidayatt.split_bill_api.model.BillGroup;
import com.renaldyhidayatt.split_bill_api.model.Expense;
import com.renaldyhidayatt.split_bill_api.model.Participant;
import com.renaldyhidayatt.split_bill_api.model.Payment;
import com.renaldyhidayatt.split_bill_api.repository.ExpenseRepository;
import com.renaldyhidayatt.split_bill_api.repository.PaymentRepository;

@Service
public class SettlementService {

    private final BillGroupService billGroupService;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceChargeCalculator serviceChargeCalculator;

    public SettlementService(BillGroupService billGroupService,
                              ExpenseRepository expenseRepository,
                              PaymentRepository paymentRepository,
                              ServiceChargeCalculator serviceChargeCalculator) {
        this.billGroupService = billGroupService;
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.serviceChargeCalculator = serviceChargeCalculator;
    }

    @Transactional(readOnly = true)
    public SettlementResponse computeSettlement(Long groupId) {
        BillGroup group = billGroupService.findGroupOrThrow(groupId);
        List<Participant> participants = group.getParticipants();
        List<Expense> expenses = expenseRepository.findByBillGroupId(groupId);
        List<Payment> payments = paymentRepository.findByBillGroupId(groupId);

        Map<Long, String> namesById = participants.stream()
                .collect(Collectors.toMap(Participant::getId, Participant::getName));

        Map<Long, BigDecimal> balances = SettlementCalculator.computeNetBalances(participants, expenses, payments);
        List<SettlementTransaction> transactions = SettlementCalculator.optimizeTransactions(balances, namesById);

        List<BalanceEntry> balanceEntries = participants.stream()
                .map(p -> new BalanceEntry(p.getId(), p.getName(), balances.get(p.getId())))
                .toList();

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int serviceChargePct = serviceChargeCalculator.getServicePct();
        BigDecimal serviceChargeAmount = serviceChargeCalculator.applyTo(totalExpenses);

        return new SettlementResponse(
                group.getId(),
                group.getName(),
                totalExpenses,
                serviceChargePct,
                serviceChargeAmount,
                balanceEntries,
                transactions
        );
    }
}
