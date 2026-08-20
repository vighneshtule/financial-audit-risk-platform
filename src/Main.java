import model.RiskReport;
import model.Transaction;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;


import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        Transaction transaction = new Transaction(
                "TXN001",
                "ABC Suppliers",
                "EMP101",
                150_000,
                LocalDateTime.now(),
                "Office Supplies");

        RiskEngine riskEngine = new RiskEngine();

        riskEngine.addRule(new HighAmountRule());
        riskEngine.addRule(new UnusualTimeRule());

        RiskReport report = riskEngine.analyze(transaction);

        System.out.println("=================================");
        System.out.println("   FINANCIAL RISK ANALYZER");
        System.out.println("=================================");

        System.out.println("Transaction ID : " + transaction.getId());
        System.out.println("Vendor         : " + transaction.getVendor());
        System.out.println("Employee       : " + transaction.getEmployee());
        System.out.println("Amount         : ₹" + transaction.getAmount());
        System.out.println("Category       : " + transaction.getCategory());

        System.out.println("---------------------------------");

        System.out.println("Risk Score     : "
        + report.getRiskScore() + "/100");

        System.out.println("Risk Reasons:");

        for (String reason : report.getReasons()) {
            System.out.println("- " + reason);
        }

        if (report.getRiskScore() >= 80) {
            System.out.println("Risk Level     : CRITICAL");
        } else if (report.getRiskScore() >= 60) {
            System.out.println("Risk Level     : HIGH");
        } else if (report.getRiskScore() >= 30) {
            System.out.println("Risk Level     : MEDIUM");
        } else {
            System.out.println("Risk Level     : LOW");
        }
    }
}