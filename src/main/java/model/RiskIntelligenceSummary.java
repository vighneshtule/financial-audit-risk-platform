package model;

import java.util.List;

public class RiskIntelligenceSummary {

    private final DatasetOverview overview;
    private final List<SeverityCount> severityDistribution;
    private final List<VendorRisk> vendors;
    private final List<EmployeeRisk> employees;
    private final List<RiskTypeDistribution> riskTypes;
    private final List<CategoryRisk> categories;

    public RiskIntelligenceSummary(
            DatasetOverview overview,
            List<SeverityCount> severityDistribution,
            List<VendorRisk> vendors,
            List<EmployeeRisk> employees,
            List<RiskTypeDistribution> riskTypes,
            List<CategoryRisk> categories) {

        this.overview = overview;
        this.severityDistribution = severityDistribution;
        this.vendors = vendors;
        this.employees = employees;
        this.riskTypes = riskTypes;
        this.categories = categories;
    }

    public DatasetOverview getOverview() {
        return overview;
    }

    public List<SeverityCount> getSeverityDistribution() {
        return severityDistribution;
    }

    public List<VendorRisk> getVendors() {
        return vendors;
    }

    public List<EmployeeRisk> getEmployees() {
        return employees;
    }

    public List<RiskTypeDistribution> getRiskTypes() {
        return riskTypes;
    }

    public List<CategoryRisk> getCategories() {
        return categories;
    }
}
