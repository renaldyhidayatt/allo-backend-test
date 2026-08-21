package com.renaldyhidayatt.split_bill_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaldyhidayatt.split_bill_api.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByBillGroupId(Long billGroupId);
}
