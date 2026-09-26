package model;

public class TransactionContext {
    private String transactionId;
    private RelatedGroup vendor;
    private RelatedGroup employee;
    private RelatedGroup category;
    private EvidenceSummary evidenceSummary;

    public TransactionContext() {}

    public TransactionContext(String transactionId, RelatedGroup vendor, RelatedGroup employee, RelatedGroup category, EvidenceSummary evidenceSummary) {
        this.transactionId = transactionId;
        this.vendor = vendor;
        this.employee = employee;
        this.category = category;
        this.evidenceSummary = evidenceSummary;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public RelatedGroup getVendor() {
        return vendor;
    }

    public void setVendor(RelatedGroup vendor) {
        this.vendor = vendor;
    }

    public RelatedGroup getEmployee() {
        return employee;
    }

    public void setEmployee(RelatedGroup employee) {
        this.employee = employee;
    }

    public RelatedGroup getCategory() {
        return category;
    }

    public void setCategory(RelatedGroup category) {
        this.category = category;
    }

    public EvidenceSummary getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(EvidenceSummary evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }
}
