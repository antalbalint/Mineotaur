package org.mineotaur.common;

import javassist.*;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by balintantal on 10/07/2015.
 */
public class ClassUtils {

    protected static final String JAVA_LANG_STRING = "java.lang.String";

    public static Class createClass(ClassPool pool, String className, List<Integer> indices, String[] header, String[] dataTypes, List<String> idFields, String dataType) throws javassist.NotFoundException, CannotCompileException {
        CtClass claz = pool.makeClass(className);
        for (Integer i : indices) {
            CtClass type;
            if (dataTypes[i].equals(dataType)) {
                type = CtClass.doubleType;
            } else {
                type = pool.get(JAVA_LANG_STRING);
            }
            CtField field = new CtField(type, header[i], claz);
            field.setModifiers(Modifier.PUBLIC);
            claz.addField(field);
        }
        if (idFields != null) {
            String methodBody = buildEquals(claz.getName(), idFields);
            CtMethod method = CtMethod.make(methodBody, claz);
            claz.addMethod(method);
        }
        return claz.toClass();
    }


    /**
     * Method to symbolicly build equals method for a generated Java class.
     * @param name The name of the class.
     * @param idFields The names of the fields act as identifiers for the class.
     * @return The text of the equals method.
     */
    public static String buildEquals(String name, List<String> idFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("public boolean equals(Object o) {\n");
        sb.append("if (this == o) return true;\n" + "        if (o == null || getClass() != o.getClass()) return false;\n").append(name).append(" obj = (").append(name).append(") o;\n");
        for (String id : idFields) {
            sb.append("if (!this.").append(id).append(".equals(obj.").append(id).append(")) return false;\n");
        }
        sb.append("return true;\n");
        sb.append("        }");
        return sb.toString();
    }

    /**
     * Method to create node using reflection from the generated object.
     * @param o The Java object containing all data extracted from the input.
     * @return The Node created from the object.
     * @throws IllegalAccessException
     */
    public static Node createNode(Object o, Label label, GraphDatabaseService db) throws IllegalAccessException {
        Class claz = o.getClass();
        Node node = db.createNode(label);
        Field[] fields = claz.getDeclaredFields();
        for (Field f : fields) {
            if (f.get(o) != null) {
                node.setProperty(f.getName(), f.get(o));
            }

        }
        return node;
    }

    /**
     * Looks up whether a Node with the same identifiers as the object provided has been already created.
     * @param o The Java object containing all data extracted from the input.
     * @return The retireved Node object or if not exists, a new one created by createNode.
     * @throws IllegalAccessException
     */
    public static Node lookupObject(Object o, Map<String, List<Node>> nonUniqueNodes, GraphDatabaseService db) throws IllegalAccessException {
        Class claz = o.getClass();
        String className = claz.getName();
        List<Node> storedNodes = nonUniqueNodes.get(className);
        if (storedNodes == null) {
            storedNodes = new ArrayList<>();
            nonUniqueNodes.put(className, storedNodes);
        }
        for (Node node : storedNodes) {
            boolean same = true;
            Field[] fields = claz.getDeclaredFields();
            for (Field f : fields) {
                String key = f.getName();
                Object nodeValue = node.getProperty(key, null);
                Object storedValue = f.get(o);
                if (storedValue == null && nodeValue == null) {
                    continue;
                }
                if (nodeValue == null || storedValue == null || !nodeValue.equals(storedValue)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return node;
            }
        }
        Node node = createNode(o, DynamicLabel.label(className), db);
        storedNodes.add(node);
        return node;
    }

}
