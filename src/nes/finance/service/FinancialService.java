package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import nes.finance.model.Wallet;
import nes.finance.model.Alert;
import nes.finance.model.AlertType;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FinancialService {
    private AuthService authService;
    private static final double BUDGET_WARNING_THRESHOLD = 0.8; // 80% использования бюджета
    private static final double LOW_BALANCE_THRESHOLD = 1000.0; // Порог низкого баланса

    public FinancialService(AuthService authService) {
        this.authService = authService;
    }

    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return authService.isAuthenticated();
    }

    // Методы для работы с транзакциями с оповещениями
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

        // Проверяем общее финансовое состояние после добавления дохода
        checkOverallFinancialHealth();

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
            // Создаем оповещение о недостатке средств
            createAlert(AlertType.LOW_BALANCE,
                    String.format("Недостаточно средств для операции. Баланс: %.2f, Требуется: %.2f",
                            wallet.getBalance(), amount));
            System.out.println("Ошибка: недостаточно средств на счете");
            return false;
        }

        Transaction transaction = new Transaction(TransactionType.EXPENSE, amount, category);
        wallet.getTransactions().add(transaction);
        wallet.setBalance(wallet.getBalance() - amount);

        // Проверяем бюджеты и общее финансовое состояние
        checkBudgetExceeded(category, amount);
        checkOverallFinancialHealth();
        checkLowBalance();

        return true;
    }

    // НОВЫЕ МЕТОДЫ ДЛЯ СИСТЕМЫ ОПОВЕЩЕНИЙ

    // Создание оповещения
    private void createAlert(AlertType type, String message) {
        if (!isAuthenticated()) return;

        User user = getCurrentUser();
        Alert alert = new Alert(type, message);
        user.getWallet().addAlert(alert);

        // Немедленный вывод критических оповещений
        if (type == AlertType.BUDGET_EXCEEDED || type == AlertType.OVERSPENDING) {
            System.out.printf("🚨 ОПОВЕЩЕНИЕ: %s%n", message);
        }
    }

    // Проверка превышения бюджета с улучшенной логикой
    private void checkBudgetExceeded(String category, double newExpense) {
        Double budgetLimit = getBudget(category);
        if (budgetLimit == null) return;

        double currentExpenses = getExpenseByCategory(category);
        double budgetUsage = currentExpenses / budgetLimit;

        // Предупреждение при достижении 80% бюджета
        if (budgetUsage >= BUDGET_WARNING_THRESHOLD && budgetUsage < 1.0) {
            double remaining = budgetLimit - currentExpenses;
            createAlert(AlertType.BUDGET_WARNING,
                    String.format("Категория '%s': использовано %.0f%% бюджета. Осталось: %.2f",
                            category, budgetUsage * 100, remaining));
        }

        // Оповещение о превышении бюджета
        if (currentExpenses > budgetLimit) {
            double exceededBy = currentExpenses - budgetLimit;
            createAlert(AlertType.BUDGET_EXCEEDED,
                    String.format("Превышен бюджет для категории '%s'! Лимит: %.2f, Факт: %.2f (превышение: %.2f)",
                            category, budgetLimit, currentExpenses, exceededBy));
        }
    }

    // Проверка общего финансового здоровья
    private void checkOverallFinancialHealth() {
        double totalIncome = getTotalIncome();
        double totalExpense = getTotalExpense();

        if (totalExpense > totalIncome) {
            double deficit = totalExpense - totalIncome;
            createAlert(AlertType.OVERSPENDING,
                    String.format("Расходы превысили доходы! Дефицит: %.2f. Доходы: %.2f, Расходы: %.2f",
                            deficit, totalIncome, totalExpense));
        }
    }

    // Проверка низкого баланса
    private void checkLowBalance() {
        double balance = getCurrentUser().getWallet().getBalance();
        if (balance < LOW_BALANCE_THRESHOLD) {
            createAlert(AlertType.LOW_BALANCE,
                    String.format("Низкий баланс: %.2f. Рекомендуется пополнить счет.", balance));
        }
    }

    // Методы для работы с оповещениями
    public void showAlerts() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        User user = getCurrentUser();
        List<Alert> unreadAlerts = user.getWallet().getUnreadAlerts();
        List<Alert> allAlerts = user.getWallet().getAlerts();

        if (allAlerts.isEmpty()) {
            System.out.println("Оповещений нет");
            return;
        }

        System.out.println("=== ОПОВЕЩЕНИЯ ===");

        // Показываем непрочитанные оповещения
        if (!unreadAlerts.isEmpty()) {
            System.out.println("Новые оповещения:");
            for (int i = 0; i < unreadAlerts.size(); i++) {
                Alert alert = unreadAlerts.get(i);
                System.out.printf("%d. %s%n", i + 1, alert.getMessage());
            }
        }

        // Показываем все оповещения
        System.out.println("\nВсе оповещения:");
        for (int i = 0; i < allAlerts.size(); i++) {
            Alert alert = allAlerts.get(i);
            String status = alert.isRead() ? "📭" : "📬";
            System.out.printf("%d. %s %s - %s%n", i + 1, status, alert.getMessage(), alert.getTimestamp());
        }

        // Помечаем все как прочитанные после показа
        user.getWallet().markAllAlertsAsRead();
    }

    public void showUnreadAlertCount() {
        if (!isAuthenticated()) return;

        int unreadCount = getCurrentUser().getWallet().getUnreadAlertCount();
        if (unreadCount > 0) {
            System.out.printf("📬 У вас %d непрочитанных оповещений. Введите 'alerts' для просмотра.%n", unreadCount);
        }
    }

    public void clearAlerts() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        User user = getCurrentUser();
        user.getWallet().getAlerts().clear();
        System.out.println("Все оповещения очищены");
    }

    // Обновленный метод showUserInfo с отображением оповещений
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
            System.out.printf("Непрочитанных оповещений: %d%n", wallet.getUnreadAlertCount());

            // Показываем счетчик непрочитанных оповещений
            showUnreadAlertCount();

            showRecentTransactions(3);
        } else {
            System.out.println("Пользователь не авторизован");
        }
    }

    // Обновленный метод showFullStatistics с проверкой оповещений
    public void showFullStatistics() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        // Проверяем финансовое здоровье перед показом статистики
        checkOverallFinancialHealth();
        checkLowBalance();

        System.out.println("=== ФИНАНСОВАЯ СТАТИСТИКА ===");

        double totalIncome = getTotalIncome();
        double totalExpense = getTotalExpense();

        System.out.printf("Общий доход: %,.1f%n", totalIncome);
        System.out.printf("Общие расходы: %,.1f%n", totalExpense);
        System.out.printf("Текущий баланс: %,.1f%n", getCurrentUser().getWallet().getBalance());
        System.out.println();

        showIncomeByCategories();
        System.out.println();

        showExpensesByCategories();
        System.out.println();

        showDetailedBudgetStatus();

        // Показываем непрочитанные оповещения
        showUnreadAlertCount();
    }

    // Остальные методы остаются без изменений (из Этапа 4)
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

    // Методы валидации
    private boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    private boolean isValidCategory(String category) {
        return category != null && !category.trim().isEmpty();
    }

    // Методы для получения данных (без изменений из Этапа 4)
    public Double getBudget(String category) {
        if (!isAuthenticated()) return null;
        return getCurrentUser().getWallet().getBudgets().get(category);
    }

    public Map<String, Double> getAllBudgets() {
        if (!isAuthenticated()) return Map.of();
        return getCurrentUser().getWallet().getBudgets();
    }

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

    // Остальные методы статистики (без изменений из Этапа 4)
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

    public void showDetailedBudgetStatus() {
        Map<String, Double> budgets = getAllBudgets();
        if (budgets.isEmpty()) {
            System.out.println("Бюджеты по категориям: не установлены");
            return;
        }

        System.out.println("Бюджет по категориям:");
        budgets.entrySet().stream()
                .sorted((a, b) -> {
                    double remainingA = a.getValue() - getExpenseByCategory(a.getKey());
                    double remainingB = b.getValue() - getExpenseByCategory(b.getKey());
                    return Double.compare(remainingA, remainingB);
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

            String status = remaining >= 0 ? "✅" : "⚠️";
            System.out.printf("  %s %s: Лимит %,.2f, Расходы %,.2f, Осталось %,.2f%n",
                    status, category, limit, expenses, remaining);
        }
    }
}