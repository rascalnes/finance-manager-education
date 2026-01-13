package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

public class DataServiceTest {
    private DataService dataService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        // Очищаем тестовые данные
        cleanupTestData();
        dataService = new DataService();

        // Создаем тестового пользователя
        testUser = new User("testuser", "password123");
        testUser.getWallet().setBalance(1000.0);
        testUser.getWallet().getTransactions().add(new Transaction(TransactionType.INCOME, 1000.0, "Salary"));
        testUser.getWallet().getBudgets().put("Food", 500.0);
    }

    private void cleanupTestData() {
        File dataDir = new File("data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".dat"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    @Test
    public void testSaveUserData() {
        boolean result = dataService.saveUserData(testUser);
        assertTrue(result);

        // Проверяем, что файл создан
        File dataFile = new File("data/testuser.dat");
        assertTrue(dataFile.exists());
    }

    @Test
    public void testLoadUserData() {
        // Сначала сохраняем
        dataService.saveUserData(testUser);

        // Затем загружаем
        User loadedUser = dataService.loadUserData("testuser");

        assertNotNull(loadedUser);
        assertEquals("testuser", loadedUser.getLogin());
        assertEquals("password123", loadedUser.getPassword());
        assertEquals(1000.0, loadedUser.getWallet().getBalance(), 0.001);
        assertEquals(1, loadedUser.getWallet().getTransactions().size());
        assertEquals(1, loadedUser.getWallet().getBudgets().size());
        assertTrue(loadedUser.getWallet().getBudgets().containsKey("Food"));
        assertEquals(500.0, loadedUser.getWallet().getBudgets().get("Food"), 0.001);
    }

    @Test
    public void testLoadNonExistentUser() {
        User loadedUser = dataService.loadUserData("nonexistent");
        assertNull(loadedUser);
    }

    @Test
    public void testUserDataExists() {
        // Проверяем несуществующего пользователя
        assertFalse(dataService.userDataExists("nonexistent"));

        // Сохраняем и проверяем существующего
        dataService.saveUserData(testUser);
        assertTrue(dataService.userDataExists("testuser"));
    }

    @Test
    public void testDeleteUserData() {
        dataService.saveUserData(testUser);
        assertTrue(dataService.userDataExists("testuser"));

        boolean result = dataService.deleteUserData("testuser");
        assertTrue(result);
        assertFalse(dataService.userDataExists("testuser"));
    }

    @Test
    public void testDeleteNonExistentUserData() {
        boolean result = dataService.deleteUserData("nonexistent");
        assertFalse(result);
    }

    @Test
    public void testGetAllSavedUsers() {
        // Сначала нет пользователей
        assertEquals(0, dataService.getAllSavedUsers().size());

        // Сохраняем двух пользователей
        dataService.saveUserData(testUser);

        User anotherUser = new User("anotheruser", "password456");
        dataService.saveUserData(anotherUser);

        // Проверяем список
        var users = dataService.getAllSavedUsers();
        assertEquals(2, users.size());
        assertTrue(users.contains("testuser"));
        assertTrue(users.contains("anotheruser"));
    }
}