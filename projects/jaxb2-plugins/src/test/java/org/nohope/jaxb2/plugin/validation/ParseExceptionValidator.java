package org.nohope.jaxb2.plugin.validation;

import org.nohope.jaxb2.plugin.Jaxb2PluginTestSupport;
import org.xml.sax.SAXParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
* @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
* @since 2013-10-17 17:10
*/
class ParseExceptionValidator implements Jaxb2PluginTestSupport.BuildFailureValidator {
    private final String message;

    ParseExceptionValidator(final String message) {
        this.message = message;
    }

    public static ParseExceptionValidator message(final String message) {
        return new ParseExceptionValidator(message);
    }

    @Override
    public void validate(final Exception e) {
        assertTrue(e instanceof IllegalStateException);
        assertNotNull(e.getCause());
        assertNotNull(e.getCause() instanceof SAXParseException);
        assertEquals(message, e.getCause().getMessage());
    }
}
