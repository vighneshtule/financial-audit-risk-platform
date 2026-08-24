package model;

import java.util.List;

public class RiskTransactionPage {

    private final List<RiskTransactionResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public RiskTransactionPage(
            List<RiskTransactionResponse> content,
            int page,
            int size,
            long totalElements) {

        this(
                content,
                page,
                size,
                totalElements,
                size == 0
                        ? 0
                        : (int) Math.ceil(
                                (double) totalElements / size
                        )
        );
    }

    public RiskTransactionPage(
            List<RiskTransactionResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<RiskTransactionResponse> getContent() {
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