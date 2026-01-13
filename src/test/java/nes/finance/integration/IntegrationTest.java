package nes.finance.integration;

import nes.finance.service.AuthService;
import nes.finance.service.FinancialService;
import nes.finance.service.DataService;
import nes.finance.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

public class IntegrationTest {

    @Test
    public void testCompleteUserFlow() {
        // Очищаем тестовые данные
        cleanupTestData();

        // 1. Создаем сервисы
        AuthService authService = new AuthService();
        FinancialService financialService = new FinancialService(authService);
        DataService dataService = new DataService();

        // 2. Регистрируем пользователя
        assertTrue(authService.register("integrationuser", "password123"));

        // 3. Авторизуемся
        assertTrue(authService.login("integrationuser", "password123"));
        assertTrue(authService.isAuthenticated());

        User user = authService.getCurrentUser();
        assertNotNull(user);
        assertEquals("integrationuser", user.getLogin());

        // 4. Добавляем доходы
        assertTrue(financialService.addIncome(50000.0, "Salary"));
        assertTrue(financialService.addIncome(5000.0, "Bonus"));

        assertEquals(55000.0, user.getWallet().getBalance(), 0.001);
        assertEquals(55000.0, financialService.getTotalIncome(), 0.001);

        // 5. Устанавливаем бюджеты
        assertTrue(financialService.setBudget("Food", 15000.0));
        assertTrue(financialService.setBudget("Transport", 5000.0));
        assertTrue(financialService.setBudget("Entertainment", 10000.0));

        // 6. Добавляем расходы
        assertTrue(financialService.addExpense(10000.0, "Food"));
        assertTrue(financialService.addExpense(3000.0, "Transport"));
        assertTrue(financialService.addExpense(5000.0, "Entertainment"));

        assertEquals(37000.0, user.getWallet().getBalance(), 0.001);
        assertEquals(18000.0, financialService.getTotalExpense(), 0.001);

        // 7. Проверяем бюджеты
        assertEquals(5000.0, financialService.getBudget("Food") - financialService.getExpenseByCategory("Food"), 0.001);
        assertEquals(2000.0, financialService.getBudget("Transport") - financialService.getExpenseByCategory("Transport"), 0.001);
        assertEquals(5000.0, financialService.getBudget("Entertainment") - financialService.getExpenseByCategory("Entertainment"), 0.001);

        // 8. Сохраняем данные
        assertTrue(dataService.saveUserData(user));

        // 9. Выходим и загружаем снова
        authService.logout();
        assertFalse(authService.isAuthenticated());

        assertTrue(authService.login("integrationuser", "password123"));
        User loadedUser = authService.getCurrentUser();

        // 10. Проверяем, что данные сохранились
        assertNotNull(loadedUser);
        assertEquals(37000.0, loadedUser.getWallet().getBalance(), 0.001);
        assertEquals(55000.0, financialService.getTotalIncome(), 0.001);
        assertEquals(18000.0, financialService.getTotalExpense(), 0.001);
        assertEquals(3, loadedUser.getWallet().getBudgets().size());

        cleanupTestData();
    }

    @Test
    public void testBudgetExceedAlertFlow() {
        cleanupTestData();

        AuthService authService = new AuthService();
        FinancialService financialService = new FinancialService(authService);

        authService.register("alertuser", "password123");
        authService.login("alertuser", "password123");

        // Устанавливаем бюджет
        financialService.setBudget("Food", 1000.0);

        // Добавляем доход
        financialService.addIncome(2000.0, "Salary");

        // Добавляем расход, превышающий бюджет
        financialService.addExpense(1200.0, "Food");

        // Проверяем, что есть оповещения
        User user = authService.getCurrentUser();
        assertFalse(user.getWallet().getAlerts().isEmpty());

        cleanupTestData();
    }

    private void cleanupTestData() {
        // Очищаем файлы данных
        File dataDir = new File("data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".dat"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        // Очищаем тестовые файлы экспорта
        String[] testFiles = {
                "test_transactions.csv",
                "test_budgets.csv",
                "test_export.json",
                "test_report.txt",
                "export_testuser_",
                "export_integrationuser_",
                "export_alertuser_",
                "report_testuser_",
                "report_integrationuser_",
                "report_alertuser_"
        };

        File currentDir = new File(".");
        File[] allFiles = currentDir.listFiles();
        if (allFiles != null) {
            for (File file : allFiles) {
                String fileName = file.getName();
                for (String pattern : testFiles) {
                    if (fileName.contains(pattern)) {
                        file.delete();
                        break;
                    }
                }
            }
        }
    }
}