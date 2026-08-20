package service;

import model.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionCsvReader {

    public List<Transaction> read(String filePath) throws IOException {

        List<Transaction> transactions = new ArrayList<>();

        List<String> lines = Files.readAllLines(Path.of(filePath));

        // Skip header
        for (int i = 1; i < lines.size(); i++) {

            String line = lines.get(i);

            String[] fields = line.split(",");

            String id = fields[0];
            String vendor = fields[1];
            String employee = fields[2];
            double amount = Double.parseDouble(fields[3]);
            LocalDateTime transactionTime =
                    LocalDateTime.parse(fields[4]);
            String category = fields[5];

            Transaction transaction = new Transaction(
                    id,
                    vendor,
                    employee,
                    amount,
                    transactionTime,
                    category
            );

            transactions.add(transaction);
        }

        return transactions;
    }
}