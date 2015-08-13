package org.mineotaur.common;

import javassist.*;
import javassist.NotFoundException;
import org.mineotaur.application.Mineotaur;
import org.neo4j.graphdb.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by balintantal on 10/07/2015.
 */
public class ClassUtils {

    protected static final String JAVA_LANG_STRING = "java.lang.String";

    public static Class createClass(ClassPool pool,
                                    String className,
                                    List<Integer> indices,
                                    String[] header,
                                    String[] dataTypes,
                                    String dataType,
                                    String methodBody) throws javassist.NotFoundException, CannotCompileException {
        if (pool == null || className == null || indices == null || header == null || dataTypes == null ||  dataType == null || methodBody == null) {
            throw new IllegalArgumentException();
        }
        if (className.equals("")) {
            throw new IllegalArgumentException("Class name is empty string.");
        }
        if (dataType.equals("")) {
            throw new IllegalArgumentException("Data type is empty string.");
        }
        if (methodBody.equals("")) {
            throw new IllegalArgumentException("Method body is empty string.");
        }
        if (indices.isEmpty()) {
            throw new IllegalArgumentException("Index list is empty.");
        }
        if (header.length==0) {
            throw new IllegalArgumentException("Header is empty.");
        }
        if (dataTypes.length==0) {
            throw new IllegalArgumentException("Header is empty.");
        }
        CtClass claz = pool.makeClass(className);
        for (Integer i : indices) {
            System.out.println(i);
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
        CtMethod method = CtMethod.make(methodBody, claz);
        claz.addMethod(method);
        return claz.toClass();
    }


    /**
     * Method to symbolicly build equals method for a generated Java class.
     * @param name The name of the class.
     * @param idFields The names of the fields act as identifiers for the class.
     * @return The text of the equals method.
     */
    public static String buildEquals(String name, List<String> idFields) {
        if (name == null || idFields == null) {
            Mineotaur.LOGGER.info(name);
            Mineotaur.LOGGER.info(idFields.toString());
            throw new IllegalArgumentException();
        }
        if (name.equals("")) {
            throw new IllegalArgumentException("Class name is empty string.");
        }
        if (idFields.isEmpty()) {
            throw new IllegalArgumentException("Id list is empty.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("public boolean equals(Object o) {\n");
        sb.append("if (this == o) return true;\nif (o == null || getClass() != o.getClass()) return false;\n").append(name).append(" obj = (").append(name).append(") o;\n");
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
        if (o == null || label == null || db == null) {
            throw new IllegalArgumentException();
        }
        Node node = null;
        try (Transaction tx = db.beginTx()) {
            Class claz = o.getClass();
            node = db.createNode(label);
            Field[] fields = claz.getFields();
            for (Field f : fields) {
                if (f.get(o) != null) {
                    node.setProperty(f.getName(), f.get(o));
                }
            }
            tx.success();
        }
        return node;
    }

    /**
     * Looks up whether a Node with the same identifiers as the object provided has been already created.
     * @param o The Java object containing all data extracted from the input.
     * @return The retireved Node object or if not exists, a new one created by createNode.
     * @throws IllegalAccessException
     */
    public static Node lookupObject(Object o, List<Node> storedNodes, GraphDatabaseService db) throws IllegalAccessException {
        if (o == null || storedNodes == null || storedNodes.isEmpty() || db == null) {
            throw new IllegalArgumentException();
        }
        Class claz = o.getClass();
        try (Transaction tx = db.beginTx()) {
            for (Node node : storedNodes) {
                boolean same = true;
                Field[] fields = claz.getFields();
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
                    tx.success();
                    return node;
                }
            }
            tx.success();
        }
        return null;
        /*Node node =
        return node;*/
    }

}
