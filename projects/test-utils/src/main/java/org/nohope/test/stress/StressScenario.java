package org.nohope.test.stress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 2013-12-26 21:39
 */
public class StressScenario {
    private StressScenario(final TimerResolution resolution) {
        this.resolution = resolution;
    }

    private final TimerResolution resolution;

    protected TimerResolution getResolution() {
        return resolution;
    }

    public static StressScenario of(final TimerResolution resolution) {
        return new StressScenario(resolution);
    }

    public StressResult measure(final int threadsNumber,
                               final int cycleCount,
                               final NamedAction... actions)
            throws InterruptedException {

        final Map<String, SingleInvocationStatCalculator> stats = new HashMap<>();
        for (final NamedAction action : actions) {
            stats.put(action.getName(),
                    new SingleInvocationStatCalculator(resolution, action));
        }

        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadsNumber; i++) {
            final int k = i;
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = k * cycleCount; j < (k + 1) * cycleCount; j++) {
                        try {
                            for (final SingleInvocationStatCalculator stat : stats.values()) {
                                stat.invoke(k, j);
                            }
                        } catch (final InvocationException e) {
                            // TODO
                        }
                    }
                }
            }, "stress-worker-" + k));
        }

        final long overallStart = resolution.currentTime();
        for (final Thread thread : threads) {
            thread.start();
        }

        for (final Thread thread : threads) {
            thread.join();
        }
        final long overallEnd = resolution.currentTime();

        final double runtime = resolution.toSeconds(overallEnd - overallStart);

        final Map<String, Result> results = new HashMap<>();
        int fails = 0;
        for (final SingleInvocationStatCalculator stat : stats.values()) {
            final Result r = stat.getResult();
            fails += r.getErrors().size();
            results.put(r.getName(), r);
        }

        return new StressResult(results, threadsNumber, cycleCount, fails,
                runtime);
    }

    public StressResult measure(final int threadsNumber,
                                final int cycleCount,
                                final Action action)
            throws InterruptedException {

        final ConcurrentMap<String, MultiInvocationStatCalculator> result =
                new ConcurrentHashMap<>();

        final List<MeasureProvider> providers = new ArrayList<>();
        for (int i = 0; i < threadsNumber; i++) {
            for (int j = i * cycleCount; j < (i + 1) * cycleCount; j++) {
                providers.add(new MeasureProvider(this, i, j, result));
            }
        }

        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadsNumber; i++) {
            final int k = i;
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = k * cycleCount; j < (k + 1) * cycleCount; j++) {
                        try {
                            action.doAction(providers.get(j));
                        } catch (final Exception e) {
                            // TODO: print skipped
                        }
                    }
                }
            }, "stress-worker-" + k));
        }

        final long overallStart = resolution.currentTime();
        for (final Thread thread : threads) {
            thread.start();
        }
        for (final Thread thread : threads) {
            thread.join();
        }
        final long overallEnd = resolution.currentTime();

        final double runtime = resolution.toSeconds(overallEnd - overallStart);

        int fails = 0;
        final Map<String, Result> results = new HashMap<>();
        for (final MultiInvocationStatCalculator stats : result.values()) {
            final Result r = stats.getResult();
            fails += r.getErrors().size();
            results.put(r.getName(), r);
        }

        return new StressResult(results, threadsNumber, cycleCount, fails,
                runtime);
    }
}