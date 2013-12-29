package org.nohope.test.stress;

/**
* @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
* @since 2013-12-27 16:18
*/
class SingleInvocationStat extends Stat {
    private final NamedAction action;

    public SingleInvocationStat(final TimerResolution resolution,
                                final NamedAction action) {
        super(resolution, action.getName());
        this.action = action;
    }

    protected void invoke(final int threadId,
                          final int operationNumber) throws InvocationException {
        final MeasureData p = new MeasureData(threadId, operationNumber);
        this.invoke(threadId, new InvocationHandler<Object>() {
            @Override
            public Object invoke() throws Exception {
                action.doAction(p);
                return null;
            }
        });
    }
}
