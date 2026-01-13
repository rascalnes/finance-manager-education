package nes.finance.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testUserCreation() {
        User user = new User("testuser", "password123");

        assertEquals("testuser", user.getLogin());
        assertEquals("password123", user.getPassword());
        assertNotNull(user.getWallet());

        // Проверяем, что кошелек инициализирован правильно
        assertEquals(0.0, user.getWallet().getBalance(), 0.001);
        assertTrue(user.getWallet().getTransactions().isEmpty());
    }

    @Test
    public void testSetPassword() {
        User user = new User("testuser", "oldpassword");
        user.setPassword("newpassword");

        assertEquals("newpassword", user.getPassword());
    }

    @Test
    public void testToString() {
        User user = new User("testuser", "password123");
        String userStr = user.toString();

        assertTrue(userStr.contains("User"));
        assertTrue(userStr.contains("testuser"));
        assertTrue(userStr.contains("Wallet"));
    }
}