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

package org.mineotaur.importer;

import javassist.*;
import javassist.NotFoundException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.FileUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class containing fields and methods to generate a graph database from the input provided by the user.
 * @author Bálint Antal
 * @version 0.5b
 */
public abstract class DatabaseGenerator {

    //protected static final String HIT = "HIT";
    protected static final String NUMBER = "NUMBER";
    protected static final String ID = "ID";
    protected static final String JAVA_LANG_STRING = "java.lang.String";
    protected static final String FILTER = "FILTER";
    protected static final String COLLECTED = "_COLLECTED";
    protected static final String FILE_SEPARATOR = File.separator;
    protected static final String CONF = "conf";
    protected static final String ARRAY = "_ARRAY";
    protected static final String DB = "db";
    protected ResourceBundle properties;
//    protected final String prop;
    protected String name;
    protected String separator;
    protected String confDir;
    protected String dbPath;
    protected String group;
    protected String descriptive;
    protected String groupName;
    protected List<String> unique = new ArrayList<>();
    protected GraphDatabaseService db;
    protected GlobalGraphOperations ggo;
    protected final Map<String, Label> labels = new HashMap<>();
    protected final Map<String, List<Node>> nonUniqueNodes = new HashMap<>();
    protected final Map<String, Map<String, RelationshipType>> relationships = new HashMap<>();
    protected final Map<String, Class> classes = new HashMap<>();
    //protected Map<Label, List<Object>> stored = new HashMap<>();
    protected String[] header;
    protected String[] nodeTypes;
    protected String[] dataTypes;
    //protected List<Integer> filters = new ArrayList<>();
    protected final Map<String, List<Integer>> signatures = new HashMap<>();
    //protected Map<String, Label> hits;
    protected final List<Integer> numericData = new ArrayList<>();
    protected final Map<String, List<String>> ids = new HashMap<>();
    protected Set<String> keySet;
    protected int classCount;
    protected Label groupLabel;
    protected Label descriptiveLabel;
    protected final Label wildTypeLabel = DynamicLabel.label("Wild type");
    protected final Properties mineotaurProperties = new Properties();
    protected String totalMemory;
    protected String cache;
    protected Set<String> relKeySet;
    protected boolean toPrecompute;
    protected final List<String> filterProps = new ArrayList<>();
    protected int limit;
    protected BufferedReader br;
    protected boolean overwrite;
    protected int relationshipCount;
    //protected String filterName;
    protected Label precomputed;



    /*public DatabaseGenerator(String prop) {
        this.prop = prop;
        init();
    }

    *//**
     *  Method for processing of environment variables and creating and starting an empty database.
     *//*
    protected void init() {
        try {
            Mineotaur.LOGGER.info("Reading properties...");
            processProperties();
            Mineotaur.LOGGER.info("Done\nStarting database...");
            startDB();
            Mineotaur.LOGGER.info("Database started.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /**
     * Processing input properties and creating a directory to store configuration files.
     * @throws IOException if there is an error with the input property file
     */
    /*protected void processProperties() throws IOException {
        properties = new PropertyResourceBundle(new FileReader(prop));
        name = properties.getString("name");
        group = properties.getString("group");
        groupName = properties.getString("groupName");
        groupLabel = DynamicLabel.label(group);
        descriptive = properties.getString("descriptive");
        descriptiveLabel = DynamicLabel.label(descriptive);
        unique = Arrays.asList(properties.getString("unique").split(","));
        separator = properties.getString("separator");

        totalMemory = properties.getString("total_memory");
        //mineotaurProperties.put("base_dir", name);
//
        cache = properties.getString("cache");
        confDir = new StringBuilder(name).append(FILE_SEPARATOR).append(CONF).append(FILE_SEPARATOR).toString();
        confDir = name + FILE_SEPARATOR + CONF + FILE_SEPARATOR;
        dbPath = name + FILE_SEPARATOR + DB + FILE_SEPARATOR;
        createDirs();
        createRelationships(properties.getString("relationships"));
        relKeySet = relationships.keySet();
        toPrecompute = "true".equals(properties.getString("precompute"));
        limit = Integer.valueOf(properties.getString("process_limit"));
        overwrite = "true".equals(properties.getString("overwrite").trim());
        Mineotaur.LOGGER.info(String.valueOf(overwrite));
    }*/


