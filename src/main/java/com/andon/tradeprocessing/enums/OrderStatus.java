package com.andon.tradeprocessing.enums;

public enum OrderStatus {
    PENDING,    // Submitted, awaiting matching (limit orders)
    FILLED,     // Fully executed
    CANCELLED,  // Cancelled by user
    REJECTED    // Rejected at validation (insufficient funds, bad symbol, etc.)
}
