package com.smartagri.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO used for creating and responding with Expense records.
 */
@Data
public class ExpenseDto {

    /** Null on create; populated on responses. */
    private Long id;

    @NotBlank(message = "Description is required")
    @Size(max = 150)
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 80)
    private String category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private String receiptReference;

    @NotNull(message = "Crop ID is required")
    private Long cropId;

    /** Populated on responses. */
    private String cropName;

    /** Populated on responses. */
    private LocalDateTime createdAt;
}
