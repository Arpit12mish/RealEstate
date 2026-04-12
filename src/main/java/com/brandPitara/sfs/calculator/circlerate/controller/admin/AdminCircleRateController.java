package com.brandPitara.sfs.calculator.circlerate.controller.admin;

import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateRuleResponse;
import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateUpsertRequest;
import com.brandPitara.sfs.calculator.circlerate.service.CircleRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/circle-rates")
@RequiredArgsConstructor
public class AdminCircleRateController {

    private final CircleRateService circleRateService;

    @PostMapping
    public CircleRateRuleResponse createRule(@Valid @RequestBody CircleRateUpsertRequest request) {
        return circleRateService.createRule(request);
    }

    @PutMapping("/{id}")
    public CircleRateRuleResponse updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CircleRateUpsertRequest request
    ) {
        return circleRateService.updateRule(id, request);
    }

    @GetMapping
    public List<CircleRateRuleResponse> getAllRules() {
        return circleRateService.getAllRules();
    }
}