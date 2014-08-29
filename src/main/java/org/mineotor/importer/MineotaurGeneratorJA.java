/*
 * Mineotaur: a visual analytics tool for high-throughput microscopy screens
 * Copyright (C) 2014  Bálint Antal (University of Cambridge)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mineotor.importer;
import javassist.*;
import org.mineotor.model.Metadata;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by balintantal on 30/06/2014.
 */
public class MineotaurGeneratorJA {

    private ResourceBundle properties;
    private String input;
    private String prop;
    private String name;
    private Metadata metadata;
    private static final String SEPARATOR = "\t";
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String CONF = "conf";
    private static final String DB = "db";
    private String aggregate;
    private String group;
    private List<String> unique;
    private GraphDatabaseService db;
    private GlobalGraphOperations ggo;
    private Map<String, Label> labels = new HashMap<>();
    private Map<String, List<Node>> nonUniqueNodes = new HashMap<>();
    private Map<String, Map<String, RelationshipType>> relationships = new HashMap<>();
    private Map<String, Class> classes = new HashMap<>();
    private Map<Label, List<Object>> stored = new HashMap<>();

    public MineotaurGeneratorJA(String input, String prop) {
        this.input = input;
        this.prop = prop;
        init();
    }

    private void createDirs() {
        new File(name).mkdir();
        System.out.println(new StringBuilder(name).append(FILE_SEPARATOR).append(CONF).toString());
        new File(new StringBuilder(name).append(FILE_SEPARATOR).append(CONF).toString()).mkdir();
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    private void startDB() {
        GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new StringBuilder(name).append(FILE_SEPARATOR).append(DB).toString());
        gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, "3G");
        gdb.setConfig(GraphDatabaseSettings.cache_type, "soft");
        db = gdb.newGraphDatabase();
        registerShutdownHook(db);
        ggo = GlobalGraphOperations.at(db);
    }

    private void createRelationships(String rels) {
        String[] terms = rels.split(",");
        for (String s : terms) {
            String[] objects = s.split("-");
            Map<String, RelationshipType> map = relationships.get(objects[0]);
            if (map == null) {
                map = new HashMap<>();
                relationships.put(objects[0], map);
            }
            map.put(objects[1], DynamicRelationshipType.withName(new StringBuilder(objects[0]).append("_AND_").append(objects[1]).toString()));


        }
    }

    private void processProperties() throws IOException {
        this.properties = new PropertyResourceBundle(new FileReader(prop));
        name = properties.getString("name");
        System.out.println(name);
        createDirs();
        this.aggregate = properties.getString("aggregate");
        this.group = properties.getString("group");
        this.unique = Arrays.asList(properties.getString("unique").split(","));
        createRelationships(properties.getString("relationships"));
    }

    private Node createNode(Label label, Map<String, Object> properties) {
        Node node = db.createNode(label);
        Set<String> keySet = properties.keySet();
        for (String key : keySet) {
            node.setProperty(key, properties.get(key));
        }
        return node;
    }

    private Node createNode(Object o) throws IllegalAccessException {
        Class claz = o.getClass();
        Label label = labels.get(claz.getName());
        Node node = db.createNode(label);
        Field[] fields = claz.getDeclaredFields();
        for (Field f: fields) {
            node.setProperty(f.getName(), f.get(o));
        }
        return node;
    }

    private Node lookupNode(String label, Map<String, Object> properties) {
        List<Node> storedNodes = nonUniqueNodes.get(label);
        if (storedNodes == null) {
            storedNodes = new ArrayList<>();
            nonUniqueNodes.put(label, storedNodes);
        }
        for (Node node : storedNodes) {
            boolean same = true;
            Set<String> keySet = properties.keySet();
            for (String key : keySet) {
                Object nodeValue = node.getProperty(key, null);
                Object storedValue = properties.get(key);
                if (!storedValue.equals(nodeValue)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return node;
            }
        }
        Node node = createNode(labels.get(label), properties);
        storedNodes.add(node);
        return node;
    }

    private Node lookupNode(Object o) throws IllegalAccessException {
        Class claz = o.getClass();
        String label = claz.getName();
        List<Node> storedNodes = nonUniqueNodes.get(label);
        if (storedNodes == null) {
            storedNodes = new ArrayList<>();
            nonUniqueNodes.put(label, storedNodes);
        }
        for (Node node : storedNodes) {
            boolean same = true;
            Field[] fields = claz.getDeclaredFields();
            for (Field f: fields) {
                String key = f.getName();
                Object nodeValue = node.getProperty(key, null);
                Object storedValue = f.get(o);
                if (!storedValue.equals(nodeValue)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return node;
            }
        }
        Node node = createNode(o);
        storedNodes.add(node);
        return node;
    }

    private boolean isStored(Object o) {
        Class claz = o.getClass();
        Label label = labels.get(claz.getName());
        List<Object> objects = stored.get(label);
        if (objects==null) {
            objects = new ArrayList<>();
            objects.add(o);
            stored.put(label, objects);
            return false;
        }
        return objects.contains(o);
    }

    private String buildEquals(String name, List<String> idFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("public boolean equals(Object o) {\n");
        sb.append("if (this == o) return true;\n" +
                        "        if (o == null || getClass() != o.getClass()) return false;\n" +
                        name + " obj = (" + name + ") o;\n");
        for (String id: idFields) {
            sb.append("if (!this.").append(id).append(".equals(obj.").append(id).append(")) return false;\n");
        }
        sb.append("return true;\n");
        sb.append("        }");
        return sb.toString();
    }

    // TODO: check
    public void readInput() throws IOException, javassist.NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        try (BufferedReader br = new BufferedReader(new FileReader(input)); Transaction tx = db.beginTx()) {

            //System.out.println(created);
            String[] header = br.readLine().split(SEPARATOR);
            String[] nodeTypes = br.readLine().split(SEPARATOR);
            //metadata.addNodeTypeNamesFromArray(nodeTypes);
            String[] dataTypes = br.readLine().split(SEPARATOR);
            metadata = new Metadata(header, nodeTypes, dataTypes);
            int lineSize = header.length;
            Map<String, List<Integer>> signatures = new HashMap<>();

            Map<String, Label> hits = new HashMap<>();
            List<Integer> numericData = new ArrayList<>();
            Map<String, List<String>> ids = new HashMap<>();

            for (int i = 0; i < lineSize; ++i) {
                if (nodeTypes[i].equals("HIT")) {
                    if (!hits.containsKey(header[i])) {
                        hits.put(header[i], DynamicLabel.label(header[i]));
                    }
                    continue;
                }
                List<Integer> indices = signatures.get(nodeTypes[i]);
                if (indices == null) {
                    indices = new ArrayList<>();
                    signatures.put(nodeTypes[i], indices);
                    labels.put(nodeTypes[i], DynamicLabel.label(nodeTypes[i]));
                }
                indices.add(i);
                if (("NUMBER").equals(dataTypes[i])) {
                    numericData.add(i);
                }
                if (("ID").equals(dataTypes[i])) {
                    List<String> idList = ids.get(nodeTypes[i]);
                    if (idList == null) {
                        idList = new ArrayList<String>();
                        ids.put(nodeTypes[i], idList);
                    }
                    idList.add(header[i]);

                }
            }
            System.out.println(signatures);
            System.out.println(hits);
            System.out.println(ids);
            System.out.println(labels);
            System.out.println(relationships);
            ClassPool pool = ClassPool.getDefault();
            Set<String> keySet = signatures.keySet();
            for (String key: keySet) {
                CtClass claz = pool.makeClass(key);
                List<Integer> indices = signatures.get(key);
                for (Integer i: indices) {
                    CtClass type;
                    if (dataTypes[i].equals("NUMBER")) {
                        type = CtClass.doubleType;
                    }
                    else {
                        type = pool.get("java.lang.String");
                    }
                    CtField field = new CtField(type, header[i], claz);
                    field.setModifiers(Modifier.PUBLIC);
                    claz.addField(field);
                    System.out.println(header[i]);
                }
                List<String> idFields = ids.get(key);
                if (idFields != null) {
                    String methodBody = buildEquals(claz.getName(), idFields);
                    System.out.println(methodBody);
                    CtMethod method = CtMethod.make(methodBody, claz);
                    claz.addMethod(method);
                }
                classes.put(key, claz.toClass());
                /*System.out.println(claz);
                Class c = claz.toClass();
                System.out.println(Arrays.toString(c.getDeclaredFields()));
                Field[] fields = c.getDeclaredFields();
                Object o = c.newInstance();
                for (Field f: fields) {
                    System.out.println(f.getName() + ": " + f.get(o));
                }
                System.out.println(Arrays.toString(c.getMethods()));

                System.out.println(o.getClass());*/

            }
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);


                String[] terms = line.split("\t");
                Map<String, Object> data = new HashMap<>();

                keySet = signatures.keySet();
                for (String key : keySet) {
                    List<Integer> indices = signatures.get(key);
                    Class claz = classes.get(key);
                    Object o = claz.newInstance();
                    //Map<String, Object> props = new HashMap<>();
                    for (Integer i : indices) {
                        if (!terms[i].isEmpty()) {
                            Field field = claz.getDeclaredField(header[i]);
                            if (numericData.contains(i)) {
                                field.setDouble(o, Double.valueOf(terms[i]));
                                //props.put(header[i], Double.valueOf(terms[i]));
                            } else {
                                field.set(o, terms[i]);
                                //props.put(header[i], terms[i]);
                            }
                            //System.out.println(field.getName() + ": " + field.get(o));
                        }

                    }

                    data.put(key, o);
                }

                Map<String, Node> newNodes = new HashMap<>();
                for (String key : keySet) {
                    if (unique.contains(key)) {
                        newNodes.put(key, createNode(data.get(key)));
                    } else {
                        newNodes.put(key, lookupNode(data.get(key)));
                    }
                }
                keySet = relationships.keySet();
                for (String key : keySet) {
                    Map<String, RelationshipType> rels = relationships.get(key);
                    Node n1 = newNodes.get(key);
                    Set<String> innerKeySet = rels.keySet();
                    for (String s : innerKeySet) {
                        Node n2 = newNodes.get(s);
                        n1.createRelationshipTo(n2, rels.get(s));
                    }
                }

            }
            tx.success();
        }
    }

    private void init() {
        try {
            processProperties();
            startDB();
            //readInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void countNodes(Label label) {
        try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
            Iterator<Node> nodes = ggo.getAllNodesWithLabel(label).iterator();
            int count = 0;
            while (nodes.hasNext()) {
                Node n = nodes.next();
                /*Iterator<String> props = n.getPropertyKeys().iterator();
                while (props.hasNext()) {
                    String p = props.next();
                    System.out.println(p + ": " + n.getProperty(p));
                }*/

                /*Iterator<Relationship> rels= n.getRelationships().iterator();
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    System.out.println(rel.getType().toString());
                    Iterator<String> relProps = rel.getPropertyKeys().iterator();
                    while (relProps.hasNext()) {
                        String prop = relProps.next();
                        System.out.println(prop + " " + rel.getProperty(prop, null));
                    }
                }*/
                count++;
            }

            System.out.println(count);
            tx.success();
        }
    }

    public void deleteAllWithLabels(Label label) {
        int count = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            ResourceIterator<Node> nodes = ggo.getAllNodesWithLabel(label).iterator();
            while (nodes.hasNext()) {
                if (count > 20000) {
                    tx.success();
                    tx.close();
                    tx = db.beginTx();
                    count=0;
                }
                System.out.println(count++);
                Node n = nodes.next();
                Iterator<Relationship> relationship = n.getRelationships().iterator();
                while (relationship.hasNext()) {
                    relationship.next().delete();
                }
                n.delete();
            }

        }
        finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }
        }
    }

    public void labelGenes() {
        String file = "input/chia_labels.tsv";
        try (Transaction tx = db.beginTx(); BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split("\t");
            System.out.println(Arrays.toString(header));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] terms = line.split("\t");
                //System.out.println(header[0] + " " + terms[0]);
                Iterator<Node> nodes = db.findNodesByLabelAndProperty(DynamicLabel.label("GENE"), header[0], terms[0]).iterator();
                if (!nodes.hasNext()) {
                    throw new IllegalArgumentException("No such gene.");
                }
                Node node = nodes.next();
                System.out.println(node.getProperty("GeneSymbol", ""));
                /*if (nodes.hasNext()) {
                    throw new IllegalArgumentException("Id is not unique.");
                }
                boolean hasLabel = false;
                for (int i = 1; i < terms.length; ++i) {
                    if (terms[i].equals("1")) {
                        count++;
                        node.addLabel(DynamicLabel.label(header[i]));
                        hasLabel = true;
                    }
                }
                if (!hasLabel) {
                    node.addLabel(DynamicLabel.label("Wild type"));
                }
                tx.success();*/
            }
            System.out.println(count);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void precompute() throws IOException {
        int count = 0, limit = 100;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            ResourceBundle props = new PropertyResourceBundle(new FileReader("conf" + File.separator + "org.mineotor.properties"));
            String relName = props.getString("query_relationship");
            RelationshipType rt = DynamicRelationshipType.withName(relName);
            String[] terms = relName.split("-");
            /*if (terms.length != 2) {
                throw new IllegalArgumentException("Incorrect query relationship name.");
            }*/
            Label group = DynamicLabel.label(terms[0]);
            Label offspring = DynamicLabel.label(terms[1]);
            Label precomputed = DynamicLabel.label(group + "_COLLECTED");
            RelationshipType preRT = DynamicRelationshipType.withName("COLLECTED");
            Iterator<Node> nodes = ggo.getAllNodesWithLabel(group).iterator();
            /*if (!nodes.hasNext()) {
                throw new IllegalStateException("There is node with the label" + terms[0]);
            }*/
            while (nodes.hasNext()) {
                count++;
                Node node = nodes.next();
                System.out.println(node.getProperty("GeneSymbol"));
                Iterator<Relationship> rels = node.getRelationships().iterator();
                Map<String, List<Double>> features = new HashMap<>();
                /*if (!rels.hasNext()) {
                    throw new IllegalStateException("There is no node with the label " + terms[0]);
                }*/
                if (node.hasRelationship(preRT)) {
                    continue;
                }
                while (rels.hasNext()) {

                    Relationship rel = rels.next();
                    //System.out.println(rel.getType().name());
                    Node other = rel.getOtherNode(node);
                    if (!other.hasLabel(offspring)) {
                        continue;
                    }
                    Iterator<String> properties = other.getPropertyKeys().iterator();
                    while (properties.hasNext()) {
                        String key = properties.next();
                        Object o = other.getProperty(key);
                        if (o instanceof Number) {
                            List<Double> values = features.get(key);
                            if (values == null) {
                                values = new ArrayList<>();
                                features.put(key, values);
                            }
                            values.add((Double) o);
                        }
                    }
                    Node pre = db.createNode(precomputed);
                    pre.createRelationshipTo(node, preRT);
                    Set<String> keySet = features.keySet();
                    //System.out.println(keySet.toString());
                    for (String s : keySet) {
                        List<Double> values = features.get(s);
                        int size = values.size();
                        double[] arr = new double[size];
                        for (int i = 0; i < size; ++i) {
                            arr[i] = values.get(i);
                        }
                        pre.setProperty(s, arr);
                    }
                    System.out.println(count++);
                    if (count % limit == 0) {
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                    }
                }

            }


        }
        finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }

        }

    }

    public static void main(String[] args) {
        MineotaurGeneratorJA gen = new MineotaurGeneratorJA("input/chia_sample.txt", "input/mineotaur.input");
        /*try {
            gen.readInput();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }*/
        //gen.countNodes(DynamicLabel.label("GENE"));
        /*gen.countNodes(DynamicLabel.label("EXPERIMENT"));
        gen.countNodes(DynamicLabel.label("GENE"));
        gen.countNodes(DynamicLabel.label("CELL"));*/
        /*gen.labelGenes();*/
        /*try {
            gen.precompute();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        gen.countNodes(DynamicLabel.label("GENE"));
    }

}
