package org.nohope.util.typetools;

/**
 * Date: 17.10.11
 * Time: 14:48
 */
public final class DoubleTools {
    private DoubleTools() {
    }

    public static boolean isDoubleCorrect(final Double val) {
        return null != val && !(val.isNaN() || val.isInfinite());
    }

    public static double asDouble(final Double val) {
        return isDoubleCorrect(val) ? val : Double.NaN;
    }

    public static Double toFiniteDouble(final Double val) {
        return isDoubleCorrect(val) ? val : null;
    }
}
