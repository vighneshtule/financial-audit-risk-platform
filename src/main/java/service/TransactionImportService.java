package service;

import model.Transaction;
import repository.TransactionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import config.DatabaseConnection;

public class TransactionImportService {

    private final TransactionCsvReader csvReader;
    private final TransactionRepository transactionRepository;
    private final javax.sql.DataSource dataSource;

    public TransactionImportService(
            TransactionCsvReader csvReader,
            TransactionRepository transactionRepository) {

        this(csvReader, transactionRepository, null);
    }

    public TransactionImportService(
            TransactionCsvReader csvReader,
            TransactionRepository transactionRepository,
            javax.sql.DataSource dataSource) {

        this.csvReader = csvReader;
        this.transactionRepository = transactionRepository;
        this.dataSource = dataSource;
    }

    private Connection openConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return DatabaseConnection.getConnection();
    }

    public int importFromCsv(String filePath)
            throws Exception {

        List<Transaction> transactions =
                csvReader.read(filePath);

        int imported = 0;

        try (Connection connection =
                    openConnection()) {

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