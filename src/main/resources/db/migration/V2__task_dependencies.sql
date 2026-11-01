-- Task dependency graph (3NF junction table)
-- task_id depends on depends_on_task_id, both within the same project.
-- Composite PK prevents duplicate edges. FKs cascade delete when either task is removed.

CREATE TABLE task_dependencies (
    task_id          CHAR(36) NOT NULL,
    depends_on_task_id  CHAR(36) NOT NULL,
    PRIMARY KEY (task_id, depends_on_task_id),
    CONSTRAINT fk_dep_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_dep_depends_on
        FOREIGN KEY (depends_on_task_id) REFERENCES tasks (id)
        ON DELETE CASCADE,
    -- A task cannot depend on itself
    CONSTRAINT chk_no_self_depend CHECK (task_id <> depends_on_task_id)
);

-- Helpful index for "what must finish before this task can start?"
CREATE INDEX idx_task_dependencies_depends_on ON task_dependencies (depends_on_task_id);