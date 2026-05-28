package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Read-only analytics snapshot for a single HARVESTED crop.
 * All monetary fields are in the farmer's default currency;
 * yield fields are in kilograms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldAnalyticsDto {

    /** Primary key of the crop. */
    private Long cropId;

    /** Human-readable name of the crop (e.g. "Wheat"). */
    private String cropName;

    /** Growing season label (e.g. "KHARIF", "RABI", "ZAID"). */
    private String season;

    // ── Yield metrics ────────────────────────────────────────────────────────

    /** Planned yield recorded at sowing time (kg). May be null if not set. */
    private Double expectedYieldKg;

    /** Actual yield recorded at harvest time (kg). May be null if not recorded. */
    private Double actualYieldKg;

    /**
     * Ratio of actual to expected yield, expressed as a percentage.
     * Formula: (actualYieldKg / expectedYieldKg) * 100
     * Null when either yield figure is absent or expectedYieldKg is zero.
     */
    private Double yieldEfficiencyPercent;

    // ── Financial metrics ────────────────────────────────────────────────────

    /** Total recorded expenses for this crop. Zero when no expenses logged. */
    private BigDecimal totalExpenses;

    /**
     * Gross revenue from selling the harvest.
     * Formula: actualYieldKg * sellingPricePerKg
     * Null when either field is absent.
     */
    private BigDecimal revenue;

    /**
     * Net profit or loss after expenses.
     * Formula: revenue - totalExpenses
     * Null when revenue cannot be calculated.
     */
    private BigDecimal profitLoss;

    /**
     * Categorical outcome derived from {@link #profitLoss}.
     * <ul>
     *   <li>PROFIT — profitLoss &gt; 0</li>
     *   <li>LOSS — profitLoss &lt; 0</li>
     *   <li>BREAK_EVEN — profitLoss == 0</li>
     * </ul>
     * Null when profitLoss cannot be calculated.
     */
    private String profitStatus;
}
