package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.DashboardReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persist generated {@code dashboard_reports}. The HTTP API only {@link #save}s
 * a new snapshot when the user opens the dashboard or a project report.
 * Historical listing finders were removed; old rows remain in MySQL as an
 * audit of past reports.
 */
@Repository
public interface DashboardReportRepository extends JpaRepository<DashboardReport, String> {
}
