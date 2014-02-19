/**
 * Copyright 2006 Envoi Solutions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jvnet.jax_ws_commons.json;

import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

/**
 * This class is copied from jettison 1.2, because we need to have the attributes "stack", "current" and the class "JSONProperty"
 * protected (in the jettison code they are private)
 */
public class MappedXMLStreamWriter extends AbstractXMLStreamWriter {
    private final MappedNamespaceConvention convention;
    protected Writer writer;
    private NamespaceContext namespaceContext;
    /**
     * What key is used for text content, when an element has both text and
     * other content?
     */
    private String valueKey = "$";
    /** Stack of open elements. */
    protected Stack<JSONProperty> stack = new Stack<>();
    /** Element currently being processed. */
    protected JSONProperty current;

    /**
     * JSON property currently being constructed. For efficiency, this is
     * concretely represented as either a property with a String value or an
     * Object value.
     */
    protected abstract class JSONProperty {
        private final String key;

        protected JSONProperty(final String key) {
            this.key = key;
        }

        /** Get the key of the property. */
        public String getKey() {
            return key;
        }

        /** Get the value of the property */
        public abstract Object getValue();

        /** Add text */
        public abstract void addText(String text);

        /** Return a new property object with this property added */
        public abstract JSONPropertyObject withProperty(JSONProperty property, boolean add);

        public JSONPropertyObject withProperty(final JSONProperty property) {
            return withProperty(property, true);
        }
    }

    /**
     * Property with a String value.
     */
    private final class JSONPropertyString extends JSONProperty {
        private final StringBuilder object = new StringBuilder();

        public JSONPropertyString(final String key) {
            super(key);
        }

        @Override
        public Object getValue() {
            return object.toString();
        }

        @Override
        public void addText(final String text) {
            object.append(text);
        }

        @Override
        public JSONPropertyObject withProperty(final JSONProperty property, final boolean add) {
            // Duplicate some code from JSONPropertyObject
            // because we can do things with fewer checks, and
            // therefore more efficiently.
            final JSONObject jo = new JSONObject();
            try {
                // only add the text property if it's non-empty
                if (object.length() > 0) {
                    jo.put(valueKey, getValue());
                }
                Object value = property.getValue();
                if (add && value instanceof String) {
                    value = convention.convertToJSONPrimitive((String) value);
                }
                if (getSerializedAsArrays().contains(property.getKey())) {
                    final JSONArray values = new JSONArray();
                    values.put(value);
                    value = values;
                }
                jo.put(property.getKey(), value);
            } catch (final JSONException e) {
                // Impossible by construction
                throw new AssertionError(e);
            }
            return new JSONPropertyObject(getKey(), jo);
        }
    }

    /**
     * Property with a JSONObject value.
     */
    private final class JSONPropertyObject extends JSONProperty {
        private final JSONObject object;

        public JSONPropertyObject(final String key, final JSONObject object) {
            super(key);
            this.object = object;
        }

        @Override
        public Object getValue() {
            return object;
        }

        @Override
        public void addText(String text) {
            try {
                // append to existing text
                // FIXME: should we store text segments in an array
                // when they are separated by child elements? That
                // would be an easy feature to add but we can worry
                // about that later.
                text = object.getString(valueKey) + text;
            } catch (final JSONException e) {
                // no existing text, that's fine
            }
            try {
                if (valueKey != null) {
                    object.put(valueKey, text);
                }
            } catch (final JSONException e) {
                // Impossible by construction
                throw new AssertionError(e);
            }
        }

        @Override
        public JSONPropertyObject withProperty(final JSONProperty property, final boolean add) {
            Object value = property.getValue();
            if (add && value instanceof String) {
                value = convention.convertToJSONPrimitive((String) value);
            }
            // if (!add) return this;
            // Object old = object.get(property.getKey());
            final Object old = object.opt(property.getKey());
            try {
                if (old != null) {
                    final JSONArray values;
                    // Convert an existing property to an array
                    // and append to the array
                    if (old instanceof JSONArray) {
                        values = (JSONArray) old;
                    } else {
                        values = new JSONArray();
                        values.put(old);
                    }
                    values.put(value);

                    object.put(property.getKey(), values);
                } else if (getSerializedAsArrays().contains(property.getKey())) {
                    final JSONArray values = new JSONArray();
                    values.put(value);
                    object.put(property.getKey(), values);
                } else {
                    // Add the property directly.
                    object.put(property.getKey(), value);
                }
            } catch (final JSONException e) {
                // TODO Auto-generated catch block
                throw new IllegalStateException(e);
            }
            return this;
        }
    }

    public MappedXMLStreamWriter(final MappedNamespaceConvention convention, final Writer writer) {
        super();
        this.convention = convention;
        this.writer = writer;
        this.namespaceContext = convention;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    @Override
    public void setNamespaceContext(final NamespaceContext context)
            throws XMLStreamException {
        this.namespaceContext = context;
    }

    public String getTextKey() {
        return valueKey;
    }

    public void setValueKey(final String valueKey) {
        this.valueKey = valueKey;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        // The document is an object with one property -- the root element
        current = new JSONPropertyObject(null, new JSONObject());
        stack.clear();
    }

    @Override
    public void writeStartElement(final String prefix,
                                  final String local,
                                  final String ns) throws XMLStreamException {
        stack.push(current);
        final String key = convention.createKey(prefix, ns, local);
        current = new JSONPropertyString(key);
    }

    @Override
    public void writeAttribute(final String prefix,
                               final String ns,
                               final String local,
                               final String value) throws XMLStreamException {
        final String key = convention.isElement(prefix, ns, local)
                ? convention.createKey(prefix, ns, local)
                : convention.createAttributeKey(prefix, ns, local);
        final JSONPropertyString prop = new JSONPropertyString(key);
        prop.addText(value);
        current = current.withProperty(prop, false);
    }

    @Override
    public void writeAttribute(final String ns, final String local, final String value) throws XMLStreamException {
        writeAttribute(null, ns, local, value);
    }

    @Override
    public void writeAttribute(final String local, final String value) throws XMLStreamException {
        writeAttribute(null, local, value);
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        current.addText(text);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        if (stack.isEmpty()) {
            throw new XMLStreamException("Too many closing tags.");
        }
        current = stack.pop().withProperty(current);
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        if (!stack.isEmpty()) {
            throw new XMLStreamException("Missing some closing tags.");
        }
        // We know the root is a JSONPropertyObject so this cast is safe
        writeJSONObject((JSONObject) current.getValue());
        try {
            writer.flush();
        } catch (final IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * For clients who want to modify the output object before writing to override.
     */
    protected void writeJSONObject(final JSONObject root) throws XMLStreamException {
        try {
            if (root == null) {
                writer.write("null");
            } else {
                root.write(writer);
            }
        } catch (final JSONException | IOException e) {
            throw new XMLStreamException(e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are supplied only to satisfy the interface

    @Override
    public void close() throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void flush() throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public String getPrefix(final String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getProperty(final String arg0) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDefaultNamespace(final String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void setPrefix(final String arg0, final String arg1) throws XMLStreamException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeDefaultNamespace(final String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeEntityRef(final String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeNamespace(final String arg0, final String arg1) throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeProcessingInstruction(final String arg0) throws XMLStreamException {
        // TODO Auto-generated method stub
    }

    @Override
    public void writeProcessingInstruction(final String arg0, final String arg1) throws XMLStreamException {
        // TODO Auto-generated method stub
    }
}
