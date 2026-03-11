package com.iispl.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettlementBatch {

    private final String batchId;
    private final LocalDate date;
    private final List<Transaction> transaction;

    public SettlementBatch(String batchId, LocalDate date, List<Transaction> transaction) {
        this.batchId = batchId;
        this.date = date;
        this.transaction = Collections.unmodifiableList(new ArrayList<>(transaction));
    }

    public static Builder builder(String batchId, LocalDate date) {
        return new Builder(batchId, date);
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

    public static final class Builder {
        private final String batchId;
        private final LocalDate date;
        private final List<Transaction> transactions = new ArrayList<>();

        private Builder(String batchId, LocalDate date) {
            this.batchId = batchId;
            this.date = date;
        }

        public Builder add(Transaction txn) {
            transactions.add(txn);
            return this;
        }

        public int previewRecordCount() {
            return transactions.size();
        }

        public SettlementBatch build() {
            return new SettlementBatch(batchId, date, transactions);
        }
    }
}
