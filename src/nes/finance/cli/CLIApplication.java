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

            String[] parts = input.split("\\s+", 3);
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "login":
                        handleLogin(parts);
                        break;
                    case "register":
                        handleRegister(parts);
                        break;
                    case "exit":
                        handleExit();
                        break;
                    case "info":
                        handleInfo();
                        break;
                    case "help":
                        showHelp();
                        break;
                    default:
                        System.out.println("Неизвестная команда. Введите 'help' для вывода списка команд.");
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

    private void handleExit() {
        System.out.println("Выход из приложения...");
        isRunning = false;
    }

    private void handleInfo() {
        financialService.showUserInfo();
    }

    private void showHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  login [логин] [пароль] - вход в систему");
        System.out.println("  register [логин] [пароль] - регистрация нового пользователя");
        System.out.println("  info - информация о текущем пользователе");
        System.out.println("  exit - выход из приложения");
        System.out.println("  help - показать эту справку");
    }
}