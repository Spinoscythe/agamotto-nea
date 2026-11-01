package com.srikrishnanethi.agamotto.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops the retired {@code projects.estimated_effort_hours} column. Hibernate's
 * {@code ddl-auto=update} never removes columns, so a live schema would
 * otherwise keep a {@code NOT NULL} column that the entity no longer writes.
 */
@Component
public class ProjectEffortHoursColumnMigrator implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public ProjectEffortHoursColumnMigrator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(@NonNull ApplicationArguments args) {
		Integer count;
		try {
			count = jdbcTemplate.queryForObject(
					"""
							SELECT COUNT(*)
							FROM information_schema.COLUMNS
							WHERE TABLE_SCHEMA = DATABASE()
							  AND TABLE_NAME = 'projects'
							  AND COLUMN_NAME = 'estimated_effort_hours'
							""",
					Integer.class);
		}
		catch (DataAccessException ex) {
			return;
		}
		if (count == null || count == 0) {
			return;
		}
		try {
			jdbcTemplate.execute("ALTER TABLE projects DROP COLUMN estimated_effort_hours");
		}
		catch (DataAccessException ignored) {
			// Leave the column as-is; project writes no longer reference it.
		}
	}
}
