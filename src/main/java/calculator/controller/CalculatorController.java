package calculator.controller;

import calculator.dto.CalculationResponse;
import calculator.service.CalculatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * GET /api/v1/{add|subtract|multiply|divide}?a=..&b=..
 * Missing or non-numeric parameters are rejected by Spring before the method
 * runs and are turned into HTTP 400 by {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/add")
    public CalculationResponse add(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
        return response("add", a, b, calculatorService.add(a, b));
    }

    @GetMapping("/subtract")
    public CalculationResponse subtract(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
        return response("subtract", a, b, calculatorService.subtract(a, b));
    }

    @GetMapping("/multiply")
    public CalculationResponse multiply(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
        return response("multiply", a, b, calculatorService.multiply(a, b));
    }

    @GetMapping("/divide")
    public CalculationResponse divide(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
        return response("divide", a, b, calculatorService.divide(a, b));
    }

    private CalculationResponse response(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {
        return new CalculationResponse(operation, a.stripTrailingZeros(), b.stripTrailingZeros(), result);
    }
}
