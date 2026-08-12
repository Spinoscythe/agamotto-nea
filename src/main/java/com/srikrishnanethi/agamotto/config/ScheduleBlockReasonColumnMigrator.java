package com.srikrishnanethi.agamotto.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Widens {@code schedule_blocks.reason} when a live MySQL schema is still VARCHAR(255).
 * Flyway is not enabled; this applies the same change as
 * {@code db/migration/V1__widen_schedule_blocks_reason.sql}.
 */
@Component
public class ScheduleBlockReasonColumnMigrator implements ApplicationRunner {

	private static final int TARGET_LENGTH = 1000;

	private final JdbcTemplate jdbcTemplate;

	public ScheduleBlockReasonColumnMigrator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		Integer length;
		try {
			length = jdbcTemplate.query(
					"""
							SELECT CHARACTER_MAXIMUM_LENGTH
							FROM information_schema.COLUMNS
							WHERE TABLE_SCHEMA = DATABASE()
							  AND TABLE_NAME = 'schedule_blocks'
							  AND COLUMN_NAME = 'reason'
							""",
					rs -> {
						if (!rs.next()) {
							return null;
						}
						Object value = rs.getObject(1);
						return value instanceof Number number ? number.intValue() : null;
					});
		}
		catch (DataAccessException ex) {
			return;
		}
		if (length == null || length >= TARGET_LENGTH) {
			return;
		}
		try {
			jdbcTemplate.execute("ALTER TABLE schedule_blocks MODIFY COLUMN reason VARCHAR("
					+ TARGET_LENGTH + ") NOT NULL");
		}
		catch (DataAccessException ignored) {
			// Leave the column as-is; generate/override still clamp reason on write.
		}
	}
}
