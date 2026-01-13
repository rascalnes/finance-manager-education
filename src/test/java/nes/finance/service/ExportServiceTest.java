package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

public class ExportServiceTest {
    private ExportService exportService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        exportService = new ExportService();

        testUser = new User("testuser", "password123");
        testUser.getWallet().setBalance(1500.0);
        testUser.getWallet().getTransactions().add(new Transaction(TransactionType.INCOME, 2000.0, "Salary"));
        testUser.getWallet().getTransactions().add(new Transaction(TransactionType.EXPENSE, 500.0, "Food"));
        testUser.getWallet().getBudgets().put("Food", 1000.0);
    }

    private void cleanupTestFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testExportTransactionsToCSV() {
        String filename = "test_transactions.csv";
        cleanupTestFile(filename);

        boolean result = exportService.exportTransactionsToCSV(testUser, filename);
        assertTrue(result);

        File file = new File(filename);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        cleanupTestFile(filename);
    }

    @Test
    public void testExportBudgetsToCSV() {
        String filename = "test_budgets.csv";
        cleanupTestFile(filename);

        boolean result = exportService.exportBudgetsToCSV(testUser, filename);
        assertTrue(result);

        File file = new File(filename);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        cleanupTestFile(filename);
    }

    @Test
    public void testExportToJSON() {
        String filename = "test_export.json";
        cleanupTestFile(filename);

        boolean result = exportService.exportToJSON(testUser, filename);
        assertTrue(result);

        File file = new File(filename);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        cleanupTestFile(filename);
    }

    @Test
    public void testExportReportToText() {
        String filename = "test_report.txt";
        cleanupTestFile(filename);

        boolean result = exportService.exportReportToText(testUser, filename);
        assertTrue(result);

        File file = new File(filename);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        cleanupTestFile(filename);
    }

    @Test
    public void testExportWithNullUser() {
        boolean result = exportService.exportTransactionsToCSV(null, "test.csv");
        assertFalse(result);
    }

    @Test
    public void testExportWithEmptyTransactions() {
        User emptyUser = new User("emptyuser", "password");
        String filename = "empty_test.csv";
        cleanupTestFile(filename);

        boolean result = exportService.exportTransactionsToCSV(emptyUser, filename);
        assertFalse(result);

        cleanupTestFile(filename);
    }
}