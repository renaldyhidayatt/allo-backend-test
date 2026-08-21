package com.renaldyhidayatt.split_bill_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaldyhidayatt.split_bill_api.dto.PaymentDtos.PaymentResponse;
import com.renaldyhidayatt.split_bill_api.dto.PaymentDtos.RecordPaymentRequest;
import com.renaldyhidayatt.split_bill_api.exception.InvalidRequestException;
import com.renaldyhidayatt.split_bill_api.model.BillGroup;
import com.renaldyhidayatt.split_bill_api.model.Participant;
import com.renaldyhidayatt.split_bill_api.model.Payment;
import com.renaldyhidayatt.split_bill_api.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillGroupService billGroupService;

    public PaymentService(PaymentRepository paymentRepository, BillGroupService billGroupService) {
        this.paymentRepository = paymentRepository;
        this.billGroupService = billGroupService;
    }

    @Transactional
    public PaymentResponse recordPayment(Long groupId, RecordPaymentRequest request) {
        BillGroup group = billGroupService.findGroupOrThrow(groupId);

        if (request.fromParticipantId().equals(request.toParticipantId())) {
            throw new InvalidRequestException("A participant cannot pay themselves");
        }

        Participant from = requireParticipantInGroup(group, request.fromParticipantId());
        Participant to = requireParticipantInGroup(group, request.toParticipantId());

        Payment payment = new Payment(group, from, to, request.amount());
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(Long groupId) {
        billGroupService.findGroupOrThrow(groupId);
        return paymentRepository.findByBillGroupId(groupId).stream().map(this::toResponse).toList();
    }

    private Participant requireParticipantInGroup(BillGroup group, Long participantId) {
        return group.getParticipants().stream()
                .filter(p -> p.getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Participant " + participantId + " is not part of group " + group.getId()));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getFrom().getId(),
                payment.getFrom().getName(),
                payment.getTo().getId(),
                payment.getTo().getName(),
                payment.getAmount(),
                payment.getCreatedAt()
        );
    }
}
