package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.DashboardReport;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardReportRepository extends JpaRepository<DashboardReport, String> {

	List<DashboardReport> findByUserIdAndPeriodOrderByGeneratedAtDesc(String userId, ReportPeriod period);

	Optional<DashboardReport> findFirstByUserIdAndPeriodOrderByGeneratedAtDesc(String userId, ReportPeriod period);
}
