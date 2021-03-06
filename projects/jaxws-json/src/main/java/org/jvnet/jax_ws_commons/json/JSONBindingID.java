package org.jvnet.jax_ws_commons.json;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.pipe.Codec;

/**
 * @author Jitendra Kotamraju
 */
public class JSONBindingID extends BindingID {
    public static final String JSON_BINDING = "https://jax-ws-commons.dev.java.net/json/";

    @Override
    public SOAPVersion getSOAPVersion() {
        return SOAPVersion.SOAP_11;
    }

    @NotNull
    @Override
    public Codec createEncoder(@NotNull final WSBinding binding) {
        return new JSONCodec(binding);
    }

    public String toString() {
        return JSON_BINDING;
    }

    @Override
    public boolean canGenerateWSDL() {
        return true;
    }
}
