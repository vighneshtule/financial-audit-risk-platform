import model.RiskFinding;
import model.RiskReport;
import model.Transaction;
import repository.TransactionRepository;
import config.DatabaseConnection;

import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;

import service.RiskEngine;
import service.TransactionCsvReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // Check if the JDBC connection is successful.
        try (var connection =
                DatabaseConnection.getConnection()) {

            System.out.println(
                    "Successfully connected to PostgreSQL!"
            );

        } catch (Exception e) {

            System.out.println(
                    "Database connection failed!"
            );

            e.printStackTrace();
        }

        // Tests database connectivity and transaction persistence.
        try {

            TransactionRepository repository =
                    new TransactionRepository();

            Transaction transaction = new Transaction(
                    "TXN-JDBC-003",
                    "JDBC Test Vendor",
                    "EMP-JDBC-001",
                    new BigDecimal("75000.00"),
                    LocalDateTime.of(
                            2026,
                            8,
                            21,
                            10,
                            30
                    ),
                    "Technology"
            );

            // Save transaction to PostgreSQL.
            repository.save(transaction);

            System.out.println(
                    "Transaction saved successfully!"
            );

            // Retrieve the transaction we just saved.
            Transaction saved =
                    repository.findById("TXN-JDBC-003");

            if (saved != null) {

                System.out.println(
                        "Found transaction: "
                                + saved.getId()
                );

                System.out.println(
                        "Vendor: "
                                + saved.getVendor()
                );

                System.out.println(
                        "Amount: ₹"
                                + saved.getAmount()
                );
            }

            // Retrieve all transactions.
            List<Transaction> databaseTransactions =
                    repository.findAll();

            System.out.println(
                    "Transactions in database: "
                            + databaseTransactions.size()
            );

        } catch (Exception e) {

            System.out.println(
                    "Database operation failed!"
            );

            e.printStackTrace();
        }

        System.out.println("=================================================");
        System.out.println("       FINANCIAL AUDIT RISK PLATFORM             ");
        System.out.println("=================================================");
        
        //Reads transactions from CSV file and analyze them for risks.
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

                if (report.getFindings().isEmpty()) {

                    System.out.println(
                            "Status: LOW RISK"
                    );

                } else {

                    System.out.println("Risk Reasons:");

                    for (RiskFinding finding :
                            report.getFindings()) {

                        System.out.println(
                                "  - " + finding.getExplanation()
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