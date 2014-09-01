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

import org.mineotaur.model.Metadata;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by balintantal on 30/06/2014.
 */
public class MineotaurGenerator {

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


    public MineotaurGenerator(String input, String prop) {
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
        gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, "5G");
        gdb.setConfig(GraphDatabaseSettings.cache_type, "strong");
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

    // TODO: check
    public void readInput() throws IOException {
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
            System.out.println(ids);
            System.out.println(labels);
            System.out.println(relationships);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);


                String[] terms = line.split("\t");
                Map<String, Map<String, Object>> data = new HashMap<>();

                Set<String> keySet = signatures.keySet();
                for (String key : keySet) {
                    List<Integer> indices = signatures.get(key);
                    Map<String, Object> props = new HashMap<>();
                    for (Integer i : indices) {
                        if (!terms[i].isEmpty()) {
                            if (numericData.contains(i)) {
                                props.put(header[i], Double.valueOf(terms[i]));
                            } else {
                                props.put(header[i], terms[i]);
                            }
                        }

                    }
                    data.put(key, props);
                }
                Map<String, Node> newNodes = new HashMap<>();
                for (String key : keySet) {
                    if (unique.contains(key)) {
                        newNodes.put(key, createNode(labels.get(key), data.get(key)));
                    } else {
                        newNodes.put(key, lookupNode(key, data.get(key)));
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

    public static void main(String[] args) {
        MineotaurGenerator gen = new MineotaurGenerator("input/chia_sample.txt", "input/mineotaur.input");
        gen.countNodes(DynamicLabel.label("EXPERIMENT"));
        gen.countNodes(DynamicLabel.label("GENE"));
        gen.countNodes(DynamicLabel.label("CELL"));

    }

}
