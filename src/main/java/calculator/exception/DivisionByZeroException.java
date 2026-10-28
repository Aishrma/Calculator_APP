package calculator.exception;

public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("Division by zero is not allowed: parameter 'b' must not be 0");
    }
}
