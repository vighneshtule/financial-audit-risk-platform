import repository.TransactionRepository;

import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.AuditApplication;
import service.RiskEngine;
import service.TransactionCsvReader;
import service.TransactionImportService;

public class Main {

    public static void main(String[] args) {

        RiskEngine riskEngine =
                new RiskEngine();

        riskEngine.addRule(
                new HighAmountRule()
        );

        riskEngine.addRule(
                new UnusualTimeRule()
        );

        riskEngine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        TransactionRepository repository =
                new TransactionRepository();

        TransactionCsvReader csvReader =
                new TransactionCsvReader();

        TransactionImportService importService =
                new TransactionImportService(
                        csvReader,
                        repository
                );

        AuditApplication application =
                new AuditApplication(
                        repository,
                        riskEngine,
                        importService
                );

        try {

            application.run(
                    "data/transactions.csv"
            );

        } catch (Exception e) {

            System.err.println(
                    "Application failed: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}