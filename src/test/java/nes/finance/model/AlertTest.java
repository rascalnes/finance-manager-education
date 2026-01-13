package nes.finance.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

public class AlertTest {
    private Alert alert;

    @BeforeEach
    public void setUp() {
        alert = new Alert(AlertType.BUDGET_EXCEEDED, "Budget limit exceeded!");
    }

    @Test
    public void testAlertCreation() {
        assertNotNull(alert);
        assertEquals(AlertType.BUDGET_EXCEEDED, alert.getType());
        assertEquals("Budget limit exceeded!", alert.getMessage());
        assertNotNull(alert.getTimestamp());
        assertFalse(alert.isRead());
    }

    @Test
    public void testAlertWithCustomTimestamp() {
        LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Alert customAlert = new Alert(AlertType.LOW_BALANCE, "Low balance", customTime, true);

        assertEquals(AlertType.LOW_BALANCE, customAlert.getType());
        assertEquals("Low balance", customAlert.getMessage());
        assertEquals(customTime, customAlert.getTimestamp());
        assertTrue(customAlert.isRead());
    }

    @Test
    public void testMarkAsRead() {
        assertFalse(alert.isRead());
        alert.markAsRead();
        assertTrue(alert.isRead());
    }

    @Test
    public void testToString() {
        String alertStr = alert.toString();

        assertTrue(alertStr.contains("BUDGET_EXCEEDED"));
        assertTrue(alertStr.contains("Budget limit exceeded!"));
        assertTrue(alertStr.contains("[НОВОЕ]") || alertStr.contains("[ПРОЧИТАНО]"));

        // Помечаем как прочитанное и проверяем снова
        alert.markAsRead();
        alertStr = alert.toString();
        assertTrue(alertStr.contains("[ПРОЧИТАНО]"));
    }
}