/**
 * The MIT License (MIT)
 Copyright (c) 2016 Emil Crumhorn

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.google.common.io.Files;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.predic8.schema.*;
import com.predic8.schema.restriction.facet.Facet;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.WSDLElement;
import com.predic8.wsdl.WSDLParser;
import data.DataComplexType;
import data.DataEnum;
import data.DataField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses a WSDL and spits out GraphQL Schema via a Pebble Template.
 * <p/>
 * This was written in a hurry, so don't analyze the code too much
 * <p>
 * by Emil Crumhorn
 */
public class Parser {

    public static final Logger _Logger = LoggerFactory.getLogger(Parser.class);

    private static final String UNBOUNDED = "unbounded";

    private File       _wsdlFile;
    private String     _wsdlUrl;
    private WSDLParser _parser;
    private String     _outputPath;

    private static Map<String, String> _graphQlTypeMap = new HashMap<>();
    private static Map<String, String> _graphSchemaTypeMap = new HashMap<>();

    private Map<String, DataComplexType> _dataComplexTypeMap         = new HashMap<>();
    private Map<String, DataComplexType> _dataComplexTypeResponseMap = new HashMap<>();

    private static List<String> _nonModifyingQueries = new ArrayList<>();
    private static List<String> _modifyingQueries = new ArrayList<>();

    static {
        _graphQlTypeMap.put("string", "GraphQLString");
        _graphQlTypeMap.put("dateTime", "GraphQLString"); // js will have to parse, we don't have a type for this in GraphQL
        _graphQlTypeMap.put("int", "GraphQLInt");
        _graphQlTypeMap.put("long", "GraphQLInt");
        _graphQlTypeMap.put("float", "GraphQLFloat");
        _graphQlTypeMap.put("boolean", "GraphQLBoolean");
        _graphQlTypeMap.put("decimal", "GraphQLFloat");

        // for Typeschema
        _graphSchemaTypeMap.put("string", "String");
        _graphSchemaTypeMap.put("dateTime", "String"); // js will have to parse, we don't have a type for this in GraphQL, user can create a custom type too
        _graphSchemaTypeMap.put("int", "Int");
        _graphSchemaTypeMap.put("long", "Int");
        _graphSchemaTypeMap.put("float", "Float");
        _graphSchemaTypeMap.put("boolean", "Boolean");
        _graphSchemaTypeMap.put("decimal", "Float");

        _nonModifyingQueries.add("find");
        _nonModifyingQueries.add("get");

        _modifyingQueries.add("create");
        _modifyingQueries.add("delete");
        _modifyingQueries.add("add");
        _modifyingQueries.add("rename");
        _modifyingQueries.add("modify");
        _modifyingQueries.add("set");
    }

    private static final String _graphQlList = "GraphQLList";

    public Parser(File file, String outputPath, boolean typeSchema) {
        _wsdlFile = file;
        _outputPath = outputPath;
        parse(typeSchema);
    }

    public Parser(String url, String outputPath, boolean typeSchema) {
        _wsdlUrl = url;
        _outputPath = outputPath;
        parse(typeSchema);
    }

