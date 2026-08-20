import model.RiskReport;
import model.Transaction;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("     FINANCIAL AUDIT RISK PLATFORM - TESTS       ");
        System.out.println("=================================================");

        RiskEngine engine = new RiskEngine();
        engine.addRule(new HighAmountRule());
        engine.addRule(new UnusualTimeRule());
        engine.addDatasetRule(new DuplicateTransactionRule());

        runTest1(engine);
        runTest2(engine);
        runTest3(engine);
        runTest4(engine);
        runTest5(engine);
    }

    // Test 1: Two identical transactions 5 minutes apart -> duplicate
    private static void runTest1(RiskEngine engine) {
        System.out.println("\n--- [TEST 1] Identical Transactions 5 Mins Apart ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", 50_000, baseTime, "Office Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", 50_000, baseTime.plusMinutes(5), "Office Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        RiskReport r1 = engine.analyze(t1, dataset);
        RiskReport r2 = engine.analyze(t2, dataset);

        printResult("TXN001", r1, true);
        printResult("TXN002", r2, true);
    }

    // Test 2: Two identical transactions 20 minutes apart -> not duplicate
    private static void runTest2(RiskEngine engine) {
        System.out.println("\n--- [TEST 2] Identical Transactions 20 Mins Apart ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", 50_000, baseTime, "Office Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", 50_000, baseTime.plusMinutes(20), "Office Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        RiskReport r1 = engine.analyze(t1, dataset);
        RiskReport r2 = engine.analyze(t2, dataset);

        printResult("TXN001", r1, false);
        printResult("TXN002", r2, false);
    }

    // Test 3: Different vendor -> not duplicate
    private static void runTest3(RiskEngine engine) {
        System.out.println("\n--- [TEST 3] Different Vendor ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", 50_000, baseTime, "Office Supplies");
        Transaction t2 = new Transaction("TXN002", "XYZ Traders", "EMP101", 50_000, baseTime.plusMinutes(5), "Office Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        RiskReport r1 = engine.analyze(t1, dataset);
        RiskReport r2 = engine.analyze(t2, dataset);

        printResult("TXN001", r1, false);
        printResult("TXN002", r2, false);
    }

    // Test 4: Same vendor but different amount -> not duplicate
    private static void runTest4(RiskEngine engine) {
        System.out.println("\n--- [TEST 4] Different Amount ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", 50_000, baseTime, "Office Supplies");
        Transaction t2 = new Transaction("TXN002", "ABC Suppliers", "EMP101", 20_000, baseTime.plusMinutes(5), "Office Supplies");

        List<Transaction> dataset = List.of(t1, t2);
        RiskReport r1 = engine.analyze(t1, dataset);
        RiskReport r2 = engine.analyze(t2, dataset);

        printResult("TXN001", r1, false);
        printResult("TXN002", r2, false);
    }

    // Test 5: Same transaction ID -> don't compare itself
    private static void runTest5(RiskEngine engine) {
        System.out.println("\n--- [TEST 5] Single Transaction (Self-Comparison Check) ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 20, 10, 0);

        Transaction t1 = new Transaction("TXN001", "ABC Suppliers", "EMP101", 50_000, baseTime, "Office Supplies");

        List<Transaction> dataset = List.of(t1);
        RiskReport r1 = engine.analyze(t1, dataset);

        printResult("TXN001", r1, false);
    }

    private static void printResult(String txnId, RiskReport report, boolean expectDuplicate) {
        boolean isDuplicate = report.getReasons().contains("Possible duplicate transaction detected");
        String status = (isDuplicate == expectDuplicate) ? "PASSED" : "FAILED";

        System.out.println(String.format("  [%s] %s | Score: %d/100 | Reasons: %s",
                status, txnId, report.getRiskScore(), report.getReasons().isEmpty() ? "None" : report.getReasons()));
    }
}