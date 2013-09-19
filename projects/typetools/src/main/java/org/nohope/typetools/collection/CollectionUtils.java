package org.nohope.typetools.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 9/18/12 5:32 PM
 */
public class CollectionUtils {
    private CollectionUtils() {
    }

    public static<T> List<T> fillList(final int count, final T placeholder) {
        final List<T> placeholders = new ArrayList<>();
        for (int i = 1; i <= count; ++i ) {
            placeholders.add(placeholder);
        }
        return placeholders;
    }
}
