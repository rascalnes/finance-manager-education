package nes.finance.service;

import nes.finance.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class FinancialServiceTest {
    private AuthService authService;
    private FinancialService financialService;

    @BeforeEach
    public void setUp() {
        authService = new AuthService();
        financialService = new FinancialService(authService);

        // Очищаем предыдущие данные и создаем нового пользователя
        authService.getUsers().clear();
        authService.register("testuser", "password123");
        authService.login("testuser", "password123");
    }

    @Test
    public void testAddIncome() {
        boolean result = financialService.addIncome(1000.0, "Salary");
        assertTrue(result);

        User user = financialService.getCurrentUser();
        assertEquals(1000.0, user.getWallet().getBalance(), 0.001);
        assertEquals(1, user.getWallet().getTransactions().size());
        assertEquals(1000.0, financialService.getTotalIncome(), 0.001);
    }

    @Test
    public void testAddIncomeInvalidAmount() {
        // Отрицательная сумма
        boolean result1 = financialService.addIncome(-1000.0, "Salary");
        assertFalse(result1);

        // Нулевая сумма
        boolean result2 = financialService.addIncome(0.0, "Salary");
        assertFalse(result2);
    }

    @Test
    public void testAddExpense() {
        // Сначала добавляем доход
        financialService.addIncome(2000.0, "Salary");

        // Теперь можем добавить расход
        boolean result = financialService.addExpense(500.0, "Food");
        assertTrue(result);

        User user = financialService.getCurrentUser();
        assertEquals(1500.0, user.getWallet().getBalance(), 0.001);
        assertEquals(2, user.getWallet().getTransactions().size());
        assertEquals(500.0, financialService.getTotalExpense(), 0.001);
    }

    @Test
    public void testAddExpenseInsufficientFunds() {
        financialService.addIncome(100.0, "Salary");

        // Попытка потратить больше, чем есть
        boolean result = financialService.addExpense(200.0, "Food");
        assertFalse(result);

        // Баланс не должен измениться
        assertEquals(100.0, financialService.getCurrentUser().getWallet().getBalance(), 0.001);
    }

    @Test
    public void testSetBudget() {
        boolean result = financialService.setBudget("Food", 5000.0);
        assertTrue(result);

        Double budget = financialService.getBudget("Food");
        assertNotNull(budget);
        assertEquals(5000.0, budget, 0.001);
    }

    @Test
    public void testEditBudget() {
        financialService.setBudget("Food", 5000.0);
        financialService.addIncome(10000.0, "Salary");
        financialService.addExpense(3000.0, "Food");

        // Редактируем бюджет
        boolean result = financialService.editBudget("Food", 4000.0);
        assertTrue(result);

        Double newBudget = financialService.getBudget("Food");
        assertEquals(4000.0, newBudget, 0.001);
    }

    @Test
    public void testGetIncomeByCategory() {
        // Очищаем предыдущие транзакции
        User user = financialService.getCurrentUser();
        user.getWallet().getTransactions().clear();
        user.getWallet().setBalance(0.0);

        // Добавляем тестовые данные
        financialService.addIncome(1000.0, "Salary");
        financialService.addIncome(500.0, "Bonus");
        financialService.addIncome(200.0, "Salary");

        double salaryIncome = financialService.getIncomeByCategory("Salary");
        double bonusIncome = financialService.getIncomeByCategory("Bonus");
        double nonExistentIncome = financialService.getIncomeByCategory("NonExistent");

        assertEquals(1200.0, salaryIncome, 0.001);
        assertEquals(500.0, bonusIncome, 0.001);
        assertEquals(0.0, nonExistentIncome, 0.001);
    }

    @Test
    public void testGetExpenseByCategory() {
        // Очищаем предыдущие транзакции
        User user = financialService.getCurrentUser();
        user.getWallet().getTransactions().clear();
        user.getWallet().setBalance(0.0);

        // Добавляем доход для расходов
        financialService.addIncome(5000.0, "Salary");
        financialService.addExpense(1000.0, "Food");
        financialService.addExpense(500.0, "Transport");
        financialService.addExpense(300.0, "Food");

        double foodExpense = financialService.getExpenseByCategory("Food");
        double transportExpense = financialService.getExpenseByCategory("Transport");
        double nonExistentExpense = financialService.getExpenseByCategory("NonExistent");

        assertEquals(1300.0, foodExpense, 0.001);
        assertEquals(500.0, transportExpense, 0.001);
        assertEquals(0.0, nonExistentExpense, 0.001);
    }

    @Test
    public void testCalculateSelectedCategories() {
        // Очищаем предыдущие транзакции
        User user = financialService.getCurrentUser();
        user.getWallet().getTransactions().clear();
        user.getWallet().setBalance(0.0);

        financialService.addIncome(1000.0, "Salary");
        financialService.addIncome(500.0, "Bonus");
        financialService.addIncome(5000.0, "Salary"); // Добавляем доход для расходов
        financialService.addExpense(300.0, "Food");
        financialService.addExpense(200.0, "Transport");

        // Метод должен выполняться без ошибок
        financialService.calculateSelectedCategories(new String[]{"Salary", "Food"});
        assertTrue(financialService.isAuthenticated());
    }

    @Test
    public void testShowUserInfo() {
        // Очищаем предыдущие транзакции
        User user = financialService.getCurrentUser();
        user.getWallet().getTransactions().clear();
        user.getWallet().setBalance(0.0);

        financialService.addIncome(1000.0, "Salary");
        financialService.addExpense(300.0, "Food");

        // Метод должен выполняться без ошибок
        financialService.showUserInfo();
        assertTrue(financialService.isAuthenticated());
    }
}