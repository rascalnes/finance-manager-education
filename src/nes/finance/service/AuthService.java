package nes.finance.service;

import nes.finance.model.User;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private Map<String, User> users; // login -> User
    private User currentUser;

    public AuthService() {
        this.users = new HashMap<>();
        this.currentUser = null;
    }

    public boolean register(String login, String password) {
        if (users.containsKey(login)) {
            return false;
        }

        User newUser = new User(login, password);
        users.put(login, newUser);
        return true;
    }

    public boolean login(String login, String password) {
        User user = users.get(login);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            return true;
        }
        return false;
    }

    public void logout() {
        currentUser = null;
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
}