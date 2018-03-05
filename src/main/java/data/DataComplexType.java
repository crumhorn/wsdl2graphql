/**
 * The MIT License (MIT)
 Copyright (c) 2016 Emil Crumhorn

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataComplexType {

    private String _name;
    private String _derivesFrom;
    private boolean _abstractClass;
    private boolean _responseClass;
    private boolean _sequence;
    private String _minOccurs;
    private String _maxOccurs;
    private String _type;
    private String _schemaType;
    private String _resolveType;
    private String _return;
    private List<DataField> _fields = new ArrayList<>();
    private List<DataComplexType> _children = new ArrayList<>();
    private List<DataComplexType> _linkedDependencies = new ArrayList<>();
    private List<DataField> _args = new ArrayList<>();

    public DataComplexType(String name, boolean isAbstract) {
        _name = name;
        _abstractClass = isAbstract;
    }

    public DataField setAttribute(String key, String value) {
        DataField df = new DataField(key, value);
        _fields.add(df);
        return df;
    }

    public void addChild(DataComplexType child) {
        _children.add(child);
    }

    public List<DataComplexType> getChildren() {
        return _children;
    }

    public List<DataComplexType> getChildrenRecrusively() {
        List<DataComplexType> ret = new ArrayList<DataComplexType>();
        recurseChildren(ret);
        return ret;
    }

    public void addLinkedDependency(DataComplexType dep) {
        if (!_linkedDependencies.contains(dep)) {
            _linkedDependencies.add(dep);
        }
    }

    public List<DataComplexType> getLinkedDependencies() {
        return _linkedDependencies;
    }

    private void recurseChildren(List<DataComplexType> ret) {
        for (DataComplexType dct : _children) {
            ret.add(dct);
            if (!dct.getChildren().isEmpty()) {
                dct.recurseChildren(ret);
            }
        }
    }

    public String getName() {
        return _name;
    }

    public List<DataField> getFields() {
        return _fields;
    }

    public boolean hasFields() {
        return _fields.size() > 0;
    }

    public boolean hasNoFields() {
        return _fields.isEmpty();
    }

    public List<String> getFieldDepdencies() {
        return getFields().stream().map(DataField::getType).collect(Collectors.toList());
    }

    public String getDerivesFrom() {
        return _derivesFrom;
    }

    public void setDerivesFrom(String derivesFrom) {
        this._derivesFrom = derivesFrom;
    }

    public String getMinOccurs() {
        return _minOccurs;
    }

    public void setMinOccurs(String minOccurs) {
        this._minOccurs = minOccurs;
    }

    public String getMaxOccurs() {
        return _maxOccurs;
    }

    public void setMaxOccurs(String maxOccurs) {
        this._maxOccurs = maxOccurs;
    }

    public void setResponseClass(boolean responseClass) {
        this._responseClass = responseClass;
    }

    public boolean isResponseClass() {
        return _responseClass;
    }

    public boolean isSequence() {
        return _sequence;
    }

    public void setSequence(boolean sequence) {
        this._sequence = sequence;
    }

    public String getType() {
        return _type;
    }

    public void setType(String type) {
        _type = type;
    }

    public String getSchemaType() {
        return _schemaType;
    }

    public void setSchemaType(String schemaType) {
        _schemaType = schemaType;
    }

    public void setResolveType(String resolveType) {
        _resolveType = resolveType;
    }

    public String getResolveType() {
        return _resolveType;
    }

    public String getReturn() {
        return _return;
    }

    public boolean isAbstract() {
        return _abstractClass;
    }

    public void setReturn(String ret) {
        this._return = ret;
    }

    public void addArgument(DataField arg) {
        this._args.add(arg);
    }

    public List<DataField> getArgs() {
        return _args;
    }

    public String getArgsAsSchemaString() {
        if (_args == null || _args.isEmpty()) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < _args.size(); i++) {
            DataField arg = _args.get(i);

            buf.append(arg.getName());
            buf.append(": ");
            buf.append(arg.getSchemaType());
            if (i != _args.size()-1) {
                buf.append(", ");
            }

        }


        return buf.toString();
    }

    public String getArgsAsTypeschemaString() {
        if (_args == null || _args.isEmpty()) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < _args.size(); i++) {
            DataField arg = _args.get(i);

            buf.append(arg.getName());
            if (i != _args.size()-1) {
                buf.append(", ");
            }

        }


        return buf.toString();
    }

    public boolean hasOnlyStandardTypes(List<String> standardFields) {
        if (_children.size() == 0) {
            for (DataField df : _fields) {
                if (!standardFields.contains(df.getType())) {
                    return false;
                }
            }

            return true;
        }
        else {
            return false;
        }

    }

    @Override
    public String toString() {
        if (true) {
            return getName();
        }
        String ret = (_abstractClass ? "abstract " : "") +  _name + (_derivesFrom == null ? "" : " extends " + _derivesFrom) + "\n";
        for (DataField df : _fields) {
            ret += " \\===> " + df.getName() + " " + df.getValue();
            ret += "\n";
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataComplexType that = (DataComplexType) o;

        return _name != null ? _name.equals(that._name) : that._name == null;

    }

    @Override
    public int hashCode() {
        return _name != null ? _name.hashCode() : 0;
    }
}