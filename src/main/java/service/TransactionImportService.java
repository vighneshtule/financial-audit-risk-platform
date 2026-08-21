package service;

import model.Transaction;
import repository.TransactionRepository;

import java.sql.Connection;

import java.util.List;


import config.DatabaseConnection;

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

        try (Connection connection =
                    DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                for (Transaction transaction :
                        transactions) {

                    transactionRepository.save(
                            connection,
                            transaction
                    );

                    imported++;
                }

                connection.commit();

                return imported;

            } catch (Exception e) {

                connection.rollback();

                throw e;
            }
        }
    }
}