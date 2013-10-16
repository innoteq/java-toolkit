package org.nohope.jaxb2.plugin.validation;

import org.junit.runner.RunWith;
import org.nohope.jaxb2.plugin.Jaxb2PluginTestSupport;
import org.nohope.test.runner.InstanceTestClassRunner;
import org.xml.sax.SAXParseException;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2013-10-16 14:09
 */
@RunWith(InstanceTestClassRunner.class)
public class ValidationPluginMissingBindingParametersTest extends Jaxb2PluginTestSupport {
    public ValidationPluginMissingBindingParametersTest() {
        super("validation", "org/nohope/jaxb2/codegen/validation/missing_bind_parameter");
    }

    @Override
    protected boolean validateBuildFailure(final Throwable e) throws Exception {
        return e instanceof IllegalStateException
                && e.getCause() instanceof SAXParseException
                && "bind element should contain 'type' attribute".equals(e.getCause().getMessage())
                ;
    }
}