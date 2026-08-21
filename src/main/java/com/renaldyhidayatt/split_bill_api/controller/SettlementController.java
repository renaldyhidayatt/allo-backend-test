package com.renaldyhidayatt.split_bill_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaldyhidayatt.split_bill_api.dto.SettlementDtos.SettlementResponse;
import com.renaldyhidayatt.split_bill_api.service.SettlementService;

@RestController
@RequestMapping("/api/groups/{groupId}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public SettlementResponse getSettlement(@PathVariable Long groupId) {
        return settlementService.computeSettlement(groupId);
    }
}
