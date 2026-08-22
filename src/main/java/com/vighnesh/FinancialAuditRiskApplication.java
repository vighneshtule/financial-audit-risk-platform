package com.vighnesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.vighnesh",
        "controller",
        "repository"
})
public class FinancialAuditRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                FinancialAuditRiskApplication.class,
                args
        );
    }
}