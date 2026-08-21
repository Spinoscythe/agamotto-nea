/**
 * In-memory scheduling algorithms (Serenity / Crunch). These types do not issue SQL.
 *
 * <p>Data-structure choices are documented on {@link com.srikrishnanethi.agamotto.service.scheduler.GreedyPlacer},
 * {@link com.srikrishnanethi.agamotto.service.scheduler.BestFitSelector}, and
 * {@link com.srikrishnanethi.agamotto.service.scheduler.SchedulerEngine}: a binary-heap
 * {@code PriorityQueue} for dynamic re-offer, an {@code IdentityHashMap} for remaining hours,
 * {@code ArrayList} copies so JPA input lists are never sorted in place, and a {@code HashSet}
 * for O(1) exclusion membership.
 */
package com.srikrishnanethi.agamotto.service.scheduler;
