package com.iispl.app;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Scanner;

import com.iispl.entity.SettlementBatch;
import com.iispl.entity.Transaction;
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

        SettlementBatch.Builder currentBuilder = null;

        while (true) {
            System.out.println("🏦 Banking Settlement System");
            System.out.println("1. Initialize New Settlement Batch");
            System.out.println("2. Add Transaction to Current Batch");
            System.out.println("3. Submit Current Batch to Database");
            System.out.println("4. View Batch Summary from Database");
            System.out.println("5. View Clearing House Report (Like Settlement Register)");
            System.out.println("6. Exit");
            System.out.print("Select an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter Batch ID (e.g., BATCH001): ");
                        String batchId = scanner.nextLine();
                        currentBuilder = SettlementBatch.builder(batchId, LocalDate.now());
                        System.out.println("✅ Batch " + batchId + " initialized.\n");
                        break;

                    case 2:
                        if (currentBuilder == null) {
                            System.out.println("❌ Please initialize a batch first (Option 1).\n");
                            break;
                        }
                        System.out.print("Enter Txn ID (e.g., TXN1001): ");
                        String txnId = scanner.nextLine();
                        System.out.print("Enter Amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());

                        Channel channel = readChannel(scanner);
                        DrCr drCr = readDrCr(scanner);
                        Status status = readStatus(scanner);

                        Transaction txn = new Transaction(
                                txnId, channel, amount, Instant.now(),
                                drCr, status);

                        currentBuilder.add(txn);
                        System.out.println("✅ Transaction Added. Current Unsaved Count: " + currentBuilder.previewRecordCount() + "\n");
                        break;

                    case 3:
                        if (currentBuilder == null || currentBuilder.previewRecordCount() == 0) {
                            System.out.println("❌ No pending batch or empty batch to submit.\n");
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
                        System.out.println("Exiting system. Goodbye!");
                        scanner.close();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Invalid option. Try again.\n");
                }
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage() + "\n");
            }
        }
    }

    private static Channel readChannel(Scanner scanner) {
        System.out.print("Enter Channel (UPI/ATM/POS/NETBANKING): ");
        String value = scanner.nextLine().trim().toUpperCase();
        return Channel.valueOf(value);
    }

    private static DrCr readDrCr(Scanner scanner) {
        System.out.print("Enter Entry Type (DR/CR): ");
        String value = scanner.nextLine().trim().toUpperCase();
        return DrCr.valueOf(value);
    }

    private static Status readStatus(Scanner scanner) {
        System.out.print("Enter Status (SUCCESS/FAILED): ");
        String value = scanner.nextLine().trim().toUpperCase();
        return Status.valueOf(value);
    }
}
