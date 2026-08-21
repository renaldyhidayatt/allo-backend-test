package com.renaldyhidayatt.split_bill_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaldyhidayatt.split_bill_api.dto.GroupDtos.CreateGroupRequest;
import com.renaldyhidayatt.split_bill_api.dto.GroupDtos.GroupResponse;
import com.renaldyhidayatt.split_bill_api.dto.GroupDtos.ParticipantResponse;
import com.renaldyhidayatt.split_bill_api.exception.ResourceNotFoundException;
import com.renaldyhidayatt.split_bill_api.model.BillGroup;
import com.renaldyhidayatt.split_bill_api.model.Participant;
import com.renaldyhidayatt.split_bill_api.repository.BillGroupRepository;

@Service
public class BillGroupService {

    private final BillGroupRepository billGroupRepository;

    public BillGroupService(BillGroupRepository billGroupRepository) {
        this.billGroupRepository = billGroupRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        BillGroup group = new BillGroup(request.name());
        for (String name : request.participants()) {
            group.addParticipant(new Participant(name, group));
        }
        return toResponse(billGroupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId) {
        return toResponse(findGroupOrThrow(groupId));
    }

    @Transactional(readOnly = true)
    public BillGroup findGroupOrThrow(Long groupId) {
        return billGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill group " + groupId + " not found"));
    }

    private GroupResponse toResponse(BillGroup group) {
        List<ParticipantResponse> participants = group.getParticipants().stream()
                .map(p -> new ParticipantResponse(p.getId(), p.getName()))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), participants);
    }
}
