package org.nohope;

import net.jcip.annotations.Immutable;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Date: 10/4/12
 * Time: 5:53 PM
 */
@Immutable
public final class Interval implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Set<Integer> daysOfWeek = new HashSet<>();
    private final LocalTime begin;
    private final LocalTime end;

    /**
     * @deprecated do not use this constructor directly.
     *             It's used for jackson serialization only
     */
    @SuppressWarnings("unused")
    @Deprecated
    private Interval() {
        begin = null;
        end = null;
    }

    public Interval(@Nonnull final LocalTime begin,
                    @Nonnull final LocalTime end) {
        this.begin = begin;
        this.end = end;
    }

    public Interval(@Nonnull final LocalTime begin,
                    @Nonnull final LocalTime end,
                    @Nonnull final Set<Integer> daysOfWeek) {
        this.begin = begin;
        this.end = end;
        setDaysOfWeek(daysOfWeek);
    }

    public Interval(@Nonnull final LocalTime begin,
                    @Nonnull final LocalTime end,
                    final int... days) {
        this.begin = begin;
        this.end = end;
        final Set<Integer> dayOfWeek = new HashSet<>();
        dayOfWeek.addAll(Arrays.asList(ArrayUtils.toObject(days)));
        setDaysOfWeek(dayOfWeek);
    }

    @Nonnull
    public Set<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    /** The values for the day of week are defined in {@link org.joda.time.DateTimeConstants}. */
    public Interval setDaysOfWeek(@Nonnull final Set<Integer> daysOfWeek) {
        for (final Integer i : daysOfWeek) {
            if (i == null || i < 1 || i > 7) {
                throw new IllegalArgumentException("Illegal day of week passed " + i);
            }
        }

        this.daysOfWeek.clear();
        this.daysOfWeek.addAll(daysOfWeek);
        return this;
    }

    @Nonnull
    public LocalTime getBegin() {
        return begin;
    }

    public Interval withBegin(@Nonnull final LocalTime begin) {
        return new Interval(begin, end);
    }

    @Nonnull
    public LocalTime getEnd() {
        return end;
    }

    public Interval withEnd(@Nonnull final LocalTime end) {
        return new Interval(begin, end);
    }

    public boolean contains(@Nonnull final DateTime timestamp) {
        if (!daysOfWeek.isEmpty()) {
            final int dayOfWeek = timestamp.getDayOfWeek();
            if (!daysOfWeek.contains(dayOfWeek)) {
                return false;
            }
        }

        final LocalTime time = timestamp.toLocalTime();
        if (begin.isBefore(end)) {
            return begin.isBefore(time) && end.isAfter(time);
        } else {
            return begin.isBefore(time) || end.isAfter(time);
        }
    }

    @Override
    public int hashCode() {
        return 31 * (31 * begin.hashCode() + end.hashCode()) + daysOfWeek.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Interval interval = (Interval) o;
        return begin.isEqual(interval.begin)
               && daysOfWeek.equals(interval.daysOfWeek)
               && end.isEqual(interval.end);
    }
}
