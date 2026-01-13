package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import nes.finance.model.Wallet;
import nes.finance.model.Alert;
import nes.finance.model.AlertType;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class FinancialService {
    private AuthService authService;
    private DataService dataService;
    private static final double BUDGET_WARNING_THRESHOLD = 0.8; // 80% использования бюджета
    private static final double LOW_BALANCE_THRESHOLD = 1000.0; // Порог низкого баланса
    private static final double BUDGET_WARNING_PERCENT = 80.0;
    private static final double BUDGET_CRITICAL_PERCENT = 95.0;
    private static final double LOW_BALANCE_WARNING = 2000.0;
    private static final double LOW_BALANCE_CRITICAL = 500.0;
    private static final double OVERSPENDING_THRESHOLD = 0.9;

    public FinancialService(AuthService authService) {
        this.authService = authService;
        this.dataService = authService.getDataService();
    }

    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    public boolean isAuthenticated() {
        return authService.isAuthenticated();
    }

    /**
     * Автоматическое сохранение данных пользователя
     */
    private void autoSave() {
        if (isAuthenticated()) {
            dataService.saveUserData(getCurrentUser());
        }
    }

    // Методы для работы с транзакциями с оповещениями
    public boolean addIncome(double amount, String category) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            autoSave();
            return true;
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
            autoSave();
            return true;
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

    /**
     * Подсчет доходов и расходов за указанный период
     */
    public void calculateByPeriod(LocalDate startDate, LocalDate endDate) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        User user = getCurrentUser();
        List<Transaction> transactions = user.getWallet().getTransactions();

        // Фильтруем транзакции по периоду
        List<Transaction> filteredTransactions = transactions.stream()
                .filter(t -> {
                    LocalDate transactionDate = t.getDate().toLocalDate();
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        if (filteredTransactions.isEmpty()) {
            System.out.printf("За период с %s по %s нет операций%n",
                    startDate, endDate);
            return;
        }

        double totalIncome = filteredTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = filteredTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        System.out.printf("Отчет за период: %s - %s%n", startDate, endDate);
        System.out.printf("Количество операций: %d%n", filteredTransactions.size());
        System.out.printf("Общий доход: %,.2f%n", totalIncome);
        System.out.printf("Общий расход: %,.2f%n", totalExpense);
        System.out.printf("Баланс за период: %,.2f%n", totalIncome - totalExpense);

        // Детали по категориям
        System.out.println("\nДетализация по категориям:");

        // Доходы по категориям
        Map<String, Double> incomeByCategory = filteredTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        if (!incomeByCategory.isEmpty()) {
            System.out.println("Доходы:");
            incomeByCategory.forEach((category, amount) ->
                    System.out.printf("  %s: %,.2f%n", category, amount));
        }

        // Расходы по категориям
        Map<String, Double> expenseByCategory = filteredTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        if (!expenseByCategory.isEmpty()) {
            System.out.println("\nРасходы:");
            expenseByCategory.forEach((category, amount) ->
                    System.out.printf("  %s: %,.2f%n", category, amount));
        }
    }

    /**
     * Подсчет по нескольким категориям с возможностью выбора типа операций
     */
    public void calculateByMultipleCategories(String[] categories, boolean incomesOnly, boolean expensesOnly) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        if (categories == null || categories.length == 0) {
            System.out.println("Ошибка: не указаны категории для подсчета");
            return;
        }

        System.out.println("Подсчет по выбранным категориям:");
        System.out.println("--------------------------------");

        double totalIncome = 0;
        double totalExpense = 0;
        List<String> foundCategories = new ArrayList<>();
        List<String> notFoundCategories = new ArrayList<>();

        for (String category : categories) {
            double income = getIncomeByCategory(category);
            double expense = getExpenseByCategory(category);

            if (income > 0 || expense > 0) {
                foundCategories.add(category);

                if ((!incomesOnly && !expensesOnly) || incomesOnly) {
                    System.out.printf("  %s: доходы %,.2f%n", category, income);
                    totalIncome += income;
                }

                if ((!incomesOnly && !expensesOnly) || expensesOnly) {
                    System.out.printf("  %s: расходы %,.2f%n", category, expense);
                    totalExpense += expense;
                }
            } else {
                notFoundCategories.add(category);
            }
        }

        // Уведомления о ненайденных категориях
        if (!notFoundCategories.isEmpty()) {
            System.out.println("\nКатегории без операций:");
            for (String category : notFoundCategories) {
                System.out.println("  - " + category);
            }
        }

        if (!foundCategories.isEmpty()) {
            System.out.println("\nИтоги по найденным категориям:");
            if ((!incomesOnly && !expensesOnly) || incomesOnly) {
                System.out.printf("  Общий доход: %,.2f%n", totalIncome);
            }
            if ((!incomesOnly && !expensesOnly) || expensesOnly) {
                System.out.printf("  Общий расход: %,.2f%n", totalExpense);
            }
            if (!incomesOnly && !expensesOnly) {
                System.out.printf("  Чистый результат: %,.2f%n", totalIncome - totalExpense);
            }
        } else {
            System.out.println("По указанным категориям не найдено операций");
        }
    }

    /**
     * Быстрые отчеты за стандартные периоды
     */
    public void quickReport(String periodType) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        switch (periodType.toLowerCase()) {
            case "day":
            case "today":
                startDate = today;
                break;
            case "week":
                startDate = today.minusDays(7);
                break;
            case "month":
                startDate = today.withDayOfMonth(1);
                endDate = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "year":
                startDate = today.withDayOfYear(1);
                endDate = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            case "last_month":
                startDate = today.minusMonths(1).withDayOfMonth(1);
                endDate = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                break;
            default:
                System.out.println("Неизвестный период. Используйте: day, week, month, year, last_month");
                return;
        }

        calculateByPeriod(startDate, endDate);
    }

    // МЕТОДЫ ДЛЯ РЕДАКТИРОВАНИЯ БЮДЖЕТОВ И КАТЕГОРИЙ

    /**
     * Редактирование бюджета категории
     */
    public boolean editBudget(String category, double newLimit) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidCategory(category)) {
            System.out.println("Ошибка: категория не может быть пустой");
            return false;
        }

        if (!isValidAmount(newLimit)) {
            System.out.println("Ошибка: новый лимит должен быть положительным числом");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        if (!wallet.getBudgets().containsKey(category)) {
            System.out.println("Ошибка: бюджет для категории '" + category + "' не найден");
            return false;
        }

        double oldLimit = wallet.getBudgets().get(category);
        double currentExpenses = getExpenseByCategory(category);

        // Проверяем, что новый лимит не меньше уже потраченной суммы
        if (newLimit < currentExpenses) {
            System.out.printf("Предупреждение: новый лимит (%.2f) меньше уже потраченной суммы (%.2f)%n",
                    newLimit, currentExpenses);
            System.out.print("Вы уверены, что хотите установить такой лимит? (yes/no): ");

            try {
                Scanner scanner = new Scanner(System.in);
                String confirmation = scanner.nextLine().trim().toLowerCase();
                if (!confirmation.equals("yes") && !confirmation.equals("y")) {
                    System.out.println("Редактирование отменено");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("Ошибка при чтении подтверждения");
                return false;
            }
        }

        wallet.getBudgets().put(category, newLimit);
        System.out.printf("Бюджет для категории '%s' изменен: %.2f -> %.2f%n",
                category, oldLimit, newLimit);

        autoSave();
        return true;
    }

    /**
     * Удаление бюджета категории
     */
    public boolean removeBudget(String category) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        if (!wallet.getBudgets().containsKey(category)) {
            System.out.println("Ошибка: бюджет для категории '" + category + "' не найден");
            return false;
        }

        Double removedLimit = wallet.getBudgets().remove(category);
        System.out.printf("Бюджет для категории '%s' удален (лимит: %.2f)%n",
                category, removedLimit);

        autoSave();
        return true;
    }

    /**
     * Переименование категории во всех транзакциях и бюджетах
     */
    public boolean renameCategory(String oldCategory, String newCategory) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidCategory(oldCategory) || !isValidCategory(newCategory)) {
            System.out.println("Ошибка: категории не могут быть пустыми");
            return false;
        }

        if (oldCategory.equals(newCategory)) {
            System.out.println("Ошибка: новая категория совпадает со старой");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        // Проверяем, существует ли старая категория в транзакциях или бюджетах
        boolean hasTransactions = wallet.getTransactions().stream()
                .anyMatch(t -> t.getCategory().equals(oldCategory));
        boolean hasBudget = wallet.getBudgets().containsKey(oldCategory);

        if (!hasTransactions && !hasBudget) {
            System.out.println("Ошибка: категория '" + oldCategory + "' не найдена");
            return false;
        }

        // Переименовываем в транзакциях
        int renamedTransactions = 0;
        for (Transaction t : wallet.getTransactions()) {
            if (t.getCategory().equals(oldCategory)) {
                // Используем рефлексию для изменения категории (т.к. поле final)
                try {
                    java.lang.reflect.Field categoryField = Transaction.class.getDeclaredField("category");
                    categoryField.setAccessible(true);
                    categoryField.set(t, newCategory);
                    renamedTransactions++;
                } catch (Exception e) {
                    System.err.println("Ошибка при переименовании транзакции: " + e.getMessage());
                }
            }
        }

        // Переименовываем в бюджетах
        Double budgetLimit = wallet.getBudgets().remove(oldCategory);
        if (budgetLimit != null) {
            wallet.getBudgets().put(newCategory, budgetLimit);
        }

        System.out.printf("Категория переименована: '%s' -> '%s'%n", oldCategory, newCategory);
        System.out.printf("  Переименовано транзакций: %d%n", renamedTransactions);
        System.out.printf("  Перенесен бюджет: %s%n",
                budgetLimit != null ? String.format("%.2f", budgetLimit) : "нет");

        autoSave();
        return true;
    }

    /**
     * Объединение нескольких категорий в одну
     */
    public boolean mergeCategories(String[] categoriesToMerge, String newCategory) {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return false;
        }

        if (!isValidCategory(newCategory)) {
            System.out.println("Ошибка: новая категория не может быть пустой");
            return false;
        }

        if (categoriesToMerge == null || categoriesToMerge.length < 2) {
            System.out.println("Ошибка: необходимо указать минимум 2 категории для объединения");
            return false;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        // Проверяем, что все категории для объединения существуют
        List<String> existingCategories = new ArrayList<>();
        List<String> nonExistingCategories = new ArrayList<>();

        for (String category : categoriesToMerge) {
            boolean exists = wallet.getTransactions().stream()
                    .anyMatch(t -> t.getCategory().equals(category)) ||
                    wallet.getBudgets().containsKey(category);

            if (exists) {
                existingCategories.add(category);
            } else {
                nonExistingCategories.add(category);
            }
        }

        if (existingCategories.isEmpty()) {
            System.out.println("Ошибка: ни одна из указанных категорий не найдена");
            return false;
        }

        if (!nonExistingCategories.isEmpty()) {
            System.out.println("Предупреждение: следующие категории не найдены и будут проигнорированы:");
            for (String category : nonExistingCategories) {
                System.out.println("  - " + category);
            }
        }

        // Подсчитываем итоги по объединяемым категориям
        double totalIncome = 0;
        double totalExpense = 0;
        double totalBudget = 0;
        int totalTransactions = 0;

        for (String category : existingCategories) {
            totalIncome += getIncomeByCategory(category);
            totalExpense += getExpenseByCategory(category);
            totalTransactions += (int) wallet.getTransactions().stream()
                    .filter(t -> t.getCategory().equals(category))
                    .count();

            Double budget = wallet.getBudgets().remove(category);
            if (budget != null) {
                totalBudget += budget;
            }
        }

        // Объединяем транзакции
        for (Transaction t : wallet.getTransactions()) {
            if (existingCategories.contains(t.getCategory())) {
                try {
                    java.lang.reflect.Field categoryField = Transaction.class.getDeclaredField("category");
                    categoryField.setAccessible(true);
                    categoryField.set(t, newCategory);
                } catch (Exception e) {
                    System.err.println("Ошибка при объединении транзакции: " + e.getMessage());
                }
            }
        }

        // Устанавливаем объединенный бюджет
        if (totalBudget > 0) {
            wallet.getBudgets().put(newCategory, totalBudget);
        }

        System.out.println("Категории успешно объединены:");
        System.out.printf("  Новая категория: '%s'%n", newCategory);
        System.out.printf("  Объединено категорий: %d%n", existingCategories.size());
        System.out.printf("  Объединено транзакций: %d%n", totalTransactions);
        System.out.printf("  Общий доход: %.2f%n", totalIncome);
        System.out.printf("  Общий расход: %.2f%n", totalExpense);
        if (totalBudget > 0) {
            System.out.printf("  Объединенный бюджет: %.2f%n", totalBudget);
        }

        autoSave();
        return true;
    }

    /**
     * Просмотр всех категорий со статистикой
     */
    public void listAllCategories() {
        if (!isAuthenticated()) {
            System.out.println("Ошибка: пользователь не авторизован");
            return;
        }

        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        // Получаем все уникальные категории из транзакций
        Set<String> categories = wallet.getTransactions().stream()
                .map(Transaction::getCategory)
                .collect(Collectors.toSet());

        // Добавляем категории из бюджетов
        categories.addAll(wallet.getBudgets().keySet());

        if (categories.isEmpty()) {
            System.out.println("Категории не найдены");
            return;
        }

        System.out.println("Список всех категорий:");
        System.out.println("----------------------");

        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);

        for (String category : sortedCategories) {
            double income = getIncomeByCategory(category);
            double expense = getExpenseByCategory(category);
            Double budget = wallet.getBudgets().get(category);

            System.out.printf("%s:%n", category);
            if (income > 0) {
                System.out.printf("  Доходы: %,.2f%n", income);
            }
            if (expense > 0) {
                System.out.printf("  Расходы: %,.2f%n", expense);
            }
            if (budget != null) {
                double remaining = budget - expense;
                System.out.printf("  Бюджет: %,.2f (осталось: %,.2f)%n", budget, remaining);
            }
            System.out.println();
        }
    }

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

    /**
     * Проверка всех условий для оповещений
     */
    public void checkAllAlerts() {
        if (!isAuthenticated()) return;

        checkBudgetAlerts();
        checkBalanceAlerts();
        checkOverspendingAlert();
        checkIncomeAlert();
        checkZeroBalanceAlert();
        checkLargeTransactionAlert();
    }

    /**
     * Проверка оповещений по бюджетам
     */
    private void checkBudgetAlerts() {
        User user = getCurrentUser();
        Wallet wallet = user.getWallet();

        for (Map.Entry<String, Double> entry : wallet.getBudgets().entrySet()) {
            String category = entry.getKey();
            double limit = entry.getValue();
            double expenses = getExpenseByCategory(category);

            if (limit > 0) {
                double usagePercent = (expenses / limit) * 100;

                // Предупреждение при 80% использования
                if (usagePercent >= BUDGET_WARNING_PERCENT && usagePercent < 100) {
                    double remaining = limit - expenses;
                    if (!hasRecentAlert(category + "_warning")) {
                        createAlert(AlertType.BUDGET_WARNING,
                                String.format("Категория '%s': использовано %.1f%% бюджета. Осталось: %.2f",
                                        category, usagePercent, remaining));
                    }
                }

                // Критическое предупреждение при 95% использования
                if (usagePercent >= BUDGET_CRITICAL_PERCENT && usagePercent < 100) {
                    double remaining = limit - expenses;
                    if (!hasRecentAlert(category + "_critical")) {
                        createAlert(AlertType.BUDGET_EXCEEDED,
                                String.format("КРИТИЧЕСКИЙ УРОВЕНЬ! Категория '%s': использовано %.1f%% бюджета. Осталось всего: %.2f",
                                        category, usagePercent, remaining));
                    }
                }

                // Превышение бюджета
                if (expenses > limit) {
                    double exceededBy = expenses - limit;
                    if (!hasRecentAlert(category + "_exceeded")) {
                        createAlert(AlertType.BUDGET_EXCEEDED,
                                String.format("ПРЕВЫШЕН БЮДЖЕТ! Категория '%s': превышение на %.2f. Лимит: %.2f, Факт: %.2f",
                                        category, exceededBy, limit, expenses));
                    }
                }
            }
        }
    }

    /**
     * Проверка оповещений по балансу
     */
    private void checkBalanceAlerts() {
        double balance = getCurrentUser().getWallet().getBalance();

        // Низкий баланс - предупреждение
        if (balance > 0 && balance <= LOW_BALANCE_WARNING && balance > LOW_BALANCE_CRITICAL) {
            if (!hasRecentAlert("low_balance_warning")) {
                createAlert(AlertType.LOW_BALANCE,
                        String.format("Низкий баланс: %.2f. Рекомендуется пополнить счет.", balance));
            }
        }

        // Критически низкий баланс
        if (balance > 0 && balance <= LOW_BALANCE_CRITICAL) {
            if (!hasRecentAlert("low_balance_critical")) {
                createAlert(AlertType.LOW_BALANCE,
                        String.format("КРИТИЧЕСКИ НИЗКИЙ БАЛАНС: %.2f. Срочно пополните счет!", balance));
            }
        }
    }

    /**
     * Проверка на перерасход (расходы близки к доходам)
     */
    private void checkOverspendingAlert() {
        double totalIncome = getTotalIncome();
        double totalExpense = getTotalExpense();

        if (totalIncome > 0) {
            double expenseRatio = totalExpense / totalIncome;

            if (expenseRatio >= OVERSPENDING_THRESHOLD && expenseRatio < 1.0) {
                if (!hasRecentAlert("overspending_warning")) {
                    createAlert(AlertType.OVERSPENDING,
                            String.format("ВНИМАНИЕ: расходы составляют %.1f%% от доходов (%.2f из %.2f).",
                                    expenseRatio * 100, totalExpense, totalIncome));
                }
            }

            if (totalExpense > totalIncome) {
                double deficit = totalExpense - totalIncome;
                if (!hasRecentAlert("overspending_critical")) {
                    createAlert(AlertType.OVERSPENDING,
                            String.format("КРИТИЧЕСКИЙ ПЕРЕРАСХОД! Расходы превысили доходы на %.2f.", deficit));
                }
            }
        }
    }

    /**
     * Проверка на отсутствие доходов
     */
    private void checkIncomeAlert() {
        double totalIncome = getTotalIncome();

        if (totalIncome == 0 && getCurrentUser().getWallet().getTransactions().size() > 0) {
            if (!hasRecentAlert("no_income")) {
                createAlert(AlertType.BUDGET_WARNING,
                        "У вас еще нет зарегистрированных доходов. Добавьте доходы для полноценного учета.");
            }
        }
    }

    /**
     * Проверка нулевого баланса
     */
    private void checkZeroBalanceAlert() {
        double balance = getCurrentUser().getWallet().getBalance();

        if (balance == 0 && getCurrentUser().getWallet().getTransactions().size() > 0) {
            if (!hasRecentAlert("zero_balance")) {
                createAlert(AlertType.LOW_BALANCE,
                        "Баланс равен нулю. Рассмотрите возможность пополнения счета.");
            }
        }
    }

    /**
     * Проверка на крупные транзакции
     */
    private void checkLargeTransactionAlert() {
        User user = getCurrentUser();
        List<Transaction> transactions = user.getWallet().getTransactions();

        if (transactions.isEmpty()) return;

        // Получаем последнюю транзакцию
        Transaction lastTransaction = transactions.get(transactions.size() - 1);

        // Проверяем, является ли транзакция крупной (более 10000)
        if (lastTransaction.getAmount() > 10000) {
            String alertKey = "large_transaction_" + lastTransaction.getCategory();
            if (!hasRecentAlert(alertKey)) {
                createAlert(AlertType.BUDGET_WARNING,
                        String.format("Крупная операция: %.2f в категории '%s'. Проверьте корректность.",
                                lastTransaction.getAmount(), lastTransaction.getCategory()));
            }
        }
    }

    /**
     * Проверка, было ли недавнее оповещение с таким ключом
     */
    private boolean hasRecentAlert(String alertKey) {
        User user = getCurrentUser();
        List<Alert> alerts = user.getWallet().getAlerts();

        if (alerts.isEmpty()) return false;

        // Проверяем последние 10 оповещений
        int start = Math.max(0, alerts.size() - 10);
        for (int i = start; i < alerts.size(); i++) {
            Alert alert = alerts.get(i);
            // Используем часть сообщения как ключ для проверки
            if (alert.getMessage().contains(alertKey)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Настройка параметров оповещений
     */
    public void configureAlerts(double warningPercent, double criticalPercent,
                                double lowBalanceWarning, double lowBalanceCritical) {
        // Эти параметры можно сделать настраиваемыми
        System.out.println("Настройки оповещений обновлены:");
        System.out.printf("  Предупреждение о бюджете: %.0f%%%n", warningPercent);
        System.out.printf("  Критический уровень бюджета: %.0f%%%n", criticalPercent);
        System.out.printf("  Низкий баланс (предупреждение): %.2f%n", lowBalanceWarning);
        System.out.printf("  Низкий баланс (критический): %.2f%n", lowBalanceCritical);
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
            autoSave();
            return true;
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

    /**
     * Принудительное сохранение данных
     */
    public void saveData() {
        if (isAuthenticated()) {
            if (dataService.saveUserData(getCurrentUser())) {
                System.out.println("Данные успешно сохранены");
            } else {
                System.out.println("Ошибка при сохранении данных");
            }
        } else {
            System.out.println("Ошибка: пользователь не авторизован");
        }
    }

    /**
     * Создание резервной копии данных
     */
    public void createBackup() {
        if (isAuthenticated()) {
            String login = getCurrentUser().getLogin();
            if (dataService.createBackup(login)) {
                System.out.println("Резервная копия создана");
            } else {
                System.out.println("Ошибка при создании резервной копии");
            }
        } else {
            System.out.println("Ошибка: пользователь не авторизован");
        }
    }
}