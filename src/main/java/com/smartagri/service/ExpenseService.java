package com.smartagri.service;

import com.smartagri.domain.dto.ExpenseDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contract for expense tracking operations.
 */
public interface ExpenseService {

    /** Log a new expense against a crop owned by the authenticated farmer. */
    ExpenseDto createExpense(ExpenseDto expenseDto, String farmerEmail);

    /** Retrieve an expense by ID (validates access). */
    ExpenseDto getExpenseById(Long id, String requesterEmail);

    /** All expenses for a specific crop. */
    List<ExpenseDto> getExpensesByCrop(Long cropId, String requesterEmail);

    /** All expenses for the authenticated farmer across all crops. */
    List<ExpenseDto> getMyExpenses(String farmerEmail);

    /** All expenses in the system (admin only). */
    List<ExpenseDto> getAllExpenses();

    /** Update an existing expense record. */
    ExpenseDto updateExpense(Long id, ExpenseDto expenseDto, String requesterEmail);

    /** Delete an expense record. */
    void deleteExpense(Long id, String requesterEmail);

    /** Total expenditure for a given crop. */
    BigDecimal getTotalExpenseForCrop(Long cropId, String requesterEmail);
}
