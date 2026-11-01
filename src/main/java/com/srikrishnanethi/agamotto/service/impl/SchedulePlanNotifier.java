package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.service.NotificationService;

final class SchedulePlanNotifier {

    private final NotificationService notificationService;

    SchedulePlanNotifier(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    void notifyOwner(Project project, SchedulePlan plan) {
        User owner = project.getOwner();
        if (owner == null) {
            return;
        }
        String summary = plan.getExplanationSummary() == null ? "" : plan.getExplanationSummary();
        notificationService.notifyUser(
                owner,
                null,
                "Schedule generated in " + plan.getMode() + " mode. " + summary);
        for (ScheduleBlock block : plan.getBlocks()) {
            if (block.getDecision() != BlockDecision.EXCLUDED && block.getDecision() != BlockDecision.DELAYED) {
                continue;
            }
            Task task = block.getTask();
            String title = task == null || task.getTitle() == null ? "a task" : task.getTitle();
            String reason = block.getReason() == null ? block.getDecision().name() : block.getReason();
            notificationService.notifyUser(owner, task, "Task '" + title + "' was " +
                    block.getDecision().name().toLowerCase() + ": " + reason);
        }
    }
}
