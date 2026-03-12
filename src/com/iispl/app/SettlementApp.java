package com.iispl.app;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Scanner;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
import com.iispl.enums.Bank;
import com.iispl.enums.Channel;
import com.iispl.enums.DrCr;
import com.iispl.enums.Status;
import com.iispl.repository.SettlementBatchRepository;
import com.iispl.repository.TransactionRepository;
import com.iispl.service.SettlementService;

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
            System.out.println("🏦 Banking Settlement System");
            System.out.println("1. Initialize New Settlement Batch");
            System.out.println("2. Add Transaction to Current Batch");
            System.out.println("3. Submit Current Batch to Database");
            System.out.println("4. View Batch Summary from Database");
            System.out.println("5. View Clearing House Report (Batch)");
            System.out.println("6. View Bank-wise Settlement Summary");
            System.out.println("7. View All Transactions");
            System.out.println("8. View Current Unsaved Batch");
            System.out.println("9. Exit");
            System.out.print("Select an option: ");

            int choice = readMenuChoice(scanner);

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter Batch ID (e.g., BATCH001): ");
                        String batchId = scanner.nextLine().trim();
                        if (batchId.isBlank()) {
                            throw new IllegalArgumentException("Batch ID cannot be blank.");
                        }
                        if (service.isBatchAlreadySubmitted(batchId)) {
                            throw new IllegalArgumentException("Batch ID already exists in database: " + batchId);
                        }
                        currentBuilder = SettlementBatch.builder(batchId, LocalDate.now());
                        System.out.println("✅ Batch " + batchId + " initialized.\n");
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

                        Bank senderBank = readBank(scanner, "Enter Sender Bank");
                        Bank receiverBank = readBank(scanner, "Enter Receiver Bank");
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

                    case 4:
                        System.out.print("Enter Batch ID to search: ");
                        String searchId = scanner.nextLine();
                        service.printBatchSummary(searchId);
                        break;

                    case 5:
                        System.out.print("Enter Batch ID to generate report: ");
                        String reportBatchId = scanner.nextLine();
                        service.printClearingHouseReport(reportBatchId);
                        break;

                    case 6:
                        service.printBankWiseSummaryReport();
                        break;

                    case 7:
                        service.printAllTransactions();
                        break;

                    case 8:
                        if (currentBuilder == null) {
                            System.out.println("ℹ️ No active unsaved batch.\n");
                            break;
                        }
                        service.printCurrentBatchPreview(currentBuilder);
                        break;

                    case 9:
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

    private static int readMenuChoice(Scanner scanner) {
        String rawInput = scanner.nextLine().trim();
        try {
            return Integer.parseInt(rawInput);
        } catch (NumberFormatException ex) {
            return -1;
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
        while (true) {
            System.out.print(prompt + " (SBI/HDFC/ICICI/AXIS/PNB/BOB): ");
            String value = scanner.nextLine().trim().toUpperCase();
            try {
                return Bank.valueOf(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("❌ Invalid bank. Allowed values: " + Arrays.toString(Bank.values()) + "\n");
            }
        }
    }

    private static Channel readChannel(Scanner scanner) {
        while (true) {
            System.out.print("Enter Channel (UPI/ATM/POS/NETBANKING): ");
            String value = scanner.nextLine().trim().toUpperCase();
            try {
                return Channel.valueOf(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("❌ Invalid channel. Allowed values: " + Arrays.toString(Channel.values()) + "\n");
            }
        }
    }

    private static DrCr readDrCr(Scanner scanner) {
        while (true) {
            System.out.print("Enter Entry Type (DR/CR): ");
            String value = scanner.nextLine().trim().toUpperCase();
            try {
                return DrCr.valueOf(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("❌ Invalid entry type. Allowed values: " + Arrays.toString(DrCr.values()) + "\n");
            }
        }
    }

    private static Status readStatus(Scanner scanner) {
        while (true) {
            System.out.print("Enter Status (SUCCESS/FAILED/PENDING): ");
            String value = scanner.nextLine().trim().toUpperCase();
            try {
                return Status.valueOf(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("❌ Invalid status. Allowed values: " + Arrays.toString(Status.values()) + "\n");
            }
        }
    }

    private static boolean confirm(Scanner scanner, String message) {
        System.out.print(message + "? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        return "y".equals(answer) || "yes".equals(answer);
    }
}