    private void parse(boolean typeSchema) {
        try {
            _parser = new WSDLParser();
            Definitions defs;
            if (_wsdlFile != null) {
                defs = _parser.parse(new FileInputStream(_wsdlFile));
            } else {
                defs = _parser.parse(_wsdlUrl);
            }

            _Logger.trace("Parsing: " + (_wsdlFile == null ? _wsdlUrl : _wsdlFile));
            _Logger.debug("Namespace: " + defs.getTargetNamespace());

            List<DataComplexType> dct = new ArrayList<>();
            List<DataEnum> enums = new ArrayList<>();

            // parse all complex types (methods + classes, they're nearly identical)
            for (Schema schema : defs.getLocalTypes().getSchemas()) {
                _Logger.trace("Parsing schema with " + schema.getComplexTypes().size() + " complex types");

                for (ComplexType ct : schema.getComplexTypes()) {
                    _Logger.trace("Parsing complex type '{}'", ct.getName());
                    DataComplexType data = getDataComplexType(ct, null, typeSchema);
                    _dataComplexTypeMap.put(data.getName(), data);
                    if (data.isResponseClass()) {
                        _dataComplexTypeResponseMap.put(data.getName(), data);
                    }
                    dct.add(data);
                }
            }

            _Logger.trace("Mapping response types to outgoing types...");

            // map the response type to the original class
            for (DataComplexType data : dct) {
                if (!data.isResponseClass()) {
                    String responseClass = data.getName() + "Response";
                    if (_dataComplexTypeResponseMap.containsKey(responseClass)) {
                        _Logger.trace("Found response type '{}' for '{}'", responseClass, data.getName());
                        DataComplexType response = _dataComplexTypeResponseMap.get(responseClass);
                        if (!response.getFields().isEmpty()) {
                            DataField resp = response.getFields().get(0);
                            String value = resp.getValue();
                            if (typeSchema) {
                                if (_graphSchemaTypeMap.containsKey(value)) {
                                    value = _graphSchemaTypeMap.get(value);
                                }
                            }
                            else {
                                if (_graphQlTypeMap.containsKey(value)) {
                                    value = _graphQlTypeMap.get(value);
                                }
                            }

                            data.setType("new " + _graphQlList + "(" + value + ")");
                            data.setSchemaType("[" + value + "]");
                        } else {
                            _Logger.warn("Could not find origin class for complex type '{}', using 'unknownClass' instead. Make sure you change it later!", responseClass);
                            data.setType("unknownClass");
                            data.setSchemaType("unknownClass");
                        }
                    }

                    if (data.isSequence()) {
                        _Logger.trace("{} is a sequence", data.getName());
                        if (data.getFields().isEmpty()) {
                            data.setResolveType("resolve()");
                            data.setReturn("db.querySOAP('" + data.getName() + "')");
                        } else {
                            StringBuilder buf = new StringBuilder("{");
                            StringBuilder buf2 = new StringBuilder("{");
                            List<DataField> fields = data.getFields();
                            for (int i = 0; i < fields.size(); i++) {
                                DataField df = fields.get(i);
                                buf.append(df.getName());
                                buf2.append(df.getName());
                                buf2.append(":");
                                buf2.append(df.getName());

                                if (i != fields.size() - 1) {
                                    buf.append(", ");
                                    buf2.append(", ");
                                }

                                // this generates the arguments
                                // as far as I understand, the input has to be normal types, it can't be full object graphs
                                String type = df.getType();
                                if (typeSchema) {
                                    if (!_graphSchemaTypeMap.values().contains(type)) {
                                        type = "String";
                                    }
                                }
                                else {
                                    if (!_graphQlTypeMap.values().contains(type)) {
                                        type = "GraphQLString";
                                    }
                                }
                                DataField adf = new DataField(df.getName(), type);
                                adf.setSchemaTypeToType(typeSchema);
                                data.addArgument(adf);
                            }
                            buf.append("}");
                            buf2.append("}");
                            data.setResolveType("resolve(root, " + buf.toString() + ")");
                            data.setReturn("db.querySOAPwithArgs('" + data.getName() + "', " + buf2.toString() + ")");
                        }
                    }
                }
            }

            _Logger.trace("Parsing enums");

            List<String> methodNames = defs.getLocalMessages().stream().map(WSDLElement::getName).collect(Collectors.toList());

            for (Schema schema : defs.getLocalTypes().getSchemas()) {
                // TODO: must be some better way determining if it's a class or a method (defs.getServices?)

                _Logger.trace("Found {} enum(s) in schema", schema.getSimpleTypes().size());

                for (SimpleType st : schema.getSimpleTypes()) {
                    // ENUMS
                    DataEnum e = getDataEnum(st);
                    enums.add(e);

                    _Logger.trace("Parsed enum '{}' with {} sub-field(s), >> {}", e.getName(), e.getEnumValues().size(), e.getEnumValues());
                }
            }

            List<DataComplexType> methods = new ArrayList<>();
            List<DataComplexType> classes = new ArrayList<>();

            for (DataComplexType d : dct) {
                boolean isMethod = methodNames.contains(d.getName());
                if (isMethod) {
                    methods.add(d);
                } else {
                    classes.add(d);
                }/*

                if (d.isSequence()) {
                    methods.add(d);
                } else {
                    classes.add(d);
                }*/
            }

            this.writeOutput(enums, classes, methods, typeSchema);

            _Logger.info("Finished! Yeay!");

        } catch (Exception err) {
            _Logger.error("Error parsing WSDL", err);
        }
    }

