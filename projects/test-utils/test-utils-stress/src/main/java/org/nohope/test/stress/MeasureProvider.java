package org.nohope.test.stress;

import org.nohope.test.stress.functors.Get;
import org.nohope.test.stress.functors.Call;

import java.util.function.Function;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2013-12-29 18:23
 */
public final class MeasureProvider extends MeasureData {
    private final Function<String, StatAccumulator> accumulatorLoader;

    MeasureProvider(final int threadId,
                    final int operationNumber,
                    final Function<String, StatAccumulator> accumulatorLoader) {
        super(threadId, operationNumber);
        this.accumulatorLoader = accumulatorLoader;
    }

    public <T> T get(final String name, final Get<T> getter) throws Exception {
        return accumulatorLoader.apply(name).measure(getThreadId(), getter);
    }

    public void call(final String name, final Call call) throws Exception {
        accumulatorLoader.apply(name).measure(getThreadId(), call);
    }
}
