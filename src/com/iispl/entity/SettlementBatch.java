package com.iispl.entity;

import java.time.LocalDate;
import java.util.List;

public class SettlementBatch {

    private final String batchId;
    private final LocalDate date;
    private final List<Transaction> transaction;
    public SettlementBatch(String batchId, LocalDate date, List<Transaction> transaction) {
        this.batchId = batchId;
        this.date = date;
        this.transaction = transaction;
    }
    public String getBatchId() {
        return batchId;
    }
    public LocalDate getDate() {
        return date;
    }
    public List<Transaction> getTransaction() {
        return transaction;
    }
    
}
