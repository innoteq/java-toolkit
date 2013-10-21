package org.nohope.typetools.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 11/8/12
 * Time: 3:28 PM
 */
public class ColorModuleTest {
    @Test
    public void testColorSerialization() throws Exception {
        final Color color = new Color(10, 20, 30, 40);

        final ObjectMapper usualMapper = getMapper();
        usualMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, "@class");

        final String colorSerialized = usualMapper.writeValueAsString(color);
        final Color c = usualMapper.readValue(colorSerialized, Color.class);

        assertEquals(color, c);
        assertEquals(color.getAlpha(), c.getAlpha());

        final JsonSchema jsonSchema = usualMapper.generateJsonSchema(Color.class);
        assertEquals("{\"type\":\"array\"}", jsonSchema.toString());
    }

    @Test
    public void invalidColourDeserialization() {
        final ObjectMapper mapper = getMapper();

        try {
            mapper.readValue("[0.039215688,0.078431375,0.11764706,0.15686275, 1]", Color.class);
            fail();
        } catch (IOException ignored) {
        }

        try {
            mapper.readValue("{}", Color.class);
            fail();
        } catch (IOException ignored) {
        }
    }

    private static ObjectMapper getMapper() {
        final ObjectMapper usualMapper = new ObjectMapper();
        usualMapper.registerModule(new ColorModule());

        return usualMapper;
    }
}
