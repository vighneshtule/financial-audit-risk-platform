package com.vighnesh.service;

import config.RiskConfiguration;
import org.springframework.stereotype.Component;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.RoundAmountRule;
import rule.TransactionVelocityRule;
import rule.UnusualTimeRule;
import rule.VendorConcentrationRule;
import service.RiskEngine;

@Component
public class RiskEngineFactory {

    private final RiskConfiguration riskConfiguration;

    public RiskEngineFactory(RiskConfiguration riskConfiguration) {
        this.riskConfiguration = riskConfiguration;
    }

    public RiskEngine create() {
        RiskEngine engine = new RiskEngine(new RiskScoreCalculator());

        engine.addRule(
                new HighAmountRule(riskConfiguration.getHighAmount())
        );

        engine.addRule(
                new UnusualTimeRule(riskConfiguration.getUnusualTime())
        );

        engine.addRule(
                new RoundAmountRule(riskConfiguration.getRoundAmount())
        );

        engine.addDatasetRule(
                new DuplicateTransactionRule(riskConfiguration.getDuplicate())
        );

        engine.addDatasetRule(
                new TransactionVelocityRule(riskConfiguration.getTransactionVelocity())
        );

        engine.addDatasetRule(
                new VendorConcentrationRule(riskConfiguration.getVendorConcentration())
        );

        return engine;
    }
}
