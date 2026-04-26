package com.smartagri.controller;

import com.smartagri.domain.dto.ExpenseDto;
import com.smartagri.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expenses", description = "Endpoints for managing crop expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Create an expense")
    public ResponseEntity<ExpenseDto> createExpense(
            @Valid @RequestBody ExpenseDto expenseDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(expenseDto, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "Get my expenses")
    public ResponseEntity<List<ExpenseDto>> getMyExpenses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getMyExpenses(userDetails.getUsername()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all expenses (ADMIN)")
    public ResponseEntity<List<ExpenseDto>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ExpenseDto> getExpenseById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpenseById(id, userDetails.getUsername()));
    }

    @GetMapping("/crop/{cropId}")
    @Operation(summary = "Get expenses by crop")
    public ResponseEntity<List<ExpenseDto>> getExpensesByCrop(
            @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpensesByCrop(cropId, userDetails.getUsername()));
    }

    @GetMapping("/crop/{cropId}/total")
    @Operation(summary = "Get total expenses for a crop")
    public ResponseEntity<BigDecimal> getTotalByCrop(
            @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getTotalExpenseForCrop(cropId, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ExpenseDto> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDto expenseDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expenseDto, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
