package org.nohope.test;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">ketoth xupack</a>
 * @since 10/4/12 4:42 PM
 */
public final class ResourceUtils {
    private ResourceUtils() {
    }

    @Nullable
    public static String getResourceAsString(final String resourceName) throws IOException {
        URL resource = ClassLoader.getSystemResource(resourceName);
        //        Thread.currentThread()
        //              .getContextClassLoader()
        //              .getResource(resourceName);

        if (resource == null) {
            resource = ResourceUtils.class.getResource(resourceName);
        }

        if (resource == null) {
            return null;
        }

        return IOUtils.toString(resource.openStream());
    }
}
