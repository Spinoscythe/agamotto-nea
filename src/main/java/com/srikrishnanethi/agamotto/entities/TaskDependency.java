package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.util.Objects;

/**
 * Junction row: {@code taskId} must be scheduled after {@code dependsOnTaskId}.
 * Maps to {@code task_dependencies}.
 */
@Entity
@Table(name = "task_dependencies")
@IdClass(TaskDependency.Key.class)
@Comment("Task prerequisite links; a task cannot start until all its dependencies are placed")
public class TaskDependency {

    @Id
    @Comment("The task that has the prerequisite (FK tasks.id)")
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Id
    @Comment("The prerequisite task that must finish first (FK tasks.id)")
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOnTask;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getDependsOnTask() {
        return dependsOnTask;
    }

    public void setDependsOnTask(Task dependsOnTask) {
        this.dependsOnTask = dependsOnTask;
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "task_id", length = 36)
        private String taskId;

        @Column(name = "depends_on_task_id", length = 36)
        private String dependsOnTaskId;

        public Key() {}

        public Key(String taskId, String dependsOnTaskId) {
            this.taskId = taskId;
            this.dependsOnTaskId = dependsOnTaskId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getDependsOnTaskId() {
            return dependsOnTaskId;
        }

        public void setDependsOnTaskId(String dependsOnTaskId) {
            this.dependsOnTaskId = dependsOnTaskId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(taskId, key.taskId)
                    && Objects.equals(dependsOnTaskId, key.dependsOnTaskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, dependsOnTaskId);
        }
    }
}