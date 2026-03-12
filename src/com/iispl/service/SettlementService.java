package com.iispl.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iispl.connectionpool.ConnectionPool;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.enums.Bank;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;
import com.iispl.repository.SettlementBatchRepository;
import com.iispl.repository.TransactionRepository;

/**
 * Application service that orchestrates repositories and prints business reports.
 */
public class SettlementService {

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SettlementBatchRepository batchRepo;
    private final TransactionRepository txnRepo;

    public SettlementService(SettlementBatchRepository batchRepo, TransactionRepository txnRepo) {
        this.batchRepo = batchRepo;
        this.txnRepo = txnRepo;
    }

    /** Validates DB connectivity and required tables before menu startup. */
    public void validateStartup() throws SQLException {
        try (Connection connection = ConnectionPool.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertTableExists(metaData, "settlement_batch");
            assertTableExists(metaData, "transactions");
        }
    }

    public boolean isBatchAlreadySubmitted(String batchId) throws SQLException {
        return batchRepo.existsByBatchId(batchId);
    }

    /** Generates a collision-resistant batch id and verifies it does not already exist. */
    public String generateUniqueBatchId() throws SQLException {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 10; attempt++) {
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String batchId = "BATCH-" + datePart + "-" + randomPart;
            if (!isBatchAlreadySubmitted(batchId)) {
                return batchId;
            }
        }
        throw new IllegalStateException("Unable to generate a unique Batch ID. Please retry.");
    }

    public boolean isTransactionAlreadyPersisted(String txnId) throws SQLException {
        return txnRepo.existsByTxnId(txnId);
    }

    /** Persists batch and transactions in a single DB transaction (all-or-nothing). */
    public void processBatch(SettlementBatch batch) throws SQLException {
        try (Connection connection = ConnectionPool.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                batchRepo.save(connection, batch);
                for (Transaction txn : batch.getTransaction()) {
                    if (txnRepo.existsByTxnId(connection, txn.getTxnId())) {
                        throw new IllegalArgumentException("Duplicate txn id found: " + txn.getTxnId());
                    }
                    txnRepo.save(connection, txn, batch.getBatchId());
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void processEndOfDayBatch(SettlementBatch batch) throws SQLException {
        processBatch(batch);
        System.out.println("✅ Batch " + batch.getBatchId() + " submitted with " + batch.getTransaction().size()
                + " transactions.");
    }

    public void printCurrentBatchPreview(SettlementBatch.Builder builder) {
        List<Transaction> transactions = builder.previewTransactions();
        if (transactions.isEmpty()) {
            System.out.println("ℹ️ Current batch has no pending transactions.\n");
            return;
        }

        System.out.println("\n🧾 Current Unsaved Batch Preview");
        System.out.printf("%-12s %-6s %-6s %-12s %-12s %-6s %-10s%n",
                "Txn ID", "From", "To", "Channel", "Amount", "DR/CR", "Status");
        System.out.println("--------------------------------------------------------------------------");
        for (Transaction tx : transactions) {
            System.out.printf("%-12s %-6s %-6s %-12s %-12.2f %-6s %-10s%n",
                    tx.getTxnId(), tx.getSenderBank(), tx.getReceiverBank(), tx.getChannel(), tx.getAmount(),
                    tx.getDrCr(), tx.getStatus());
        }
        System.out.println();
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
            if (transaction.getStatus() != Status.SUCCESS) {
                continue;
            }
            channelStats.computeIfAbsent(transaction.getChannel(), ignored -> new ChannelSettlement()).include(transaction);
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

    public void printBankWiseSummaryReport() throws SQLException {
        List<Transaction> transactions = txnRepo.findAll();
        if (transactions.isEmpty()) {
            System.out.println("❌ No transactions available in database.\n");
            return;
        }

        System.out.println("\nBANK-WISE SUMMARY REPORT");
        System.out.printf("%-10s %12s %16s %12s %16s %16s%n", "Bank", "Items Deliv.", "Amt To Recv",
                "Items Recv.", "Amt To Pay", "Net");
        System.out.println("--------------------------------------------------------------------------------");

        for (Bank bank : Bank.values()) {
            int delivered = 0;
            int received = 0;
            BigDecimal amtToRecv = BigDecimal.ZERO;
            BigDecimal amtToPay = BigDecimal.ZERO;

            for (Transaction txn : transactions) {
                if (txn.getStatus() != Status.SUCCESS) {
                    continue;
                }
                if (txn.getReceiverBank() == bank) {
                    delivered++;
                    amtToRecv = amtToRecv.add(txn.getAmount());
                }
                if (txn.getSenderBank() == bank) {
                    received++;
                    amtToPay = amtToPay.add(txn.getAmount());
                }
            }

            if (delivered == 0 && received == 0) {
                continue;
            }

            BigDecimal net = amtToRecv.subtract(amtToPay);
            System.out.printf("%-10s %12d %16.2f %12d %16.2f %16.2f%n",
                    bank, delivered, amtToRecv, received, amtToPay, net);
        }
        System.out.println();
    }

    public void printAllTransactions() throws SQLException {
        printTransactionList("ALL TRANSACTIONS", txnRepo.findAll());
    }

    public void printAllBatches() throws SQLException {
        Map<String, LocalDate> batches = batchRepo.findAllBatchDates();
        if (batches.isEmpty()) {
            System.out.println("❌ No batches available in database.\n");
            return;
        }

        printSectionTitle("ALL BATCHES", batches.size());
        System.out.printf("%-4s %-16s %-12s%n", "#", "Batch ID", "Batch Date");
        System.out.println("-----------------------------------");
        int rowNum = 1;
        for (Map.Entry<String, LocalDate> batch : batches.entrySet()) {
            System.out.printf("%-4d %-16s %-12s%n", rowNum++, batch.getKey(), batch.getValue());
        }
        System.out.println();
    }

    public void printBatchesByDate(LocalDate batchDate) throws SQLException {
        Map<String, LocalDate> batches = batchRepo.findBatchDatesByDate(batchDate);
        if (batches.isEmpty()) {
            System.out.println("❌ No batches found for date: " + batchDate + "\n");
            return;
        }

        printSectionTitle("BATCHES ON DATE: " + batchDate, batches.size());
        System.out.printf("%-4s %-16s %-12s%n", "#", "Batch ID", "Batch Date");
        System.out.println("-----------------------------------");
        int rowNum = 1;
        for (Map.Entry<String, LocalDate> batch : batches.entrySet()) {
            System.out.printf("%-4d %-16s %-12s%n", rowNum++, batch.getKey(), batch.getValue());
        }
        System.out.println();
    }

    public void printTransactionsByTxnId(String txnId) throws SQLException {
        printTransactionList("TRANSACTIONS FOR ID: " + txnId, txnRepo.findByTxnId(txnId));
    }

    public void printTransactionsByDate(LocalDate txnDate) throws SQLException {
        printTransactionList("TRANSACTIONS ON DATE: " + txnDate, txnRepo.findByTxnDate(txnDate));
    }

    public void printTransactionsByBank(Bank bank) throws SQLException {
        printTransactionList("TRANSACTIONS FOR BANK: " + bank, txnRepo.findByBank(bank));
    }

    public void printTransactionsByChannel(Channel channel) throws SQLException {
        printTransactionList("TRANSACTIONS FOR CHANNEL: " + channel, txnRepo.findByChannel(channel));
    }

    public void printTransactionsByDrCr(DrCr drCr) throws SQLException {
        printTransactionList("TRANSACTIONS FOR DR/CR: " + drCr, txnRepo.findByDrCr(drCr));
    }

    public void printTransactionsByStatus(Status status) throws SQLException {
        printTransactionList("TRANSACTIONS FOR STATUS: " + status, txnRepo.findByStatus(status));
    }

    public void printTransactionsByBankAndChannel(Bank bank, Channel channel) throws SQLException {
        printTransactionList("TRANSACTIONS FOR BANK " + bank + " AND CHANNEL " + channel,
                txnRepo.findByBankAndChannel(bank, channel));
    }

    public void printTransactionsByBankAndStatus(Bank bank, Status status) throws SQLException {
        printTransactionList("TRANSACTIONS FOR BANK " + bank + " AND STATUS " + status,
                txnRepo.findByBankAndStatus(bank, status));
    }

    public void printTransactionsByBankChannelAndStatus(Bank bank, Channel channel, Status status) throws SQLException {
        printTransactionList("TRANSACTIONS FOR BANK " + bank + ", CHANNEL " + channel + ", STATUS " + status,
                txnRepo.findByBankAndChannelAndStatus(bank, channel, status));
    }

    /** Prints transaction rows in a consistent tabular format for all filter views. */
    private void printTransactionList(String title, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("❌ No transactions found.\n");
            return;
        }

        printSectionTitle(title, transactions.size());
        System.out.printf("%-4s %-12s %-6s %-6s %-10s %12s %-6s %-10s %-19s%n",
                "#", "Txn ID", "From", "To", "Channel", "Amount", "DR/CR", "Status", "Txn Time");
        System.out.println("------------------------------------------------------------------------------------------------");

        int rowNum = 1;
        for (Transaction transaction : transactions) {
            String displayTime = DISPLAY_TIME_FORMAT.format(transaction.getTxnTime().atZone(ZoneId.systemDefault()));
            System.out.printf("%-4d %-12s %-6s %-6s %-10s %12.2f %-6s %-10s %-19s%n",
                    rowNum++,
                    transaction.getTxnId(),
                    transaction.getSenderBank(),
                    transaction.getReceiverBank(),
                    transaction.getChannel(),
                    transaction.getAmount(),
                    transaction.getDrCr(),
                    transaction.getStatus(),
                    displayTime);
        }
        System.out.println();
    }

    private void printSectionTitle(String title, int totalRecords) {
        String divider = "=".repeat(96);
        System.out.println("\n" + divider);
        System.out.println(title);
        System.out.println("Total Records: " + totalRecords);
        System.out.println(divider);
    }

    private void assertTableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (java.sql.ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            if (!tables.next()) {
                throw new IllegalStateException("Required table missing: " + tableName);
            }
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
