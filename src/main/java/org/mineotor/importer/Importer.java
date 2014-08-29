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

import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.util.*;

/**
 * Created by balintantal on 09/05/2014.
 */
public class Importer {

    private static ResourceBundle properties;
    private static Importer importer = null;

    private Importer() {

    }

    public static Importer getImporter() throws IOException {
        if (properties == null) {
            properties = new PropertyResourceBundle(new FileReader("conf/org.mineotor.properties"));
            importer = new Importer();
        }
        return importer;
    }

    private static Map<String, Label> nodeLabelMap = new HashMap<>();
    private static List<Label> nodeLabels = new ArrayList<>();
    private static RelationshipType[][] relationships;

    public void loadDataFromCSV(String file, GraphDatabaseService db) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split(",");
            //System.out.println(Arrays.deepToString(header));
            String[] nodeTypes = br.readLine().split(",");
            List<String> types = new ArrayList<>();
            for (String type : nodeTypes) {
                if (!types.contains(type) && !type.equals("FILTER")) {
                    types.add(type);
                    nodeLabelMap.put(type, DynamicLabel.label(type));
                }
            }
            int size = types.size();

            relationships = new RelationshipType[size][size];
            for (int i = 0; i < size; ++i) {
                for (int j = i + 1; j < size; ++j) {
                    relationships[i][j] = relationships[j][i] = DynamicRelationshipType.withName(types.get(i) + "_AND_" + types.get(j));
                }
            }
            //System.out.println(nodeLabels.toString());
            //System.out.println(Arrays.deepToString(relationships));
            //System.out.println(Arrays.deepToString(nodeTypes));
            String[] format = br.readLine().split(",");
            Map<String, String> ids = new HashMap<>();
            for (int i = 0; i < format.length; ++i) {
                if (format[i].equals("id")) {
                    ids.put(nodeTypes[i], header[i]);
                }
            }
            //System.out.println(Arrays.deepToString(format));
            Map<String, Object> strains = new HashMap<>();

