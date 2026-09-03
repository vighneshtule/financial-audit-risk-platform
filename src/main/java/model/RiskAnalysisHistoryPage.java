package model;

import java.util.List;

public class RiskAnalysisHistoryPage {

    private final String transactionId;
    private final List<RiskAnalysisHistoryItem> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public RiskAnalysisHistoryPage(
            String transactionId,
            List<RiskAnalysisHistoryItem> content,
            int page,
            int size,
            long totalElements) {

        this.transactionId = transactionId;
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil(
                        (double) totalElements / size
                );
    }

    public String getTransactionId() {
        return transactionId;
    }

    public List<RiskAnalysisHistoryItem> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
