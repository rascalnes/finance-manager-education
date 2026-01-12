package nes.finance.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String login;
    private String password;
    private Wallet wallet;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.wallet = new Wallet();
    }

    // Getters and Setters
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public Wallet getWallet() { return wallet; }

    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return String.format("User{login='%s', wallet=%s}", login, wallet);
    }
}