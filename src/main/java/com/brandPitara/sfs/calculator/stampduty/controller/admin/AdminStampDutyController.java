package com.brandPitara.sfs.calculator.stampduty.controller.admin;

import com.brandPitara.sfs.calculator.stampduty.dto.StampDutyRuleResponse;
import com.brandPitara.sfs.calculator.stampduty.dto.StampDutyUpsertRequest;
import com.brandPitara.sfs.calculator.stampduty.service.StampDutyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stamp-duty")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStampDutyController {

    private final StampDutyService stampDutyService;

    @PostMapping
    public StampDutyRuleResponse createRule(@Valid @RequestBody StampDutyUpsertRequest request) {
        return stampDutyService.createRule(request);
    }

    @PutMapping("/{id}")
    public StampDutyRuleResponse updateRule(
            @PathVariable Long id,
            @Valid @RequestBody StampDutyUpsertRequest request
    ) {
        return stampDutyService.updateRule(id, request);
    }

    @GetMapping
    public List<StampDutyRuleResponse> getAllRules() {
        return stampDutyService.getAllRules();
    }
}