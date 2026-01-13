package nes.finance.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    private double balance;
    private List<Transaction> transactions;
    private Map<String, Double> budgets;
    private List<Alert> alerts;

    public Wallet() {
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
        this.budgets = new HashMap<>();
        this.alerts = new ArrayList<>();
    }

    // Getters
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return transactions; }
    public Map<String, Double> getBudgets() { return budgets; }
    public List<Alert> getAlerts() { return alerts; }

    public void setBalance(double balance) { this.balance = balance; }

    // Методы для работы с оповещениями
    public void addAlert(Alert alert) {
        this.alerts.add(alert);
    }

    public List<Alert> getUnreadAlerts() {
        List<Alert> unread = new ArrayList<>();
        for (Alert alert : alerts) {
            if (!alert.isRead()) {
                unread.add(alert);
            }
        }
        return unread;
    }

    public void markAllAlertsAsRead() {
        for (Alert alert : alerts) {
            alert.markAsRead();
        }
    }

    public int getUnreadAlertCount() {
        return getUnreadAlerts().size();
    }

    @Override
    public String toString() {
        return String.format("Wallet{balance=%.2f, transactions=%d, budgets=%d, alerts=%d}",
                balance, transactions.size(), budgets.size(), alerts.size());
    }
}