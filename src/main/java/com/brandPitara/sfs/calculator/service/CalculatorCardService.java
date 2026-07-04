package com.brandPitara.sfs.calculator.service;

import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;

import java.util.List;

public interface CalculatorCardService {
    List<CalculatorCardResponse> publicListHomeCalculatorCards(int limit);
}
