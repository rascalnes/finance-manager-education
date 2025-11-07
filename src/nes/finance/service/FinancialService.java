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

    // Методы для работы с транзакциями
    public boolean addIncome(double amount, String category) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidAmount(amount)) {
            System.out.println("Ошибка: сумма должна быть положительным числом");
            return false;
        }

        if (!isValidCategory(category)) {
            System.out.println("Ошибка: категория не может быть пустой");
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
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidAmount(amount)) {
            System.out.println("Ошибка: сумма должна быть положительным числом");
            return false;
        }

        if (!isValidCategory(category)) {
            System.out.println("Ошибка: категория не может быть пустой");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        if (wallet.getBalance() < amount) {
            System.out.println("Ошибка: недостаточно средств на счете");
            return false;
        }

        Transaction transaction = new Transaction(TransactionType.EXPENSE, amount, category);
        wallet.getTransactions().add(transaction);

        wallet.setBalance(wallet.getBalance() - amount);

        checkBudgetExceeded(category, amount);

        return true;
    }

    // Методы для работы с бюджетами
    public boolean setBudget(String category, double limit) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidCategory(category)) {
            System.out.println("Ошибка: категория не может быть пустой");
            return false;
        }

        if (!isValidAmount(limit)) {
            System.out.println("Ошибка: лимит бюджета должен быть положительным числом");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        wallet.getBudgets().put(category, limit);
        System.out.printf("Бюджет для категории '%s' установлен: %.2f%n", category, limit);

        return true;
    }

    public Double getBudget(String category) {
        if (!isAuthenticated()) return null;

        User user = getCurrentUser();
        return user.getWallet().getBudgets().get(category);
    }

    public Map<String, Double> getAllBudgets() {
        if (!isAuthenticated()) return Map.of();

        User user = getCurrentUser();
        return user.getWallet().getBudgets();
    }

    // Метод для проверки превышения бюджета
    private void checkBudgetExceeded(String category, double newExpense) {
        Double budgetLimit = getBudget(category);
        if (budgetLimit == null) return;

        double currentExpenses = getExpenseByCategory(category);
        if (currentExpenses > budgetLimit) {
            System.out.printf("[ВНИМАНИЕ] Превышен бюджет для категории '%s'! Лимит: %.2f, Факт: %.2f%n",
                    category, budgetLimit, currentExpenses);
        }
    }

    // Методы валидации
    private boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    private boolean isValidCategory(String category) {
        return category != null && !category.trim().isEmpty();
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

    public double getIncomeByCategory(String category) {
        if (!isAuthenticated()) return 0;

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME && t.getCategory().equals(category))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getExpenseByCategory(String category) {
        if (!isAuthenticated()) return 0;

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory().equals(category))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    // Метод для получения статуса бюджетов
    public void showBudgetStatus() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        Map<String, Double> budgets = getAllBudgets();
        if (budgets.isEmpty()) {
            System.out.println("Бюджеты не установлены");
            return;
        }

        System.out.println("Статус бюджетов:");
        for (Map.Entry<String, Double> entry : budgets.entrySet()) {
            String category = entry.getKey();
            double limit = entry.getValue();
            double expenses = getExpenseByCategory(category);
            double remaining = limit - expenses;

            System.out.printf("  %s: Лимит %.2f, Расходы %.2f, Осталось %.2f%n",
                    category, limit, expenses, remaining);
        }
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
            System.out.printf("Кол-во бюджетов: %d%n", wallet.getBudgets().size());

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

    public void showCategoriesSummary() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

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

        // Показываем статус бюджетов, если они есть
        if (!wallet.getBudgets().isEmpty()) {
            showBudgetStatus();
        }
    }
}