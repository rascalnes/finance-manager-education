package nes.finance.cli;

import nes.finance.service.AuthService;
import nes.finance.service.FinancialService;
import java.util.Scanner;

public class CLIApplication {
    private AuthService authService;
    private FinancialService financialService;
    private Scanner scanner;
    private boolean isRunning;

    public CLIApplication() {
        this.authService = new AuthService();
        this.financialService = new FinancialService(authService);
        this.scanner = new Scanner(System.in);
        this.isRunning = true;
    }

    public void run() {
        System.out.println("=== Система управления личными финансами ===");
        showHelp();

        while (isRunning) {
            showPrompt();
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "login":
                        handleLogin(parts);
                        break;
                    case "register":
                        handleRegister(parts);
                        break;
                    case "add_income":
                        handleAddIncome(parts);
                        break;
                    case "add_expense":
                        handleAddExpense(parts);
                        break;
                    case "set_budget":
                        handleSetBudget(parts);
                        break;
                    case "info":
                        handleInfo();
                        break;
                    case "stats":
                        handleStats();
                        break;
                    case "budgets":
                        handleBudgets();
                        break;
                    case "calculate":
                        handleCalculate(parts);
                        break;
                    case "exit":
                        handleExit();
                        break;
                    case "help":
                        showHelp();
                        break;
                    default:
                        System.out.println("Неизвестная команда. Введите 'help' для списка команд.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка выполнения команды: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private void showPrompt() {
        if (financialService.isAuthenticated()) {
            System.out.printf("[%s] > ", financialService.getCurrentUser().getLogin());
        } else {
            System.out.print("> ");
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Использование: login [логин] [пароль]");
            return;
        }

        String login = parts[1];
        String password = parts[2];

        if (authService.login(login, password)) {
            System.out.println("Успешный вход! Добро пожаловать, " + login);
        } else {
            System.out.println("Ошибка: неверный логин или пароль");
        }
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Использование: register [логин] [пароль]");
            return;
        }

        String login = parts[1];
        String password = parts[2];

        if (authService.register(login, password)) {
            System.out.println("Пользователь " + login + " успешно зарегистрирован");
        } else {
            System.out.println("Ошибка: пользователь с таким логином уже существует");
        }
    }

    private void handleAddIncome(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 3) {
            System.out.println("Использование: add_income [сумма] [категория]");
            return;
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            String category = parts[2];

            financialService.addIncome(amount, category);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом");
        }
    }

    private void handleAddExpense(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 3) {
            System.out.println("Использование: add_expense [сумма] [категория]");
            return;
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            String category = parts[2];

            financialService.addExpense(amount, category);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом");
        }
    }

    private void handleSetBudget(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 3) {
            System.out.println("Использование: set_budget [категория] [лимит]");
            return;
        }

        try {
            String category = parts[1];
            double limit = Double.parseDouble(parts[2]);

            financialService.setBudget(category, limit);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: лимит должен быть числом");
        }
    }

    private void handleInfo() {
        financialService.showUserInfo();
    }

    private void handleStats() {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        financialService.showFullStatistics();
    }

    private void handleBudgets() {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        financialService.showBudgetStatus();
    }

    // Новая команда для подсчета по выбранным категориям
    private void handleCalculate(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 2) {
            System.out.println("Использование: calculate [категория1] [категория2] ...");
            return;
        }

        String[] categories = new String[parts.length - 1];
        System.arraycopy(parts, 1, categories, 0, categories.length);

        financialService.calculateSelectedCategories(categories);
    }

    private void handleExit() {
        System.out.println("Выход из приложения...");
        isRunning = false;
    }

    private void showHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  login [логин] [пароль] - вход в систему");
        System.out.println("  register [логин] [пароль] - регистрация нового пользователя");
        System.out.println("  add_income [сумма] [категория] - добавить доход");
        System.out.println("  add_expense [сумма] [категория] - добавить расход");
        System.out.println("  set_budget [категория] [лимит] - установить бюджет для категории");
        System.out.println("  info - краткая информация о пользователе");
        System.out.println("  stats - полная финансовая статистика");
        System.out.println("  budgets - статус бюджетов с индикаторами");
        System.out.println("  calculate [кат1] [кат2] ... - подсчет по выбранным категориям");
        System.out.println("  exit - выход из приложения");
        System.out.println("  help - показать эту справку");
    }
}