package com.brandPitara.sfs.calculator.interiorcost.controller.admin;

import com.brandPitara.sfs.calculator.interiorcost.dto.*;
import com.brandPitara.sfs.calculator.interiorcost.service.InteriorCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/interior-cost")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInteriorCostController {

    private final InteriorCostService interiorCostService;

    @PostMapping("/base-rules")
    public InteriorCostRuleResponse createBaseRule(@Valid @RequestBody InteriorCostRuleUpsertRequest request) {
        return interiorCostService.createBaseRule(request);
    }

    @PostMapping("/addon-rules")
    public InteriorCostAddonRuleResponse createAddonRule(@Valid @RequestBody InteriorCostAddonRuleUpsertRequest request) {
        return interiorCostService.createAddonRule(request);
    }

    @GetMapping("/base-rules")
    public List<InteriorCostRuleResponse> getAllBaseRules() {
        return interiorCostService.getAllBaseRules();
    }

    @GetMapping("/addon-rules")
    public List<InteriorCostAddonRuleResponse> getAllAddonRules() {
        return interiorCostService.getAllAddonRules();
    }
}