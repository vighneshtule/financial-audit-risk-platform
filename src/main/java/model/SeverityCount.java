package model;

public class SeverityCount {

    private final String severity;
    private final int count;

    public SeverityCount(String severity, int count) {
        this.severity = severity;
        this.count = count;
    }

    public String getSeverity() {
        return severity;
    }

    public int getCount() {
        return count;
    }
}
