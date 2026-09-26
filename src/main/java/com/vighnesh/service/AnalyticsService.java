package com.vighnesh.service;

import model.*;
import org.springframework.stereotype.Service;
import repository.AnalyticsRepository;

import java.util.List;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public RiskIntelligenceSummary getRiskIntelligenceSummary() throws Exception {
        DatasetOverview overview = analyticsRepository.getDatasetOverview();
        List<SeverityCount> severityDistribution = analyticsRepository.getSeverityDistribution();
        List<VendorRisk> vendors = analyticsRepository.getVendorRisk();
        List<EmployeeRisk> employees = analyticsRepository.getEmployeeRisk();
        List<RiskTypeDistribution> riskTypes = analyticsRepository.getRiskTypeDistribution();
        List<CategoryRisk> categories = analyticsRepository.getCategoryRisk();

        return new RiskIntelligenceSummary(
                overview,
                severityDistribution,
                vendors,
                employees,
                riskTypes,
                categories
        );
    }
}
