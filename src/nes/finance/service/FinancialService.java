package nes.finance.service;

import nes.finance.model.User;

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

    public void showUserInfo() {
        if (isAuthenticated()) {
            User user = getCurrentUser();
            System.out.printf("Пользователь: %s%n", user.getLogin());
            System.out.printf("Баланс: %.2f%n", user.getWallet().getBalance());
            System.out.printf("Кол-во транзакций: %d%n", user.getWallet().getTransactions().size());
        } else {
            System.out.println("Пользователь не авторизован");
        }
    }
}