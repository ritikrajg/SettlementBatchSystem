package com.iispl.app;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.enums.Bank;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;
import com.iispl.repository.SettlementBatchRepository;
import com.iispl.repository.TransactionRepository;
import com.iispl.service.SettlementService;

/**
 * Console entry-point for the settlement system.
 *
 * It manages user interaction, validates inputs, and delegates persistence/reporting
 * operations to {@link com.iispl.service.SettlementService}.
 */
public class SettlementApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SettlementService service = new SettlementService(new SettlementBatchRepository(), new TransactionRepository());

        if (!runStartupChecks(service)) {
            scanner.close();
            return;
        }

        SettlementBatch.Builder currentBuilder = null;

        while (true) {
            printMainMenu();
            int choice = readMenuChoice(scanner);

            try {
                switch (choice) {
                    case 1:
                        String batchId = service.generateUniqueBatchId();
                        currentBuilder = SettlementBatch.builder(batchId, LocalDate.now());
                        System.out.println("✅ Batch " + batchId + " initialized automatically.\n");
                        break;

                    case 2:
                        if (currentBuilder == null) {
                            System.out.println("❌ Please initialize a batch first (Option 1).\n");
                            break;
                        }
                        System.out.print("Enter Txn ID (e.g., TXN1001): ");
                        String txnId = scanner.nextLine().trim();
                        if (txnId.isBlank()) {
                            throw new IllegalArgumentException("Transaction ID cannot be blank.");
                        }
                        if (service.isTransactionAlreadyPersisted(txnId)) {
                            throw new IllegalArgumentException("Transaction ID already exists in database: " + txnId);
                        }

                        Bank senderBank = readBank(scanner, "Select Sender Bank");
                        Bank receiverBank = readBank(scanner, "Select Receiver Bank");
                        if (senderBank == receiverBank) {
                            throw new IllegalArgumentException("Sender and receiver bank cannot be same.");
                        }
                        Channel channel = readChannel(scanner);
                        BigDecimal amount = readAmount(scanner);
                        DrCr drCr = readDrCr(scanner);
                        Status status = readStatus(scanner);

                        Transaction txn = new Transaction(
                                txnId,
                                senderBank,
                                receiverBank,
                                channel,
                                amount,
                                Instant.now(),
                                drCr,
                                status);

                        currentBuilder.add(txn);
                        System.out.println(
                                "✅ Transaction Added. Current Unsaved Count: " + currentBuilder.previewRecordCount() + "\n");
                        break;

                    case 3:
                        if (currentBuilder == null) {
                            System.out.println("❌ Please initialize a batch first (Option 1).\n");
                            break;
                        }
                        importTransactionsFromCsv(scanner, service, currentBuilder);
                        break;

                    case 4:
                        if (currentBuilder == null || currentBuilder.previewRecordCount() == 0) {
                            System.out.println("❌ No pending batch or empty batch to submit.\n");
                            break;
                        }
                        if (!confirm(scanner, "Submit current batch to database")) {
                            System.out.println("⚠️ Batch submission cancelled.\n");
                            break;
                        }
                        SettlementBatch batchToSave = currentBuilder.build();
                        service.processEndOfDayBatch(batchToSave);
                        currentBuilder = null;
                        break;

                    case 5:
                        System.out.print("Enter Batch ID to search: ");
                        String searchId = scanner.nextLine();
                        service.printBatchSummary(searchId);
                        break;

                    case 6:
                        System.out.print("Enter Batch ID to generate report: ");
                        String reportBatchId = scanner.nextLine();
                        service.printClearingHouseReport(reportBatchId);
                        break;

                    case 7:
                        service.printBankWiseSummaryReport();
                        break;

                    case 8:
                        service.printAllTransactions();
                        break;

                    case 9:
                        if (currentBuilder == null) {
                            System.out.println("ℹ️ No active unsaved batch.\n");
                            break;
                        }
                        service.printCurrentBatchPreview(currentBuilder);
                        break;

                    case 10:
                        runAdvancedBatchViewMenu(scanner, service);
                        break;

                    case 11:
                        runAdvancedTransactionViewMenu(scanner, service);
                        break;

                    case 12:
                        if (currentBuilder != null && currentBuilder.previewRecordCount() > 0
                                && !confirm(scanner, "You have unsaved transactions. Exit anyway")) {
                            System.out.println();
                            break;
                        }
                        System.out.println("Exiting system. Goodbye!");
                        scanner.close();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Invalid option. Try again.\n");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Validation Error: " + e.getMessage() + "\n");
            } catch (SQLException e) {
                System.out.println("❌ Database Error: " + e.getMessage());
                System.out.println("➡️ Please verify DB connection, credentials, and schema setup.\n");
            } catch (Exception e) {
                System.out.println("❌ Unexpected Error: " + e.getMessage());
                System.out.println("➡️ Please retry and contact support if the issue persists.\n");
            }
        }
    }

    private static boolean runStartupChecks(SettlementService service) {
        try {
            service.validateStartup();
            System.out.println("✅ Startup check passed: database connectivity and core tables are available.\n");
            return true;
        } catch (IllegalStateException e) {
            System.out.println("❌ Startup Validation Error: " + e.getMessage());
            System.out.println("➡️ Ensure required tables exist: settlement_batch, transactions.\n");
        } catch (SQLException e) {
            System.out.println("❌ Startup Database Error: " + e.getMessage());
            System.out.println("➡️ Check db.properties URL, credentials, and database availability.\n");
        }
        return false;
    }

    private static void printMainMenu() {
        System.out.println("🏦 Banking Settlement System");
        System.out.println("1. Initialize New Settlement Batch");
        System.out.println("2. Add Transaction to Current Batch");
        System.out.println("3. Import Transactions from CSV to Current Batch");
        System.out.println("4. Submit Current Batch to Database");
        System.out.println("5. View Batch Summary from Database");
        System.out.println("6. View Clearing House Report (Batch)");
        System.out.println("7. View Bank-wise Settlement Summary");
        System.out.println("8. View All Transactions");
        System.out.println("9. View Current Unsaved Batch");
        System.out.println("10. View Batches (Advanced)");
        System.out.println("11. View Transactions (Advanced Filters)");
        System.out.println("12. Exit");
        System.out.print("Select an option: ");
    }

    private static void runAdvancedBatchViewMenu(Scanner scanner, SettlementService service) throws SQLException {
        System.out.println("\n📦 Advanced Batch Views");
        System.out.println("1. View all batches");
        System.out.println("2. View batches date-wise");
        System.out.println("3. Back to main menu");
        System.out.print("Select an option: ");

        int choice = readMenuChoice(scanner);
        switch (choice) {
            case 1:
                service.printAllBatches();
                break;
            case 2:
                LocalDate date = readDate(scanner, "Enter batch date (YYYY-MM-DD): ");
                service.printBatchesByDate(date);
                break;
            case 3:
                System.out.println();
                break;
            default:
                System.out.println("Invalid option. Try again.\n");
        }
    }

    private static void runAdvancedTransactionViewMenu(Scanner scanner, SettlementService service) throws SQLException {
        System.out.println("\n🔎 Advanced Transaction Views");
        System.out.println("1. View transaction by transaction ID");
        System.out.println("2. View transactions date-wise");
        System.out.println("3. View transactions bank-wise");
        System.out.println("4. View transactions by channel");
        System.out.println("5. View transactions by DR/CR");
        System.out.println("6. View transactions by status");
        System.out.println("7. View transactions by bank and channel");
        System.out.println("8. View transactions by bank and status");
        System.out.println("9. View transactions by bank, channel and status");
        System.out.println("10. Back to main menu");
        System.out.print("Select an option: ");

        int choice = readMenuChoice(scanner);
        switch (choice) {
            case 1:
                System.out.print("Enter transaction ID: ");
                String txnId = scanner.nextLine().trim();
                if (txnId.isBlank()) {
                    throw new IllegalArgumentException("Transaction ID cannot be blank.");
                }
                service.printTransactionsByTxnId(txnId);
                break;
            case 2:
                LocalDate date = readDate(scanner, "Enter transaction date (YYYY-MM-DD): ");
                service.printTransactionsByDate(date);
                break;
            case 3:
                service.printTransactionsByBank(readBank(scanner, "Select Bank"));
                break;
            case 4:
                service.printTransactionsByChannel(readChannel(scanner));
                break;
            case 5:
                service.printTransactionsByDrCr(readDrCr(scanner));
                break;
            case 6:
                service.printTransactionsByStatus(readStatus(scanner));
                break;
            case 7:
                Bank bankForChannel = readBank(scanner, "Select Bank");
                Channel channel = readChannel(scanner);
                service.printTransactionsByBankAndChannel(bankForChannel, channel);
                break;
            case 8:
                Bank bankForStatus = readBank(scanner, "Select Bank");
                Status status = readStatus(scanner);
                service.printTransactionsByBankAndStatus(bankForStatus, status);
                break;
            case 9:
                Bank bank = readBank(scanner, "Select Bank");
                Channel combinedChannel = readChannel(scanner);
                Status combinedStatus = readStatus(scanner);
                service.printTransactionsByBankChannelAndStatus(bank, combinedChannel, combinedStatus);
                break;
            case 10:
                System.out.println();
                break;
            default:
                System.out.println("Invalid option. Try again.\n");
        }
    }

    private static int readMenuChoice(Scanner scanner) {
        String rawInput = scanner.nextLine().trim();
        try {
            return Integer.parseInt(rawInput);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String rawValue = scanner.nextLine().trim();
            try {
                return LocalDate.parse(rawValue);
            } catch (DateTimeParseException ex) {
                System.out.println("❌ Invalid date. Please use YYYY-MM-DD format.\n");
            }
        }
    }

    private static BigDecimal readAmount(Scanner scanner) {
        while (true) {
            System.out.print("Enter Amount: ");
            String rawValue = scanner.nextLine().trim();
            try {
                BigDecimal amount = new BigDecimal(rawValue);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("❌ Amount must be greater than zero.\n");
                    continue;
                }
                return amount;
            } catch (NumberFormatException ex) {
                System.out.println("❌ Invalid amount. Please enter a valid numeric amount.\n");
            }
        }
    }

    private static Bank readBank(Scanner scanner, String prompt) {
        return readEnumSelection(scanner, prompt, Bank.class);
    }

    private static Channel readChannel(Scanner scanner) {
        return readEnumSelection(scanner, "Select Channel", Channel.class);
    }

    private static DrCr readDrCr(Scanner scanner) {
        return readEnumSelection(scanner, "Select Entry Type", DrCr.class);
    }

    private static Status readStatus(Scanner scanner) {
        return readEnumSelection(scanner, "Select Status", Status.class);
    }

    private static <T extends Enum<T>> T readEnumSelection(Scanner scanner, String prompt, Class<T> enumClass) {
        List<T> options = Arrays.asList(enumClass.getEnumConstants());

        while (true) {
            System.out.println(prompt + ":");
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ". " + options.get(i));
            }
            System.out.print("Choose option number: ");

            String rawValue = scanner.nextLine().trim();
            try {
                int selectedIndex = Integer.parseInt(rawValue) - 1;
                if (selectedIndex >= 0 && selectedIndex < options.size()) {
                    return options.get(selectedIndex);
                }
            } catch (NumberFormatException ex) {
                // keep prompting with a friendly message below
            }

            System.out.println("❌ Invalid selection. Please enter a number from 1 to " + options.size() + ".\n");
        }
    }

    private static boolean confirm(Scanner scanner, String message) {
        System.out.print(message + "? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        return "y".equals(answer) || "yes".equals(answer);
    }

    private static void importTransactionsFromCsv(
            Scanner scanner,
            SettlementService service,
            SettlementBatch.Builder currentBuilder) throws Exception {
        System.out.print("Enter CSV file path: ");
        String csvPathInput = scanner.nextLine().trim();
        if (csvPathInput.isBlank()) {
            throw new IllegalArgumentException("CSV file path cannot be blank.");
        }

        Path csvPath = Path.of(csvPathInput);
        if (!Files.exists(csvPath) || !Files.isRegularFile(csvPath)) {
            throw new IllegalArgumentException("CSV file not found: " + csvPath);
        }

        List<String> rows = Files.readAllLines(csvPath);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }

        // Keep a fast lookup of in-memory transaction IDs so we can reject duplicates
        // before the batch is submitted.
        Set<String> existingTxnIds = new HashSet<>();
        for (Transaction transaction : currentBuilder.previewTransactions()) {
            existingTxnIds.add(transaction.getTxnId().toUpperCase());
        }

        // Parse and validate all rows first, then add to the builder in one go.
        // This prevents partial imports when a later row is invalid.
        List<Transaction> parsedTransactions = new java.util.ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i).trim();
            if (row.isBlank()) {
                continue;
            }

            // Allow files with a header row; skip it when present.
            if (i == 0 && row.toLowerCase().startsWith("txn_id,")) {
                continue;
            }

            String[] fields = row.split(",", -1);
            if (fields.length < 7) {
                throw new IllegalArgumentException("Invalid CSV format at line " + (i + 1)
                        + ". Expected columns: txn_id,sender_bank,receiver_bank,channel,amount,dr_cr,status");
            }

            String txnId = fields[0].trim();
            if (txnId.isBlank()) {
                throw new IllegalArgumentException("Transaction ID is blank at line " + (i + 1));
            }
            String normalizedTxnId = txnId.toUpperCase();
            if (existingTxnIds.contains(normalizedTxnId)) {
                throw new IllegalArgumentException("Duplicate txn id in current batch/CSV at line " + (i + 1)
                        + ": " + txnId);
            }
            // Also guard against IDs that already exist in persisted transactions.
            if (service.isTransactionAlreadyPersisted(txnId)) {
                throw new IllegalArgumentException("Transaction ID already exists in database at line " + (i + 1)
                        + ": " + txnId);
            }

            Bank senderBank;
            Bank receiverBank;
            Channel channel;
            DrCr drCr;
            Status status;
            try {
                senderBank = Bank.valueOf(fields[1].trim().toUpperCase());
                receiverBank = Bank.valueOf(fields[2].trim().toUpperCase());
                channel = Channel.valueOf(fields[3].trim().toUpperCase());
                drCr = DrCr.valueOf(fields[5].trim().toUpperCase());
                status = Status.valueOf(fields[6].trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid enum value at line " + (i + 1)
                        + ". Allowed banks=" + Arrays.toString(Bank.values())
                        + ", channels=" + Arrays.toString(Channel.values())
                        + ", dr_cr=" + Arrays.toString(DrCr.values())
                        + ", status=" + Arrays.toString(Status.values()));
            }

            if (senderBank == receiverBank) {
                throw new IllegalArgumentException("Sender and receiver bank cannot be same at line " + (i + 1));
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(fields[4].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid amount at line " + (i + 1));
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero at line " + (i + 1));
            }

            // CSV does not carry transaction timestamp; use import time as txn_time.
            Transaction txn = new Transaction(
                    txnId,
                    senderBank,
                    receiverBank,
                    channel,
                    amount,
                    Instant.now(),
                    drCr,
                    status);
            parsedTransactions.add(txn);
            existingTxnIds.add(normalizedTxnId);
        }

        for (Transaction transaction : parsedTransactions) {
            currentBuilder.add(transaction);
        }

        System.out.println("✅ Imported " + parsedTransactions.size() + " transaction(s) from CSV. Current Unsaved Count: "
                + currentBuilder.previewRecordCount() + "\n");
    }
}
