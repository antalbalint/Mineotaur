package org.mineotaur.provider;

import org.mineotaur.common.AggregationMode;
import org.mineotaur.common.FileUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by balintantal on 28/07/2015.
 */
public class DataDescriptor {

    private ResourceBundle properties;
    private String baseDir;
    private GraphDatabaseService db;
    private List<String> features;
    private String filterName;
    private Map<String, String> filters;
    private List<String> groupNames;
    private List<String> aggregationModes;
    private String groupName;
    private Label groupLabel;
    private String omeroURL;
    private List<String> hitNames;
    private Map<String, Label> labelsForHitNames;
    private Map<Label, String> hitNameForLabels;
    private Map<String, Node> groupByGroupName;

    public DataDescriptor(String baseDir, GraphDatabaseService db, ResourceBundle properties) {
        this.baseDir = baseDir;
        this.db = db;
        this.properties = properties;
        initProperties();
    }

    public DataDescriptor(String baseDir) throws IOException {
        this.baseDir = baseDir;
        this.properties = new PropertyResourceBundle(new FileReader(baseDir + "mineotaur.properties"));
        initProperties();
    }

    public List<String> getFeatures() {
        return features;
    }

    public String getFilterName() {
        return filterName;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public List<String> getAggregationModes() {
        return aggregationModes;
    }

    public String getGroupName() {
        return groupName;
    }

    public Label getGroupLabel() {
        return groupLabel;
    }

    public String getOmeroURL() {
        return omeroURL;
    }

    public List<String> getHitNames() {
        return hitNames;
    }

    public GraphDatabaseService getDatabase() {
        return db;
    }

    public Label getLabelForHitName(String name) {
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException();
        }
        Label label = labelsForHitNames.get(name);
        if (label == null) {
            throw new IllegalArgumentException();
        }
        return label;
    }

    public String getHitNameForLabel(Label label) {
        if (label == null) {
            throw new IllegalArgumentException();
        }
        String hitName = hitNameForLabels.get(label);
        if (hitName == null) {
            throw new IllegalArgumentException();
        }
        return hitName;
    }

    public Node getGroupObjectbyGroupName(String name) {
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException();
        }
        Node node = groupByGroupName.get(name);
        if (node == null) {
            throw new IllegalArgumentException();
        }
        return node;
    }

    protected void initProperties() {
        checkEntries(properties);
        if (db == null) {
            initDatabase();
        }
        features = FileUtils.processTextFile(baseDir + "mineotaur.features");
        if (properties.getString("hasFilter").equals("true")) {
            filters = loadFilters(baseDir + "mineotaur.filters");
            filterName = properties.getString("filterName");
        }
        loadGroupNames();
        loadHitLabels();
        initAggregationModes();
        preFetchGroupNames();
        loadIntegrations();
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


    private Map<String, String> loadFilters(String file) {
        List<String> filterList = FileUtils.processTextFile(file);
        Map<String, String> filters = new HashMap<>();
        for (String filter : filterList) {
            if (filter.contains("/")) {
                String[] terms = filter.split("/");
                filters.put(terms[0], terms[1]);
            } else {
                filters.put(filter, filter);
            }
        }
        return filters;
    }


    private void loadGroupNames() {
        String groupPath = baseDir + "mineotaur.groupNames";
        groupNames = FileUtils.processTextFile(groupPath);
        /*List<String> labels = FileUtils.processTextFile(groupPath);
        Map<String, Label> labelMap = new HashMap<>();
        for (String label: labels) {
            labelMap.put(label, DynamicLabel.label(label));
        }
        context.put("nodeLabels", labelMap);*/
        groupName = properties.getString("groupName");

        //groupLabel = DynamicLabel.label(properties.getString("group"));
    }

    private void loadHitLabels() {
        hitNames = FileUtils.processTextFile(baseDir + "mineotaur.hitLabels");
        hitNameForLabels = new HashMap<>();
        labelsForHitNames = new HashMap<>();
        for (String label : hitNames) {
            Label l = DynamicLabel.label(label);
            hitNameForLabels.put(l, label);
            labelsForHitNames.put(label, l);
        }
    }

    private void preFetchGroupNames() {
        groupByGroupName = new HashMap<>();
        try (Transaction tx = db.beginTx()) {
            for (String name : groupNames) {
                Iterator<Node> nodes = db.findNodesByLabelAndProperty(groupLabel, groupName, name).iterator();
                Node node = nodes.next();
                groupByGroupName.put(name, node);
            }
        }
    }

   /* private void loadQueryRelationship() {
        context.put("rel", DynamicRelationshipType.withName(properties.getString("query_relationship")));
    }*/

    private void loadIntegrations() {
        if (properties.containsKey("omero")) {
            omeroURL = properties.getString("omero");
        }
    }

    private void initAggregationModes() {
        aggregationModes = new ArrayList<>();
        AggregationMode[] modes = AggregationMode.values();
        for (AggregationMode am : modes) {
            aggregationModes.add(am.toString());
        }
    }
    private void initDatabase() {
        if (db ==  null) {
            db = GraphDatabaseUtils.createNewGraphDatabaseService(properties.getString("db_path"), properties.getString("total_memory"), properties.getString("cache"));
        }
        /*if (ggo == null) {
            ggo = GlobalGraphOperations.at(database);
        }*/
    }
}





