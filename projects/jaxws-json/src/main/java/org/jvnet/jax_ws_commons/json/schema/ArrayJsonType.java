package org.jvnet.jax_ws_commons.json.schema;

import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class ArrayJsonType extends JsonType {
    public final JsonType componentType;

    public ArrayJsonType(final JsonType componentType) {
        this.componentType = componentType;
    }

    @Override
    public String getLink() {
        return "array of " + componentType.getLink();
    }

    @Override
    public void listCompositeTypes(final Set<CompositeJsonType> result) {
        componentType.listCompositeTypes(result);
    }
}
