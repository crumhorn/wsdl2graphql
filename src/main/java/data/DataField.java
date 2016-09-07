/**
 * The MIT License (MIT)
 Copyright (c) 2016 Emil Crumhorn

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package data;

public class DataField implements Cloneable {

    public String _name;
    public String _value;
    public String _type;
    public String _notes;

    public DataField(String name, String value) {
        _name = name;
        _value = value;
        _type = value;
    }

    public String getName() {
        return _name;
    }

    public String getValue() {
        return _value;
    }

    public void setType(String type) {
        _type = type;
    }

    public String getType() {
        return _type;
    }

    public String getNotes() { return _notes; };

    public void setNotes(String notes) {
        _notes = notes;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DataField clone = new DataField(getName(), getValue());
        clone.setType(getType());
        clone.setNotes(getNotes());
        return clone;
    }

    @Override
    public String toString() {
        return "DataField{" +
                "_name='" + _name + '\'' +
                ", _value='" + _value + '\'' +
                ", _type='" + _type + '\'' +
                '}';
    }
}
