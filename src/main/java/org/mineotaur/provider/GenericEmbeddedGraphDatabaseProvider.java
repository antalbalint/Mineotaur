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


import org.mineotaur.application.Mineotaur;
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
 * An implementation of GraphDatabaseProvider interface.
 * Initializes a context object and starts the database for the requested Mineotaur database.
 *
 * Created by balintantal on 28/05/2014.
 */
public class GenericEmbeddedGraphDatabaseProvider implements GraphDatabaseProvider {
    public static final String AVERAGE = "Average";
    public static final String MAXIMUM = "Maximum";
    public static final String MINIMUM = "Minimum";
    public static final String MEDIAN = "Median";
    public static final String STANDARD_DEVIATION = "Standard deviation";
    public static final String COUNT = "Count";

    private static ResourceBundle PROPERTIES;
    private static GraphDatabaseService DATABASE;
    private static GlobalGraphOperations GGO;
    private static Map<String, Object> CONTEXT = new HashMap<>();
    private static List<String> AGGREGATION_MODES;
    private static List<String> GROUP_NAMES;
    private static List<String> HIT_LABELS;
    private Label groupLabel;
    private String groupName;

    /**
     * Method to load initial properties from.
     */
    protected void initProperties() {
            try {
                String baseDir = Mineotaur.name + File.separator + "conf" + File.separator;
                PROPERTIES = new PropertyResourceBundle(new FileReader(baseDir + "mineotaur.properties"));
                CONTEXT.put("features", FileUtil.processTextFile(baseDir + "mineotaur.features"));
                if (PROPERTIES.getString("hasFilters").equals("false")) {
                    CONTEXT.put("hasFilter", false);
                }
                else {
                    CONTEXT.put("hasFilter", true);
                    List<String> filterList = FileUtil.processTextFile(baseDir + "mineotaur.filters");
                    Map<String, String> filters = new HashMap<>();
                    for (String filter: filterList) {
                        if (filter.contains("/")) {
                            String[] terms = filter.split("/");
                            filters.put(terms[0], terms[1]);
                        }
                        else {
                            filters.put(filter, filter);
                        }
                    }
                    CONTEXT.put("filters", filters);
                    CONTEXT.put("filterName", PROPERTIES.getString("filterName"));
                }
                String groupPath = baseDir + "mineotaur.groupNames";
                GROUP_NAMES = FileUtil.processTextFile(groupPath);
                CONTEXT.put("groupNames", GROUP_NAMES);
                List<String> labels = FileUtil.processTextFile(groupPath);
                Map<String, Label> labelMap = new HashMap<>();
                for (String label: labels) {
                    labelMap.put(label, DynamicLabel.label(label));
                }
                CONTEXT.put("nodeLabels", labelMap);
                HIT_LABELS = FileUtil.processTextFile(baseDir + "mineotaur.hitLabels");
                CONTEXT.put("hitNames", HIT_LABELS);
                Map<String, Label> labelMap2 = new HashMap<>();
                Map<Label, String> labelMap3 = new HashMap<>();
                for (String label: HIT_LABELS) {
                    Label l = DynamicLabel.label(label);
                    labelMap2.put(label, l);
                    labelMap3.put(l, label);
                }
                CONTEXT.put("hitLabels", labelMap2);
                CONTEXT.put("hitsByLabel", labelMap3);
                CONTEXT.put("rel", DynamicRelationshipType.withName(PROPERTIES.getString("query_relationship")));
                CONTEXT.put("aggValues", getAggregationModes());
                groupName = PROPERTIES.getString("groupName");
                CONTEXT.put("groupName", groupName);
                groupLabel = DynamicLabel.label(PROPERTIES.getString("group"));
                CONTEXT.put("groupLabel", groupLabel);
                GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(PROPERTIES.getString("db_path"));
//                gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, PROPERTIES.getString("total_memory"));
                gdb.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true");
                gdb.setConfig(GraphDatabaseSettings.cache_type, PROPERTIES.getString("cache"));
                DATABASE = gdb.newGraphDatabase();
                GGO = GlobalGraphOperations.at(DATABASE);
                Map<String, Node> groupByGroupName = new HashMap<>();
                try (Transaction tx = DATABASE.beginTx()) {
                        for (String name: GROUP_NAMES) {
                            Iterator<Node> nodes = DATABASE.findNodes(groupLabel, groupName, name);
                            Node node = nodes.next();
                            if (nodes.hasNext()) {
                                throw new IllegalStateException("There are more group objects in the database with the same name.");
                            }
                            groupByGroupName.put(name, node);
                        }
                    tx.success();
                }
                CONTEXT.put("groupByGroupName", groupByGroupName);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    /**
     * Method to retrieve the list of aggregation modes.
     * @return A list containing the aggregation modes.
     */
    private List<String> getAggregationModes() {
        if (AGGREGATION_MODES == null) {
            AGGREGATION_MODES = new ArrayList<>();
            AGGREGATION_MODES.add(AVERAGE);
            AGGREGATION_MODES.add(MAXIMUM);
            AGGREGATION_MODES.add(MINIMUM);
            AGGREGATION_MODES.add(MEDIAN);
            AGGREGATION_MODES.add(STANDARD_DEVIATION);
            AGGREGATION_MODES.add(COUNT);
        }
        return AGGREGATION_MODES;
    }

    /**
     * Method to start the database.
     */
    protected void initDatabase() {
        if (PROPERTIES == null) {
            initProperties();
        }
        if (DATABASE ==  null) {
            GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(PROPERTIES.getString("db_path"));
//            gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, PROPERTIES.getString("total_memory"));
            gdb.setConfig(GraphDatabaseSettings.cache_type, PROPERTIES.getString("cache"));
            DATABASE = gdb.newGraphDatabase();

        }
        if (GGO == null) {
            GGO = GlobalGraphOperations.at(DATABASE);
        }

    }

    /**
     * Method to access the database instance started.
     * @return The graph database service instance.
     */
    @Override
    public GraphDatabaseService getDatabaseService() {
        if (DATABASE == null) {
            initDatabase();
        }
        return DATABASE;
    }

    /**
     * Method to access the GlobalGraphOperations instance.
     * @return the GlobalGraphOperations instance.
     */
    @Override
    public GlobalGraphOperations getGlobalGraphOperations() {
        if (DATABASE == null) {
            initDatabase();
        }
        return GGO;
    }

    /**
     * Method to access the context variables.
     * @return A map containing all the context vaiables.
     */
    @Override
    public Map<String, Object> getContext() {
        return CONTEXT;
    }

}
