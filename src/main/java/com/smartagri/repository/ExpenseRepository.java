package com.smartagri.repository;

import com.smartagri.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
