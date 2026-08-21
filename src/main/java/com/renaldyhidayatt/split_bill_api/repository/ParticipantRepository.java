package com.renaldyhidayatt.split_bill_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaldyhidayatt.split_bill_api.model.Participant;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByBillGroupId(Long billGroupId);
}
