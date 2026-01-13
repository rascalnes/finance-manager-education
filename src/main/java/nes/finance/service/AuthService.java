package nes.finance.service;

import nes.finance.model.User;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private Map<String, User> users;
    private User currentUser;
    private DataService dataService;

    public AuthService() {
        this.users = new HashMap<>();
        this.currentUser = null;
        this.dataService = new DataService();

        // Загружаем список сохраненных пользователей
        loadAllUsers();
    }

    /**
     * Регистрация нового пользователя
     */
    public boolean register(String login, String password) {
        if (users.containsKey(login)) {
            System.out.println("Ошибка: пользователь с таким логином уже существует");
            return false;
        }

        // Проверяем, есть ли сохраненные данные для этого пользователя
        User existingUser = dataService.loadUserData(login);
        if (existingUser != null) {
            // Пользователь уже существует в файловой системе
            users.put(login, existingUser);
            System.out.println("Восстановлены сохраненные данные для пользователя: " + login);
            return true;
        }

        // Создаем нового пользователя
        User newUser = new User(login, password);
        users.put(login, newUser);

        // Сохраняем нового пользователя
        dataService.saveUserData(newUser);
        System.out.println("Пользователь " + login + " успешно зарегистрирован");
        return true;
    }

    /**
     * Авторизация пользователя
     */
    public boolean login(String login, String password) {
        // Сначала проверяем в памяти
        User user = users.get(login);

        // Если пользователя нет в памяти, пробуем загрузить из файла
        if (user == null) {
            user = dataService.loadUserData(login);
            if (user != null) {
                users.put(login, user);
            }
        }

        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            System.out.println("Успешный вход! Добро пожаловать, " + login);
            return true;
        }

        System.out.println("Ошибка: неверный логин или пароль");
        return false;
    }

    /**
     * Выход пользователя с сохранением данных
     */
    public boolean logout() {
        if (currentUser != null) {
            // Сохраняем данные перед выходом
            dataService.saveUserData(currentUser);
            System.out.println("Данные пользователя сохранены");
            currentUser = null;
            return true;
        }
        return false;
    }

    /**
     * Удаление пользователя
     */
    public boolean deleteUser(String login, String password) {
        if (!users.containsKey(login)) {
            System.out.println("Ошибка: пользователь не найден");
            return false;
        }

        User user = users.get(login);
        if (!user.getPassword().equals(password)) {
            System.out.println("Ошибка: неверный пароль");
            return false;
        }

        // Удаляем из памяти
        users.remove(login);

        // Удаляем текущего пользователя, если это он
        if (currentUser != null && currentUser.getLogin().equals(login)) {
            currentUser = null;
        }

        // Удаляем файл с данными
        boolean deleted = dataService.deleteUserData(login);
        if (deleted) {
            System.out.println("Пользователь " + login + " удален");
        } else {
            System.out.println("Пользователь удален из памяти, но файл данных не найден");
        }

        return deleted;
    }

    /**
     * Загрузка всех пользователей из файловой системы
     */
    private void loadAllUsers() {
        for (String login : dataService.getAllSavedUsers()) {
            User user = dataService.loadUserData(login);
            if (user != null) {
                users.put(login, user);
            }
        }
        System.out.println("Загружено пользователей из файлов: " + users.size());
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public DataService getDataService() {
        return dataService;
    }
}