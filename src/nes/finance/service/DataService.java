package nes.finance.service;

import nes.finance.model.User;
import nes.finance.model.Wallet;
import nes.finance.model.Transaction;
import nes.finance.model.Alert;
import nes.finance.model.TransactionType;
import nes.finance.model.AlertType;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DataService {
    private static final String DATA_DIR = "data";
    private static final String FILE_EXTENSION = ".dat";

    public DataService() {
        // Создаем директорию для данных, если она не существует
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для данных: " + e.getMessage());
        }
    }

    /**
     * Сохраняет данные пользователя в файл
     */
    public boolean saveUserData(User user) {
        if (user == null || user.getLogin() == null) {
            System.out.println("Ошибка: неверные данные пользователя для сохранения");
            return false;
        }

        String fileName = getFileName(user.getLogin());

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(fileName))) {

            // Сохраняем данные пользователя
            oos.writeObject(user.getLogin());
            oos.writeObject(user.getPassword());

            // Сохраняем данные кошелька
            Wallet wallet = user.getWallet();
            oos.writeDouble(wallet.getBalance());

            // Сохраняем транзакции
            List<Transaction> transactions = wallet.getTransactions();
            oos.writeInt(transactions.size());
            for (Transaction t : transactions) {
                oos.writeObject(t.getType());
                oos.writeDouble(t.getAmount());
                oos.writeObject(t.getCategory());
                oos.writeObject(t.getDate());
            }

            // Сохраняем бюджеты
            Map<String, Double> budgets = wallet.getBudgets();
            oos.writeInt(budgets.size());
            for (Map.Entry<String, Double> entry : budgets.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeDouble(entry.getValue());
            }

            // Сохраняем оповещения
            List<Alert> alerts = wallet.getAlerts();
            oos.writeInt(alerts.size());
            for (Alert alert : alerts) {
                oos.writeObject(alert.getType());
                oos.writeObject(alert.getMessage());
                oos.writeObject(alert.getTimestamp());
                oos.writeBoolean(alert.isRead());
            }

            System.out.printf("Данные пользователя '%s' успешно сохранены%n", user.getLogin());
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении данных пользователя: " + e.getMessage());
            return false;
        }
    }

    /**
     * Загружает данные пользователя из файла
     */
    public User loadUserData(String login) {
        if (login == null || login.trim().isEmpty()) {
            return null;
        }

        String fileName = getFileName(login);
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("Файл с данными пользователя не найден. Будет создан новый кошелек.");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(fileName))) {

            // Загружаем базовые данные пользователя
            String loadedLogin = (String) ois.readObject();
            String password = (String) ois.readObject();

            User user = new User(loadedLogin, password);
            Wallet wallet = user.getWallet();

            // Загружаем баланс
            wallet.setBalance(ois.readDouble());

            // Загружаем транзакции
            int transactionCount = ois.readInt();
            for (int i = 0; i < transactionCount; i++) {
                TransactionType type = (TransactionType) ois.readObject();
                double amount = ois.readDouble();
                String category = (String) ois.readObject();
                LocalDateTime date = (LocalDateTime) ois.readObject();

                // Создаем транзакцию
                Transaction transaction = new Transaction(type, amount, category);
                // Устанавливаем дату (требует изменения в классе Transaction)
                setTransactionDate(transaction, date);
                wallet.getTransactions().add(transaction);
            }

            // Загружаем бюджеты
            int budgetCount = ois.readInt();
            for (int i = 0; i < budgetCount; i++) {
                String category = (String) ois.readObject();
                double limit = ois.readDouble();
                wallet.getBudgets().put(category, limit);
            }

            // Загружаем оповещения
            int alertCount = ois.readInt();
            for (int i = 0; i < alertCount; i++) {
                AlertType type = (AlertType) ois.readObject();
                String message = (String) ois.readObject();
                LocalDateTime timestamp = (LocalDateTime) ois.readObject();
                boolean isRead = ois.readBoolean();

                Alert alert = new Alert(type, message);
                setAlertTimestamp(alert, timestamp);
                if (isRead) {
                    alert.markAsRead();
                }
                wallet.getAlerts().add(alert);
            }

            System.out.printf("Данные пользователя '%s' успешно загружены%n", login);
            return user;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при загрузке данных пользователя: " + e.getMessage());
            return null;
        }
    }

    /**
     * Удаляет файл с данными пользователя
     */
    public boolean deleteUserData(String login) {
        String fileName = getFileName(login);
        File file = new File(fileName);

        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Проверяет существование файла с данными пользователя
     */
    public boolean userDataExists(String login) {
        String fileName = getFileName(login);
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * Получает список всех сохраненных пользователей
     */
    public List<String> getAllSavedUsers() {
        List<String> users = new java.util.ArrayList<>();
        File dir = new File(DATA_DIR);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String login = fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
                    users.add(login);
                }
            }
        }

        return users;
    }

    /**
     * Создает резервную копию данных пользователя
     */
    public boolean createBackup(String login) {
        String originalFile = getFileName(login);
        String backupFile = getFileName(login + "_backup");

        try {
            Files.copy(Paths.get(originalFile), Paths.get(backupFile),
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("Резервная копия данных пользователя '%s' создана%n", login);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при создании резервной копии: " + e.getMessage());
            return false;
        }
    }

    private String getFileName(String login) {
        return DATA_DIR + File.separator + login + FILE_EXTENSION;
    }

    // Вспомогательные методы для установки дат (из-за неизменяемости полей в моделях)
    private void setTransactionDate(Transaction transaction, LocalDateTime date) {
        try {
            // Используем рефлексию для установки даты транзакции
            java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("date");
            dateField.setAccessible(true);
            dateField.set(transaction, date);
        } catch (Exception e) {
            System.err.println("Ошибка при установке даты транзакции: " + e.getMessage());
        }
    }

    private void setAlertTimestamp(Alert alert, LocalDateTime timestamp) {
        try {
            // Используем рефлексию для установки timestamp оповещения
            java.lang.reflect.Field timestampField = Alert.class.getDeclaredField("timestamp");
            timestampField.setAccessible(true);
            timestampField.set(alert, timestamp);

            // Также устанавливаем флаг isRead через отдельный метод
            java.lang.reflect.Field isReadField = Alert.class.getDeclaredField("isRead");
            isReadField.setAccessible(true);
            // Флаг isRead устанавливается через markAsRead() после создания
        } catch (Exception e) {
            System.err.println("Ошибка при установке timestamp оповещения: " + e.getMessage());
        }
    }
}