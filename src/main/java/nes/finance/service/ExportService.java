package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Transaction;
import nes.finance.model.TransactionType;
import nes.finance.model.Wallet;
import nes.finance.model.Alert;
import nes.finance.model.AlertType;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ExportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Экспорт транзакций в CSV файл
     */
    public boolean exportTransactionsToCSV(User user, String filePath) {
        if (user == null) {
            System.out.println("Ошибка: пользователь не указан");
            return false;
        }

        Wallet wallet = user.getWallet();
        List<Transaction> transactions = wallet.getTransactions();

        if (transactions.isEmpty()) {
            System.out.println("Нет транзакций для экспорта");
            return false;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Заголовок CSV
            writer.println("Дата,Тип,Категория,Сумма,Баланс после операции");

            double runningBalance = 0;

            for (Transaction t : transactions) {
                String date = t.getDate().format(DATE_FORMATTER);
                String type = t.getType() == TransactionType.INCOME ? "Доход" : "Расход";
                String category = t.getCategory();
                double amount = t.getAmount();

                // Обновляем баланс
                if (t.getType() == TransactionType.INCOME) {
                    runningBalance += amount;
                } else {
                    runningBalance -= amount;
                }

                writer.printf("%s,%s,%s,%.2f,%.2f%n",
                        date, type, category, amount, runningBalance);
            }

            System.out.printf("Транзакции экспортированы в файл: %s%n", filePath);
            System.out.printf("Количество записей: %d%n", transactions.size());
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка при экспорте в CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Экспорт бюджетов в CSV файл
     */
    public boolean exportBudgetsToCSV(User user, String filePath) {
        if (user == null) {
            System.out.println("Ошибка: пользователь не указан");
            return false;
        }

        Wallet wallet = user.getWallet();
        Map<String, Double> budgets = wallet.getBudgets();

        if (budgets.isEmpty()) {
            System.out.println("Нет бюджетов для экспорта");
            return false;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Заголовок CSV
            writer.println("Категория,Лимит,Текущие расходы,Остаток,Процент использования");

            for (Map.Entry<String, Double> entry : budgets.entrySet()) {
                String category = entry.getKey();
                double limit = entry.getValue();

                // Подсчитываем расходы по категории
                double expenses = wallet.getTransactions().stream()
                        .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory().equals(category))
                        .mapToDouble(Transaction::getAmount)
                        .sum();

                double remaining = limit - expenses;
                double usagePercent = limit > 0 ? (expenses / limit) * 100 : 0;

                writer.printf("%s,%.2f,%.2f,%.2f,%.1f%%%n",
                        category, limit, expenses, remaining, usagePercent);
            }

            System.out.printf("Бюджеты экспортированы в файл: %s%n", filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка при экспорте бюджетов в CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Экспорт всей финансовой информации в JSON файл
     */
    public boolean exportToJSON(User user, String filePath) {
        if (user == null) {
            System.out.println("Ошибка: пользователь не указан");
            return false;
        }

        Wallet wallet = user.getWallet();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("{");
            writer.printf("  \"user\": \"%s\",%n", user.getLogin());
            writer.printf("  \"balance\": %.2f,%n", wallet.getBalance());
            writer.printf("  \"last_export\": \"%s\",%n", LocalDateTime.now().format(DATE_FORMATTER));

            // Транзакции
            writer.println("  \"transactions\": [");
            List<Transaction> transactions = wallet.getTransactions();
            for (int i = 0; i < transactions.size(); i++) {
                Transaction t = transactions.get(i);
                writer.println("    {");
                writer.printf("      \"date\": \"%s\",%n", t.getDate().format(DATE_FORMATTER));
                writer.printf("      \"type\": \"%s\",%n", t.getType().toString());
                writer.printf("      \"category\": \"%s\",%n", t.getCategory());
                writer.printf("      \"amount\": %.2f%n", t.getAmount());
                writer.print(i < transactions.size() - 1 ? "    }," : "    }");
                writer.println();
            }
            writer.println("  ],");

            // Бюджеты
            writer.println("  \"budgets\": {");
            Map<String, Double> budgets = wallet.getBudgets();
            int budgetIndex = 0;
            for (Map.Entry<String, Double> entry : budgets.entrySet()) {
                writer.printf("    \"%s\": %.2f", entry.getKey(), entry.getValue());
                writer.print(budgetIndex < budgets.size() - 1 ? "," : "");
                writer.println();
                budgetIndex++;
            }
            writer.println("  },");

            // Общая статистика
            writer.println("  \"statistics\": {");
            double totalIncome = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .mapToDouble(Transaction::getAmount)
                    .sum();
            double totalExpense = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            writer.printf("    \"total_income\": %.2f,%n", totalIncome);
            writer.printf("    \"total_expense\": %.2f,%n", totalExpense);
            writer.printf("    \"net_balance\": %.2f,%n", totalIncome - totalExpense);
            writer.printf("    \"transaction_count\": %d%n", transactions.size());
            writer.println("  }");

            writer.println("}");

            System.out.printf("Данные экспортированы в JSON файл: %s%n", filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка при экспорте в JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Импорт транзакций из CSV файла
     */
    public boolean importTransactionsFromCSV(User user, String filePath) {
        if (user == null) {
            System.out.println("Ошибка: пользователь не указан");
            return false;
        }

        Wallet wallet = user.getWallet();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Ошибка: файл не найден");
            return false;
        }

        int importedCount = 0;
        int skippedCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Пропускаем заголовок
                }

                String[] parts = line.split(",");
                if (parts.length < 4) {
                    skippedCount++;
                    continue;
                }

                try {
                    String typeStr = parts[0].trim();
                    String category = parts[1].trim();
                    double amount = Double.parseDouble(parts[2].trim());
                    String dateStr = parts.length > 3 ? parts[3].trim() : "";

                    TransactionType type;
                    if (typeStr.equalsIgnoreCase("доход") || typeStr.equalsIgnoreCase("income")) {
                        type = TransactionType.INCOME;
                    } else if (typeStr.equalsIgnoreCase("расход") || typeStr.equalsIgnoreCase("expense")) {
                        type = TransactionType.EXPENSE;
                    } else {
                        skippedCount++;
                        continue;
                    }

                    // Создаем транзакцию
                    Transaction transaction;
                    if (!dateStr.isEmpty()) {
                        try {
                            LocalDateTime date = LocalDateTime.parse(dateStr, DATE_FORMATTER);
                            transaction = new Transaction(type, amount, category, date);
                        } catch (Exception e) {
                            transaction = new Transaction(type, amount, category);
                        }
                    } else {
                        transaction = new Transaction(type, amount, category);
                    }

                    wallet.getTransactions().add(transaction);

                    // Обновляем баланс
                    if (type == TransactionType.INCOME) {
                        wallet.setBalance(wallet.getBalance() + amount);
                    } else {
                        // Проверяем, достаточно ли средств
                        if (wallet.getBalance() >= amount) {
                            wallet.setBalance(wallet.getBalance() - amount);
                        } else {
                            System.out.printf("Предупреждение: недостаточно средств для импорта расхода %.2f%n", amount);
                            skippedCount++;
                            continue;
                        }
                    }

                    importedCount++;

                } catch (NumberFormatException e) {
                    skippedCount++;
                }
            }

            System.out.printf("Импорт завершен. Успешно: %d, Пропущено: %d%n", importedCount, skippedCount);
            return importedCount > 0;

        } catch (IOException e) {
            System.err.println("Ошибка при импорте из CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Экспорт отчета в текстовый файл
     */
    public boolean exportReportToText(User user, String filePath) {
        if (user == null) {
            System.out.println("Ошибка: пользователь не указан");
            return false;
        }

        Wallet wallet = user.getWallet();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("=".repeat(60));
            writer.println("ФИНАНСОВЫЙ ОТЧЕТ");
            writer.println("=".repeat(60));
            writer.println();
            writer.printf("Пользователь: %s%n", user.getLogin());
            writer.printf("Дата отчета: %s%n", LocalDateTime.now().format(DATE_FORMATTER));
            writer.printf("Текущий баланс: %.2f%n", wallet.getBalance());
            writer.println();

            // Общая статистика
            double totalIncome = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .mapToDouble(Transaction::getAmount)
                    .sum();
            double totalExpense = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            writer.println("ОБЩАЯ СТАТИСТИКА");
            writer.println("-".repeat(40));
            writer.printf("Всего доходов: %.2f%n", totalIncome);
            writer.printf("Всего расходов: %.2f%n", totalExpense);
            writer.printf("Чистый баланс: %.2f%n", totalIncome - totalExpense);
            writer.printf("Количество транзакций: %d%n", wallet.getTransactions().size());
            writer.println();

            // Доходы по категориям
            writer.println("ДОХОДЫ ПО КАТЕГОРИЯМ");
            writer.println("-".repeat(40));
            Map<String, Double> incomeByCategory = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .collect(java.util.stream.Collectors.groupingBy(
                            Transaction::getCategory,
                            java.util.stream.Collectors.summingDouble(Transaction::getAmount)
                    ));

            if (incomeByCategory.isEmpty()) {
                writer.println("Нет данных");
            } else {
                incomeByCategory.forEach((category, amount) ->
                        writer.printf("  %-20s %10.2f%n", category + ":", amount));
            }
            writer.println();

            // Расходы по категориям
            writer.println("РАСХОДЫ ПО КАТЕГОРИЯМ");
            writer.println("-".repeat(40));
            Map<String, Double> expenseByCategory = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(java.util.stream.Collectors.groupingBy(
                            Transaction::getCategory,
                            java.util.stream.Collectors.summingDouble(Transaction::getAmount)
                    ));

            if (expenseByCategory.isEmpty()) {
                writer.println("Нет данных");
            } else {
                expenseByCategory.forEach((category, amount) ->
                        writer.printf("  %-20s %10.2f%n", category + ":", amount));
            }
            writer.println();

            // Бюджеты
            writer.println("БЮДЖЕТЫ");
            writer.println("-".repeat(40));
            if (wallet.getBudgets().isEmpty()) {
                writer.println("Бюджеты не установлены");
            } else {
                for (Map.Entry<String, Double> entry : wallet.getBudgets().entrySet()) {
                    String category = entry.getKey();
                    double limit = entry.getValue();
                    double expenses = expenseByCategory.getOrDefault(category, 0.0);
                    double remaining = limit - expenses;
                    double percent = limit > 0 ? (expenses / limit) * 100 : 0;

                    writer.printf("  %s:%n", category);
                    writer.printf("    Лимит: %.2f%n", limit);
                    writer.printf("    Расходы: %.2f (%.1f%%)%n", expenses, percent);
                    writer.printf("    Остаток: %.2f%n", remaining);
                    writer.println();
                }
            }

            writer.println("=".repeat(60));
            writer.println("КОНЕЦ ОТЧЕТА");

            System.out.printf("Отчет экспортирован в файл: %s%n", filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка при экспорте отчета: " + e.getMessage());
            return false;
        }
    }
}