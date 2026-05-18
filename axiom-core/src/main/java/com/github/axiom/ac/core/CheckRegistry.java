package com.github.axiom.ac.core;

import com.github.axiom.ac.api.Check;
import com.github.axiom.ac.api.EventBus;
import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.api.Violation;
import com.github.axiom.ac.api.event.CheckFaultEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the registered {@link Check}s and runs them against player
 * data. Each check runs inside a try/catch, so a throwing check
 * cannot break the others. Consecutive faults are counted per check;
 * a successful inspection resets the count. A check that reaches the
 * fault threshold is removed and a {@link CheckFaultEvent} is
 * published.
 */
public final class CheckRegistry {

    private static final int DEFAULT_FAULT_THRESHOLD = 3;

    private final EventBus eventBus;
    private final int faultThreshold;
    private final Map<String, Check> checks = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFaults = new ConcurrentHashMap<>();

    public CheckRegistry(EventBus eventBus) {
        this(eventBus, DEFAULT_FAULT_THRESHOLD);
    }

    public CheckRegistry(EventBus eventBus, int faultThreshold) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        if (faultThreshold < 1) {
            throw new IllegalArgumentException("faultThreshold must be >= 1");
        }
        this.faultThreshold = faultThreshold;
    }

    /** Registers {@code check}, keyed by its id. */
    public void register(Check check) {
        checks.put(check.id(), check);
    }

    /** Removes the check with the given id, if present. */
    public void unregister(String id) {
        checks.remove(id);
        consecutiveFaults.remove(id);
    }

    /** True when a check with {@code id} is currently registered. */
    public boolean isRegistered(String id) {
        return checks.containsKey(id);
    }

    /** Number of currently registered checks. */
    public int size() {
        return checks.size();
    }

    /**
     * Runs every registered check against {@code data} and returns
     * the violations they produced. A check that throws is isolated;
     * one that reaches the fault threshold is removed.
     */
    public List<Violation> inspect(PlayerData data) {
        List<Violation> violations = new ArrayList<>();
        for (Check check : checks.values()) {
            try {
                check.inspect(data).ifPresent(violations::add);
                consecutiveFaults.remove(check.id());
            } catch (RuntimeException fault) {
                recordFault(check.id());
            }
        }
        return violations;
    }

    private void recordFault(String checkId) {
        int faults = consecutiveFaults.merge(checkId, 1, Integer::sum);
        if (faults >= faultThreshold) {
            checks.remove(checkId);
            consecutiveFaults.remove(checkId);
            eventBus.publish(new CheckFaultEvent(checkId,
                    faults + " consecutive faults"));
        }
    }
}
