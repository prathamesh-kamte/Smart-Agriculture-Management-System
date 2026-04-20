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

/**
 * Endpoints for logging and querying crop expenses.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expenses", description = "Crop expense tracking")
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * POST /api/expenses — Log a new expense.
     */
    @PostMapping
    @Operation(summary = "Log expense")
    public ResponseEntity<ExpenseDto> createExpense(
            @Valid @RequestBody ExpenseDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(dto, userDetails.getUsername()));
    }

    /**
     * GET /api/expenses — Get all expenses for the authenticated farmer.
     */
    @GetMapping
    @Operation(summary = "Get my expenses")
    public ResponseEntity<List<ExpenseDto>> getMyExpenses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getMyExpenses(userDetails.getUsername()));
    }

    /**
     * GET /api/expenses/all — Get all expenses (ADMIN only).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all expenses (Admin)")
    public ResponseEntity<List<ExpenseDto>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    /**
     * GET /api/expenses/{id} — Get a single expense by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ExpenseDto> getExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpenseById(id, userDetails.getUsername()));
    }

    /**
     * GET /api/expenses/crop/{cropId} — All expenses for a given crop.
     */
    @GetMapping("/crop/{cropId}")
    @Operation(summary = "Get expenses by crop")
    public ResponseEntity<List<ExpenseDto>> getByCrop(
            @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpensesByCrop(cropId, userDetails.getUsername()));
    }

    /**
     * GET /api/expenses/crop/{cropId}/total — Total spend for a crop.
     */
    @GetMapping("/crop/{cropId}/total")
    @Operation(summary = "Get total expense for a crop")
    public ResponseEntity<BigDecimal> getTotalByCrop(
            @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getTotalExpenseForCrop(cropId, userDetails.getUsername()));
    }

    /**
     * PUT /api/expenses/{id} — Update an expense record.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update expense")
    public ResponseEntity<ExpenseDto> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.updateExpense(id, dto, userDetails.getUsername()));
    }

    /**
     * DELETE /api/expenses/{id} — Delete an expense record.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
