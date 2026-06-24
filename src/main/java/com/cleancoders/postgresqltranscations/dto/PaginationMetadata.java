package com.cleancoders.postgresqltranscations.dto;

import org.springframework.data.domain.Page;

public class PaginationMetadata {

    private long totalElements;  // Total number of employees matching the search criteria
    private int totalPages;      // Total number of pages
    private int currentPage;     // Current page number (zero-based)
    private int pageSize;        // Number of items in current page
    private boolean hasNext;     // True if there is a next page
    private boolean hasPrevious; // True if there is a previous page

    public PaginationMetadata() {
    }

    public PaginationMetadata(long totalElements, int totalPages, int currentPage, int pageSize, boolean hasNext, boolean hasPrevious) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    /**
     * Factory method to extract pagination metadata from Spring Data Page.
     */
    public static PaginationMetadata fromPage(Page<?> page) {
        PaginationMetadata metadata = new PaginationMetadata();
        metadata.setTotalElements(page.getTotalElements());
        metadata.setTotalPages(page.getTotalPages());
        metadata.setCurrentPage(page.getNumber());
        metadata.setPageSize(page.getSize());
        metadata.setHasNext(page.hasNext());
        metadata.setHasPrevious(page.hasPrevious());
        return metadata;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}

