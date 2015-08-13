package org.mineotaur.importer;

import javassist.*;
import javassist.NotFoundException;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.ClassUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by balintantal on 07/07/2015.
 */
public class DatabaseGeneratorFromFile extends DatabaseGenerator{
    protected static final String NUMBER = "NUMBER";
    protected static final String ID = "ID";
    protected static final String FILTER = "FILTER";

    //protected ResourceBundle properties;
    protected final String prop;
/*
    protected List<String> unique = new ArrayList<>();
*/
    protected final Map<String, List<Node>> nonUniqueNodes = new HashMap<>();
    protected Map<String, Map<String, RelationshipType>> relationships;
    protected final Map<String, Class> classes = new HashMap<>();
    protected String[] header;
    protected String[] nodeTypes;
    protected String[] dataTypes;
    protected final Map<String, List<Integer>> signatures = new HashMap<>();
    protected final List<Integer> numericData = new ArrayList<>();
    protected final Map<String, List<String>> ids = new HashMap<>();
    protected Set<String> keySet;
    protected Set<String> relKeySet;
    protected BufferedReader br;
    protected boolean overwrite;
    protected final String dataFile;
    protected final String labelFile;
    protected String relationshipString;
    protected List<String> hitList;


    public DatabaseGeneratorFromFile(String prop, String dataFile, String labelFile) {
        this.prop = prop;
        this.dataFile = dataFile;
        this.labelFile = labelFile;
    }

    /**
     * Processing input properties and creating a directory to store configuration files.
     * @throws IOException if there is an error with the input property file
     */
    protected void processProperties(String prop) throws IOException {
        ResourceBundle properties = new PropertyResourceBundle(new FileReader(prop));
        if (properties.containsKey("name")) {
            name = properties.getString("name");
        }
        else {
            throw new IllegalArgumentException("The input property file should contain a name property.");
        }
        if (properties.containsKey("group")) {
            group = properties.getString("group");
        }
        else {
            group = (String) DefaultProperty.GROUP.getValue();
            Mineotaur.LOGGER.warning("Property group not provided, using default value: " + group);
        }
        if (properties.containsKey("groupName")) {
            groupName = properties.getString("groupName");
        }
        else {
            groupName = (String) DefaultProperty.GROUP_NAME.getValue();
            Mineotaur.LOGGER.warning("Property groupName not provided, using default value: " + groupName);
        }
        if (properties.containsKey("descriptive")) {
            descriptive = properties.getString("descriptive");
        }
        else {
            descriptive = (String) DefaultProperty.DESCRIPTIVE.getValue();
            Mineotaur.LOGGER.warning("Property descriptive not provided, using default value: " + descriptive);
        }
        if (properties.containsKey("separator")) {
            separator = properties.getString("separator");
        }
        else {
            separator = (String) DefaultProperty.SEPARATOR.getValue();
            Mineotaur.LOGGER.warning("Property separator not provided, using default value: " + separator);
        }
        if (properties.containsKey("total_memory")) {
            totalMemory = properties.getString("total_memory");
        }
        else {
            totalMemory = (String) DefaultProperty.TOTAL_MEMORY.getValue();
            Mineotaur.LOGGER.warning("Property total_memory not provided, using default value: " + totalMemory);
        }
        if (properties.containsKey("relationships")) {
            relationshipString = properties.getString("relationships");
        }
        else {
            relationshipString = (group+"-"+descriptive);
            Mineotaur.LOGGER.warning("Property relationships not provided, using default value: " + relationshipString);
        }
        if (properties.containsKey("limit")) {
            limit = Integer.valueOf(properties.getString("limit"));
        }
        else {
            limit = (int) DefaultProperty.LIMIT.getValue();
            Mineotaur.LOGGER.warning("Property limit not provided, using default value: " + limit);
        }
        if (properties.containsKey("overwrite")) {
            overwrite = Boolean.valueOf(properties.getString("overwrite"));
        }
        else {
            overwrite = (boolean) DefaultProperty.OVERWRITE.getValue();
            Mineotaur.LOGGER.warning("Property overwrite not provided, using default value: " + overwrite);
        }
        groupLabel = DynamicLabel.label(group);
        descriptiveLabel = DynamicLabel.label(descriptive);
        precomputedLabel = DynamicLabel.label(group+COLLECTED);
        confDir = name + FILE_SEPARATOR + CONF + FILE_SEPARATOR;
        dbPath = name + FILE_SEPARATOR + DB + FILE_SEPARATOR;

    }