            Transaction tx = null;
            try {
                tx = db.beginTx();
                /*Label strainLabel = nodeLabelMap.get("STRAIN");
                String strainID = ids.get("STRAIN");*/
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    System.out.println(count++);
                    if (count > 10000) {
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                        count=0;
                        //return;
                    }
                    String[] data = line.split(",");
                    Map<String, Map<String, Object>> nodes = new HashMap<>();
                    Map<String, Object> filters = new HashMap<>();
                    Map<String, Object> strain = new HashMap<>();
                    for (int i = 0; i < data.length; ++i) {
                        if (nodeTypes[i].equals("FILTER")) {
                            filters.put(header[i], data[i]);
                            continue;
                        }
                        if (nodeTypes[i].equals("STRAIN")) {
                            strain.put(header[i], data[i]);
                            continue;
                        }
                        Map<String, Object> nodeData = nodes.get(nodeTypes[i]);
                        if (nodeData == null) {
                            nodeData = new HashMap<>();
                        }
                        Object o;
                        if (format[i].equals("number")) {
                            o = Double.parseDouble(data[i]);
                            if (((Double)o).isNaN()) {
                                continue;
                            }
                        } else {
                            o = data[i];
                        }
                        nodeData.put(header[i], o);
                        nodes.put(nodeTypes[i], nodeData);
                        //System.out.println(nodes.toString());
                        //System.out.println(header[i] + " (" + nodeTypes[i] + ")" + " " + o);
                    }
                    Set<String> keys = strain.keySet();
                    //String strainID = ids.get("STRAIN");
                    Object[] keySet = (keys.toArray());
                    //Iterator<Node> strainIt = db.findNodesByLabelAndProperty(strainLabel, strainID, strain.get(strainID)).iterator();

                    Node strainNode=null;
                    /*if (strainIt.hasNext()) {
                        strainNode = strainIt.next();
                    }
                    else {
                        strainNode = db.createNode(nodeLabelMap.get("STRAIN"));
                        Set<String> strainProps = strain.keySet();
                        for (String prop: strainProps) {
                            strainNode.setProperty(prop, strain.get(prop));
                        }
                    }*/
                    if (keySet.length == 1) {
                        strainNode = (Node) strain.get(strain.get(keySet[0]));
                        if (strainNode == null) {
                            strainNode = db.createNode(nodeLabelMap.get("STRAIN"));
                            strainNode.setProperty((String) keySet[0], strain.get(keySet[0]));
                            strains.put((String) keySet[0], strainNode);
                        }
                    } else if (keySet.length == 2) {
                        Map<String, Node> map = (Map<String, Node>) strains.get(strain.get(keySet[0]));
                        if (map == null) {
                            map = new HashMap<>();
                        }
                        strainNode = (Node) map.get(strain.get(keySet[1]));
                        if (strainNode == null) {
                            strainNode = db.createNode(nodeLabelMap.get("STRAIN"));
                            for (int j = 0; j < keySet.length; ++j) {
                                strainNode.setProperty((String) keySet[j], strain.get(keySet[j]));
                            }
                        }
                        map.put((String) strain.get(keySet[1]), strainNode);
                        strains.put((String) strain.get(keySet[0]), map);
                    }
                    keys = nodes.keySet();
                    keySet = keys.toArray();
                    size = keySet.length;
                    int strainIdx = types.indexOf("STRAIN");
                    Set<String> filterKeys = filters.keySet();
                    for (int i = 0; i < size-1; i++) {
                        int idx1 = types.indexOf(keySet[i]);
                        Label label1 = nodeLabelMap.get(keySet[i]);

                        Map<String, Object> n1Data = nodes.get(keySet[i]);
                        Set<String> n1Keys = n1Data.keySet();
                        String id1 = ids.get(keySet[i]);
                        Node n1 = null;
                        if (id1 != null) {
                            String id1Value = (String) n1Data.get(id1);
                            Iterator<Node> it = db.findNodesByLabelAndProperty(label1, id1, id1Value).iterator();
                            if (it.hasNext()) {
                                n1 = it.next();
                            }
                        }
                        if (n1 == null) {
                            n1 = db.createNode(label1);
                        }
                        for (String n: n1Keys) {
                            if (!n1.hasProperty(n)) {
                                n1.setProperty(n, n1Data.get(n));
                            }

                            //System.out.println(n + " " + n1Data.get(n));
                        }
                        for (int j = i+1; j < size; ++j) {
                            int idx2 = types.indexOf(keySet[j]);
                            Label label2 = nodeLabelMap.get(keySet[j]);

                            Map<String, Object> n2Data = nodes.get(keySet[j]);
                            Set<String> n2Keys = n2Data.keySet();
                            String id2 = ids.get(keySet[j]);
                            Node n2 = null;
                            if (id1 != null) {
                                String id2Value = (String) n2Data.get(id2);
                                Iterator<Node> it = db.findNodesByLabelAndProperty(label2, id2, id2Value).iterator();
                                if (it.hasNext()) {
                                    n2 = it.next();
                                }
                            }
                            if (n2 == null) {
                                n2 = db.createNode(label2);
                            }
                            for (String n: n2Keys) {
                                if (!n2.hasProperty(n)) {
                                    n2.setProperty(n, n2Data.get(n));
                                }

                                //System.out.println(n + " " + n1Data.get(n));
                            }
                            /*Node n2 = db.createNode(nodeLabelMap.get(keySet[j]));
                            Map<String, Object> n2Data = nodes.get(keySet[j]);
                            Set<String> n2Keys = n2Data.keySet();
                            for (String n: n2Keys) {
                                n2.setProperty(n, n2Data.get(n));
                                //System.out.println(n + " " + n2Data.get(n));
                            }*/
                            n2.createRelationshipTo(n1, relationships[idx1][idx2]);
                            Relationship rel = n2.createRelationshipTo(strainNode, relationships[strainIdx][idx2]);
                           /* System.out.println(keySet[i] + " " + keySet[j] + " " + relationships[idx1][idx2].toString());
                            System.out.println(keySet[strainIdx] + " " + keySet[j] + " " + relationships[strainIdx][idx2].toString());*/

                            for (String filter: filterKeys) {
                                rel.setProperty(filter, filters.get(filter));
                                //System.out.println(filter + ": " + filters.get(filter));
                            }
                        }

                        Relationship rel = n1.createRelationshipTo(strainNode, relationships[strainIdx][idx1]);
                           /* System.out.println(keySet[i] + " " + keySet[j] + " " + relationships[idx1][idx2].toString());
                            System.out.println(keySet[strainIdx] + " " + keySet[j] + " " + relationships[strainIdx][idx2].toString());*/

                        for (String filter: filterKeys) {
                            rel.setProperty(filter, filters.get(filter));
                            //System.out.println(filter + ": " + filters.get(filter));
                        }
                    }
                }

