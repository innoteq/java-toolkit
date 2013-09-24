package org.nohope.spring;

import org.nohope.reflection.TypeReference;

import javax.annotation.Nonnull;

/**
 * Class-helper allowing to unify relation between bean name and it's underlying type.
 *
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 10/16/12 7:24 PM
 * @apiviz.uses org.nohope.reflection.TypeReference
 * @apiviz.uses java.lang.Class
 */
public final class BeanDefinition<T> {
    private final String name;
    private final Class<T> clazz;

    private BeanDefinition(@Nonnull final String name,
                           @Nonnull final Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public static<T> BeanDefinition<T> of(@Nonnull final String name,
                                          @Nonnull final Class<T> clazz) {
        return new BeanDefinition<>(name, clazz);
    }

    public static<T> BeanDefinition<T> of(@Nonnull final String name,
                                          @Nonnull final TypeReference<T> ref) {
        return of(name, ref.getTypeClass());
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Class<T> getBeanClass() {
        return clazz;
    }
}