    /**
     * Public method to generate the database from the inputs provided.
     */
    @Override
    public void generateDatabase() {
        try {
            Mineotaur.LOGGER.info("Reading properties...");
            processProperties(prop);
            Mineotaur.LOGGER.info("Creating directories...");
            createDirs(name, confDir, overwrite);
            Mineotaur.LOGGER.info("Creating relationship types...");
            relationships = createRelationshipTypes(relationshipString);
            relKeySet = relationships.keySet();
            Mineotaur.LOGGER.info("Starting database...");
            startDB(dbPath, totalMemory, cache);
            Mineotaur.LOGGER.info("Processing metadata.");
            processMetadata();
            Mineotaur.LOGGER.info("Generating classes.");
            generateClasses(keySet, signatures, ids, classes, header, dataTypes);
            GraphDatabaseUtils.createIndex(db, groupLabel, groupName);
            Mineotaur.LOGGER.info("Processing input data.");
            processData(db, br, separator, keySet, signatures, classes, header, numericData, nonUniqueNodes, labels, descriptive, relKeySet, relationships, limit);
            Mineotaur.LOGGER.info("Processing label data.");
            labelGenes();
            if (filterProps != null && !filterProps.isEmpty()) {
                Mineotaur.LOGGER.info("Creating filters...");
                createFilters(db, ggo, groupLabel, descriptiveLabel, relationships.get(group).get(descriptive), filterProps, limit);
            }
            Mineotaur.LOGGER.info("Precomputing nodes.");
            precomputeOptimized(db, ggo, groupLabel, descriptiveLabel, precomputedLabel, relationships, filterProps, group, descriptive, limit);
            Mineotaur.LOGGER.info("Generating property files.");
            storeConfigurationFiles();
            /*generateFeatureNameList();
            generateGroupnameList(db, groupLabel, groupName, confDir);
            generatePropertyFile();*/
            // TODO: fix
            /*if (properties.containsKey("omero")) {
                getImageIDs(relationships.get(group).get("EXPERIMENT"));
            }*/
            Mineotaur.LOGGER.info("Database generation finished. Start Mineotaur instance with -start " + name);
        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    /**
     * Method for processing the header information provided in the input.
     */
    protected void processMetadata(){
        try {
            br = new BufferedReader(new FileReader(dataFile));
            header = br.readLine().split(separator);
            nodeTypes = br.readLine().split(separator);
            dataTypes = br.readLine().split(separator);
            int lineSize = header.length;
            for (int i = 0; i < lineSize; ++i) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * Method for generating classes for the object types defined in the input file.
     * @throws javassist.NotFoundException
     * @throws CannotCompileException
     */
    protected void generateClasses(Set<String> keySet,
                                   Map<String, List<Integer>> signatures,
                                   Map<String, List<String>> ids,
                                   Map<String, Class> classes, String[] header,
                                   String[] dataTypes) throws javassist.NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        for (String key : keySet) {
            List<Integer> indices = signatures.get(key);
            List<String> idFields = ids.get(key);
            classes.put(key, ClassUtils.createClass(pool, key, indices, header, dataTypes,  NUMBER, ClassUtils.buildEquals(key, idFields)));
        }
    }

    protected Map<String, Object> generateObjectsFromLine(String[] terms,
                                                          Set<String> keySet,
                                                          Map<String, List<Integer>> signatures,
                                                          Map<String, Class> classes, String[] header,
                                                          List<Integer> numericData) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
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

    protected Map<String, Node> getNodesForObjects(GraphDatabaseService db, Map<String, Object> data, Set<String> keySet, String descriptive, Map<String, Label> labels, Map<String, List<Node>> nonUniqueNodes) throws IllegalAccessException {
        Map<String, Node> newNodes = new HashMap<>();
        for (String key : keySet) {
            if (key.equals(descriptive)) {
                newNodes.put(key, ClassUtils.createNode(data.get(key), labels.get(key), db));
            } else {
                List<Node> storedNodes = nonUniqueNodes.get(key);
                Node newNode = null;
                if (storedNodes != null) {
                    newNode = ClassUtils.lookupObject(data.get(key), storedNodes, db);
                }
                else {
                    storedNodes = new ArrayList<>();
                    nonUniqueNodes.put(key, storedNodes);
                }
                if (newNode == null) {
                    newNode = ClassUtils.createNode(data.get(key), labels.get(key), db);
                    storedNodes.add(newNode);
                }
                newNodes.put(key, newNode);
            }
        }
        return newNodes;
    }

    protected void connectNodes(Map<String, Node> newNodes, Set<String> relKeySet, Map<String, Map<String, RelationshipType>> relationships) {
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

    @Override
    protected void processData(GraphDatabaseService db) {
        processData(db, br, separator, keySet, signatures, classes, header, numericData, nonUniqueNodes, labels, descriptive, relKeySet, relationships, limit);
    }



    /**
     * Method to process the data provided in the input file line by line. Field br should be set to the first data line.
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     * @param db
     * @param br
     * @param separator
     * @param keySet
     * @param signatures
     * @param classes
     * @param header
     * @param numericData
     * @param nonUniqueNodes
     * @param labels
     * @param descriptive
     * @param relKeySet
     * @param relationships
     * @param limit
     */
    public void processData(GraphDatabaseService db,
                            BufferedReader br,
                            String separator,
                            Set<String> keySet,
                            Map<String, List<Integer>> signatures,
                            Map<String, Class> classes,
                            String[] header,
                            List<Integer> numericData,
                            Map<String, List<Node>> nonUniqueNodes,
                            Map<String, Label> labels,
                            String descriptive,
                            Set<String> relKeySet,
                            Map<String, Map<String, RelationshipType>> relationships,
                            int limit){
        String line;
        Mineotaur.LOGGER.info("Processing data...");
        int lineCount = 0;
        int nodeCount = 0;
        int classCount = keySet.size();
        int relationshipCount = relationships.size();
        Transaction tx = null;
        try {
            tx = db.beginTx();
            while ((line = br.readLine()) != null) {
                Mineotaur.LOGGER.info("Line #" + (lineCount++));
                String[] terms = line.split(separator);
                Map<String, Object> data = generateObjectsFromLine(terms, keySet, signatures, classes, header, numericData);
                Map<String, Node> newNodes = getNodesForObjects(db, data, keySet, descriptive, labels, nonUniqueNodes);
                connectNodes(newNodes, relKeySet, relationships);
                nodeCount += classCount + relationshipCount;
                /*if (nodeCount > limit) {
                    nodeCount = 0;
                    tx.success();
                    tx.close();
                    tx = db.beginTx();
                }*/
            }
        }
        /*catch (Exception e) {
            Mineotaur.LOGGER.info(e.toString());
        }*/ catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }
        }

        Mineotaur.LOGGER.info(lineCount + " lines processed.");
    }

     /**
     * Method to label group objects.
      * @param db
      * @param separator
      * @param groupLabel
      * @param wildTypeLabel
      * @param labelFile
      */
    protected List<String> labelGenes(GraphDatabaseService db,
                                      String labelFile,
                                      String separator,
                                      Label groupLabel,
                                      Label wildTypeLabel) {
        String[] header = null;
        Mineotaur.LOGGER.info(labelFile);
        List<String> hitList = new ArrayList<>();
        try (Transaction tx = db.beginTx(); BufferedReader br = new BufferedReader(new FileReader(labelFile))) {
            header = br.readLine().split(separator);
            String line;
            while ((line = br.readLine()) != null) {
                String[] terms = line.split(separator);
                Iterator<Node> nodes = db.findNodesByLabelAndProperty(groupLabel, header[0], terms[0]).iterator();
                if (!nodes.hasNext()) {
                    Mineotaur.LOGGER.warning("No such gene: " + terms[0]);
                    continue;
                }
                Node node = nodes.next();
                if (nodes.hasNext()) {
                    throw new IllegalStateException("Id is not unique: " + terms[0]);
                }
                boolean hasLabel = false;
                for (int i = 1; i < terms.length; ++i) {
                    if (terms[i].equals("1")) {
                        node.addLabel(DynamicLabel.label(header[i]));
                        hasLabel = true;
                    }
                }
                if (!hasLabel) {
                    node.addLabel(wildTypeLabel);
                    String wildType = wildTypeLabel.name();
                    if (!hitList.contains(wildType)) {
                        hitList.add(wildType);
                    }
                }
                tx.success();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        hitList.addAll(Arrays.asList(header).subList(1, header.length));
        return hitList;
    }

    @Override
    protected void labelGenes() {
        hitList = labelGenes(db, labelFile, separator, groupLabel, wildTypeLabel);
    }


    @Override
    protected List<String> getHits() {
        return hitList;
    }

    @Override
    protected List<String> generateFeatureNameList() {
        return generateFeatureNameList(numericData, header);
    }

    /**
     * Method to store feature names in an external file.
     * @param numericData
     * @param header
     */
    protected List<String> generateFeatureNameList(List<Integer> numericData, String[] header) {
        List<String> features = new ArrayList<>();
        for (int i = 0; i < numericData.size(); ++i) {
            features.add(header[numericData.get(i)]);
        }
        return features;
        //FileUtils.saveList(confDir + "mineotaur.features", features);
    }
}


