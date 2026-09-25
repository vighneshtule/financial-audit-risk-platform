package com.vighnesh.controller;

import com.vighnesh.service.TransactionImportFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/transactions")
public class TransactionImportController {

    private final TransactionImportFacade importFacade;

    public TransactionImportController(
            TransactionImportFacade importFacade) {
        this.importFacade = importFacade;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importCsv(
            @RequestParam(name = "file") MultipartFile file)
            throws Exception {

        // Validate: file must be present
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file is missing or empty"
            );
        }

        // Validate: filename must exist and end with .csv
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename is missing"
            );
        }
        if (!originalFilename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException(
                    "Only CSV files are accepted (.csv extension required)"
            );
        }

        // Write to a temp file, call existing service, then delete
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("aurex-import-", ".csv");
            file.transferTo(tempFile);

            int count = importFacade.importFromCsv(
                    tempFile.toAbsolutePath().toString()
            );

            return ResponseEntity.ok(
                    new ImportResponse(
                            "Transactions imported successfully",
                            count
                    )
            );

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            // Wrap import/parse/DB failures as a descriptive
            // IllegalArgumentException so the global handler
            // returns 400 with the cause message.
            throw new IllegalArgumentException(
                    "Import failed: " + e.getMessage()
            );

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Best-effort cleanup; temp file will be
                    // cleaned by the OS on next reboot at worst.
                }
            }
        }
    }
}
