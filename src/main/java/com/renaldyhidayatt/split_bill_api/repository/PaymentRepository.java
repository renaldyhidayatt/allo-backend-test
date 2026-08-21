package com.renaldyhidayatt.split_bill_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaldyhidayatt.split_bill_api.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillGroupId(Long billGroupId);
}
