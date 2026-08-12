-- Live DBs created before @Column(length = 1000) may still store reason as VARCHAR(255).
-- Hibernate ddl-auto=update does not reliably widen existing MySQL columns.
ALTER TABLE schedule_blocks
    MODIFY COLUMN reason VARCHAR(1000) NOT NULL;
