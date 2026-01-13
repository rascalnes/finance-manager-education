package nes.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

public class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        // Очищаем данные перед каждым тестом
        cleanupTestData();
        authService = new AuthService();
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
    public void testRegisterNewUser() {
        boolean result = authService.register("newuser", "password123");
        assertTrue(result);

        // Проверяем, что пользователь добавлен в систему
        assertTrue(authService.getUsers().containsKey("newuser"));
    }

    @Test
    public void testRegisterExistingUser() {
        authService.register("existinguser", "password123");
        boolean result = authService.register("existinguser", "newpassword");

        // Регистрация с существующим логином должна вернуть false
        assertFalse(result);
    }

    @Test
    public void testLoginSuccess() {
        authService.register("testuser", "password123");
        boolean result = authService.login("testuser", "password123");

        assertTrue(result);
        assertTrue(authService.isAuthenticated());
        assertNotNull(authService.getCurrentUser());
        assertEquals("testuser", authService.getCurrentUser().getLogin());
    }

    @Test
    public void testLoginWrongPassword() {
        authService.register("testuser", "password123");
        boolean result = authService.login("testuser", "wrongpassword");

        assertFalse(result);
        assertFalse(authService.isAuthenticated());
        assertNull(authService.getCurrentUser());
    }

    @Test
    public void testLoginNonExistentUser() {
        boolean result = authService.login("nonexistent", "password123");
        assertFalse(result);
    }

    @Test
    public void testLogout() {
        authService.register("testuser", "password123");
        authService.login("testuser", "password123");

        assertTrue(authService.isAuthenticated());
        authService.logout();

        assertFalse(authService.isAuthenticated());
        assertNull(authService.getCurrentUser());
    }

    @Test
    public void testDeleteUser() {
        authService.register("todelete", "password123");
        authService.login("todelete", "password123");

        // Неправильный пароль
        boolean result1 = authService.deleteUser("todelete", "wrongpass");
        assertFalse(result1);

        // Правильный пароль
        boolean result2 = authService.deleteUser("todelete", "password123");
        assertTrue(result2);

        // Пользователь должен быть удален из системы
        assertFalse(authService.getUsers().containsKey("todelete"));
        assertFalse(authService.isAuthenticated());
    }

    @Test
    public void testIsAuthenticated() {
        assertFalse(authService.isAuthenticated());

        authService.register("testuser", "password123");
        authService.login("testuser", "password123");

        assertTrue(authService.isAuthenticated());
    }
}