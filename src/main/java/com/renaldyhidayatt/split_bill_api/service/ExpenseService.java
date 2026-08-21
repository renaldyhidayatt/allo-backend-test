package com.renaldyhidayatt.split_bill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.AddExpenseRequest;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.CategorySummaryResponse;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.ExpenseResponse;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.ExpenseShareResponse;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.ShareInput;
import com.renaldyhidayatt.split_bill_api.exception.InvalidRequestException;
import com.renaldyhidayatt.split_bill_api.model.BillGroup;
import com.renaldyhidayatt.split_bill_api.model.Expense;
import com.renaldyhidayatt.split_bill_api.model.ExpenseCategory;
import com.renaldyhidayatt.split_bill_api.model.ExpenseShare;
import com.renaldyhidayatt.split_bill_api.model.Participant;
import com.renaldyhidayatt.split_bill_api.repository.ExpenseRepository;
import com.renaldyhidayatt.split_bill_api.util.MoneySplitter;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BillGroupService billGroupService;

    public ExpenseService(ExpenseRepository expenseRepository, BillGroupService billGroupService) {
        this.expenseRepository = expenseRepository;
        this.billGroupService = billGroupService;
    }

    @Transactional
    public ExpenseResponse addExpense(Long groupId, AddExpenseRequest request) {
        BillGroup group = billGroupService.findGroupOrThrow(groupId);
        Participant paidBy = requireParticipantInGroup(group, request.paidByParticipantId());

        List<Participant> targetParticipants;
        List<BigDecimal> amounts;

        switch (request.splitType()) {
            case EQUAL -> {
                List<Long> ids = (request.participantIds() == null || request.participantIds().isEmpty())
                        ? group.getParticipants().stream().map(Participant::getId).toList()
                        : request.participantIds();
                targetParticipants = resolveParticipants(group, ids);
                amounts = MoneySplitter.splitEqually(request.amount(), targetParticipants.size());
            }
            case PERCENTAGE -> {
                requireShares(request);
                targetParticipants = resolveParticipants(group, shareParticipantIds(request));
                amounts = MoneySplitter.splitByPercentage(request.amount(), shareValues(request));
            }
            case EXACT -> {
                requireShares(request);
                targetParticipants = resolveParticipants(group, shareParticipantIds(request));
                amounts = MoneySplitter.splitExact(request.amount(), shareValues(request));
            }
            default -> throw new InvalidRequestException("Unsupported split type: " + request.splitType());
        }

        Expense expense = new Expense();
        expense.setBillGroup(group);
        expense.setDescription(request.description());
        expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setPaidBy(paidBy);
        expense.setCategory(request.category() != null ? request.category() : ExpenseCategory.OTHER);
        expense.setSplitType(request.splitType());

        for (int i = 0; i < targetParticipants.size(); i++) {
            expense.addShare(new ExpenseShare(expense, targetParticipants.get(i), amounts.get(i)));
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(Long groupId) {
        billGroupService.findGroupOrThrow(groupId);
        return expenseRepository.findByBillGroupId(groupId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategorySummary(Long groupId) {
        billGroupService.findGroupOrThrow(groupId);
        Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
        for (Expense expense : expenseRepository.findByBillGroupId(groupId)) {
            totals.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> new CategorySummaryResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(r -> r.category().name()))
                .toList();
    }

    private List<Long> shareParticipantIds(AddExpenseRequest request) {
        return request.shares().stream().map(ShareInput::participantId).toList();
    }

    private List<BigDecimal> shareValues(AddExpenseRequest request) {
        return request.shares().stream().map(ShareInput::value).toList();
    }

    private void requireShares(AddExpenseRequest request) {
        if (request.shares() == null || request.shares().isEmpty()) {
            throw new InvalidRequestException(request.splitType() + " split requires a non-empty 'shares' list");
        }
    }

    private Participant requireParticipantInGroup(BillGroup group, Long participantId) {
        return group.getParticipants().stream()
                .filter(p -> p.getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Participant " + participantId + " is not part of group " + group.getId()));
    }

    private List<Participant> resolveParticipants(BillGroup group, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new InvalidRequestException("At least one participant is required for this split");
        }
        Map<Long, Participant> byId = group.getParticipants().stream()
                .collect(Collectors.toMap(Participant::getId, p -> p));

        List<Participant> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Participant participant = byId.get(id);
            if (participant == null) {
                throw new InvalidRequestException("Participant " + id + " is not part of group " + group.getId());
            }
            result.add(participant);
        }
        return result;
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ExpenseShareResponse> shares = expense.getShares().stream()
                .map(s -> new ExpenseShareResponse(s.getParticipant().getId(), s.getParticipant().getName(), s.getAmountOwed()))
                .toList();
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getCategory(),
                expense.getSplitType(),
                expense.getCreatedAt(),
                shares
        );
    }
}
