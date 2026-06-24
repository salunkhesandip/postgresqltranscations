package com.cleancoders.postgresqltranscations.dto;

import com.cleancoders.postgresqltranscations.validation.ValidDateRange;
import com.cleancoders.postgresqltranscations.validation.ValidSalaryRange;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidSalaryRange
@ValidDateRange
public class EmployeeSearchCriteria {

    // Pagination parameters
    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;

    // Filter parameters (all optional)
    private String name; // Partial match, case-insensitive

    @PositiveOrZero(message = "Minimum salary must be zero or positive")
    private BigDecimal minSalary;

    @PositiveOrZero(message = "Maximum salary must be zero or positive")
    private BigDecimal maxSalary;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAfter;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdBefore;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }

    public LocalDate getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDate createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDate getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDate createdBefore) {
        this.createdBefore = createdBefore;
    }
}

