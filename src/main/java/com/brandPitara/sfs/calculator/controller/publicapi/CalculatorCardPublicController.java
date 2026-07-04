package com.brandPitara.sfs.calculator.controller.publicapi;

import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;
import com.brandPitara.sfs.calculator.service.CalculatorCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/calculators")
@RequiredArgsConstructor
public class CalculatorCardPublicController {

    private final CalculatorCardService calculatorCardService;

    @GetMapping("/cards")
    public List<CalculatorCardResponse> cards(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return calculatorCardService.publicListHomeCalculatorCards(limit);
    }
}
