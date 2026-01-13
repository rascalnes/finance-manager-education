package nes.finance.model;

import java.io.Serializable;

public enum AlertType implements Serializable {
    BUDGET_EXCEEDED,
    OVERSPENDING,
    LOW_BALANCE,
    BUDGET_WARNING
}