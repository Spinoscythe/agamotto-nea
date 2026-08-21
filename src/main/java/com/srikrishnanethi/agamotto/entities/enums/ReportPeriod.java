package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Aggregation window stored in {@code dashboard_reports.period}.
 */
public enum ReportPeriod {
    /** Single day ending on as-of. */
    DAILY,
    /** Seven days ending on as-of. */
    WEEKLY,
    /** Thirty days ending on as-of. */
    MONTHLY
}
