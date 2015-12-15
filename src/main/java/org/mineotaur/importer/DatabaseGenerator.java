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


import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.FileUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
/**
 * Class containing fields and methods to generate a graph database from the input provided by the user.
 * @author Bálint Antal
 */
public abstract class DatabaseGenerator {

    protected static final String COLLECTED = "_COLLECTED";
    protected static final String FILE_SEPARATOR = File.separator;
    protected static final String CONF = "conf";
    protected static final String ARRAY = "_ARRAY";
    protected static final String DB = "db";
    protected ResourceBundle properties;
    protected String name;
    protected String separator;
    protected String confDir;
    protected String dbPath;
    protected String group;
    protected String descriptive;
    protected String groupName;
    protected GraphDatabaseService db;
    protected GlobalGraphOperations ggo;
    protected final Map<String, Label> labels = new HashMap<>();
    protected final Map<String, Map<String, RelationshipType>> relationships = new HashMap<>();
    protected Label groupLabel;
    protected Label descriptiveLabel;
    protected String totalMemory;
    protected String cache = "none";
    protected boolean toPrecompute;
    protected final List<String> filterProps = new ArrayList<>();
    protected int limit;
    protected boolean overwrite;
    protected Label precomputedLabel;
    protected String omero;
    protected final Label wildTypeLabel = DynamicLabel.label("Wild type");


    /**
     * Method for creating a directory for the configuration files.
     */
    protected void createDirs(String name, String confDir, boolean overwrite) {
        if (name == null || "".equals(name) || confDir == null || "".equals(confDir)) {
            throw new IllegalArgumentException();
        }
        FileUtils.createDir(name, overwrite);
        FileUtils.createDir(confDir, overwrite);
    }

    /**
     * Creates relationships between the appropriate nodes.
     *
     * @param rels A string containing the names. Each relationship should be separated by a ',', while the nodes should be separated by '-'.
     */
    protected Map<String, Map<String, RelationshipType>> createRelationshipTypes(String rels) {
        if (rels == null || "".equals(rels)) {
            throw new IllegalArgumentException();
        }
        if (!rels.matches("(\\w+\\-{1}\\w+,*)+")) {
            throw new IllegalArgumentException("Relationships string is in wrong format. It should be: XXX-XXX[,...]");
        }
        Map<String, Map<String, RelationshipType>> relationships = new HashMap<>();
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
        return relationships;
    }

    /**
     * Starts the database. If there was no database in the confDir present, a new instance is created.
     */
    protected void startDB(String dbPath, String totalMemory, String cache) {
        if (dbPath == null || "".equals(dbPath) || totalMemory == null || "".equals(totalMemory) || cache == null || "".equals(cache)) {
            throw new IllegalArgumentException();
        }
        if (!totalMemory.matches("\\dG")) {
            throw new IllegalArgumentException("The total memory string is in wrong format. It should be: [0-9]G");
        }
        db = GraphDatabaseUtils.createNewGraphDatabaseService(dbPath, totalMemory, cache);
        ggo = GlobalGraphOperations.at(db);
    }

