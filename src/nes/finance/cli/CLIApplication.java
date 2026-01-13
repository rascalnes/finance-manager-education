package nes.finance.cli;

import nes.finance.service.AuthService;
import nes.finance.service.FinancialService;
import nes.finance.service.ExportService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class CLIApplication {
    private AuthService authService;
    private FinancialService financialService;
    private ExportService exportService;
    private Scanner scanner;
    private boolean isRunning;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CLIApplication() {
        this.authService = new AuthService();
        this.financialService = new FinancialService(authService);
        this.exportService = new ExportService();
        this.scanner = new Scanner(System.in);
        this.isRunning = true;
    }

    public void run() {
        printWelcomeMessage();
        printMainHelp();

        while (isRunning) {
            showPrompt();
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        scanner.close();
        System.out.println("До свидания!");
    }

    private void printWelcomeMessage() {
        System.out.println("=".repeat(60));
        System.out.println("        СИСТЕМА УПРАВЛЕНИЯ ЛИЧНЫМИ ФИНАНСАМИ");
        System.out.println("=".repeat(60));
        System.out.println("Введите 'help' для списка команд или 'help [команда]' для справки");
        System.out.println();
    }

    private void printMainHelp() {
        System.out.println("ОСНОВНЫЕ КОМАНДЫ:");
        System.out.println("  account    - Управление аккаунтом (регистрация, вход, выход)");
        System.out.println("  money      - Работа с финансами (доходы, расходы)");
        System.out.println("  budget     - Управление бюджетами");
        System.out.println("  report     - Отчеты и статистика");
        System.out.println("  category   - Управление категориями");
        System.out.println("  export     - Экспорт данных");
        System.out.println("  alert      - Оповещения и настройки");
        System.out.println("  system     - Системные команды");
        System.out.println();
        System.out.println("Введите 'help [группа]' для подробной справки по группе команд");
    }

    private void showPrompt() {
        if (financialService.isAuthenticated()) {
            String username = financialService.getCurrentUser().getLogin();
            double balance = financialService.getCurrentUser().getWallet().getBalance();
            int alerts = financialService.getCurrentUser().getWallet().getUnreadAlertCount();

            String alertIndicator = alerts > 0 ? String.format(" [%d alerts]", alerts) : "";
            System.out.printf("%s [Balance: %.2f]%s > ", username, balance, alertIndicator);
        } else {
            System.out.print("finance > ");
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "help":
                    handleHelp(parts);
                    break;
                case "login":
                    handleLogin(parts);
                    break;
                case "register":
                case "reg":
                    handleRegister(parts);
                    break;
                case "logout":
                    handleLogout();
                    break;
                case "exit":
                case "quit":
                    handleExit();
                    break;

                // Команды работы с финансами
                case "add":
                    handleAdd(parts);
                    break;
                case "income":
                    handleIncome(parts);
                    break;
                case "expense":
                case "spend":
                    handleExpense(parts);
                    break;

                // Команды работы с бюджетами
                case "budget":
                    handleBudget(parts);
                    break;
                case "budgets":
                    handleBudgets();
                    break;

                // Команды отчетов
                case "report":
                case "stats":
                    handleReport(parts);
                    break;
                case "summary":
                    handleSummary();
                    break;
                case "period":
                    handlePeriod(parts);
                    break;

                // Команды категорий
                case "categories":
                case "cats":
                    handleCategories();
                    break;
                case "rename":
                    handleRename(parts);
                    break;
                case "merge":
                    handleMerge(parts);
                    break;

                // Команды экспорта
                case "export":
                    handleExport(parts);
                    break;
                case "import":
                    handleImport(parts);
                    break;

                // Команды оповещений
                case "alerts":
                    handleAlerts();
                    break;
                case "check":
                    handleCheckAlerts();
                    break;

                // Системные команды
                case "save":
                    handleSave();
                    break;
                case "backup":
                    handleBackup();
                    break;
                case "clear":
                    handleClear();
                    break;

                default:
                    System.out.println("Неизвестная команда: " + command);
                    System.out.println("Введите 'help' для списка доступных команд");
            }
        } catch (Exception e) {
            System.out.println("Ошибка выполнения команды: " + e.getMessage());
            System.out.println("Использование: " + getCommandUsage(command));
        }
    }

    private void handleHelp(String[] parts) {
        if (parts.length == 1) {
            printMainHelp();
        } else {
            String topic = parts[1].toLowerCase();
            printDetailedHelp(topic);
        }
    }

    private void printDetailedHelp(String topic) {
        System.out.println();
        System.out.println("СПРАВКА: " + topic.toUpperCase());
        System.out.println("-".repeat(60));

        switch (topic) {
            case "account":
                System.out.println("Команды управления аккаунтом:");
                System.out.println("  login <username> <password>     - Вход в систему");
                System.out.println("  register <username> <password>  - Регистрация нового пользователя");
                System.out.println("  logout                          - Выход из системы");
                System.out.println("  exit                            - Выход из приложения");
                break;

            case "money":
                System.out.println("Команды работы с финансами:");
                System.out.println("  income <amount> <category>      - Добавить доход");
                System.out.println("  expense <amount> <category>     - Добавить расход");
                System.out.println("  add income <amount> <category>  - Альтернативный синтаксис");
                System.out.println("  add expense <amount> <category> - Альтернативный синтаксис");
                System.out.println("Пример: income 5000 Зарплата");
                System.out.println("Пример: expense 1500 Продукты");
                break;

            case "budget":
                System.out.println("Команды управления бюджетами:");
                System.out.println("  budget set <category> <limit>   - Установить бюджет");
                System.out.println("  budget edit <category> <limit>  - Изменить бюджет");
                System.out.println("  budget remove <category>        - Удалить бюджет");
                System.out.println("  budgets                         - Показать все бюджеты");
                System.out.println("Пример: budget set Продукты 10000");
                break;

            case "report":
                System.out.println("Команды отчетов и статистики:");
                System.out.println("  report full                     - Полный отчет");
                System.out.println("  report today                    - Отчет за сегодня");
                System.out.println("  report week                     - Отчет за неделю");
                System.out.println("  report month                    - Отчет за месяц");
                System.out.println("  period <start> <end>            - Отчет за период");
                System.out.println("  summary                         - Краткая сводка");
                System.out.println("Формат даты: YYYY-MM-DD");
                System.out.println("Пример: period 2024-01-01 2024-01-31");
                break;

            case "category":
                System.out.println("Команды управления категориями:");
                System.out.println("  categories                      - Список всех категорий");
                System.out.println("  rename <old> <new>              - Переименовать категорию");
                System.out.println("  merge <cat1> <cat2> ... <new>   - Объединить категории");
                System.out.println("Пример: rename Еда Продукты");
                System.out.println("Пример: merge Кафе Ресторан Развлечения Еда_вне_дома");
                break;

            case "export":
                System.out.println("Команды экспорта данных:");
                System.out.println("  export csv                      - Экспорт транзакций в CSV");
                System.out.println("  export budgets                  - Экспорт бюджетов в CSV");
                System.out.println("  export json                     - Экспорт всех данных в JSON");
                System.out.println("  export report                   - Экспорт отчета в текстовый файл");
                System.out.println("  import csv <file>               - Импорт транзакций из CSV");
                break;

            case "alert":
                System.out.println("Команды оповещений:");
                System.out.println("  alerts                          - Показать все оповещения");
                System.out.println("  check                           - Проверить все условия для оповещений");
                System.out.println("  clear alerts                    - Очистить все оповещения");
                break;

            case "system":
                System.out.println("Системные команды:");
                System.out.println("  save                            - Сохранить данные");
                System.out.println("  backup                          - Создать резервную копию");
                System.out.println("  clear                           - Очистить экран");
                break;

            default:
                System.out.println("Раздел справки не найден: " + topic);
                System.out.println("Доступные разделы: account, money, budget, report, category, export, alert, system");
        }
        System.out.println();
    }

    private String getCommandUsage(String command) {
        switch (command) {
            case "login": return "login <username> <password>";
            case "register": return "register <username> <password>";
            case "income": return "income <amount> <category>";
            case "expense": return "expense <amount> <category>";
            case "budget": return "budget set|edit|remove <category> [limit]";
            case "period": return "period <start_date> <end_date>";
            case "rename": return "rename <old_category> <new_category>";
            case "merge": return "merge <cat1> <cat2> ... <new_category>";
            case "export": return "export csv|budgets|json|report";
            case "import": return "import csv <filename>";
            default: return command;
        }
    }

    // Реализация конкретных команд
    private void handleLogin(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Использование: login <username> <password>");
            return;
        }

        if (authService.login(parts[1], parts[2])) {
            financialService.checkAllAlerts();
        }
    }

    private void handleRegister(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Использование: register <username> <password>");
            return;
        }

        authService.register(parts[1], parts[2]);
    }

    private void handleLogout() {
        if (authService.logout()) {
            System.out.println("Вы вышли из системы");
        } else {
            System.out.println("Вы не авторизованы");
        }
    }

    private void handleAdd(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Использование: add <income|expense> <amount> <category>");
            return;
        }

        String type = parts[1];
        try {
            double amount = Double.parseDouble(parts[2]);
            String category = parts[3];

            if (type.equalsIgnoreCase("income")) {
                financialService.addIncome(amount, category);
            } else if (type.equalsIgnoreCase("expense")) {
                financialService.addExpense(amount, category);
            } else {
                System.out.println("Неизвестный тип операции: " + type);
                System.out.println("Используйте: income или expense");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом");
        }
    }

    private void handleIncome(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Использование: income <amount> <category>");
            return;
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            StringBuilder categoryBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                categoryBuilder.append(parts[i]);
                if (i < parts.length - 1) categoryBuilder.append(" ");
            }

            financialService.addIncome(amount, categoryBuilder.toString());
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом");
        }
    }

    private void handleExpense(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Использование: expense <amount> <category>");
            return;
        }

        try {
            double amount = Double.parseDouble(parts[1]);
            StringBuilder categoryBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                categoryBuilder.append(parts[i]);
                if (i < parts.length - 1) categoryBuilder.append(" ");
            }

            financialService.addExpense(amount, categoryBuilder.toString());
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом");
        }
    }

    private void handleBudget(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Использование: budget <set|edit|remove> <category> [limit]");
            return;
        }

        String action = parts[1].toLowerCase();
        String category = parts[2];

        switch (action) {
            case "set":
                if (parts.length != 4) {
                    System.out.println("Использование: budget set <category> <limit>");
                    return;
                }
                try {
                    double limit = Double.parseDouble(parts[3]);
                    financialService.setBudget(category, limit);
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: лимит должен быть числом");
                }
                break;

            case "edit":
                if (parts.length != 4) {
                    System.out.println("Использование: budget edit <category> <new_limit>");
                    return;
                }
                try {
                    double newLimit = Double.parseDouble(parts[3]);
                    financialService.editBudget(category, newLimit);
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: новый лимит должен быть числом");
                }
                break;

            case "remove":
                financialService.removeBudget(category);
                break;

            default:
                System.out.println("Неизвестное действие: " + action);
                System.out.println("Используйте: set, edit или remove");
        }
    }

    private void handleBudgets() {
        financialService.showBudgetStatus();
    }

    private void handleReport(String[] parts) {
        if (parts.length == 1) {
            financialService.showFullStatistics();
            return;
        }

        String period = parts[1].toLowerCase();
        financialService.quickReport(period);
    }

    private void handleSummary() {
        financialService.showUserInfo();
    }

    private void handlePeriod(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Использование: period <start_date> <end_date>");
            System.out.println("Формат даты: YYYY-MM-DD");
            return;
        }

        try {
            LocalDate startDate = LocalDate.parse(parts[1], DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(parts[2], DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                System.out.println("Ошибка: начальная дата не может быть позже конечной");
                return;
            }

            financialService.calculateByPeriod(startDate, endDate);
        } catch (DateTimeParseException e) {
            System.out.println("Ошибка: неверный формат даты");
            System.out.println("Используйте формат: YYYY-MM-DD");
        }
    }

    private void handleCategories() {
        financialService.listAllCategories();
    }

    private void handleRename(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Использование: rename <old_category> <new_category>");
            return;
        }

        financialService.renameCategory(parts[1], parts[2]);
    }

    private void handleMerge(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Использование: merge <cat1> <cat2> ... <new_category>");
            System.out.println("Минимум 2 категории для объединения");
            return;
        }

        String[] categoriesToMerge = new String[parts.length - 2];
        System.arraycopy(parts, 1, categoriesToMerge, 0, categoriesToMerge.length);
        String newCategory = parts[parts.length - 1];

        financialService.mergeCategories(categoriesToMerge, newCategory);
    }

    private void handleExport(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 2) {
            System.out.println("Использование: export <csv|budgets|json|report>");
            return;
        }

        String type = parts[1].toLowerCase();
        String filename = String.format("export_%s_%s.%s",
                financialService.getCurrentUser().getLogin(),
                LocalDate.now().toString(),
                type.equals("json") ? "json" : "csv");

        switch (type) {
            case "csv":
                exportService.exportTransactionsToCSV(financialService.getCurrentUser(), filename);
                break;
            case "budgets":
                exportService.exportBudgetsToCSV(financialService.getCurrentUser(), filename);
                break;
            case "json":
                exportService.exportToJSON(financialService.getCurrentUser(), filename);
                break;
            case "report":
                String reportFile = String.format("report_%s_%s.txt",
                        financialService.getCurrentUser().getLogin(),
                        LocalDate.now().toString());
                exportService.exportReportToText(financialService.getCurrentUser(), reportFile);
                break;
            default:
                System.out.println("Неизвестный тип экспорта: " + type);
                System.out.println("Доступные типы: csv, budgets, json, report");
        }
    }

    private void handleImport(String[] parts) {
        if (!financialService.isAuthenticated()) {
            System.out.println("Ошибка: необходимо авторизоваться");
            return;
        }

        if (parts.length < 3) {
            System.out.println("Использование: import csv <filename>");
            return;
        }

        String type = parts[1].toLowerCase();
        String filename = parts[2];

        if (type.equals("csv")) {
            System.out.print("Вы уверены, что хотите импортировать транзакции из " + filename + "? (yes/no): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (confirmation.equals("yes") || confirmation.equals("y")) {
                exportService.importTransactionsFromCSV(financialService.getCurrentUser(), filename);
            } else {
                System.out.println("Импорт отменен");
            }
        } else {
            System.out.println("Поддерживается только импорт из CSV файлов");
        }
    }

    private void handleAlerts() {
        financialService.showAlerts();
    }

    private void handleCheckAlerts() {
        financialService.checkAllAlerts();
        System.out.println("Проверка оповещений завершена");
    }

    private void handleSave() {
        financialService.saveData();
    }

    private void handleBackup() {
        financialService.createBackup();
    }

    private void handleClear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void handleExit() {
        // Сохраняем данные перед выходом
        if (financialService.isAuthenticated()) {
            System.out.println("Сохранение данных...");
            financialService.saveData();
            authService.logout();
        }

        System.out.println("Выход из приложения...");
        isRunning = false;
    }
}