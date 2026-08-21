package service;

import model.RiskFinding;
import model.RiskReport;
import model.Transaction;
import repository.TransactionRepository;

import java.sql.SQLException;
import java.util.List;

public class AuditApplication {

    private final TransactionRepository transactionRepository;
    private final RiskEngine riskEngine;
    private final TransactionImportService importService;
    

    public AuditApplication(
            TransactionRepository transactionRepository,
            RiskEngine riskEngine,
            TransactionImportService importService) {

        this.transactionRepository =
                transactionRepository;

        this.riskEngine = riskEngine;

        this.importService = importService;
    }

    public void run(String csvPath)
            throws Exception {

        int imported =
                importService.importFromCsv(csvPath);

        System.out.println(
                "Imported transactions: "
                        + imported
        );

        List<Transaction> transactions =
                transactionRepository.findAll();

        System.out.println(
                "Transactions in database: "
                        + transactions.size()
        );

        for (Transaction transaction : transactions) {

            RiskReport report =
                    riskEngine.analyze(
                            transaction,
                            transactions
                    );

            printReport(transaction, report);
        }
    }

    private void printReport(
            Transaction transaction,
            RiskReport report) {

        System.out.println();
        System.out.println(
                "-----------------------------------------"
        );

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

        System.out.println(
                "Risk Level: "
                        + report.getRiskLevel()
        );

        if (report.getFindings().isEmpty()) {

            System.out.println(
                    "Status: LOW RISK"
            );

        } else {

            System.out.println(
                    "Risk Findings:"
            );

            for (RiskFinding finding :
                    report.getFindings()) {

                System.out.println(
                        "  - "
                                + finding.getType()
                                + " | "
                                + finding.getSeverity()
                                + " | +"
                                + finding.getScore()
                                + " | "
                                + finding.getExplanation()
                );
            }
        }
    }
}