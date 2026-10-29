package com.example.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {
    private final CalculatorService calculator = new CalculatorService();

    @Test void addsNumbers() { assertEquals(5.0, calculator.add(2, 3)); }
    @Test void subtractsNumbers() { assertEquals(-1.0, calculator.subtract(2, 3)); }
    @Test void multipliesNumbers() { assertEquals(6.0, calculator.multiply(2, 3)); }
    @Test void dividesNumbers() { assertEquals(2.5, calculator.divide(5, 2)); }
    @Test void rejectsDivisionByZero() { assertThrows(DivisionByZeroException.class, () -> calculator.divide(1, 0)); }
}