    private DataEnum getDataEnum(SimpleType st) throws Exception {
        DataEnum e = new DataEnum(st.getName());

        for (Facet f : st.getRestriction().getFacets()) {
            e.addEnumValue(f.getValue());
        }

        return e;
    }

    private DataComplexType getDataComplexType(ComplexType ct, String nameOverride, Boolean typeSchema) throws Exception {
        boolean isAbstract = "true".equals(ct.getAbstractAttr());

        String name = nameOverride != null ? nameOverride : ct.getName();

//        if (name.equalsIgnoreCase("trackedIdEntity") || name.equalsIgnoreCase("eventConfigEntity")) {
//            System.err.println();
//        }

        DataComplexType clz = new DataComplexType(name, isAbstract);

        if (name.endsWith("Response")) {
            clz.setResponseClass(true);
        }

        if (ct.getModel() instanceof ComplexContent) {
            ComplexContent ctx = (ComplexContent) ct.getModel();
            Derivation dev = ctx.getDerivation();

            clz.setDerivesFrom(dev.getBase().getLocalPart());

            Sequence devSeq = (Sequence) dev.getModel();

            for (SchemaComponent p : devSeq.getParticles()) {
                if (p instanceof Element) {
                    setDataFieldForParticle(clz, (Element) p, typeSchema);
                }
            }
        } else if (ct.getModel() instanceof Sequence) {
            clz.setSequence(true);
            Sequence seq = (Sequence) ct.getModel();
            for (SchemaComponent comp : seq.getParticles()) {
                if (comp instanceof Element) {
                    setDataFieldForParticle(clz, (Element) comp, typeSchema);
                }
            }

        }

        return clz;
    }

    private void setDataFieldForParticle(DataComplexType clz, Element pe, Boolean typeSchema) throws Exception {
        if (pe.getType() == null) {

            // sub-complex type
            if (pe.getEmbeddedType() instanceof ComplexType) {
                DataComplexType dct = getDataComplexType((ComplexType) pe.getEmbeddedType(), pe.getName(), typeSchema);

                //String type = "EMILWASHERE: " +pe.getName(); //pe.getType().getLocalPart();
                // create a field with the name of the sub-complex type
                DataField df = clz.setAttribute(pe.getName(), dct.getName());
                clz.setMinOccurs(pe.getMinOccurs());
                clz.setMaxOccurs(pe.getMaxOccurs());

                if (pe.getMaxOccurs().equals(UNBOUNDED)) {
                    df.setType("new " + _graphQlList + "(" + pe.getName() + ")");
                    df.setSchemaType("[" + pe.getName() + "]");
                } else {
                    int val = Integer.valueOf(pe.getMaxOccurs());
                    if (val == 1) {
                        df.setType(pe.getName());
                        df.setSchemaType(pe.getName());
                    } else {
                        df.setType("new " + _graphQlList + "(" + pe.getName() + ")");
                        df.setSchemaType("[" + pe.getName() + "]");
                    }
                }

                if (!_dataComplexTypeMap.containsKey(dct.getName())) {
                    _dataComplexTypeMap.put(dct.getName(), dct);
                }

                // since this is basically an internal class, we also need to add it to the global class list or it won't get added as a GraphQLObjectType that it can map to
                clz.addChild(dct);
            }
            return;
        }

        String type = pe.getType().getLocalPart();
        DataField df = clz.setAttribute(pe.getName(), type);
        // raw type on schema
        df.setSchemaType(type);
        clz.setMinOccurs(pe.getMinOccurs());
        //System.err.println(pe.getName() + " :: " + pe.getMaxOccurs());
        clz.setMaxOccurs(pe.getMaxOccurs());

        if (!typeSchema) {
            if ("unbounded".equals(pe.getMaxOccurs())) {
                df.setSchemaType("[" + type + (pe.isNillable() ? "!" : "") + "]");
            }
        }

        if (pe.isNillable()) {
            df.setNillable(true);
        }

        // convert type to graphql type if it is one
        if (_graphQlTypeMap.containsKey(type)) {
            df.setType(_graphQlTypeMap.get(type));
            df.setSchemaTypeToType(typeSchema);
        }
    }

