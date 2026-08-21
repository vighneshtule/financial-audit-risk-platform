package service;

import model.Transaction;
import org.junit.jupiter.api.Test;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionImportServiceTest {

    @Test
    void failedImportShouldRollbackAllTransactions()
            throws Exception {

        TransactionRepository repository =
                new TransactionRepository();

        TransactionCsvReader csvReader =
                new TransactionCsvReader();

        TransactionImportService importService =
                new TransactionImportService(
                        csvReader,
                        repository
                );

        int beforeCount =
                repository.findAll().size();

        assertThrows(
                Exception.class,
                () -> importService.importFromCsv(
                        "data/transactions.csv"
                )
        );

        int afterCount =
                repository.findAll().size();

        assertEquals(
                beforeCount,
                afterCount,
                "Database should remain unchanged after failed import"
        );
    }
}