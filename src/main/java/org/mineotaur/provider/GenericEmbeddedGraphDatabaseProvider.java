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
import org.mineotaur.common.AggregationMode;
import org.mineotaur.common.FileUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;
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
    /*public static final String AVERAGE = "Average";
    public static final String MAXIMUM = "Maximum";
    public static final String MINIMUM = "Minimum";
    public static final String MEDIAN = "Median";
    public static final String STANDARD_DEVIATION = "Standard deviation";
    public static final String COUNT = "Count";*/
    protected String baseDir;
    private ResourceBundle properties;
    private GraphDatabaseService database;
    //private GlobalGraphOperations ggo;
    private Map<String, Object> context = new HashMap<>();
    private List<String> aggregationModes;
    private List<String> groupNames;
    private Label groupLabel;
    private String groupName;

    public GenericEmbeddedGraphDatabaseProvider() {
        initProperties();
    }

    private void loadFeatures() {
        context.put("features", FileUtils.processTextFile(baseDir + "mineotaur.features"));
    }

    private void loadFilters() {
        if (properties == null) {
            throw new IllegalStateException("Property file has not been loaded yet.");
        }
        if (properties.getString("hasFilters").equals("false")) {
            context.put("hasFilter", false);
            context.put("filters", new ArrayList());
        }
        else {
            context.put("hasFilter", true);
            List<String> filterList = FileUtils.processTextFile(baseDir + "mineotaur.filters");
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
            context.put("filters", filters);
            context.put("filterName", properties.getString("filterName"));
        }
    }

    private void loadGroupNames() {
        if (properties == null) {
            throw new IllegalStateException("Property file has not been loaded yet.");
        }
        String groupPath = baseDir + "mineotaur.groupNames";
        groupNames = FileUtils.processTextFile(groupPath);
        context.put("groupNames", groupNames);
        List<String> labels = FileUtils.processTextFile(groupPath);
        Map<String, Label> labelMap = new HashMap<>();
        for (String label: labels) {
            labelMap.put(label, DynamicLabel.label(label));
        }
        context.put("nodeLabels", labelMap);
        groupName = properties.getString("groupName");
        context.put("groupName", groupName);
        groupLabel = DynamicLabel.label(properties.getString("group"));
        context.put("groupLabel", groupLabel);
    }

    private void loadHitLabels() {
        if (properties == null) {
            throw new IllegalStateException("Property file has not been loaded yet.");
        }
        List<String> hitLabels = FileUtils.processTextFile(baseDir + "mineotaur.hitLabels");
        context.put("hitNames", hitLabels);
        Map<String, Label> labelMap2 = new HashMap<>();
        Map<Label, String> labelMap3 = new HashMap<>();
        for (String label: hitLabels) {
            Label l = DynamicLabel.label(label);
            labelMap2.put(label, l);
            labelMap3.put(l, label);
        }
        context.put("hitLabels", labelMap2);
        context.put("hitsByLabel", labelMap3);
    }

    private void preFecthGroupNames() {
        if (properties == null) {
            throw new IllegalStateException("Property file has not been loaded yet.");
        }
        if (database == null) {
            throw new IllegalStateException("The database has not been started yet.");

        }
        Map<String, Node> groupByGroupName = new HashMap<>();
        try (Transaction tx = database.beginTx()) {
            for (String name: groupNames) {
                Iterator<Node> nodes = database.findNodesByLabelAndProperty(groupLabel, groupName, name).iterator();
                Node node = nodes.next();
                groupByGroupName.put(name, node);
            }
            tx.success();
        }
        context.put("groupByGroupName", groupByGroupName);
    }

   /* private void loadQueryRelationship() {
        context.put("rel", DynamicRelationshipType.withName(properties.getString("query_relationship")));
    }*/

    private void loadIntegrations() {
        if (properties.containsKey("omero")) {
            context.put("omeroURL", properties.getString("omero"));
        }
        else {
            context.put("omeroURL", null);
        }
    }

    private void loadAggregationModes() {
        if (aggregationModes == null) {
            aggregationModes = new ArrayList<>();
            AggregationMode[] modes = AggregationMode.values();
            for (AggregationMode am: modes) {
                aggregationModes.add(am.toString());
            }
        }
        context.put("aggValues", aggregationModes);
    }

    /**
     * Method to load initial properties from.
     */
    protected void initProperties() {
            try {
                if (baseDir == null) {
                    baseDir = Mineotaur.name + File.separator + "conf" + File.separator;
                }
                properties = new PropertyResourceBundle(new FileReader(baseDir + "mineotaur.properties"));
                checkEntries(properties);
                loadFeatures();
                loadFilters();
                loadGroupNames();
                loadHitLabels();
                //loadQueryRelationship();
                loadAggregationModes();
                initDatabase();
                preFecthGroupNames();
                addMenus();
                loadIntegrations();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void checkEntries(ResourceBundle properties) {

        if (properties == null) {
            throw new IllegalArgumentException();
        }
        String[] requiredTerms = {"db_path", "total_memory", "cache", "hasFilters", "group", "groupName",};
        for (String term : requiredTerms) {
            if (!properties.containsKey(term)) {
                throw new IllegalArgumentException("Required property missing: " + term);
            }
        }
        if (properties.getString("hasFilters").equals("true")) {
            if (!properties.containsKey("filterName")) {
                throw new IllegalArgumentException("If hasFilter is true filterName must be set, as well.");
            }
        }
    }

    /**
     * Method to start the database.
     */
    private void initDatabase() {
        if (database ==  null) {
            database = GraphDatabaseUtils.createNewGraphDatabaseService(properties.getString("db_path"), properties.getString("total_memory"), properties.getString("cache"));
        }
        /*if (ggo == null) {
            ggo = GlobalGraphOperations.at(database);
        }*/
    }

    /**
     * Method to access the database instance started.
     * @return The graph database service instance.
     */
    @Override
    public GraphDatabaseService getDatabaseService() {
        if (database == null) {
            initDatabase();
        }
        return database;
    }

    private void addMenus() {
        Map<String, String> menu1 = new HashMap<>();
        menu1.put("cellwiseScatter", "Cell-wise scatter plot");
        menu1.put("cellwiseHistogramX", "Histogram (X axis)");
        menu1.put("cellwiseHistogramY", "Histogram (Y axis)");
        menu1.put("cellwiseKDEX", "Kernel Density Estimation (X axis)");
        menu1.put("cellwiseKDEY", "Kernel Density Estimation (Y axis)");
        context.put("menu1", menu1);
        Map<String, String> menu2 = new HashMap<>();
        menu2.put("analyze", "Analyze");
        context.put("menu2", menu2);
    }

    /**
     * Method to access the GlobalGraphOperations instance.
     * @return the GlobalGraphOperations instance.
     */
    /*@Override
    public GlobalGraphOperations getGlobalGraphOperations() {
        if (database == null) {
            initDatabase();
        }
        return ggo;
    }*/

    /**
     * Method to access the context variables.
     * @return A map containing all the context vaiables.
     */
    @Override
    public Map<String, Object> getContext() {
        return context;
    }

}
