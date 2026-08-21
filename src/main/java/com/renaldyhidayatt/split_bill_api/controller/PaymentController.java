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

import com.renaldyhidayatt.split_bill_api.dto.PaymentDtos.PaymentResponse;
import com.renaldyhidayatt.split_bill_api.dto.PaymentDtos.RecordPaymentRequest;
import com.renaldyhidayatt.split_bill_api.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups/{groupId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable Long groupId,
                                                           @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PaymentResponse> getPayments(@PathVariable Long groupId) {
        return paymentService.getPayments(groupId);
    }
}