    private void writeOutput(List<DataEnum> enums, List<DataComplexType> types, List<DataComplexType> methods, boolean typeSchema) {
        try {
            PebbleEngine engine = new PebbleEngine.Builder().autoEscaping(false).build();
            PebbleTemplate template = engine.getTemplate(typeSchema ? "templates/typeSchema.pebble" : "templates/schema.pebble");

            Map<String, Object> context = new HashMap<>();

            List<DataComplexType> methodsWithoutResponses = new ArrayList<>();
            for (DataComplexType dct : methods) {
                if (!dct.isResponseClass() && dct.getType() != null) {
                    methodsWithoutResponses.add(dct);
                }
            }

            List<DataComplexType> allTypes = new ArrayList<>();
            for (DataComplexType dct : types) {
                allTypes.add(dct);
                allTypes.addAll(dct.getChildrenRecrusively());
            }

            // clear dupes
            allTypes = new ArrayList<>(new LinkedHashSet<>(allTypes));

            // do mappings (not for typeschema)
            if (!typeSchema) {
                for (DataComplexType dct : allTypes) {
                    if (dct.getDerivesFrom() != null) {
                        // get all fields from the derived class, nested
                        this.mapDerivedFieldsOntoComplexType(dct, allTypes);
                    }
                }
            }

            // link them class-dependency style
            linkTypes(allTypes);

            if (typeSchema) {
                // sort the types, order doesn't matter for type schema
                allTypes.sort(Comparator.comparing(DataComplexType::getName));

                for (DataComplexType t : allTypes) {
                    t.getFields().sort((o1, o2) -> {
                        if (o1._name.equals("id")) {
                            return -1;
                        }
                        if (o2._name.equals("id")) {
                            return 1;
                        }

                        return 0;
                    });
                }

                // also sort the ops (uncomment if you want it sorted, default is that it does the same order as the WSDL)
//                methodsWithoutResponses.sort(Comparator.comparing(DataComplexType::getName));
            } else {
                // sort by most used first
                allTypes = sortTypes(allTypes, enums);
            }

            context.put("enums", enums);
            context.put("types", allTypes);
            if (typeSchema) {
                List<DataComplexType> nonMods = new ArrayList<>();
                List<DataComplexType> mods = new ArrayList<>();
                methodsWithoutResponses.forEach(x -> {
                    boolean any = false;
                    for (String mod : _modifyingQueries) {
                        if (x.getName().startsWith(mod)) {
                            any = true;
                        }
                    }
                    if (any) {
                        mods.add(x);
                    }
                    else {
                        nonMods.add(x);
                    }

                });

                context.put("nonModifyingOperations", nonMods);
                context.put("modifyingOperations", mods);
            }
            else {
                context.put("operations", methodsWithoutResponses);
            }

            Writer writer = new StringWriter();
            template.evaluate(writer, context);
            String output = writer.toString();

            String f = _outputPath;
            if (typeSchema) {
                f += ".typeSchema";
            }

            _Logger.info("Writing file '{}'", f);
            Files.write(output.getBytes(), new File(f));

        } catch (Exception err) {
            _Logger.error("Error creating template", err);
        }
    }

