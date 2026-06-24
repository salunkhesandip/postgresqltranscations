package com.cleancoders.postgresqltranscations.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public class PagedEmployeeResponse {

    private List<EmployeeDTO> content;
    private PaginationMetadata pagination;

    public PagedEmployeeResponse() {
    }

    public PagedEmployeeResponse(List<EmployeeDTO> content, PaginationMetadata pagination) {
        this.content = content;
        this.pagination = pagination;
    }

    /**
     * Factory method to convert Spring Data Page<EmployeeDTO> to PagedEmployeeResponse.
     */
    public static PagedEmployeeResponse fromPage(Page<EmployeeDTO> page) {
        PagedEmployeeResponse response = new PagedEmployeeResponse();
        response.setContent(page.getContent());
        response.setPagination(PaginationMetadata.fromPage(page));
        return response;
    }

    public List<EmployeeDTO> getContent() {
        return content;
    }

    public void setContent(List<EmployeeDTO> content) {
        this.content = content;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }
}

