package nes.finance.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

public class TransactionTest {
    private Transaction incomeTransaction;
    private Transaction expenseTransaction;

    @BeforeEach
    public void setUp() {
        incomeTransaction = new Transaction(TransactionType.INCOME, 1000.0, "Salary");
        expenseTransaction = new Transaction(TransactionType.EXPENSE, 500.0, "Food");
    }

    @Test
    public void testTransactionCreation() {
        assertNotNull(incomeTransaction);
        assertNotNull(expenseTransaction);

        assertEquals(TransactionType.INCOME, incomeTransaction.getType());
        assertEquals(TransactionType.EXPENSE, expenseTransaction.getType());

        assertEquals(1000.0, incomeTransaction.getAmount(), 0.001);
        assertEquals(500.0, expenseTransaction.getAmount(), 0.001);

        assertEquals("Salary", incomeTransaction.getCategory());
        assertEquals("Food", expenseTransaction.getCategory());

        assertNotNull(incomeTransaction.getDate());
        assertNotNull(expenseTransaction.getDate());
    }

    @Test
    public void testTransactionWithDate() {
        LocalDateTime customDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        Transaction transaction = new Transaction(TransactionType.INCOME, 1000.0, "Salary", customDate);

        assertEquals(customDate, transaction.getDate());
    }

    @Test
    public void testToString() {
        String incomeStr = incomeTransaction.toString();
        String expenseStr = expenseTransaction.toString();

        // Проверяем ключевые элементы строки
        assertTrue(incomeStr.contains("Transaction"));
        assertTrue(incomeStr.contains("INCOME"));
        assertTrue(incomeStr.contains("1000"));
        assertTrue(incomeStr.contains("Salary"));

        assertTrue(expenseStr.contains("Transaction"));
        assertTrue(expenseStr.contains("EXPENSE"));
        assertTrue(expenseStr.contains("500"));
        assertTrue(expenseStr.contains("Food"));
    }
}