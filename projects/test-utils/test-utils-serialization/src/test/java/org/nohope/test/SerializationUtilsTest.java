package org.nohope.test;

import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.nohope.test.SerializationUtils.*;

/**
 * Just
 *
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 9/21/12 1:06 AM
 */
public class SerializationUtilsTest {

    @Test
    public void isUtility() throws Exception {
        UtilityClassUtils.assertUtilityClass(SerializationUtils.class);
    }

    @Test
    public void basic() {
        final FinalFieldNoDefaultConstructor origin = new FinalFieldNoDefaultConstructor(1);
        final FinalFieldNoDefaultConstructor result = assertJavaClonedEquals(origin);
        assertEquals(origin.state, result.state);
    }

    @Test
    public void basicJson() {
        final Bean1 origin = new Bean1(1);
        final Bean1 result = assertMongoClonedEquals(origin);
        assertEquals(origin.state, result.state);
        assertEquals(origin.state, fromMongo(toMongo(result), Bean1.class).state);
        assertEquals(origin.state, cloneMongo(result).state);

    }

    @Test(expected = AssertionError.class)
    public void unmarshallingException() {
        assertMongoClonedEquals(new Bean2(1));
    }

    @Test(expected = AssertionError.class)
    public void unmarshallingException2() {
        fromMongo("{xxx}", Bean1.class);
    }

    @Test(expected = AssertionError.class)
    public void marshallingException() {
        toMongo(new CyclicBean());
    }

    private static final class Bean2 implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int state;

        private Bean2(final int state) {
            this.state = state;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Bean2 that = (Bean2) o;
            return state == that.state;
        }

        @Override
        public int hashCode() {
            return state;
        }
    }

    private static final class CyclicBean implements Serializable {
        private static final long serialVersionUID = 1L;
        private CyclicBean bean;

        public CyclicBean() {
            bean = this;
        }
    }

    private static final class Bean1 implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int state;

        private Bean1() {
            state = 0;
        }

        private Bean1(final int state) {
            this.state = state;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Bean1 that = (Bean1) o;
            return state == that.state;
        }

        @Override
        public int hashCode() {
            return state;
        }
    }

    private static final class FinalFieldNoDefaultConstructor implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int state;

        private FinalFieldNoDefaultConstructor(final int state) {
            this.state = state;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FinalFieldNoDefaultConstructor that =
                    (FinalFieldNoDefaultConstructor) o;
            return state == that.state;
        }

        @Override
        public int hashCode() {
            return state;
        }
    }
}
