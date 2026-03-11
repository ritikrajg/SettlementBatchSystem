package com.iispl.app;

import java.math.BigDecimal;
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
            System.out.println("8. Exit");
            System.out.print("Select an option: ");

            int choice = readMenuChoice(scanner);

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

                        Bank senderBank = readBank(scanner, "Enter Sender Bank");
                        Bank receiverBank = readBank(scanner, "Enter Receiver Bank");
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
                return new BigDecimal(rawValue);
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
}
