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

package org.mineotaur.provider;


import org.mineotaur.common.FileUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by balintantal on 28/05/2014.
 */
public class GenericEmbeddedGraphDatabaseProvider implements org.mineotaur.provider.GraphDatabaseProvider {

    /*static {
        initProperties();
        initDatabase();
    }*/

    private static ResourceBundle PROPERTIES;
    private static ResourceBundle EXTERNAL_TEXTS;
    private static GraphDatabaseService DATABASE;
    private static GlobalGraphOperations GGO;
    private static Map<String, Object> CONTEXT = new HashMap<>();
    private static Map<String, String> TEXTS = new HashMap<>();
    private static List<String> AGGREGATION_MODES;
    private static Map<String, Node> STRAINS_BY_GENE_NAMES;
    private static List<String> GENE_NAMES;
    private static List<String> hitLabels;
    private Label groupLabel;
    private String groupName;

    private void initProperties() {
            try {
                PROPERTIES = new PropertyResourceBundle(new FileReader("conf" + File.separator + "mineotaur.properties"));
                EXTERNAL_TEXTS = new PropertyResourceBundle(new FileReader("conf" + File.separator + "mineotaur.strings"));
                String baseDir = PROPERTIES.getString("base_dir");
                CONTEXT.put("features", FileUtil.processTextFile("conf" + File.separator + "mineotaur.features"));
                if (PROPERTIES.getString("hasFilters").equals("false")) {
                    CONTEXT.put("hasFilter", false);
                }
                else {
                    CONTEXT.put("hasFilter", true);
                    CONTEXT.put("filters", FileUtil.processTextFile(baseDir + File.separator + PROPERTIES.getString("filters_path")));
                }
                GENE_NAMES = FileUtil.processTextFile(baseDir + File.separator + PROPERTIES.getString("strain_names_path"));
                CONTEXT.put("geneNames", GENE_NAMES);
                List<String> labels = FileUtil.processTextFile(baseDir + File.separator + PROPERTIES.getString("node_labels_path"));
                Map<String, Label> labelMap = new HashMap<>();
                for (String label: labels) {
                    labelMap.put(label, DynamicLabel.label(label));
                }
                CONTEXT.put("nodeLabels", labelMap);
                hitLabels = FileUtil.processTextFile(baseDir + File.separator + PROPERTIES.getString("hit_labels_path"));
                CONTEXT.put("hitNames", hitLabels);
                Map<String, Label> labelMap2 = new HashMap<>();
                Map<Label, String> labelMap3 = new HashMap<>();
                for (String label: hitLabels) {
                    Label l = DynamicLabel.label(label);
                    labelMap2.put(label, l);
                    labelMap3.put(l, label);
                }
                CONTEXT.put("hitLabels", labelMap2);
                CONTEXT.put("hitsByLabel", labelMap3);
                CONTEXT.put("rel", DynamicRelationshipType.withName(PROPERTIES.getString("query_relationship")));
                groupLabel = DynamicLabel.label(PROPERTIES.getString("group"));
                groupName = PROPERTIES.getString("group_name");
                String[] aggModes = {"avg", "max", "min", "median", "stdev", "number"};
                for (String mode: aggModes) {
                    TEXTS.put(mode, EXTERNAL_TEXTS.getString(mode));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public List<String> getAggregationModes() {
        if (AGGREGATION_MODES == null) {
            AGGREGATION_MODES = new ArrayList<>();
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("avg"));
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("max"));
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("min"));
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("median"));
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("stdev"));
            AGGREGATION_MODES.add(EXTERNAL_TEXTS.getString("number"));
        }
        return AGGREGATION_MODES;
    }

    private void initDatabase() {
        if (PROPERTIES == null) {
            initProperties();
        }
        if (DATABASE ==  null) {
            GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(PROPERTIES.getString("db_path"));
            //gdb.setConfig( GraphDatabaseSettings.read_only, "true" );
            gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, PROPERTIES.getString("total_memory"));
            gdb.setConfig(GraphDatabaseSettings.cache_type, PROPERTIES.getString("cache"));
            //gdb.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true");
            DATABASE = gdb.newGraphDatabase();

        }
        if (GGO == null) {
            GGO = GlobalGraphOperations.at(DATABASE);
        }
        /*try (Transaction tx = DATABASE.beginTx()) {
            Iterator<Node> strains = GGO.getAllNodesWithLabel(((Map<String, Label>)CONTEXT.get("nodeLabels")).get("STRAIN")).iterator();
            Map<String, Map<String, Node>> strainMap = new HashMap<>();
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
                CONTEXT.put("strains", strainMap);
            }
            tx.success();
        }*/

    }

    @Override
    public GraphDatabaseService getDatabaseService() {
        if (DATABASE == null) {
            initDatabase();
        }
        return DATABASE;
    }

    @Override
    public GlobalGraphOperations getGlobalGraphOperations() {
        if (DATABASE == null) {
            initDatabase();
        }
        return GGO;
    }

    @Override
    public List<String> getGeneNames() {
        return GENE_NAMES;
    }

    @Override
    public List<String> getCellProperties() {
        return null;
    }


    @Override
    public Map<String, String> getTimePoints() {
        return null;
    }

    @Override
    public Map<String, Label> getHitLabels() {
        return null;
    }

    @Override
    public Map<Label, String> getHitNames() {

        return null;
    }

    @Override
    public Map<String, Node> getStrainsByName() {
        try (Transaction tx = DATABASE.beginTx()) {
            Iterator<Node> nodes = GGO.getAllNodesWithLabel(groupLabel).iterator();
            /*while (nodes.hasNext()) {
                System.out.println(nodes.next().getProperty(groupName));
            }*/
            if (STRAINS_BY_GENE_NAMES == null) {
                List<String> geneNames = getGeneNames();
                STRAINS_BY_GENE_NAMES = new HashMap<>();
                for (String name: geneNames) {
                    STRAINS_BY_GENE_NAMES.put(name, findStrainByGenename(name));
                }
            }
            tx.success();
        }

        return STRAINS_BY_GENE_NAMES;
    }

    @Override
    public Node findStrainByGenename(String name) {

        Iterator<Node> nodes = DATABASE.findNodesByLabelAndProperty(groupLabel, groupName, name).iterator();
        Node node = nodes.next();
        return node;
    }

    @Override
    public Map<String, Object> getContext() {
        return CONTEXT;
    }

    @Override
    public Map<String, String> getTexts() {
        return GenericEmbeddedGraphDatabaseProvider.TEXTS;
    }


}
