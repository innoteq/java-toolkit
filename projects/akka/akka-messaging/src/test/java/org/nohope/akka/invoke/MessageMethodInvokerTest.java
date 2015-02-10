package org.nohope.akka.invoke;

import org.junit.Ignore;
import org.junit.Test;
import org.nohope.akka.OnReceive;
import org.nohope.test.UtilitiesTestSupport;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Date: 25.07.12
 * Time: 11:29
 */
@SuppressWarnings("MethodMayBeStatic")
public class MessageMethodInvokerTest extends UtilitiesTestSupport<MessageMethodInvoker> {

    @Override
    protected Class<MessageMethodInvoker> getUtilityClass() {
        return MessageMethodInvoker.class;
    }

    @OnReceive
    private Integer processInteger(final Integer a) {
        return a;
    }

    @OnReceive
    private Double processDouble(final Double b) {
        return b;
    }

    @Test
    public void testAnnotatedInvoker() throws Exception {
        final Object a1 = 100;
        assertEquals(a1, MessageMethodInvoker.invokeOnReceive(this, a1));
        final Object a2 = 100.0;
        assertEquals(a2, MessageMethodInvoker.invokeOnReceive(this, a2));
    }

    @Test
    public void testCache() throws Exception {
        MessageMethodInvoker.invokeOnReceive(this, 100);
        assertTrue(MessageMethodInvoker.CACHE.containsKey(
                Signature.of(InvokeStrategy.CLOSEST_BY_PARAMETER,
                        new Class<?>[]{Integer.class},
                        MessageMethodInvokerTest.class)));

        final Signature p1 = Signature.of(InvokeStrategy.CLOSEST_BY_PARAMETER,
                new Class<?>[]{Integer.class}, MessageMethodInvokerTest.class);
        final Signature p2 = Signature.of(InvokeStrategy.CLOSEST_BY_PARAMETER,
                new Class<?>[]{Integer.class}, MessageMethodInvokerTest.class);
        assertEquals(p1, p2);
        assertEquals(p1, p1);
        assertNotEquals(p1, "");
        assertFalse(p1.equals(null));
    }

    @Test
    public void testNoAnnotatedMethod() throws Exception {
        try {
            MessageMethodInvoker.invokeOnReceive(this, "xxx");
            fail();
        } catch (final NoSuchMethodException e) {
        }

        try {
            MessageMethodInvoker.invokeOnReceive(new AnnotatedTestClass(), "yyy");
            fail();
        } catch (final NoSuchMethodException e) {
        }
    }

    @Test
    public void inheritance() throws Exception {
        assertEquals(1, MessageMethodInvoker.invokeOnReceive(new AnnotatedTestClass(), 1));
        assertEquals(2, MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), 2));

        // should not throw an exception
        MessageMethodInvoker.invokeOnReceive(new AnnotatedTestClass(), 1d);
    }

    @Test
    public void handlers() throws Exception {
        final BigDecimal decimal = new BigDecimal(1);

        assertEquals(decimal, MessageMethodInvoker.invokeOnReceive(new AnnotatedTestClass(), decimal, new AnnotatedHandler()));
        assertEquals(decimal, MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), decimal, new AnnotatedHandler()));
    }

    @Test
    @Ignore("Old behavior is changed")
    public void multipleMatch() throws Exception {
        try {
            MessageMethodInvoker.invokeOnReceive(new AnnotatedTestClass(), 1L);
            fail();
        } catch (final NoSuchMethodException ignored) {
        }

        try {
            MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), 1L);
            fail();
        } catch (final NoSuchMethodException ignored) {
        }
    }

    @Test(expected = UserException.class)
    public void exceptionRethrowing() throws Exception {
        MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), String.class);
    }

    @Test(expected = UserRuntimeException.class)
    public void runtimeExceptionRethrowing() throws Exception {
        MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), '\n');
    }

    @Test(expected = UserError.class)
    public void errorRethrowing() throws Exception {
        MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), 1d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwableRethrowing() throws Exception {
        MessageMethodInvoker.invokeOnReceive(new AnnotatedParentClass(), 1f);
    }

    @Test
    public void signaturePairEquals() {
        final Signature pair1 = new Signature(
                InvokeStrategy.CLOSEST_BY_PARAMETER, new Class<?>[] {String.class, Integer.class}, String.class);
        final Signature pair2 = new Signature(
                InvokeStrategy.CLOSEST_BY_PARAMETER, new Class<?>[] {String.class, Integer.class}, String.class);
        final Signature pair3 = new Signature(
                InvokeStrategy.CLOSEST_BY_PARAMETER, new Class<?>[] {String.class, String.class}, String.class);

        assertEquals(pair1, pair2);
        assertEquals(pair1.hashCode(), pair2.hashCode());

        assertFalse(pair1.equals(pair3));
        assertFalse(pair2.equals(pair3));
    }

    private static class UserException extends Exception {
        private static final long serialVersionUID = 5588207554344224650L;
    }

    private static class UserRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 5588207554344224650L;
    }

    private static class UserError extends Error {
        private static final long serialVersionUID = 5588207554344224650L;
    }

    private static class UserThrowable extends Throwable {
        private static final long serialVersionUID = 5588207554344224650L;
    }

    private static class AnnotatedParentClass {
        @OnReceive
        private Integer onConcreteMessage(final Integer x) {
            return x;
        }
        @OnReceive
        private Long one(final Long x) {
            return x;
        }

        @OnReceive
        private Long another(final Long x) {
            return x;
        }

        @OnReceive
        private void exceptional(final Class<?> x) throws UserException {
            throw new UserException();
        }

        @OnReceive
        private void runtimeExceptional(final Character x) {
            throw new UserRuntimeException();
        }

        @OnReceive
        protected void erroneous(final Double x) {
            throw new UserError();
        }

        @OnReceive
        private void throwable(final Float x) throws UserThrowable {
            throw new UserThrowable();
        }
    }

    private static class AnnotatedTestClass extends AnnotatedParentClass {
        @SuppressWarnings("unused")
        private native void onConcreteMessage(final String x);

        @Override
        protected void erroneous(final Double x) {
        }

        @Override
        public String toString() {
            return "TestClass";
        }
    }

    private static class AnnotatedHandler {
        @OnReceive
        private BigDecimal onConcreteMessage(final BigDecimal x) {
            return x;
        }

        @Override
        public String toString() {
            return "TestHandler";
        }
    }
}