    protected void createDir(String name) {
        File dir = new File(name);
        boolean dirExists = dir.exists();
        if (!dirExists || overwrite) {
            if (dirExists) {
                dir.delete();
            }
            dir.mkdir();
        }
    }
    /**
     * Method for creating a directory for the configuration files.
     */
    protected void createDirs() {
        createDir(name);
        createDir(confDir);
    }

    /**
     * Creates relationships between the appropriate nodes.
     * @param rels A string containing the names. Each relationship should be separated by a ',', while the nodes should be separated by '-'.
     */
    protected void createRelationships(String rels) {
        String[] terms = rels.split(",");
        for (String s : terms) {
            String[] objects = s.split("-");
            Map<String, RelationshipType> map = relationships.get(objects[0]);
            if (map == null) {
                map = new HashMap<>();
                relationships.put(objects[0], map);
            }
            map.put(objects[1], DynamicRelationshipType.withName(objects[0] + "_AND_" + objects[1]));
        }
        relationshipCount = relationships.size();
    }

    /**
     * Starts the database. If there was no database in the confDir present, a new instance is created.
     */
    protected void startDB() {
        GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath);
        gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, totalMemory);
        gdb.setConfig(GraphDatabaseSettings.cache_type, cache);
        db = gdb.newGraphDatabase();
        registerShutdownHook(db);
        ggo = GlobalGraphOperations.at(db);
    }

    /**
     * Registers a shutdown hook for the database (from neo4j.com).
     * @param graphDb The database.
     */
    protected static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public abstract void generateDatabase();
    public abstract void processData();
    public abstract void labelGenes();

    /**
     * Public method to generate the database from the inputs provided.
     * @param dataFile A ?SV file containing the data to be processed.
     * @param labelFile A ?SV file containg the label information for the group object.
     * @throws IllegalAccessException
     * @throws IOException
     * @throws InstantiationException
     * @throws CannotCompileException
     * @throws NotFoundException
     * @throws NoSuchFieldException
     */
    /*public void generateDatabase(String dataFile, String labelFile) throws IllegalAccessException, IOException, InstantiationException, CannotCompileException, NotFoundException, NoSuchFieldException {
        Mineotaur.LOGGER.info("Processing metadata.");
        processMetadata(dataFile);
        Mineotaur.LOGGER.info("Generating classes.");
        generateClasses();
        createIndex(db);
        Mineotaur.LOGGER.info("Processing input data.");
        processData();
        Mineotaur.LOGGER.info("Processing label data.");
        labelGenes(labelFile);
        //filters.add(6);
        if (filterProps != null && !filterProps.isEmpty()) {
            createFilters();
        }
        if (toPrecompute) {
            Mineotaur.LOGGER.info("Precomputing nodes.");
            precomputed = DynamicLabel.label(group+COLLECTED);
            precomputeOptimized(Integer.valueOf(properties.getString("precompute_limit")));
        }
        else {
            mineotaurProperties.put("query_relationship", relationships.get(group).get(descriptive));
        }
        Mineotaur.LOGGER.info("Generating property files.");
        storeFeatureNames();
        storeGroupnames(db);
        generatePropertyFile();

        Mineotaur.LOGGER.info("Database generation finished. Start Mineotaur instance with -start " + name);
    }*/

    /**
     * Method for processing the input provided.

     * @throws IOException
     * @throws javassist.NotFoundException
     * @throws CannotCompileException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    /*protected void readInput() throws IOException, javassist.NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException, NoSuchFieldException {

    }*/

    /**
     * Method for processing the header information provided in the input.
     * @param input Path to the input file.
     */
    /*protected void processMetadata(String input) throws IOException {
        br = new BufferedReader(new FileReader(input));
        header = br.readLine().split(separator);
        nodeTypes = br.readLine().split(separator);
        dataTypes = br.readLine().split(separator);
        int lineSize = header.length;
//        signatures = new HashMap<>();
        //hits = new HashMap<>();
//        numericData = new ArrayList<>();

//        ids = new HashMap<>();

        for (int i = 0; i < lineSize; ++i) {
            *//*if (nodeTypes[i].equals(HIT)) {
                if (!hits.containsKey(header[i])) {
                    hits.put(header[i], DynamicLabel.label(header[i]));
                }
                continue;
            }*//*
            List<Integer> indices = signatures.get(nodeTypes[i]);
            if (indices == null) {
                indices = new ArrayList<>();
                signatures.put(nodeTypes[i], indices);
                labels.put(nodeTypes[i], DynamicLabel.label(nodeTypes[i]));
            }
            indices.add(i);
            if (NUMBER.equals(dataTypes[i])) {
                numericData.add(i);
            }
            if (FILTER.equals(dataTypes[i])) {
                //filters.add(i);
                filterProps.add(header[i]);
            }
            if (ID.equals(dataTypes[i])) {
                List<String> idList = ids.get(nodeTypes[i]);
                if (idList == null) {
                    idList = new ArrayList<>();
                    ids.put(nodeTypes[i], idList);
                }
                idList.add(header[i]);

            }
        }
        keySet = signatures.keySet();
        classCount = keySet.size();
    }*/

    /*protected Class createClass(ClassPool pool, String className, List<Integer> indices, String[] header, String[] dataTypes, List<String> idFields) throws NotFoundException, CannotCompileException {
        CtClass claz = pool.makeClass(className);
        for (Integer i : indices) {
            CtClass type;
            if (dataTypes[i].equals(NUMBER)) {
                type = CtClass.doubleType;
            } else {
                type = pool.get(JAVA_LANG_STRING);
            }
            CtField field = new CtField(type, header[i], claz);
            field.setModifiers(Modifier.PUBLIC);
            claz.addField(field);
        }
//        List<String> idFields = ids.get(className);
        if (idFields != null) {
            String methodBody = buildEquals(claz.getName(), idFields);
            CtMethod method = CtMethod.make(methodBody, claz);
            claz.addMethod(method);
        }
        return claz.toClass();
    }

    *//**
     * Method for generating classes for the object types defined in the input file.
     * @throws NotFoundException
     * @throws CannotCompileException
     *//*
    protected void generateClasses() throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        for (String key : keySet) {
            //CtClass claz = pool.makeClass(key);
            List<Integer> indices = signatures.get(key);
            List<String> idFields = ids.get(key);
            classes.put(key, createClass(pool, key, indices, header, dataTypes, idFields));
            *//*for (Integer i : indices) {
                CtClass type;
                if (dataTypes[i].equals(NUMBER)) {
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
            classes.put(key, claz.toClass());
            *//*

        }
    }

    *//**
     * Method to symbolicly build equals method for a generated Java class.
     * @param name The name of the class.
     * @param idFields The names of the fields act as identifiers for the class.
     * @return The text of the equals method.
     *//*
    protected String buildEquals(String name, List<String> idFields) {
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



    protected Map<String, Object> generateObjectsFromLine(String[] terms) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Map<String, Object> data = new HashMap<>();
        for (String key : keySet) {
            List<Integer> indices = signatures.get(key);
            Class claz = classes.get(key);
            Object o = claz.newInstance();
            for (Integer i : indices) {
                if (!terms[i].isEmpty()) {
                    Field field = claz.getDeclaredField(header[i]);
                    if (numericData.contains(i)) {
                        field.setDouble(o, Double.valueOf(terms[i]));
                    } else {
                        field.set(o, terms[i]);
                    }
                }
            }
            data.put(key, o);
        }
        return data;
    }

    protected Map<String, Node> getNodesForObjects(Map<String, Object> data) throws IllegalAccessException {
        Map<String, Node> newNodes = new HashMap<>();
        for (String key : keySet) {
            if (unique.contains(key)) {
                newNodes.put(key, createNode(data.get(key)));

            } else {
                newNodes.put(key, lookupObject(data.get(key)));
            }
        }
        return newNodes;
    }
*/
    protected void connectNodes(Map<String, Node> newNodes) {
        for (String key : relKeySet) {
            Map<String, RelationshipType> rels = relationships.get(key);
            Node n1 = newNodes.get(key);
            Set<String> innerKeySet = rels.keySet();
            for (String s : innerKeySet) {
                Node n2 = newNodes.get(s);
                n1.createRelationshipTo(n2, rels.get(s));
            }
        }
    }
    /**
     * Method to process the data provided in the input file line by line. Field br should be set to the first data line.
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    /*protected void processData() throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        String line;
        Mineotaur.LOGGER.info("Processing data...");
        int lineCount = 0, nodeCount = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            while ((line = br.readLine()) != null) {
                Mineotaur.LOGGER.info("Line #" + (lineCount++));
                //Mineotaur.LOGGER.info(line);
                //System.out.println(line);
                String[] terms = line.split(separator);
                //System.out.println(terms.length);
                Map<String, Object> data = generateObjectsFromLine(terms);
                Map<String, Node> newNodes = getNodesForObjects(data);
                tx.success();
                connectNodes(newNodes);
                nodeCount += classCount + relationshipCount;
                *//*Map<String, Object> data = new HashMap<>();
                for (String key : keySet) {
                    List<Integer> indices = signatures.get(key);
                    Class claz = classes.get(key);
                    Object o = claz.newInstance();
                    for (Integer i : indices) {
                        if (!terms[i].isEmpty()) {
                            Field field = claz.getDeclaredField(header[i]);
                            if (numericData.contains(i)) {
                                field.setDouble(o, Double.valueOf(terms[i]));
                            } else {
                                field.set(o, terms[i]);
                            }
                        }
                    }
                    data.put(key, o);
                }Map<String, Node> newNodes = new HashMap<>();
                for (String key : keySet) {
                    nodeCount++;
                    if (unique.contains(key)) {
                        newNodes.put(key, createNode(data.get(key)));

                    } else {
                        newNodes.put(key, lookupObject(data.get(key)));
                    }
                }
                for (String key : relKeySet) {
                    Map<String, RelationshipType> rels = relationships.get(key);
                    nodeCount++;
                    Node n1 = newNodes.get(key);
                    Set<String> innerKeySet = rels.keySet();
                    for (String s : innerKeySet) {
                        Node n2 = newNodes.get(s);
                        if (n2 == null) {
                            System.out.println(s);
                        }
                        n1.createRelationshipTo(n2, rels.get(s));
                    }
                }*//*
                if (nodeCount > limit) {
                    nodeCount = 0;
                    tx.success();
                    tx.close();
                    tx = db.beginTx();
                }
            }
        }
        catch (Exception e) {
            Mineotaur.LOGGER.info(e.toString());
        }
        finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }
        }

        Mineotaur.LOGGER.info(lineCount + " lines processed.");
    }*/

    protected void createFilters() {
        Mineotaur.LOGGER.info("Creating filters...");
        int nodeCount = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
//            Iterator<Node> groups = db.findNodes(groupLabel);
            Iterator<Node> groups = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (groups.hasNext()) {
                Node group = groups.next();
                nodeCount++;
                Iterator<Relationship> rels = group.getRelationships(relationships.get(groupLabel.name()).get(descriptiveLabel.name())).iterator();
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    Node descriptive = rel.getOtherNode(group);
                    nodeCount++;
                    for (String f: filterProps) {
                        Object val = descriptive.getProperty(f,null);
                        if (val == null) {
                            Mineotaur.LOGGER.warning("Descriptive node #" + descriptive.getId() + " does not have the property " + f);
                            continue;
                        }
                        rel.setProperty(f, val);
                    }
                }
                if (nodeCount > limit) {
                    nodeCount = 0;
                    tx.success();
                    tx.close();
                    tx = db.beginTx();
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

    /**
     * Method to create node using reflection from the generated object.
     * @param o The Java object containing all data extracted from the input.
     * @return The Node created from the object.
     * @throws IllegalAccessException
     */
    /*protected Node createNode(Object o) throws IllegalAccessException {
        Class claz = o.getClass();
        Label label = labels.get(claz.getName());
        Node node = db.createNode(label);
        Field[] fields = claz.getDeclaredFields();
        for (Field f : fields) {
            if (f.get(o) != null) {
                node.setProperty(f.getName(), f.get(o));
            }

        }
        return node;
    }*/

    /**
     * Looks up whether a Node with the same identifiers as the object provided has been already created.
     * @param o The Java object containing all data extracted from the input.
     * @return The retireved Node object or if not exists, a new one created by createNode.
     * @throws IllegalAccessException
     */
    /*protected Node lookupObject(Object o) throws IllegalAccessException {
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
        Node node = createNode(o);
        *//*if (node.hasLabel(groupLabel)) {
            Mineotaur.LOGGER.info("New gene: " + node.getProperty(groupName, node.getProperty("reference","")));
        }*//*
        storedNodes.add(node);
        return node;
    }*/

    /**
     * Method to label group objects.
     * @param file The ?SV file containing the labels.
     */
    /*protected void labelGenes(String file) {
        try (Transaction tx = db.beginTx(); BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split(separator);
            //System.out.println(Arrays.toString(header));
            String line;
            //int count = 0;
            List<String> list = new ArrayList<>(Arrays.asList(header).subList(1, header.length));
            //list.add(wildTypeLabel.name());
            FileUtil.saveList(confDir + "mineotaur.hitLabels", list);
            while ((line = br.readLine()) != null) {
                String[] terms = line.split(separator);
                *//*Iterator<Node> nodes = db.findNodes(groupLabel);
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    Iterator<String> props = node.getPropertyKeys().iterator();
                    while (props.hasNext()) {
                        System.out.println(props.next());
                    }
                    System.out.println(nodes.next().getProperty("geneName"));
                }*//*
                Iterator<Node> nodes = db.findNodesByLabelAndProperty(groupLabel, header[0], terms[0]).iterator();
//                Iterator<Node> nodes = db.findNodes(groupLabel, header[0], terms[0]);

                *//*Node node = db.findNode(groupLabel, header[0], terms[0]);
                if (node == null) {
                    throw new IllegalArgumentException("No such gene:" + terms[0]);
                }*//*
                if (!nodes.hasNext()) {
                  Mineotaur.LOGGER.warning("No such gene: " + terms[0]);
                    continue;
                    *//*
                    nodes = db.findNodes(groupLabel, "reference", terms[0]);
                    if (!nodes.hasNext()) {*//*
                    //throw new IllegalStateException("No such gene: " + terms[0]);
                    //}

                    *//*Mineotaur.LOGGER.info(String.valueOf(nodes.hasNext()));
                    continue;*//*
                }
                Node node = nodes.next();
                *//*if (!node.hasProperty(header[0])) {
                    node.setProperty(header[0], node.getProperty("reference"));
                }*//*
                if (nodes.hasNext()) {
                    //Mineotaur.LOGGER.info("Id is not unique: " + terms[0]);
                    throw new IllegalStateException("Id is not unique: " + terms[0]);
                    *//*int count= 0;
                    while (nodes.hasNext()) {
                        nodes.next();
                        count++;
                    }
                    Mineotaur.LOGGER.info(String.valueOf(count));*//*
                }
                boolean hasLabel = false;
                for (int i = 1; i < terms.length; ++i) {
                    if (terms[i].equals("1")) {
                        //count++;
                        node.addLabel(DynamicLabel.label(header[i]));
                        hasLabel = true;
                    }
                }
                if (!hasLabel) {
                    node.addLabel(wildTypeLabel);
                }
                tx.success();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
*/
    /**
     * Method to create precomputated Nodes.
     * @param limit The maximum number of fetched Nodes per transactions.
     * @throws IOException
     */
    /*protected void precompute(int limit) throws IOException {
        int count = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            Label precomputed = DynamicLabel.label(group + "_" + COLLECTED);
            RelationshipType preRT = DynamicRelationshipType.withName(COLLECTED);
            RelationshipType rt = relationships.get(group).get(descriptive);
            Iterator<Node> nodes = db.findNodes(groupLabel);

//            Iterator<Node> nodes = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (nodes.hasNext()) {
                count++;
                Node node = nodes.next();
                Iterator<Relationship> rels = node.getRelationships(rt).iterator();
                Map<String, List<Double>> features = new HashMap<>();
                if (node.hasRelationship(preRT)) {
                    continue;
                }
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    Node other = rel.getOtherNode(node);
                    if (!other.hasLabel(descriptiveLabel)) {
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
                    for (String s : keySet) {
                        List<Double> values = features.get(s);
                        int size = values.size();
                        double[] arr = new double[size];
                        for (int i = 0; i < size; ++i) {
                            arr[i] = values.get(i);
                        }
                        pre.setProperty(s, arr);
                    }
                    count++;
                    if (count % limit == 0) {
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                    }
                }
            }
        } finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }

        }
    }*/

    /*protected Map<String, List<Double>> collectData(Node node, RelationshipType rt) {
        Iterator<Relationship> rels = node.getRelationships(rt).iterator();
        Map<String, List<Double>> features = new HashMap<>();
//                if (node.hasRelationship(preRT)) {
//                    continue;
//                }

        while (rels.hasNext()) {
            Relationship rel = rels.next();
            Node other = rel.getOtherNode(node);
            if (!other.hasLabel(descriptiveLabel)) {
                continue;
            }
            Iterator<String> properties = other.getPropertyKeys().iterator();
            if (!other.hasProperty(filterProps.get(0))) {
                continue;
            }
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
        }
            return features;
    }

    protected Map<String, List<String>> collectFilters(Relationship rel) {
        Map<String, List<String>> innerFilter = new HashMap<>();
        for (String name : filterProps) {
            List<String> filtValues = innerFilter.get(name);
            if (filtValues == null) {
                filtValues = new ArrayList<>();
                innerFilter.put(name, filtValues);
            }
            Object val = rel.getProperty(name, null);
            if (val != null) {
                filtValues.add((String) rel.getProperty(name));
            }

        }
        return innerFilter;
    }*/



    protected void precomputeOptimized(int limit) {
        int count = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
//            Label precomputed = DynamicLabel.label(group + "_" + COLLECTED);
//            RelationshipType preRT = DynamicRelationshipType.withName("COLLECTED_OPTIMIZED");
            RelationshipType rt = relationships.get(group).get(descriptive);
//            Iterator<Node> nodes = db.findNodes(groupLabel);

            Iterator<Node> nodes = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (nodes.hasNext()) {
                Mineotaur.LOGGER.info("Precomputing: " + (count++));
//                count++;
                Node node = nodes.next();
                //Map<String, List<Double>> features = collectData(node, rt);
                Iterator<Relationship> rels = node.getRelationships(rt).iterator();
                Map<String, List<Double>> features = new HashMap<>();
//                if (node.hasRelationship(preRT)) {
//                    continue;
//                }
                Map<String, List<String>> innerfilter = new HashMap<>();
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    Node other = rel.getOtherNode(node);
                    if (!other.hasLabel(descriptiveLabel)) {
                        continue;
                    }
                    if (!filterProps.isEmpty()) {
                        boolean filterSet = false;
                        for (String name: filterProps) {
                            if (other.hasProperty(name)) {
                                filterSet = true;
                            }
                        }
                        if (!filterSet) {
                            continue;
                        }
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
                    for (String name: filterProps) {
                        List<String> filtValues = innerfilter.get(name);
                        if (filtValues == null) {
                            filtValues = new ArrayList<>();
                            innerfilter.put(name, filtValues);
                        }
                        Object val =  rel.getProperty(name,null);
                        if (val != null) {
                            String value = String.valueOf(val);
                            if (!val.equals("NaN")) {
                                filtValues.add(value);
                            }
                        }

                    }

                    count++;
                    if (count % limit == 0) {
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                    }
                }
                String[] filterArr = null;
                if (!filterProps.isEmpty()) {
                    List<String> filterList = innerfilter.get(filterProps.get(0));
                    filterArr = filterList.toArray(new String[filterList.size()]);

                }
                Set<String> keySet = features.keySet();
                Map<String, List<Double>> valuesByFilter = new HashMap<>();
                for (String s : keySet) {
                    Node pre = db.createNode(precomputed);
                    pre.createRelationshipTo(node, DynamicRelationshipType.withName(s + ARRAY));
                    Mineotaur.LOGGER.info("Created array node for property " + s);
                    List<Double> values = features.get(s);
                    int size = values.size();
                    double[] arr = new double[size];
/*
                    DescriptiveStatistics stat = new DescriptiveStatistics();
*/
                    for (int i = 0; i < size; ++i) {
                        double value = values.get(i);
                        arr[i] = value;
                        if (filterArr != null) {
                            List<Double> vals = valuesByFilter.get(filterArr[i]);
                            if (vals == null) {
                                vals = new ArrayList<>();
                                valuesByFilter.put(filterArr[i], vals);
                            }
                            vals.add(arr[i]);
                        }
                        /*stat.addValue(value);*/
                    }
                    if (filterArr != null) {
                        pre.setProperty("filter", filterArr);
                    }
                    pre.setProperty(s, arr);
//                    rel.setProperty("aggregated", false);
                    Set<String> uniqueFilters = valuesByFilter.keySet();
                    String[] uniqueArr = uniqueFilters.toArray(new String[uniqueFilters.size()]);
                    int filterSize = uniqueArr.length;
                    double maxSize = Math.pow(2,filterSize);
                    int set = 1;
                    while (set < maxSize) {
//                        System.out.println("set: " + Integer.toBinaryString(set));
//                        int mask = 1;
                        DescriptiveStatistics stat = new DescriptiveStatistics();
                        Node precomputedAgg = db.createNode(precomputed);
                        Relationship aggRel = precomputedAgg.createRelationshipTo(node, DynamicRelationshipType.withName(s));
//                        aggRel.setProperty("aggregated", true);
                        List<String> actualFilters = new ArrayList<>();
                        for (int i = 0; i < filterSize; ++i) {
//                System.out.println("mask: "+ Integer.toBinaryString(mask));
//                System.out.println("and: "+Integer.toBinaryString((set >> i) & 1));

                            if (((set >> i) & 1)  == 1) {
                                List<Double> v = valuesByFilter.get(uniqueArr[i]);
                                actualFilters.add(uniqueArr[i]);
//                                aggRel.setProperty(uniqueArr[i], true);
                                for (Double d: v) {
                                    if (!Double.isNaN(d)) {
                                        stat.addValue(d);
                                    }

                                }
//                                System.out.println(uniqueArr[i]);
                            }
//                mask <<= 1;
                        }

                        aggRel.setProperty("filter", actualFilters.toArray(new String[actualFilters.size()]));
                        precomputedAgg.setProperty("Average", stat.getMean());
                        precomputedAgg.setProperty("Minimum", stat.getMin());
                        precomputedAgg.setProperty("Maximum", stat.getMax());
                        precomputedAgg.setProperty("Standard deviation", stat.getStandardDeviation());
                        precomputedAgg.setProperty("Median", stat.getPercentile(50));
                        precomputedAgg.setProperty("Count", stat.getN());
                        Mineotaur.LOGGER.info("Created aggregated node for property " + s);
                        set += 1;
                    }
                    /*pre.setProperty("Average", stat.getMean());
                    pre.setProperty("Minimum", stat.getMin());
                    pre.setProperty("Maximum", stat.getMax());
                    pre.setProperty("Standard deviation", stat.getStandardDeviation());
                    pre.setProperty("Median", stat.getPercentile(50));
                    pre.setProperty("Count", stat.getN());*/
                }
            }
        } finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }

        }
    }

    /**
     * Method to store feature names in an external file.
     */
    protected abstract void storeFeatureNames();
    /*protected void storeFeatureNames() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < numericData.size(); ++i) {
            labels.add(header[numericData.get(i)]);
        }
        FileUtil.saveList(confDir + "mineotaur.features", labels);

        *//*try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
            Iterator<Node> iterator = ggo.getAllNodesWithLabel(descriptiveLabel).iterator();
            List<String> labels = new ArrayList<>();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                Iterator<String> props = node.getPropertyKeys().iterator();
                while (props.hasNext()) {
                    String prop = props.next();
                    if (!labels.contains(prop)) {
                        labels.add(prop);
                    }
                }
            }
            System.out.println(labels.toString());
            FileUtil.saveList(new StringBuilder(confDir).append("mineotaur.features").toString(), labels);
            tx.success();
        }*//*
    }*/

    /**
     * Method to store the names of the group objects in an external file.
     * @param db The GraphDatabaseService instance.
     */
    protected void storeGroupnames(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
            Iterator<Node> groups = ggo.getAllNodesWithLabel(groupLabel).iterator();
            //Iterator<Node> groups = db.findNodes(groupLabel);

            List<String> names = new ArrayList<>();
            while (groups.hasNext()) {
                Node node = groups.next();
                String gname = (String) node.getProperty(groupName, null);
                if (gname != null) {

                    names.add(gname);
                }
                else {
                    gname = (String) node.getProperty("reference", null);
                    if (gname != null) {
                        node.setProperty(groupName, gname);
                        names.add(gname);
                    }
                }

                //labels.add(node.getProperty("GFP","") + ", " + node.getProperty("Deletion",""));
            }
            //System.out.println(labels.toString());
            Collections.sort(names);
            FileUtil.saveList(confDir + "mineotaur.groupNames", names);
            tx.success();
        }
    }

    /**
     * Method to store the filters in an external file.
     */
    protected void storeFilters() {
        /*List<String> filterNames = new ArrayList<>();
        for (Integer i : filters) {
            filterNames.add(header[i]);
        }*/
//        filterNames.add("GrowthStage");
        try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
//            Iterator<Node> iterator = db.findNodes(descriptiveLabel);

            Iterator<Node> iterator = ggo.getAllNodesWithLabel(descriptiveLabel).iterator();
            List<String> filterValues = new ArrayList<>();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                Iterator<String> props = node.getPropertyKeys().iterator();
                while (props.hasNext()) {
                    String prop = props.next();
                    if (filterProps.contains(prop)) {
                        String value = String.valueOf(node.getProperty(prop));

                        if (!value.equals("NaN") && !filterValues.contains(value)) {
                            filterValues.add(value);
                        }

                    }
                }
            }
            FileUtil.saveList(confDir + "mineotaur.filters", filterValues);
            tx.success();
        }
        //FileUtil.saveList(new StringBuilder(confDir).append("mineotaur.filters").toString(), filterNames);
    }

    /**
     * Method to store the environment properties for the Mineotaur instance.
     * @throws IOException
     */
    protected void generatePropertyFile() throws IOException {
        if (filterProps == null || filterProps.isEmpty()) {
            mineotaurProperties.put("hasFilters", "false");
        }
        else {
            mineotaurProperties.put("hasFilters", "true");
            mineotaurProperties.put("filterName", filterProps.get(0));
            storeFilters();
        }
        if (toPrecompute) {
            mineotaurProperties.put("query_relationship", precomputed.name());
        }
        else {
            mineotaurProperties.put("query_relationship", relationships.get(groupLabel.name()).get(descriptiveLabel.name()));
        }
        mineotaurProperties.put("group", group);
        mineotaurProperties.put("groupName", groupName);
//        mineotaurProperties.put("total_memory", totalMemory);
        mineotaurProperties.put("db_path", dbPath);
        mineotaurProperties.put("cache", "soft");
        mineotaurProperties.put("omero", properties.getString("omero_server"));
        Mineotaur.LOGGER.info(mineotaurProperties.toString());
        mineotaurProperties.store(new FileWriter(confDir + "mineotaur.properties"), "Mineotaur configuration properties");
    }

    public void createIndex(GraphDatabaseService db) {
        IndexDefinition indexDefinition;
        try ( Transaction tx = db.beginTx() )
        {
            Schema schema = db.schema();
            indexDefinition = schema.indexFor( groupLabel )
                    .on(groupName )
                    .create();
            tx.success();
        }

        try ( Transaction tx = db.beginTx() )
        {
            Schema schema = db.schema();
            schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
            tx.success();
        }
    }

    protected void getImageIDs(RelationshipType rt) {
        try (Transaction tx = db.beginTx()) {
            Iterator<Node> nodes = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (nodes.hasNext()) {
                Node group = nodes.next();

                List<String> imageIDs = new ArrayList<>();
                Iterator<Relationship> rels = group.getRelationships(rt).iterator();
                while (rels.hasNext()) {
                    Node other = rels.next().getOtherNode(group);
                    String imageID = String.valueOf(other.getProperty("imageID", null));
                    if (imageID != null && !imageIDs.contains(imageID)) {
                        imageIDs.add(imageID);
                    }
                }
                Mineotaur.LOGGER.info("Gene " + group.getId() + ": " + imageIDs.toString());
                group.setProperty("imageIDs", imageIDs.toArray(new String[imageIDs.size()]));
            }
            tx.success();
        }

    }

    public static void main(String[] args) {
        /*String[] uniqueArr = {"B", "S", "U", "N"};
        int filterSize = uniqueArr.length;
        double maxSize = Math.pow(2,filterSize);
        int set = 1;
        while (set < maxSize) {
            System.out.println("set: " + Integer.toBinaryString(set));
            //int mask = 1;
            for (int i = 0; i < filterSize; ++i) {
//                System.out.println("mask: "+ Integer.toBinaryString(mask));
//                System.out.println("and: "+Integer.toBinaryString((set >> i) & 1));

                if (((set >> i) & 1)  == 1) {
                    System.out.println(uniqueArr[i]);
                }
//                mask <<= 1;
            }
            set += 1;
        }*/
        /*DatabaseGenerator databaseGenerator = new DatabaseGenerator("input/Pravda.input");

        *//*databaseGenerator.descriptiveLabel = DynamicLabel.label("CELL");
        databaseGenerator.storeFeatureNames(databaseGenerator.db);
        databaseGenerator.storeFilters();*//*
        databaseGenerator.createIndex(databaseGenerator.db);
*/
        /*try {
           databaseGenerator.generatePropertyFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }




}
