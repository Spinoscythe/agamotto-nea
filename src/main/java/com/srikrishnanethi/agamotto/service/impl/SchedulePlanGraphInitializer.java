package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import org.hibernate.Hibernate;

final class SchedulePlanGraphInitializer {

    private SchedulePlanGraphInitializer() {
    }

    static void initializePlanGraph(SchedulePlan plan) {
        if (plan.getProject() != null) {
            Hibernate.initialize(plan.getProject());
        }
        Hibernate.initialize(plan.getBlocks());
        for (ScheduleBlock block : plan.getBlocks()) {
            initializeBlockGraph(block);
        }
    }

    static void initializeBlockGraph(ScheduleBlock block) {
        if (block.getSchedule() != null) {
            Hibernate.initialize(block.getSchedule());
            if (block.getSchedule().getProject() != null) {
                Hibernate.initialize(block.getSchedule().getProject());
            }
        }
        if (block.getTask() != null) {
            Hibernate.initialize(block.getTask());
        }
    }
}
