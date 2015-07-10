package org.mineotaur.importer;

import javassist.*;
import javassist.NotFoundException;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.ClassUtils;
import org.mineotaur.common.FileUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by balintantal on 07/07/2015.
 */
public class ImportFromFile extends DatabaseGenerator{
    protected static final String NUMBER = "NUMBER";
    protected static final String ID = "ID";
    protected static final String FILTER = "FILTER";

    protected ResourceBundle properties;
    protected final String prop;
    protected List<String> unique = new ArrayList<>();
    protected final Map<String, List<Node>> nonUniqueNodes = new HashMap<>();
    protected final Map<String, Map<String, RelationshipType>> relationships = new HashMap<>();
    protected final Map<String, Class> classes = new HashMap<>();
    protected String[] header;
    protected String[] nodeTypes;
    protected String[] dataTypes;
    protected final Map<String, List<Integer>> signatures = new HashMap<>();
    protected final List<Integer> numericData = new ArrayList<>();
    protected final Map<String, List<String>> ids = new HashMap<>();
    protected Set<String> keySet;
    protected int classCount;
    protected Set<String> relKeySet;
    protected BufferedReader br;
    protected boolean overwrite;
    protected int relationshipCount;
    protected Label precomputed;
    protected final String dataFile;
    protected final String labelFile;


    public ImportFromFile(String prop, String dataFile, String labelFile) {
        this.prop = prop;
        this.dataFile = dataFile;
        this.labelFile = labelFile;
        init();
    }

    /**
     *  Method for processing of environment variables and creating and starting an empty database.
     */
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
    }

    /**
     * Processing input properties and creating a directory to store configuration files.
     * @throws IOException if there is an error with the input property file
     */
    protected void processProperties() throws IOException {
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
     * Public method to generate the database from the inputs provided.
     */
    @Override
    public void generateDatabase() {
        try {
            Mineotaur.LOGGER.info("Processing metadata.");
            processMetadata();
            Mineotaur.LOGGER.info("Generating classes.");
            generateClasses();
            GraphDatabaseUtils.createIndex(db, groupLabel, groupName);
            Mineotaur.LOGGER.info("Processing input data.");
            processData();
            Mineotaur.LOGGER.info("Processing label data.");
            labelGenes();
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
            // TODO: fix
            if (properties.containsKey("omero")) {
                getImageIDs(relationships.get(group).get("EXPERIMENT"));
            }
            Mineotaur.LOGGER.info("Database generation finished. Start Mineotaur instance with -start " + name);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
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
            classCount = keySet.size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * Method for generating classes for the object types defined in the input file.
     * @throws javassist.NotFoundException
     * @throws CannotCompileException
     */
    protected void generateClasses() throws javassist.NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        for (String key : keySet) {
            List<Integer> indices = signatures.get(key);
            List<String> idFields = ids.get(key);
            classes.put(key, ClassUtils.createClass(pool, key, indices, header, dataTypes, idFields, NUMBER));
        }
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
                newNodes.put(key, ClassUtils.createNode(data.get(key), labels.get(key), db));
            } else {
                newNodes.put(key, ClassUtils.lookupObject(data.get(key), nonUniqueNodes, db));
            }
        }
        return newNodes;
    }

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
    public void processData(){
        String line;
        Mineotaur.LOGGER.info("Processing data...");
        int lineCount = 0, nodeCount = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            while ((line = br.readLine()) != null) {
                Mineotaur.LOGGER.info("Line #" + (lineCount++));
                String[] terms = line.split(separator);
                Map<String, Object> data = generateObjectsFromLine(terms);
                Map<String, Node> newNodes = getNodesForObjects(data);
                tx.success();
                connectNodes(newNodes);
                nodeCount += classCount + relationshipCount;
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
    }

     /**
     * Method to label group objects.
     */
    @Override
    protected void labelGenes() {
        try (Transaction tx = db.beginTx(); BufferedReader br = new BufferedReader(new FileReader(labelFile))) {
            String[] header = br.readLine().split(separator);
            String line;
            List<String> list = new ArrayList<>(Arrays.asList(header).subList(1, header.length));
            FileUtils.saveList(confDir + "mineotaur.hitLabels", list);
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
                }
                tx.success();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Method to store feature names in an external file.
     */
    @Override
    protected void storeFeatureNames() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < numericData.size(); ++i) {
            labels.add(header[numericData.get(i)]);
        }
        FileUtils.saveList(confDir + "mineotaur.features", labels);
    }
}


