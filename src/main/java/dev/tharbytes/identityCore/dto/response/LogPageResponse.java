package dev.tharbytes.identityCore.dto.response;

import java.util.List;
import java.util.Map;

public class LogPageResponse {
    private List<LogEntry> logEntries;
    private Map<String, Long> logStats;
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private int totalItems;
    private boolean hasPrev;
    private boolean hasNext;

    public LogPageResponse() {}

    public LogPageResponse(List<LogEntry> logEntries, Map<String, Long> logStats, int currentPage, int totalPages, int pageSize, int totalItems, boolean hasPrev, boolean hasNext) {
        this.logEntries = logEntries;
        this.logStats = logStats;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.hasPrev = hasPrev;
        this.hasNext = hasNext;
    }

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    public Map<String, Long> getLogStats() {
        return logStats;
    }

    public void setLogStats(Map<String, Long> logStats) {
        this.logStats = logStats;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public boolean isHasPrev() {
        return hasPrev;
    }

    public void setHasPrev(boolean hasPrev) {
        this.hasPrev = hasPrev;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
