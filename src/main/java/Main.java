import model.RiskReport;
import model.Transaction;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;
import service.TransactionCsvReader;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("       FINANCIAL AUDIT RISK PLATFORM             ");
        System.out.println("=================================================");

        try {

            TransactionCsvReader reader =
                    new TransactionCsvReader();

            List<Transaction> transactions =
                    reader.read("data/transactions.csv");

            System.out.println(
                    "Loaded transactions: "
                            + transactions.size()
            );

            RiskEngine engine = new RiskEngine();

            engine.addRule(new HighAmountRule());
            engine.addRule(new UnusualTimeRule());

            engine.addDatasetRule(
                    new DuplicateTransactionRule()
            );

            for (Transaction transaction : transactions) {

                RiskReport report =
                        engine.analyze(
                                transaction,
                                transactions
                        );

                System.out.println();
                System.out.println("-----------------------------------------");

                System.out.println(
                        "Transaction: "
                                + transaction.getId()
                );

                System.out.println(
                        "Vendor: "
                                + transaction.getVendor()
                );

                System.out.println(
                        "Amount: ₹"
                                + transaction.getAmount()
                );

                System.out.println(
                        "Risk Score: "
                                + report.getRiskScore()
                                + "/100"
                );

                if (report.getReasons().isEmpty()) {

                    System.out.println(
                            "Status: LOW RISK"
                    );

                } else {

                    System.out.println("Risk Reasons:");

                    for (String reason :
                            report.getReasons()) {

                        System.out.println(
                                "  - " + reason
                        );
                    }
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Error reading transaction file: "
                            + e.getMessage()
            );
        }
    }
}