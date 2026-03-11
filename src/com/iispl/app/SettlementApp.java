import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Scanner;

public class SettlementApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SettlementService service = new SettlementService(new SettlementRepository());
        
        // Simulating a working session state
        SettlementBatch.Builder currentBuilder = null;

        while (true) {
            System.out.println("🏦 Banking Settlement System");
            System.out.println("1. Initialize New Settlement Batch");
            System.out.println("2. Add Transaction to Current Batch");
            System.out.println("3. Submit Current Batch to Database");
            System.out.println("4. View Batch Summary from Database");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

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
                        
                        Transaction txn = new Transaction(
                                txnId, Transaction.Channel.UPI, amount, Instant.now(), 
                                Transaction.DrCr.DR, Transaction.Status.SUCCESS
                        );
                        
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
                        currentBuilder = null; // Reset session after saving
                        break;
                        
                    case 4:
                        System.out.print("Enter Batch ID to search: ");
                        String searchId = scanner.nextLine();
                        service.printBatchSummary(searchId);
                        break;
                        
                    case 5:
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
}