    /**
     * Method to put filter property on the relationship edges between group and descriptive nodes.
     */
    protected void createFilters(GraphDatabaseService db, GlobalGraphOperations ggo, Label groupLabel, Label descriptiveLabel, RelationshipType rt, List<String> filterProps, int limit) {
        if (db == null || ggo == null || groupLabel == null || descriptiveLabel == null || rt== null || filterProps == null || filterProps.isEmpty()) {
            throw new IllegalArgumentException();
        }
        int nodeCount = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            Iterator<Node> groups = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (groups.hasNext()) {
                Node group = groups.next();
                nodeCount++;
                Iterator<Relationship> rels = group.getRelationships(rt).iterator();
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    Node descriptive = rel.getOtherNode(group);
                    nodeCount++;
                    for (String f : filterProps) {
                        Object val = descriptive.getProperty(f, null);
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

        } finally {
            if (tx != null) {
                tx.success();
                tx.close();
            }
        }
    }

    /**
     * Method to create precomputated Nodes.
     *
     * @param limit The maximum number of fetched Nodes per transactions.
     */
    protected void precomputeOptimized(GraphDatabaseService db, GlobalGraphOperations ggo, Label groupLabel, Label descriptiveLabel, Label precomputed, Map<String, Map<String, RelationshipType>> relationships, List<String> filterProps, String group, String descriptive, int limit) {
        int count = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            RelationshipType rt = relationships.get(group).get(descriptive);
            List<Node> groupNodes = new ArrayList<>();
            Iterator<Node> nodes = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (nodes.hasNext()) {
                groupNodes.add(nodes.next());
            }
            for (Node node: groupNodes) {
                Mineotaur.LOGGER.info("Precomputing: " + (count++));
//                Node node = nodes.next();
                Iterator<Relationship> rels = node.getRelationships(rt).iterator();
                Map<String, List<Double>> features = new HashMap<>();
                Map<String, List<String>> innerfilter = new HashMap<>();
                while (rels.hasNext()) {
                    Relationship rel = rels.next();
                    Node other = rel.getOtherNode(node);
                    if (!other.hasLabel(descriptiveLabel)) {
                        continue;
                    }
                    if (!filterProps.isEmpty()) {
                        boolean filterSet = false;
                        for (String name : filterProps) {
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
                    for (String name : filterProps) {
                        List<String> filtValues = innerfilter.get(name);
                        if (filtValues == null) {
                            filtValues = new ArrayList<>();
                            innerfilter.put(name, filtValues);
                        }
                        Object val = rel.getProperty(name, null);
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
                    }
                    if (filterArr != null) {
                        pre.setProperty("filter", filterArr);
                    }
                    pre.setProperty(s, arr);
                    Set<String> uniqueFilters = valuesByFilter.keySet();
                    String[] uniqueArr = uniqueFilters.toArray(new String[uniqueFilters.size()]);
                    int filterSize = uniqueArr.length;

                    if (filterSize == 0) {
                        DescriptiveStatistics stat = new DescriptiveStatistics();
                        Node precomputedAgg = db.createNode(precomputed);
                        precomputedAgg.createRelationshipTo(node, DynamicRelationshipType.withName(s));

                        List<Double> vals = features.get(s);
                        for (Double d: vals) {
                            stat.addValue(d);
                        }
                        precomputedAgg.setProperty("Average", stat.getMean());
                        precomputedAgg.setProperty("Minimum", stat.getMin());
                        precomputedAgg.setProperty("Maximum", stat.getMax());
                        precomputedAgg.setProperty("Standard deviation", stat.getStandardDeviation());
                        precomputedAgg.setProperty("Median", stat.getPercentile(50));
                        precomputedAgg.setProperty("Count", stat.getN());
                        Mineotaur.LOGGER.info("Created aggregated node for property " + s);
                    }
                    else {
                        double maxSize = Math.pow(2, filterSize);
                        int set = 1;
                        while (set < maxSize) {

                            DescriptiveStatistics stat = new DescriptiveStatistics();
                            Node precomputedAgg = db.createNode(precomputed);
                            Relationship aggRel = precomputedAgg.createRelationshipTo(node, DynamicRelationshipType.withName(s));
                            List<String> actualFilters = new ArrayList<>();
                            for (int i = 0; i < filterSize; ++i) {
                                if (((set >> i) & 1) == 1) {
                                    List<Double> v = valuesByFilter.get(uniqueArr[i]);
                                    actualFilters.add(uniqueArr[i]);
                                    for (Double d : v) {
                                        if (!Double.isNaN(d)) {
                                            stat.addValue(d);
                                        }

                                    }
                                }
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
                    }

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
     * Method to store the names of the group objects in an external file.
     *
     * @param db The GraphDatabaseService instance.
     */
    protected List<String> generateGroupnameList(GraphDatabaseService db, Label groupLabel, String groupName) {
        Mineotaur.LOGGER.info(groupLabel.toString());
        Mineotaur.LOGGER.info(groupName);
        List<String> names = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
            Iterator<Node> groups = ggo.getAllNodesWithLabel(groupLabel).iterator();

            while (groups.hasNext()) {
                Node node = groups.next();

                String gname = (String) node.getProperty(groupName, null);
                Mineotaur.LOGGER.info(gname);
                if (gname != null) {
                    names.add(gname);
                }
                else {
                    throw new IllegalStateException("The property " + groupName + " does not exists for node.");
                }
            }
            Collections.sort(names);
            //FileUtils.saveList(confDir + "mineotaur.groupNames", names);
            tx.success();
        }
        return names;
    }

    /**
     * Method to store the filters in an external file.
     */
    protected List<String> generateFilterList(GraphDatabaseService db, GlobalGraphOperations ggo, Label descriptiveLabel, List<String> filterProps) {
        List<String> filterValues = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Iterator<Node> iterator = ggo.getAllNodesWithLabel(descriptiveLabel).iterator();
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
            //FileUtils.saveList(confDir + "mineotaur.filters", filterValues);
            tx.success();
        }
        return filterValues;
    }

    /**
     * Method to store the environment properties for the Mineotaur instance.
     *
     * @throws IOException
     */
    protected Properties generatePropertyFile(List<String> filterProps, String group, String groupName, String totalMemory, String dbPath,  String omero) {
        Properties mineotaurProperties = new Properties();
        boolean hasFilter = filterProps != null && !filterProps.isEmpty();
        mineotaurProperties.put("hasFilters", String.valueOf(hasFilter));
        if (hasFilter) {
            mineotaurProperties.put("filterName", filterProps.get(0));
        }

        /*if (filterProps == null || filterProps.isEmpty()) {
            mineotaurProperties.put("hasFilters", "false");
        } else {
            mineotaurProperties.put("hasFilters", "true");
            mineotaurProperties.put("filterName", filterProps.get(0));
            generateFilterList(db, descriptiveLabel, filterProps, confDir);
        }*/
        /*if (toPrecompute) {
            mineotaurProperties.put("query_relationship", precomputedLabel.name());
        } else {
            mineotaurProperties.put("query_relationship", relationships.get(groupLabel.name()).get(descriptiveLabel.name()));
        }*/
        mineotaurProperties.put("group", group);
        mineotaurProperties.put("groupName", groupName);
        mineotaurProperties.put("total_memory", totalMemory);
        mineotaurProperties.put("db_path", dbPath);
        mineotaurProperties.put("cache", "soft");
        if (omero != null) {
            mineotaurProperties.put("omero", omero);
        }
        return mineotaurProperties;
        //mineotaurProperties.store(new FileWriter(confDir + "mineotaur.properties"), "Mineotaur configuration properties");
    }


    /**
     * Method to collect all image ids for the grouping objects.
     * TODO: make distinct property
     *
     * @param rt
     */
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

    protected void storeConfigurationFiles() throws IOException {
        FileUtils.saveList(confDir + "mineotaur.features", generateFeatureNameList());
        FileUtils.saveList(confDir + "mineotaur.groupNames", generateGroupnameList(db, groupLabel, groupName));
        if (filterProps != null && !filterProps.isEmpty()) {
            FileUtils.saveList(confDir + "mineotaur.filters", generateFilterList(db, ggo, descriptiveLabel, filterProps));
        }
        FileUtils.saveList(confDir + "mineotaur.hitLabels", getHits());
        generatePropertyFile(filterProps, group, groupName, totalMemory, dbPath, omero).store(new FileWriter(confDir + "mineotaur.properties"), "Mineotaur configuration properties");
    }

    protected abstract List<String> getHits();

    /**
     * Method to store feature names in an external file.

     */
    protected abstract List<String> generateFeatureNameList();

    public abstract void generateDatabase();

    protected abstract void processData(GraphDatabaseService db);

    protected abstract void labelGenes();

    protected abstract void processMetadata();
}