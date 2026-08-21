package service;

import model.Transaction;
import repository.TransactionRepository;

import java.sql.SQLException;
import java.util.List;

public class TransactionImportService {

    private final TransactionCsvReader csvReader;
    private final TransactionRepository transactionRepository;

    public TransactionImportService(
            TransactionCsvReader csvReader,
            TransactionRepository transactionRepository) {

        this.csvReader = csvReader;
        this.transactionRepository = transactionRepository;
    }

    public int importFromCsv(String filePath)
            throws Exception {

        List<Transaction> transactions =
                csvReader.read(filePath);

        int imported = 0;

        for (Transaction transaction : transactions) {

            try {

                transactionRepository.save(transaction);

                imported++;

            } catch (SQLException e) {

                System.out.println(
                        "Could not import transaction "
                                + transaction.getId()
                                + ": "
                                + e.getMessage()
                );
            }
        }

        return imported;
    }
}