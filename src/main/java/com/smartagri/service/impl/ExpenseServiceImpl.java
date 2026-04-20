package com.smartagri.service.impl;

import com.smartagri.domain.dto.ExpenseDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.Expense;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.ExpenseRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link ExpenseService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CropRepository cropRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ExpenseDto createExpense(ExpenseDto dto, String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);
        Crop crop = findCropOrThrow(dto.getCropId());
        assertCropOwnerOrAdmin(crop, farmerEmail);

        Expense expense = Expense.builder()
                .description(dto.getDescription())
                .category(dto.getCategory())
                .amount(dto.getAmount())
                .expenseDate(dto.getExpenseDate())
                .receiptReference(dto.getReceiptReference())
                .crop(crop)
                .recordedBy(farmer)
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("Expense id={} logged for crop id={} by {}", saved.getId(), crop.getId(), farmerEmail);
        return toDto(saved);
    }

    @Override
    public ExpenseDto getExpenseById(Long id, String requesterEmail) {
        Expense expense = findExpenseOrThrow(id);
        assertCropOwnerOrAdmin(expense.getCrop(), requesterEmail);
        return toDto(expense);
    }

    @Override
    public List<ExpenseDto> getExpensesByCrop(Long cropId, String requesterEmail) {
        Crop crop = findCropOrThrow(cropId);
        assertCropOwnerOrAdmin(crop, requesterEmail);
        return expenseRepository.findByCropId(cropId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto> getMyExpenses(String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);
        return expenseRepository.findAllByFarmerId(farmer.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseDto updateExpense(Long id, ExpenseDto dto, String requesterEmail) {
        Expense expense = findExpenseOrThrow(id);
        assertCropOwnerOrAdmin(expense.getCrop(), requesterEmail);

        expense.setDescription(dto.getDescription());
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setReceiptReference(dto.getReceiptReference());

        return toDto(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void deleteExpense(Long id, String requesterEmail) {
        Expense expense = findExpenseOrThrow(id);
        assertCropOwnerOrAdmin(expense.getCrop(), requesterEmail);
        expenseRepository.delete(expense);
        log.info("Expense id={} deleted by {}", id, requesterEmail);
    }

    @Override
    public BigDecimal getTotalExpenseForCrop(Long cropId, String requesterEmail) {
        Crop crop = findCropOrThrow(cropId);
        assertCropOwnerOrAdmin(crop, requesterEmail);
        return expenseRepository.sumByCropId(cropId);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Expense findExpenseOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    private Crop findCropOrThrow(Long id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + id));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertCropOwnerOrAdmin(Crop crop, String requesterEmail) {
        boolean isAdmin = userRepository.findByEmail(requesterEmail)
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);
        if (!isAdmin && !crop.getFarmer().getEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("Access denied to crop expenses for crop id: " + crop.getId());
        }
    }

    private ExpenseDto toDto(Expense e) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(e.getId());
        dto.setDescription(e.getDescription());
        dto.setCategory(e.getCategory());
        dto.setAmount(e.getAmount());
        dto.setExpenseDate(e.getExpenseDate());
        dto.setReceiptReference(e.getReceiptReference());
        dto.setCropId(e.getCrop().getId());
        dto.setCropName(e.getCrop().getCropName());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
