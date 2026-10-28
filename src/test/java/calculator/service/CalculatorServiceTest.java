package calculator.service;

import calculator.exception.DivisionByZeroException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure JUnit 5 unit tests - no Spring context is started. */
class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** compareTo ignores scale, so 5 and 5.0 are considered equal. */
    private static void assertNumber(BigDecimal actual, String expected) {
        assertThat(actual).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "add: {0} + {1} = {2}")
    @CsvSource({
            "2, 3, 5",
            "-4, 10, 6",
            "-2.5, -2.5, -5",
            "0.1, 0.2, 0.3",
            "0, 0, 0"
    })
    void add(String a, String b, String expected) {
        assertNumber(service.add(bd(a), bd(b)), expected);
    }

    @ParameterizedTest(name = "subtract: {0} - {1} = {2}")
    @CsvSource({
            "10, 4, 6",
            "4, 10, -6",
            "-3, -3, 0",
            "5.5, 0.25, 5.25"
    })
    void subtract(String a, String b, String expected) {
        assertNumber(service.subtract(bd(a), bd(b)), expected);
    }

    @ParameterizedTest(name = "multiply: {0} * {1} = {2}")
    @CsvSource({
            "6, 7, 42",
            "-3, 4, -12",
            "-3, -4, 12",
            "123456, 0, 0",
            "1.5, 2, 3"
    })
    void multiply(String a, String b, String expected) {
        assertNumber(service.multiply(bd(a), bd(b)), expected);
    }

    @ParameterizedTest(name = "divide: {0} / {1} = {2}")
    @CsvSource({
            "10, 2, 5",
            "7, 2, 3.5",
            "-9, 3, -3",
            "0, 5, 0",
            "1, 3, 0.3333333333333333"
    })
    void divide(String a, String b, String expected) {
        assertNumber(service.divide(bd(a), bd(b)), expected);
    }

    @Test
    @DisplayName("divide: throws DivisionByZeroException when b is 0")
    void divideByZeroThrows() {
        assertThatThrownBy(() -> service.divide(bd("10"), BigDecimal.ZERO))
                .isInstanceOf(DivisionByZeroException.class)
                .hasMessageContaining("Division by zero");
    }

    @Test
    @DisplayName("divide: treats 0.00 as zero as well")
    void divideByZeroWithScaleThrows() {
        assertThatThrownBy(() -> service.divide(bd("10"), bd("0.00")))
                .isInstanceOf(DivisionByZeroException.class);
    }

    @Test
    @DisplayName("results are normalised (no trailing zeros)")
    void resultHasNoTrailingZeros() {
        assertThat(service.add(bd("2.50"), bd("2.50")).toPlainString()).isEqualTo("5");
    }
}
