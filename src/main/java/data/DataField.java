/**
 * The MIT License (MIT)
 * Copyright (c) 2016 Emil Crumhorn
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package data;

import java.util.HashMap;
import java.util.Map;

public class DataField implements Cloneable {

    public static final String TYPESCHEMA_ID = "ID";

    // conversion of schema types to general types
    public static Map<String, String> typeMappings = new HashMap<>();
    public static Map<String, String> schemaMappings = new HashMap<>();

    static {
        typeMappings.put("GraphQLString", "String");
        typeMappings.put("GraphQLInt", "Int");
        typeMappings.put("GraphQLFloat", "Float");
        typeMappings.put("GraphQLBoolean", "Boolean");

        schemaMappings.put("GraphQLString", "String");
        schemaMappings.put("GraphQLInt", "Int");
        schemaMappings.put("GraphQLFloat", "Float");
        schemaMappings.put("GraphQLBoolean", "Boolean");
    }

    public String _name;
    public String _value;
    public String _type;
    public String _notes;
    public String _schemaType;
    public boolean _nillable;

    public DataField(String name, String value) {
        _name = name;
        _value = value;
        _type = value;
        if (typeMappings.containsKey(_type)) {
            _schemaType = typeMappings.get(_type);
        }
    }

    public DataField(String name, String value, String schemaType) {
        _name = name;
        _value = value;
        _type = value;
        _schemaType = schemaType;
    }

    public String getName() {
        return _name;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }

    public void setType(String type) {
        _type = type;
    }

    public void setSchemaTypeToType(Boolean typeSchema) {
        if (typeSchema) {
            if (_name.toLowerCase().equals("id")) {
                _schemaType = TYPESCHEMA_ID;
                return;
            }
            if (schemaMappings.containsKey(_type)) {
                _schemaType = schemaMappings.get(_type);
            }
            else {
                _schemaType = _type;
            }
/*
            if (_type.equals(STR_ID)) {
                _schemaType = "ID";
            }*/

        } else {
            if (typeMappings.containsKey(_type)) {
                _schemaType = typeMappings.get(_type);
            }
        }
    }

    public String getType() {
        return _type;
    }

    public String getNotes() {
        return _notes;
    }

    public void setNotes(String notes) {
        _notes = notes;
    }

    public String getSchemaType() {
        return _schemaType;
    }

    public void setSchemaType(String type) {
        _schemaType = type;
    }


    public boolean isNillable() {
        return _nillable;
    }

    public boolean isNotNillable() {
        return !isNillable();
    }

    public void setNillable(boolean nillable) {
        this._nillable = nillable;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DataField clone = new DataField(getName(), getValue());
        clone.setType(getType());
        clone.setValue(getValue());
        clone.setNotes(getNotes());
        clone.setNillable(isNillable());
        clone.setSchemaType(getSchemaType());
        return clone;
    }

    @Override
    public String toString() {
        return "DataField{" +
                "_name='" + _name + '\'' +
                ", _value='" + _value + '\'' +
                ", _type='" + _type + '\'' +
                ", _schemaType='" + _schemaType + '\'' +
                '}';
    }
}
