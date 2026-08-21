package com.renaldyhidayatt.split_bill_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaldyhidayatt.split_bill_api.model.BillGroup;

public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {
}
