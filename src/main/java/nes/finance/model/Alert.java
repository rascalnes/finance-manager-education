package nes.finance.model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Alert implements Serializable {
    private static final long serialVersionUID = 1L;

    private AlertType type;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;

    public Alert(AlertType type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    // Конструктор для загрузки из файла
    public Alert(AlertType type, String message, LocalDateTime timestamp, boolean isRead) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters
    public AlertType getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    public void markAsRead() { this.isRead = true; }

    @Override
    public String toString() {
        String status = isRead ? "[ПРОЧИТАНО]" : "[НОВОЕ]";
        return String.format("%s %s: %s (%s)", status, type, message, timestamp);
    }
}