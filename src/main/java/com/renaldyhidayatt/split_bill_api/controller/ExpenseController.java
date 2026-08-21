package com.renaldyhidayatt.split_bill_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.AddExpenseRequest;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.CategorySummaryResponse;
import com.renaldyhidayatt.split_bill_api.dto.ExpenseDtos.ExpenseResponse;
import com.renaldyhidayatt.split_bill_api.service.ExpenseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@PathVariable Long groupId,
                                                        @Valid @RequestBody AddExpenseRequest request) {
        ExpenseResponse response = expenseService.addExpense(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ExpenseResponse> getExpenses(@PathVariable Long groupId) {
        return expenseService.getExpenses(groupId);
    }

    @GetMapping("/summary/by-category")
    public List<CategorySummaryResponse> getCategorySummary(@PathVariable Long groupId) {
        return expenseService.getCategorySummary(groupId);
    }
}

