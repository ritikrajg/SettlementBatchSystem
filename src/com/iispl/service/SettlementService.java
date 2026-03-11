package com.iispl.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
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

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Void>> futures = batch.getTransaction().stream()
                    .map(txn -> executor.submit(new SaveTransactionTask(txn, batch.getBatchId())))
                    .toList();

            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Batch processing interrupted.", e);
        } catch (ExecutionException e) {
            throw new SQLException("Error while saving transactions in parallel.", e.getCause());
        } finally {
            executor.shutdown();
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

    public void printClearingHouseReport(String batchId) throws SQLException {
        LocalDate batchDate = batchRepo.findBatchDate(batchId);
        if (batchDate == null) {
            System.out.println("❌ Batch not found for ID: " + batchId + "\n");
            return;
        }

        List<Transaction> transactions = txnRepo.findByBatchId(batchId);
        if (transactions.isEmpty()) {
            System.out.println("❌ No transactions found for batch: " + batchId + "\n");
            return;
        }

        Map<Channel, ChannelSettlement> channelStats = new EnumMap<>(Channel.class);
        for (Transaction transaction : transactions) {
            channelStats.computeIfAbsent(transaction.getChannel(), ignored -> new ChannelSettlement())
                    .include(transaction);
        }

        System.out.println("\nCLEARING HOUSE REPORTS");
        System.out.println("SETTLEMENT REGISTER for Date " + batchDate);
        System.out.println("Batch ID: " + batchId + "\n");

        System.out.printf("%-12s %12s %16s %12s %16s %16s%n", "Channel", "Items Deliv.", "Amt To Recv",
                "Items Recv.", "Amt To Pay", "Amt(+CR/-DR)");
        System.out.println("--------------------------------------------------------------------------------");

        ChannelSettlement total = new ChannelSettlement();
        for (Channel channel : Channel.values()) {
            ChannelSettlement stats = channelStats.get(channel);
            if (stats == null) {
                continue;
            }

            System.out.printf("%-12s %12d %16.2f %12d %16.2f %16.2f%n",
                    channel,
                    stats.receiveCount,
                    stats.receiveAmount,
                    stats.payCount,
                    stats.payAmount,
                    stats.netAmount());

            total.merge(stats);
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-12s %12d %16.2f %12d %16.2f %16.2f%n%n", "TOTAL",
                total.receiveCount, total.receiveAmount, total.payCount, total.payAmount, total.netAmount());
    }

    private final class SaveTransactionTask implements Callable<Void> {
        private final Transaction txn;
        private final String batchId;

        private SaveTransactionTask(Transaction txn, String batchId) {
            this.txn = txn;
            this.batchId = batchId;
        }

        @Override
        public Void call() throws Exception {
            txnRepo.save(txn, batchId);
            return null;
        }
    }

    private static final class ChannelSettlement {
        private int receiveCount;
        private int payCount;
        private BigDecimal receiveAmount = BigDecimal.ZERO;
        private BigDecimal payAmount = BigDecimal.ZERO;

        private void include(Transaction transaction) {
            if (transaction.getDrCr() == DrCr.CR) {
                receiveCount++;
                receiveAmount = receiveAmount.add(transaction.getAmount());
                return;
            }

            payCount++;
            payAmount = payAmount.add(transaction.getAmount());
        }

        private BigDecimal netAmount() {
            return receiveAmount.subtract(payAmount);
        }

        private void merge(ChannelSettlement other) {
            receiveCount += other.receiveCount;
            payCount += other.payCount;
            receiveAmount = receiveAmount.add(other.receiveAmount);
            payAmount = payAmount.add(other.payAmount);
        }
    }
}
