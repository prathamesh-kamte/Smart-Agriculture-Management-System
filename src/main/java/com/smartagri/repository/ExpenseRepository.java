package com.smartagri.repository;

import com.smartagri.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Expense} entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** All expenses linked to a specific crop. */
    List<Expense> findByCropId(Long cropId);

    /** All expenses recorded by a specific user (farmer). */
    List<Expense> findByRecordedById(Long userId);

    /** All expenses for crops owned by a given farmer. */
    @Query("SELECT e FROM Expense e WHERE e.crop.farmer.id = :farmerId ORDER BY e.expenseDate DESC")
    List<Expense> findAllByFarmerId(Long farmerId);

    /** Sum of expenses for a given crop. */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.crop.id = :cropId")
    BigDecimal sumByCropId(Long cropId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.crop.farmer.id = :farmerId AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate")
    BigDecimal sumExpensesForFarmerInDateRange(@Param("farmerId") Long farmerId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate >= :startDate AND e.expenseDate <= :endDate")
    BigDecimal sumAllExpensesInDateRange(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT YEAR(e.expenseDate), MONTH(e.expenseDate), SUM(e.amount) FROM Expense e WHERE e.crop.farmer.id = :farmerId AND e.expenseDate >= :startDate GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
    List<Object[]> getMonthlyTrendForFarmer(@Param("farmerId") Long farmerId, @Param("startDate") java.time.LocalDate startDate);

    @Query("SELECT YEAR(e.expenseDate), MONTH(e.expenseDate), SUM(e.amount) FROM Expense e WHERE e.expenseDate >= :startDate GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
    List<Object[]> getMonthlyTrendForAdmin(@Param("startDate") java.time.LocalDate startDate);

    @Query("SELECT e FROM Expense e WHERE e.crop.farmer.email = :email " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (CAST(:fromDate AS java.time.LocalDate) IS NULL OR e.expenseDate >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.LocalDate) IS NULL OR e.expenseDate <= :toDate)")
    org.springframework.data.domain.Page<Expense> findByFarmerEmailAndFilters(
            @Param("email") String email,
            @Param("category") String category,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);
}
