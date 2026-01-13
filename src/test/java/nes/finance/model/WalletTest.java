package nes.finance.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class WalletTest {
    private Wallet wallet;

    @BeforeEach
    public void setUp() {
        wallet = new Wallet();
    }

    @Test
    public void testWalletInitialization() {
        assertEquals(0.0, wallet.getBalance(), 0.001);
        assertTrue(wallet.getTransactions().isEmpty());
        assertTrue(wallet.getBudgets().isEmpty());
        assertTrue(wallet.getAlerts().isEmpty());
    }

    @Test
    public void testSetBalance() {
        wallet.setBalance(1000.0);
        assertEquals(1000.0, wallet.getBalance(), 0.001);

        wallet.setBalance(500.0);
        assertEquals(500.0, wallet.getBalance(), 0.001);
    }

    @Test
    public void testAddAlert() {
        Alert alert = new Alert(AlertType.BUDGET_EXCEEDED, "Budget exceeded");
        wallet.addAlert(alert);

        assertEquals(1, wallet.getAlerts().size());
        assertEquals(alert, wallet.getAlerts().get(0));
    }

    @Test
    public void testUnreadAlerts() {
        Alert readAlert = new Alert(AlertType.BUDGET_EXCEEDED, "Read alert");
        readAlert.markAsRead();

        Alert unreadAlert = new Alert(AlertType.LOW_BALANCE, "Unread alert");

        wallet.addAlert(readAlert);
        wallet.addAlert(unreadAlert);

        List<Alert> unreadAlerts = wallet.getUnreadAlerts();
        assertEquals(1, unreadAlerts.size());
        assertEquals(unreadAlert, unreadAlerts.get(0));
    }

    @Test
    public void testMarkAllAlertsAsRead() {
        Alert alert1 = new Alert(AlertType.BUDGET_EXCEEDED, "Alert 1");
        Alert alert2 = new Alert(AlertType.LOW_BALANCE, "Alert 2");

        wallet.addAlert(alert1);
        wallet.addAlert(alert2);

        assertEquals(2, wallet.getUnreadAlertCount());

        wallet.markAllAlertsAsRead();

        assertEquals(0, wallet.getUnreadAlertCount());
        assertTrue(wallet.getUnreadAlerts().isEmpty());
    }

    @Test
    public void testToString() {
        String walletStr = wallet.toString();

        // Проверяем ключевые элементы строки
        assertTrue(walletStr.contains("Wallet"));
        assertTrue(walletStr.contains("balance="));
        assertTrue(walletStr.contains("transactions="));
        assertTrue(walletStr.contains("budgets="));
        assertTrue(walletStr.contains("alerts="));
    }
}