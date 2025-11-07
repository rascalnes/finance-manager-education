package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import nes.finance.model.Wallet;
import java.util.List;
import java.util.Map;

public class FinancialService {
    private AuthService authService;

    public FinancialService(AuthService authService) {
        this.authService = authService;
    }

    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return authService.isAuthenticated();
    }

    public boolean addIncome(double amount, String category) {
        if (!isAuthenticated() || amount <= 0) {
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        Transaction transaction = new Transaction(TransactionType.INCOME, amount, category);
        wallet.getTransactions().add(transaction);

        wallet.setBalance(wallet.getBalance() + amount);

        return true;
    }

    public boolean addExpense(double amount, String category) {
        if (!isAuthenticated() || amount <= 0) {
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        if (wallet.getBalance() < amount) {
            return false; // Превышение средств
        }

        Transaction transaction = new Transaction(TransactionType.EXPENSE, amount, category);
        wallet.getTransactions().add(transaction);

        wallet.setBalance(wallet.getBalance() - amount);

        return true;
    }

    // Методы для получения статистики
    public double getTotalIncome() {
        if (!isAuthenticated()) return 0;

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getTotalExpense() {
        if (!isAuthenticated()) return 0;

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public void showUserInfo() {
        if (isAuthenticated()) {
            User user = getCurrentUser();
            Wallet wallet = user.getWallet();

            System.out.printf("Пользователь: %s%n", user.getLogin());
            System.out.printf("Баланс: %.2f%n", wallet.getBalance());
            System.out.printf("Общий доход: %.2f%n", getTotalIncome());
            System.out.printf("Общий расход: %.2f%n", getTotalExpense());
            System.out.printf("Кол-во транзакций: %d%n", wallet.getTransactions().size());

            // TODO Последние 5 транзакций
            showRecentTransactions(5);
        } else {
            System.out.println("Пользователь не авторизован");
        }
    }

    private void showRecentTransactions(int count) {
        User user = getCurrentUser();
        List<Transaction> transactions = user.getWallet().getTransactions();

        if (transactions.isEmpty()) {
            System.out.println("Транзакций нет");
            return;
        }

        System.out.println("Последние транзакции:");
        int start = Math.max(0, transactions.size() - count);
        for (int i = start; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            String typeSymbol = t.getType() == TransactionType.INCOME ? "+" : "-";
            System.out.printf("  %s %.2f (%s) - %s%n",
                    typeSymbol, t.getAmount(), t.getCategory(), t.getDate()); // TODO Формат даты
        }
    }

    // Проверка категорий
    public void showCategoriesSummary() {
        if (!isAuthenticated()) return;

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        Map<String, Double> incomeByCategory = new java.util.HashMap<>();
        Map<String, Double> expenseByCategory = new java.util.HashMap<>();

        for (Transaction t : wallet.getTransactions()) {
            String category = t.getCategory();
            double amount = t.getAmount();

            if (t.getType() == TransactionType.INCOME) {
                incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
            } else {
                expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
            }
        }

        if (!incomeByCategory.isEmpty()) {
            System.out.println("Доходы по категориям:");
            incomeByCategory.forEach((category, total) ->
                    System.out.printf("  %s: %.2f%n", category, total));
        }

        if (!expenseByCategory.isEmpty()) {
            System.out.println("Расходы по категориям:");
            expenseByCategory.forEach((category, total) ->
                    System.out.printf("  %s: %.2f%n", category, total));
        }
    }
}