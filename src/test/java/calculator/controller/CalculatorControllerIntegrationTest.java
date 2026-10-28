package calculator.controller;

import calculator.service.CalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC integration tests: real controller + real service + real
 * exception handler + Jackson, driven through MockMvc.
 */
@WebMvcTest(CalculatorController.class)
@Import(CalculatorService.class)
class CalculatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/add returns the documented JSON shape")
    void add() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "operation": "add", "a": 2, "b": 3, "result": 5 }
                        """));
    }

    @Test
    void subtract() throws Exception {
        mockMvc.perform(get("/api/v1/subtract").param("a", "10").param("b", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.result").value(6));
    }

    @Test
    void multiply() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "6").param("b", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("multiply"))
                .andExpect(jsonPath("$.result").value(42));
    }

    @Test
    void divide() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "7").param("b", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("divide"))
                .andExpect(jsonPath("$.result").value(3.5));
    }

    @Test
    @DisplayName("decimal inputs avoid floating-point error (0.1 + 0.2 = 0.3)")
    void decimalPrecision() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "0.1").param("b", "0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(0.3));
    }

    @Test
    @DisplayName("divide by zero -> 400 with error JSON")
    void divideByZero() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "5").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Division by zero is not allowed: parameter 'b' must not be 0"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("missing parameter -> 400 with error JSON")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'b'"));
    }

    @Test
    @DisplayName("non-numeric parameter -> 400 with error JSON")
    void invalidNumber() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "abc").param("b", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Parameter 'a' must be a valid number but was 'abc'"));
    }
}