                /*System.out.println(strains.toString());
                System.out.println(strains.size());*/
                tx.success();
            }
            finally {
                if (tx != null) {
                    tx.success();
                    tx.close();
                }
            }

        }
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

    public static void countNodes(Label label, GraphDatabaseService db) {
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

    public static void deleteAllWithLabels(GraphDatabaseService db, Label label) {
        int count = 0;
        Transaction tx = null;
        try {
            tx = db.beginTx();
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
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

    private static Map<String, Label> hitLabels = new HashMap<>();

    public void labelFromCSV(String file, GraphDatabaseService db) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split(",");
            if (header.length != 2 && header.length != 3) {
                System.out.println(header.length);
                throw new IllegalStateException("The number of columns must be 2 or 3.");
            }
            //ToDO MAP!!!
            int labelIdx = -1;
            int gfpIdx=-1, deletionIdx=-1;
            for (int i = 0; i < header.length; ++i) {
                if (header[i].equals("label")) {
                    labelIdx = i;
                }
                if (header[i].equals("GFP")) {
                    gfpIdx = i;
                }
                if (header[i].equals("Deletion")) {
                    deletionIdx = i;
                }
            }
            if (labelIdx==-1) {
                throw new IllegalStateException("No label provided in the input file.");
            }
            String line;
            Transaction tx = null;
            try {
                tx = db.beginTx();
                GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
                Iterator<Node> strains = ggo.getAllNodesWithLabel(DynamicLabel.label("STRAIN")).iterator();
                Map<String, Map<String, Node>> strainMap = new HashMap<>();
                int count = 0;
                while (strains.hasNext()) {
                    Node strainNode = strains.next();
                    //TODO store strain properties, select group by property
                    String deletion = (String) strainNode.getProperty("Deletion", null);
                    String gfp = (String) strainNode.getProperty("GFP", null);
                    Map<String, Node> map = strainMap.get(deletion);
                    if (map == null) {
                        map = new HashMap<>();
                    }
                    map.put(gfp, strainNode);

                    strainMap.put(deletion, map);
                }
                while ((line = br.readLine()) != null)  {
                    String[] data = line.split(",");
                    String labelName = data[labelIdx];
                    if (labelName.equals("WT")) {
                        continue;
                    }
                    Label label = hitLabels.get(labelName);
                    if (label == null) {
                        label = DynamicLabel.label(labelName);
                        hitLabels.put(labelName, label);
                    }
                    String deletion = data[gfpIdx];
                    String gfp = data[deletionIdx];
                    Map<String, Node> map = strainMap.get(deletion);
                    if (map!= null) {
                        Node node = map.get(gfp);
                        if (node == null) {
                            System.out.println(deletion + " " + gfp);
                            continue;
                        }
                        if (!node.hasLabel(label)) {
                            node.addLabel(label);
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
    }

    public static void collectProperties(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            GlobalGraphOperations ggo = GlobalGraphOperations.at(db);
            Iterator<Node> iterator = ggo.getAllNodesWithLabel(DynamicLabel.label("CELL")).iterator();
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
                //labels.add(node.getProperty("GFP","") + ", " + node.getProperty("Deletion",""));
            }
            //System.out.println(labels.toString());
            export("conf/org.mineotor.features", labels);
            tx.success();
        }
    }

    public static void export(String file, List<String> nodes) {
        try (PrintWriter pw = new PrintWriter(file)) {
            for (String node: nodes) {
                pw.println(node);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        /*try {
            ResourceBundle prop = new PropertyResourceBundle(new FileReader("conf" + File.separator + "org.mineotor.properties"));
            System.out.println(prop.getString("labels"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        /*GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("target/matrix");
        gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, "5G");
        gdb.setConfig(GraphDatabaseSettings.cache_type, "strong");
        GraphDatabaseService db = gdb.newGraphDatabase();
        registerShutdownHook(db);
        try {

            Importer importer = Importer.getImporter();
            Importer.collectProperties(db);
            //importer.loadDataFromCSV("matrix_input.csv", db);
            //importer.labelFromCSV("matrix_labels.csv", db);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        /*AggregationMode mode = AggregationMode.byName("Min");
        System.out.println(mode.toString());*/
        try (BufferedReader br = new BufferedReader(new FileReader("/Users/balintantal/Downloads/Kinome_Cell_features.txt")); PrintWriter pw = new PrintWriter("chia_sample.tsv")){


            //String line = br.readLine();
            String line;
            Map<String, Integer> cellsPerGene = new HashMap<>();
            //System.out.println(br.readLine());
            int count=0;
            while ((line = br.readLine()) != null && count <5000){

                String[] terms = line.split("\t");
                if (terms.length == 1) {
                    continue;
                }
                pw.println(line);
                count++;
                //System.out.println(Arrays.toString(terms));
                //System.out.println(terms.length);
                String gene = terms[2];
                Integer i = cellsPerGene.get(gene);
                if (i == null) {
                    i = 0;
                }
                cellsPerGene.put(gene, i+1);
            }
            /*System.out.println(count);
            System.out.println(cellsPerGene.toString());
            System.out.println(cellsPerGene.size());*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
