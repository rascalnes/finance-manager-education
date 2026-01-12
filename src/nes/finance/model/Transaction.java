package nes.finance.model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private TransactionType type;
    private double amount;
    private String category;
    private LocalDateTime date;

    public Transaction(TransactionType type, double amount, String category) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = LocalDateTime.now();
    }

    // Конструктор для загрузки из файла
    public Transaction(TransactionType type, double amount, String category, LocalDateTime date) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    // Getters
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDateTime getDate() { return date; }

    @Override
    public String toString() {
        return String.format("Transaction{type=%s, amount=%.2f, category='%s', date=%s}",
                type, amount, category, date);
    }
}