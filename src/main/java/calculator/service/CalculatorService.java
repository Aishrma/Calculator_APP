package calculator.service;

import calculator.exception.DivisionByZeroException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Core arithmetic. BigDecimal is used instead of double so that
 * 0.1 + 0.2 returns 0.3 (not 0.30000000000000004).
 */
@Service
public class CalculatorService {

    /** 16 significant digits, rounding HALF_EVEN - enough for a calculator. */
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(a.add(b));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(a.subtract(b));
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return normalize(a.multiply(b));
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            throw new DivisionByZeroException();
        }
        return normalize(a.divide(b, PRECISION));
    }

    /** Removes trailing zeros so 5.00 is returned as 5. */
    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
