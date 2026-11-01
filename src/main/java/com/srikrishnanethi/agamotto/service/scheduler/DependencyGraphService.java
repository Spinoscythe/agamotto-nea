package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Task dependency graph operations.
 *
 * <p>Three jobs:
 * <ol>
 *   <li>{@link #detectCycle(List)} — reject circular dependencies at save time.</li>
 *   <li>{@link #topologicalOrder(List, List)} — order tasks so prerequisites come first.</li>
 *   <li>{@link #criticalPath(List, List)} — forward/backward pass to find slack and the critical chain.</li>
 * </ol>
 *
 * <p>All methods are pure functions over in-memory lists. The scheduler loads the
 * project's tasks and dependency edges once, then passes them here. No DB calls
 * inside the algorithm — makes it fast and trivial to unit-test.
 */
@Service
public class DependencyGraphService {

    private final TaskDependencyRepository dependencyRepository;

    public DependencyGraphService(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    // ---------------------------------------------------------------------
    // 1. Cycle detection — depth-first search with three-colour marking
    // ---------------------------------------------------------------------

    /**
     * True if the given edges contain a cycle.
     * Called before saving a new dependency.
     */
    public boolean hasCycle(List<Task> tasks, List<TaskDependency> edges) {
        Map<String, Task> byId = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        Map<String, List<String>> adj = buildAdjacency(edges);
        Map<String, Integer> state = new HashMap<>(); // 0=unvisited, 1=visiting, 2=done

        for (Task task : tasks) {
            if (state.getOrDefault(task.getId(), 0) == 0) {
                if (dfsCycle(task.getId(), adj, state)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsCycle(String nodeId, Map<String, List<String>> adj, Map<String, Integer> state) {
        state.put(nodeId, 1); // visiting
        for (String neighbour : adj.getOrDefault(nodeId, List.of())) {
            int neighbourState = state.getOrDefault(neighbour, 0);
            if (neighbourState == 1) {
                return true; // back edge → cycle
            }
            if (neighbourState == 0 && dfsCycle(neighbour, adj, state)) {
                return true;
            }
        }
        state.put(nodeId, 2); // done
        return false;
    }

    /**
     * Human-readable cycle description for the UI.
     * Returns empty string if no cycle.
     */
    public String findCyclePath(List<Task> tasks, List<TaskDependency> edges) {
        Map<String, Task> byId = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        Map<String, List<String>> adj = buildAdjacency(edges);
        Map<String, Integer> state = new HashMap<>();
        Map<String, String> parent = new HashMap<>();

        for (Task task : tasks) {
            if (state.getOrDefault(task.getId(), 0) == 0) {
                String cycleStart = dfsCyclePath(task.getId(), adj, state, parent);
                if (cycleStart != null) {
                    // reconstruct cycle
                    List<String> cycle = new ArrayList<>();
                    String cur = cycleStart;
                    do {
                        cycle.add(byId.get(cur).getTitle());
                        cur = parent.get(cur);
                    } while (!cur.equals(cycleStart));
                    cycle.add(byId.get(cycleStart).getTitle());
                    Collections.reverse(cycle);
                    return join(" → ", cycle);
                }
            }
        }
        return "";
    }

    private String dfsCyclePath(String nodeId, Map<String, List<String>> adj,
                                Map<String, Integer> state, Map<String, String> parent) {
        state.put(nodeId, 1);
        for (String neighbour : adj.getOrDefault(nodeId, List.of())) {
            int neighbourState = state.getOrDefault(neighbour, 0);
            if (neighbourState == 1) {
                parent.put(neighbour, nodeId); // close the loop
                return neighbour;
            }
            if (neighbourState == 0) {
                parent.put(neighbour, nodeId);
                String found = dfsCyclePath(neighbour, adj, state, parent);
                if (found != null) {
                    return found;
                }
            }
        }
        state.put(nodeId, 2);
        return null;
    }

    // Helper for Java 16+ String.join that takes Iterable
    private static String join(String delimiter, Iterable<String> elements) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String e : elements) {
            if (!first) sb.append(delimiter);
            sb.append(e);
            first = false;
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // 2. Topological order — Kahn's algorithm (BFS with indegree counting)
    // ---------------------------------------------------------------------

    /**
     * Returns tasks ordered so every task appears after all its prerequisites.
     * If the graph has a cycle, throws {@link IllegalStateException}.
     *
     * <p>Used by the scheduler: the morning queue only admits tasks whose
     * prerequisites are already fully placed. A topological order guarantees
     * we can process tasks in a single pass without re-checking dependencies
     * constantly.
     */
    public List<Task> topologicalOrder(List<Task> tasks, List<TaskDependency> edges) {
        Map<String, Task> byId = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        Map<String, List<String>> adj = buildAdjacency(edges);
        Map<String, Integer> indegree = new HashMap<>();

        // initialise indegree for every task
        for (Task task : tasks) {
            indegree.put(task.getId(), 0);
        }
        // count incoming edges
        for (TaskDependency edge : edges) {
            String to = edge.getTask().getId();
            indegree.put(to, indegree.get(to) + 1);
        }

        // queue of tasks with no unmet prerequisites
        Deque<String> zeroQueue = new ArrayDeque<>();
        for (Task task : tasks) {
            if (indegree.get(task.getId()) == 0) {
                zeroQueue.add(task.getId());
            }
        }

        List<Task> ordered = new ArrayList<>();
        while (!zeroQueue.isEmpty()) {
            String current = zeroQueue.removeFirst();
            ordered.add(byId.get(current));

            for (String neighbour : adj.getOrDefault(current, List.of())) {
                int remaining = indegree.get(neighbour) - 1;
                indegree.put(neighbour, remaining);
                if (remaining == 0) {
                    zeroQueue.add(neighbour);
                }
            }
        }

        if (ordered.size() != tasks.size()) {
            throw new IllegalStateException("Cycle detected — topological order impossible");
        }
        return ordered;
    }

    // ---------------------------------------------------------------------
    // 3. Critical Path Method — forward + backward pass
    // ---------------------------------------------------------------------

    /**
     * Result of the critical path calculation.
     */
    public record CriticalPathResult(
            Map<String, Double> earliestStart,   // hours from project start
            Map<String, Double> earliestFinish,
            Map<String, Double> latestStart,
            Map<String, Double> latestFinish,
            Map<String, Double> totalSlack,      // latestStart - earliestStart
            List<Task> criticalPath              // tasks with zero slack
    ) {}

    /**
     * Computes earliest/latest times and slack for every task.
     *
     * <p>Assumes the input is a DAG (call {@link #hasCycle} first).
     * Durations are taken from {@link ScoringStrategy#getEffectiveDurationHoursFor(Task)}.
     *
     * <p>Forward pass: earliestFinish = earliestStart + duration.
     * A task's earliestStart is the maximum of its prerequisites' earliestFinish.
     *
     * <p>Backward pass: latestStart = latestFinish - duration.
     * A task's latestFinish is the minimum of its dependents' latestStart.
     * The project finish time is the maximum earliestFinish across all tasks.
     *
     * <p>Slack = latestStart - earliestStart. Zero slack = critical path.
     */
    public CriticalPathResult criticalPath(List<Task> tasks, List<TaskDependency> edges,
                                           ScoringStrategy scoring) {
        List<Task> topo = topologicalOrder(tasks, edges);
        Map<String, Task> byId = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        Map<String, List<String>> adj = buildAdjacency(edges);
        Map<String, List<String>> reverseAdj = buildReverseAdjacency(edges);

        Map<String, Double> earliestStart = new HashMap<>();
        Map<String, Double> earliestFinish = new HashMap<>();
        Map<String, Double> latestStart = new HashMap<>();
        Map<String, Double> latestFinish = new HashMap<>();

        // ---- Forward pass ----
        for (Task task : topo) {
            String id = task.getId();
            double duration = scoring.getEffectiveDurationHoursFor(task);

            double es = 0.0;
            for (String prereqId : reverseAdj.getOrDefault(id, List.of())) {
                es = Math.max(es, earliestFinish.getOrDefault(prereqId, 0.0));
            }
            earliestStart.put(id, es);
            earliestFinish.put(id, es + duration);
        }

        // project finish = max of all earliest finishes
        double projectFinish = earliestFinish.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        // ---- Backward pass ----
        List<Task> reverseTopo = new ArrayList<>(topo);
        Collections.reverse(reverseTopo);

        for (Task task : reverseTopo) {
            String id = task.getId();
            double duration = scoring.getEffectiveDurationHoursFor(task);

            double lf = projectFinish;
            if (!adj.getOrDefault(id, List.of()).isEmpty()) {
                lf = adj.get(id).stream()
                        .map(latestStart::get)
                        .mapToDouble(Double::doubleValue)
                        .min()
                        .orElse(projectFinish);
            }
            latestFinish.put(id, lf);
            latestStart.put(id, lf - duration);
        }

        // ---- Slack and critical path ----
        Map<String, Double> totalSlack = new HashMap<>();
        List<Task> critical = new ArrayList<>();

        for (Task task : tasks) {
            String id = task.getId();
            double slack = latestStart.get(id) - earliestStart.get(id);
            totalSlack.put(id, slack);
            if (Math.abs(slack) < 1e-9) { // zero within floating tolerance
                critical.add(task);
            }
        }

        return new CriticalPathResult(earliestStart, earliestFinish,
                latestStart, latestFinish, totalSlack, critical);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Map<String, List<String>> buildAdjacency(List<TaskDependency> edges) {
        Map<String, List<String>> adj = new HashMap<>();
        for (TaskDependency edge : edges) {
            String from = edge.getDependsOnTask().getId(); // prerequisite
            String to = edge.getTask().getId();            // dependent
            adj.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        }
        return adj;
    }

    private Map<String, List<String>> buildReverseAdjacency(List<TaskDependency> edges) {
        Map<String, List<String>> rev = new HashMap<>();
        for (TaskDependency edge : edges) {
            String from = edge.getDependsOnTask().getId();
            String to = edge.getTask().getId();
            rev.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
        }
        return rev;
    }

    // ---------------------------------------------------------------------
    // Public helper used by the scheduler
    // ---------------------------------------------------------------------

    /**
     * True if all prerequisites of {@code task} are in {@code completedTaskIds}.
     * The scheduler calls this before admitting a task to the daily queue.
     */
    public boolean arePrerequisitesMet(Task task, Set<String> completedTaskIds,
                                       List<TaskDependency> allEdges) {
        for (TaskDependency edge : allEdges) {
            if (edge.getTask().getId().equals(task.getId())) {
                String prereqId = edge.getDependsOnTask().getId();
                if (!completedTaskIds.contains(prereqId)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Loads the full dependency graph for a project.
     * One round trip instead of lazy-loading inside the algorithm.
     */
    public List<TaskDependency> loadProjectGraph(String projectId) {
        return dependencyRepository.findByProjectId(projectId);
    }
}