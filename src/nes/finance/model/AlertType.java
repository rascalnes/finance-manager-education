package nes.finance.model;

public enum AlertType {
    BUDGET_EXCEEDED,      // Превышен бюджет по категории
    OVERSPENDING,         // Общие расходы превысили доходы
    LOW_BALANCE,          // Низкий баланс
    BUDGET_WARNING        // Близкое превышение бюджета (80% использования)
}