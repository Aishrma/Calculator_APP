package calculator.dto;

import java.math.BigDecimal;

/** Success body, e.g. { "operation": "add", "a": 2, "b": 3, "result": 5 } */
public record CalculationResponse(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {
}
