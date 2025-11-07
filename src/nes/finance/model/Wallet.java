package nes.finance.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {
    private double balance;
    private List<Transaction> transactions;
    private Map<String, Double> budgets;

    public Wallet() {
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
        this.budgets = new HashMap<>();
    }

    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return transactions; }
    public Map<String, Double> getBudgets() { return budgets; }

    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return String.format("Wallet{balance=%.2f, transactions=%d, budgets=%d}",
                balance, transactions.size(), budgets.size());
    }
}