package com.vighnesh.service;

import config.RiskConfiguration;
import org.springframework.stereotype.Component;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;

@Component
public class RiskEngineFactory {

    private final RiskConfiguration riskConfiguration;

    public RiskEngineFactory(RiskConfiguration riskConfiguration) {
        this.riskConfiguration = riskConfiguration;
    }

    public RiskEngine create() {
        RiskEngine engine = new RiskEngine();

        engine.addRule(
                new HighAmountRule(riskConfiguration.getHighAmount())
        );

        engine.addRule(
                new UnusualTimeRule(riskConfiguration.getUnusualTime())
        );

        engine.addDatasetRule(
                new DuplicateTransactionRule(riskConfiguration.getDuplicate())
        );

        return engine;
    }
}
