package com.iispl.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.repository.SettlementBatchRepository;
import com.iispl.repository.TransactionRepository;

public class SettlementService {

    private final SettlementBatchRepository batchRepo;
    private final TransactionRepository txnRepo;

    public SettlementService(SettlementBatchRepository batchRepo, TransactionRepository txnRepo) {
        this.batchRepo = batchRepo;
        this.txnRepo = txnRepo;
    }

    public void processBatch(SettlementBatch batch) throws SQLException {
        batchRepo.save(batch);

        for (Transaction t : batch.getTransaction()) {
            txnRepo.save(t, batch.getBatchId());
        }
    }

    public void processEndOfDayBatch(SettlementBatch batch) throws SQLException {
        processBatch(batch);
        System.out.println("✅ Batch " + batch.getBatchId() + " submitted with " + batch.getTransaction().size()
                + " transactions.");
    }

    public void printBatchSummary(String batchId) throws SQLException {
        LocalDate batchDate = batchRepo.findBatchDate(batchId);
        if (batchDate == null) {
            System.out.println("❌ Batch not found for ID: " + batchId + "\n");
            return;
        }

        List<Transaction> transactions = txnRepo.findByBatchId(batchId);
        BigDecimal total = txnRepo.totalAmountForBatch(batchId);

        System.out.println("📄 Batch Summary");
        System.out.println("Batch ID: " + batchId);
        System.out.println("Batch Date: " + batchDate);
        System.out.println("Transactions: " + transactions.size());
        System.out.println("Total Amount: " + total + "\n");
    }
}