    private void linkTypes(List<DataComplexType> all) {
        for (DataComplexType dct : all) {
            List<String> deps = dct.getFieldDepdencies();

            for (String str : deps) {
                // remove "new SomeType(...)" wrapping
                if (str.contains("(")) {
                    str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));
                }

                // ignore GraphQL types and Enums (TODO: Use enum array/map)
                if (_graphQlTypeMap.values().contains(str) || str.endsWith("Enum")) {
                    continue;
                }

                DataComplexType linked = _dataComplexTypeMap.get(str);
                if (linked != null) {
                    dct.addLinkedDependency(linked);
                } else {
                    System.err.println("Found no dep for " + str);
                }
            }
        }
    }

    private List<DataComplexType> sortTypes(List<DataComplexType> all, List<DataEnum> allEnums) {
        _Logger.trace("Sorting all ComplexTypes in regards to dependencies");
//        List<DataComplexType> ret = new ArrayList<>(all);

        List<String> allValues = new ArrayList<>();
        allValues.addAll(_graphQlTypeMap.values());
        allValues.addAll(allEnums.stream().map(DataEnum::getName).collect(Collectors.toList()));

        List<DataComplexType> r = new ArrayList<>();
        // put the classes on top that have nothing but standard variable type dependencies AND are abstract
        for (DataComplexType dct : all) {
            boolean std = dct.hasOnlyStandardTypes(allValues);
            boolean abs = dct.isAbstract();

            if (abs && std) {
                r.add(dct);
            }
        }
        // after that, do the same, but the ones that have standard dependencies still
        for (DataComplexType dct : all) {
            if (dct.hasOnlyStandardTypes(allValues) && !r.contains(dct)) {
                r.add(dct);
            }
        }

        // at this point we've put all the "clean" stuff at the top, now it's time to figure out what comes before something else
        Map<String, Integer> countMap = new LinkedHashMap<>();
        // see how many times something links to another class, put the most popular one at the top
        countDependencies(all, countMap);
        // reorder, highest count on top
        countMap = sortByValue(countMap, true);
        // now put the most popular ones at the top
        for (String key : countMap.keySet()) {
            if (countMap.get(key) <= 1) {
                continue;
            }

            DataComplexType dct = _dataComplexTypeMap.get(key);
            if (!r.contains(dct)) {
                r.add(dct);
            }
        }

        // now we sort the remaining ones by how many fields they have, just becaus the chance of them depending on something bigger is smaller that way
        List<DataComplexType> remaining = new ArrayList<>(all);
        remaining.removeAll(r);
        remaining.sort((o1, o2) -> Integer.valueOf(o2.getFields().size()).compareTo(o1.getFields().size()));
        Collections.reverse(remaining);

        // last but not least, add all the missing ones that didn't fall into any other category
        for (DataComplexType dct : remaining) {
            r.add(dct);
        }

        return r;
    }

    private static <K, V> Map<K, V> sortByValue(Map<K, V> map, boolean reverse) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> {
            if (reverse) {
                return ((Comparable<V>) o2.getValue()).compareTo(o1.getValue());
            } else {
                return ((Comparable<V>) o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Iterator<Map.Entry<K, V>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<K, V> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private void countDependencies(List<DataComplexType> list, Map<String, Integer> countMap) {
        for (DataComplexType dct : list) {
            for (DataComplexType linked : dct.getLinkedDependencies()) {
                if (!countMap.containsKey(linked.getName())) {
                    countMap.put(linked.getName(), 1);
                } else {
                    countMap.put(linked.getName(), countMap.get(linked.getName()) + 1);
                }

                // depth first
                if (!linked.getLinkedDependencies().isEmpty()) {
                    // recurse
                    countDependencies(linked.getLinkedDependencies(), countMap);
                }
            }
        }
    }

    private void printTree(List<DataComplexType> list, int level) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < level; i++) {
            buf.append(" ");
        }

        for (DataComplexType dct : list) {
            if (level == 0) {
                System.out.println();
            }
            System.out.println(level + ": " + buf.toString() + (level > 0 ? "\\" : "") + dct.getName());
            for (DataComplexType linked : dct.getLinkedDependencies()) {
                System.out.println(level + 1 + ": " + buf.toString() + " \\" + linked.getName());
                // depth first
                if (!linked.getLinkedDependencies().isEmpty()) {
                    // recurse
                    printTree(linked.getLinkedDependencies(), level + 2);
                }
            }
        }
    }

    private void mapDerivedFieldsOntoComplexType(DataComplexType data, List<DataComplexType> all) {
        Map<String, DataComplexType> map = new HashMap<>();
        for (DataComplexType d : all) {
            map.put(d.getName(), d);
        }

        if (map.containsKey(data.getDerivesFrom())) {
            DataComplexType dct = map.get(data.getDerivesFrom());
            _Logger.trace("ComplexType '{}' derives from '{}', mapping fields from it to parent", data.getName(), dct.getDerivesFrom());
            List<DataField> cloned = new ArrayList<>();
            for (DataField df : dct.getFields()) {
                try {
                    DataField clone = (DataField) df.clone();
                    clone.setNotes("// Derived field from '" + data.getDerivesFrom() + "'");
                    cloned.add(clone);
                } catch (Exception err) {
                    _Logger.error("Error cloning", err);
                }
            }
            data.getFields().addAll(cloned);

            if (dct.getDerivesFrom() != null) {
                _Logger.trace("Recursing as the derived from ({} -> {}) derives further...", data.getDerivesFrom(), dct.getDerivesFrom());
                this.mapDerivedFieldsOntoComplexType(dct, all);
            }
        }
    }

    private static void out(String str) {
        _Logger.debug(str);
    }
}
