package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import nes.finance.model.Wallet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return getCurrentUser().getWallet().getBudgets().get(category);
    }

    public Map<String, Double> getAllBudgets() {
        if (!isAuthenticated()) return Map.of();
        return getCurrentUser().getWallet().getBudgets();
    }

    // Методы валидации
    private boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    private boolean isValidCategory(String category) {
        return category != null && !category.trim().isEmpty();
    }

    private void checkBudgetExceeded(String category, double newExpense) {
        Double budgetLimit = getBudget(category);
        if (budgetLimit == null) return;

        double currentExpenses = getExpenseByCategory(category);
        if (currentExpenses > budgetLimit) {
            System.out.printf("[ВНИМАНИЕ] Превышен бюджет для категории '%s'! Лимит: %.2f, Факт: %.2f%n",
                    category, budgetLimit, currentExpenses);
        }
    }

    // Основной метод для вывода полной статистики
    public void showFullStatistics() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        System.out.println("=== ФИНАНСОВАЯ СТАТИСТИКА ===");

        // Общие доходы и расходы
        double totalIncome = getTotalIncome();
        double totalExpense = getTotalExpense();

        System.out.printf("Общий доход: %,.1f%n", totalIncome);
        System.out.printf("Общие расходы: %,.1f%n", totalExpense);
        System.out.printf("Текущий баланс: %,.1f%n", getCurrentUser().getWallet().getBalance());
        System.out.println();

        // Доходы по категориям
        showIncomeByCategories();
        System.out.println();

        // Расходы по категориям
        showExpensesByCategories();
        System.out.println();

        // Статус бюджетов
        showDetailedBudgetStatus();
    }

    // Доходы по категориям с форматированием
    private void showIncomeByCategories() {
        Map<String, Double> incomeByCategory = getIncomeByCategories();

        if (incomeByCategory.isEmpty()) {
            System.out.println("Доходы по категориям: нет данных");
            return;
        }

        System.out.println("Доходы по категориям:");
        incomeByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("  - %s: %,.1f%n", entry.getKey(), entry.getValue()));
    }

    // Расходы по категориям с форматированием
    private void showExpensesByCategories() {
        Map<String, Double> expenseByCategory = getExpenseByCategories();

        if (expenseByCategory.isEmpty()) {
            System.out.println("Расходы по категориям: нет данных");
            return;
        }

        System.out.println("Расходы по категориям:");
        expenseByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("  - %s: %,.1f%n", entry.getKey(), entry.getValue()));
    }

    // Детальный статус бюджетов
    public void showDetailedBudgetStatus() {
        Map<String, Double> budgets = getAllBudgets();
        if (budgets.isEmpty()) {
            System.out.println("Бюджеты по категориям: не установлены");
            return;
        }

        System.out.println("Бюджет по категориям:");

        // Сортировка бюджет по убыванию превышения лимита (сначала те, где превышен бюджет)
        budgets.entrySet().stream()
                .sorted((a, b) -> {
                    double remainingA = a.getValue() - getExpenseByCategory(a.getKey());
                    double remainingB = b.getValue() - getExpenseByCategory(b.getKey());
                    return Double.compare(remainingA, remainingB); // Сначала отрицательные (превышенные)
                })
                .forEach(entry -> {
                    String category = entry.getKey();
                    double limit = entry.getValue();
                    double expenses = getExpenseByCategory(category);
                    double remaining = limit - expenses;

                    System.out.printf("  - %s: %,.1f, Оставшийся бюджет: %,.1f%n",
                            category, limit, remaining);
                });
    }

    // Методы для получения агрегированных данных по категориям
    public Map<String, Double> getIncomeByCategories() {
        if (!isAuthenticated()) return Map.of();

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    public Map<String, Double> getExpenseByCategories() {
        if (!isAuthenticated()) return Map.of();

        return getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // Метод для подсчета по нескольким выбранным категориям
    public void calculateSelectedCategories(String[] categories) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        if (categories == null || categories.length == 0) {
            System.out.println("Ошибка: не указаны категории для подсчета");
            return;
        }

        System.out.println("Подсчет по выбранным категориям:");

        double totalIncome = 0;
        double totalExpense = 0;
        boolean hasValidCategories = false;

        for (String category : categories) {
            double income = getIncomeByCategory(category);
            double expense = getExpenseByCategory(category);

            if (income > 0 || expense > 0) {
                hasValidCategories = true;
                totalIncome += income;
                totalExpense += expense;

                System.out.printf("  %s: доходы %,.1f, расходы %,.1f%n",
                        category, income, expense);
            } else {
                System.out.printf("  Категория '%s' не найдена или нет операций%n", category);
            }
        }

        if (hasValidCategories) {
            System.out.printf("Итого по выбранным категориям: доходы %,.1f, расходы %,.1f%n",
                    totalIncome, totalExpense);
        }
    }

    // Методы для вывода статистки статистики
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

    public void showUserInfo() {
        if (isAuthenticated()) {
            User user = getCurrentUser();
            Wallet wallet = user.getWallet();

            System.out.printf("Пользователь: %s%n", user.getLogin());
            System.out.printf("Баланс: %,.2f%n", wallet.getBalance());
            System.out.printf("Общий доход: %,.2f%n", getTotalIncome());
            System.out.printf("Общий расход: %,.2f%n", getTotalExpense());
            System.out.printf("Кол-во транзакций: %d%n", wallet.getTransactions().size());
            System.out.printf("Кол-во бюджетов: %d%n", wallet.getBudgets().size());

            showRecentTransactions(5);
        } else {
            System.out.println("Пользователь не авторизован");
        }
    }

    public void showCategoriesSummary() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        showFullStatistics();
    }

    // Метод для показа недавних транзакций
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
            System.out.printf("  %s %,.2f (%s) - %s%n",
                    typeSymbol, t.getAmount(), t.getCategory(), t.getDate().toLocalDate());
        }
    }

    // Метод для вывода статуса бюджетов
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

            String status = remaining >= 0 ? "Норма" : "Превышен";
            System.out.printf("  %s %s: Лимит %,.2f, Расходы %,.2f, Осталось %,.2f%n",
                    status, category, limit, expenses, remaining);
        }
    }
}