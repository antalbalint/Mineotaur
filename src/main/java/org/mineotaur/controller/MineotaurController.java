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

package org.mineotaur.controller;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.provider.GenericEmbeddedGraphDatabaseProvider;
import org.mineotaur.provider.GraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Controller object for Mineotaur.
 * Maps HTTP requests to appropriate actions.
 */
@Controller
public class MineotaurController {


    private GraphDatabaseProvider provider;
    private GraphDatabaseService db;
    private Map<String, Node> strainMap = null;
    private Map<String, Object> context;
    private List<String> aggValues;
    private Map<String, Label> hitsByName = null;
    private Map<Label, String> hitsByLabel = null;
    private List<String> hitNames = null;
    private List<Label> allHitLabels = null;
    private RelationshipType rt = null;
    private boolean hasFilter;
    private Map<String, String> filters;
    private Map<String, String> menu1;
    private Map<String, String> menu2;
    private List<String> features;
    private List<String> groupNames;
    private Label groupLabel;
    private String groupName;
    private String filterName;

    /**
     * Method to initalize the controller. The database provider is injected by Spring.
     * @param provider The database provider.
     */
    @Autowired
    public void setProvider (GraphDatabaseProvider provider) {
        this.provider = provider;
        this.db = provider.getDatabaseService();
        this.context = provider.getContext();
        this.aggValues = (List<String>) context.get("aggValues");
        this.hitsByName = (Map<String, Label>) context.get("hitLabels");
        this.hitsByLabel = (Map<Label, String>) context.get("hitsByLabel");
        this.hitNames = (List<String>) context.get("hitNames");
        this.allHitLabels = new ArrayList<>();
        allHitLabels.addAll(hitsByLabel.keySet());
        this.strainMap = (Map<String, Node>) context.get("groupByGroupName");
        this.rt = (RelationshipType) context.get("rel");
        menu1 = new HashMap<>();
        menu1.put("cellwiseScatter", "Cell-wise scatter plot");
        menu1.put("cellwiseHistogramX", "Histogram (X axis)");
        menu1.put("cellwiseHistogramY", "Histogram (Y axis)");
        menu1.put("cellwiseKDEX", "Kernel Density Estimation (X axis)");
        menu1.put("cellwiseKDEY", "Kernel Density Estimation (Y axis)");
        menu2 = new HashMap<>();
        menu2.put("analyze", "Analyze");
        features = (List<String>) context.get("features");
        filters = (Map<String, String>) context.get("filters");
        groupNames = (List<String>) context.get("groupNames");
        hasFilter = (boolean) context.get("hasFilter");
        groupLabel = (Label) context.get("groupLabel");
        groupName = (String) context.get("groupName");
        filterName = (String) context.get("filterName");
    }

    private void processFilters() {

    }

    /**
     * Method for mapping the starting page.
     * @param model The model.
     * @return The requested page.
     */
    @RequestMapping("/")
    public String start(Model model) {
        return "index";
    }


    /**
     * Method for providing a JSON response to a genewise scatter plot request.
     * @param model The model.
     * @param prop1 First property.
     * @param prop2 Second property.
     * @param aggProp1  First aggregation mode.
     * @param aggProp2 Second aggregation mode.
     * @param mapValuesProp1 Checked filters for the first property.
     * @param hitCheckbox The selected labels.
     * @param mapValuesProp2 Checked filters for the first property.
     * @return A JSON object containing the data points.
     */
    @RequestMapping("/genewiseScatterJSON")
    public @ResponseBody List<Map<String, Object>> getScatterPlotDataGeneJSONSeparate(Model model,
                                                                                      @RequestParam String[] geneList,
                                                                                      @RequestParam String prop1,
                                                                                      @RequestParam String prop2,
                                                                                      @RequestParam String aggProp1,
                                                                                      @RequestParam String aggProp2,
                                                                                      @RequestParam(required = false) List<String> mapValuesProp1,
                                                                                      @RequestParam String[] hitCheckbox,
                                                                                      @RequestParam(required = false) List<String> mapValuesProp2) {

        List<Label> hitLabels = manageHitCheckbox(hitCheckbox);
        //Mineotaur.LOGGER.info(String.valueOf(model.containsAttribute("mapValuesProp1")));
        Map<String, Object> map = model.asMap();
        // TODO: fix it!
        List<Map<String, Object>> dataPoints = getGenewiseScatterplotData(geneList, prop1, prop2, aggProp1, (List<String>) map.get("mapValuesProp1"), aggProp2, (List<String>) map.get("mapValuesProp2"), hitLabels);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", prop1);
        model.addAttribute("prop2", prop2);
        model.addAttribute("selectedGenes", geneList);
        return dataPoints;
    }


    /**
     * Method for providing a JSON response to a genewise distribution plot request.
     * @param model The model.
     * @param geneListDist The gene list.
     * @param propGWDist The selected feature.
     * @param aggGWDist The aggregation mode.
     * @param mapValuesGWDist The selected filters.
     * @param hitCheckboxGWDist The selected hits.
     * @return A JSON object containing the data points.
     */
    @RequestMapping("/genewiseDistributionJSON")
    public @ResponseBody List<Map<String, Object>> getDistributionDataGenewiseJSON(Model model,
                                                                                   @RequestParam String[] geneListDist,
                                                                                   @RequestParam String propGWDist,
                                                                                   @RequestParam String aggGWDist,
                                                                                   @RequestParam(required = false) List<String> mapValuesGWDist,
                                                                                   @RequestParam String[] hitCheckboxGWDist) {
        List<Label> hitLabels = manageHitCheckbox(hitCheckboxGWDist);
        //TODO: fix it!
        List<Map<String, Object>> dataPoints = getGenewiseDistributionData(geneListDist, propGWDist, aggGWDist, mapValuesGWDist, hitLabels);
        model.addAttribute("prop1", propGWDist);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("selectedGenes", geneListDist);
        return dataPoints;
    }

    /**
     * Method for providing a JSON response to a cellwise scatter plot request.
     * @param model The model.
     * @param cellwiseProp1 First property.
     * @param cellwiseProp2 Second property.
     * @param mapValuesCellwiseProp1 Checked filters for the first property.
     * @param mapValuesCellwiseProp2 Checked filters for the second property.
     * @param geneCWProp1 The selected gene.
     * @return A JSON object containing the data points.
     */
    @RequestMapping("/cellwiseScatterJSON")
    public @ResponseBody List<Map<String, Object>> cellwiseScatterJSON(Model model,
                                                                       @RequestParam String cellwiseProp1,
                                                                       @RequestParam String cellwiseProp2,
                                                                       @RequestParam(required = false) List<String> mapValuesCellwiseProp1,
                                                                       @RequestParam(required = false) List<String> mapValuesCellwiseProp2,
                                                                       @RequestParam String geneCWProp1) {

        Node strain1 = strainMap.get(geneCWProp1);
        Mineotaur.LOGGER.info(mapValuesCellwiseProp1.toString());
        List<Map<String, Object>> dataPoints = getCellwiseScatterplotData(cellwiseProp1, cellwiseProp2, mapValuesCellwiseProp1, mapValuesCellwiseProp2, strain1, geneCWProp1);
        model.addAttribute("genename", geneCWProp1);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", cellwiseProp1);
        model.addAttribute("prop2", cellwiseProp2);
        return dataPoints;
    }

    /**
     * Method for providing a JSON response to a cellwise distribution plot request.
     * @param model The model.
     * @param propCWDist The selected property.
     * @param geneCWDist The selected gene.
     * @param mapValuesCWDist Checked filters for the property.
     * @return A JSON object containing the data points.
     */
    @RequestMapping("/cellwiseDistributionJSON")
    public @ResponseBody List<Map<String, Object>> getDistributionDataCellwiseJSON(Model model,
                                                                                   @RequestParam String propCWDist,
                                                                                   @RequestParam String geneCWDist,
                                                                                   @RequestParam(required = false) List<String> mapValuesCWDist) {
        Node strain = strainMap.get(geneCWDist);
        List<Map<String, Object>> dataPoints = getCellwiseDistributionData(propCWDist, (List<String>) model.asMap().get("mapValuesCWDist"), strain);
        model.addAttribute("genename", geneCWDist);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", propCWDist);
        return dataPoints;
    }

    /**
     * Method to query data for a cellwise scatterplot.
     * @param prop1 First property.
     * @param prop2 Second property.
     * @param mapValuesProp1 Checked filters for the first property.
     * @param mapValuesProp2 Checked filters for the second property.
     * @param strain Node for the selected gene.
     * @param genename The selected gene.
     * @return A list of datapoints.
     */
    @Deprecated
    protected List<Map<String, Object>> getHitsDecoupled(String prop1, String prop2, List<String> mapValuesProp1, List<String> mapValuesProp2, Node strain, String genename) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
            Object x, y;
            List<double[]> xList = new ArrayList<>(), yList = new ArrayList<>();
            while (smds.hasNext()) {
                Relationship rel = smds.next();
                boolean in1, in2;
                if (hasFilter) {
                    /*String stage = (String) rel.getProperty("stage", null);
                    in1 = mapValuesProp1.contains(stage);
                    in2 = mapValuesProp2.contains(stage);
                    if (stage == null || (!in1 && !in2))  {
                        continue;
                    }*/
                    in1 = in2 = true;
                }
                else {
                    in1 = in2 = true;
                }

                Node smd = rel.getOtherNode(strain);
                if (in1) {
                    x = smd.getProperty(prop1, null);
                    if (x != null) {
                        xList.add((double[]) x);
                    }
                }
                if (in2) {
                    y = smd.getProperty(prop2, null);
                    if (y != null) {
                        yList.add((double[]) y);
                    }
                }
            }
            int idx = 0;
            int size = Math.min(xList.size(), yList.size());
            while (idx < size) {
                double[] xArr = xList.get(idx);
                double[] yArr = yList.get(idx);
                idx++;
                int length = Math.min(xArr.length, yArr.length);

                for (int i = 0; i < length; ++i) {
                    Map map = new HashMap<>();
                    map.put("x", xArr[i]);
                    map.put("y", yArr[i]);
                    map.put("logX", Math.log(xArr[i]));
                    map.put("logY", Math.log(yArr[i]));
                    map.put("name", genename);
                    map.put("labels", actualLabels);
                    dataPoints.add(map);
                }
            }

            tx.success();
        }
        return dataPoints;
    }

    protected double[] getArrayData(Node strain, String prop1, String genename) {
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1)).iterator();
        if (!prop1Iterator.hasNext()) {
            throw new IllegalStateException("There is no node stored for strain " + genename + " for property " + prop1);
        }
        Relationship rel = prop1Iterator.next();
        if (prop1Iterator.hasNext()) {
            throw new IllegalStateException("There are multiple nodes stored for strain " + genename + " for property " + prop1);
        }
        Node node = rel.getOtherNode(strain);
        double[] prop1Arr = (double[]) node.getProperty(prop1);
        return prop1Arr;
    }

    protected double[] getFilteredArrayData(Node strain, String prop1, String genename, List<String> filter) {
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1+"_ARRAY")).iterator();
        Mineotaur.LOGGER.info(prop1 + ": " + prop1Iterator.hasNext());
        Node node = null;
        while (prop1Iterator.hasNext()) {
            Relationship rel = prop1Iterator.next();
            /*if ((Boolean)rel.getProperty("aggregated",false)) {
                continue;
            }*/
            node = rel.getOtherNode(strain);
        }
        if (node == null) {
            throw new IllegalStateException("There is no node stored for strain " + genename + " for property " + prop1);
        }
        /*Relationship rel = prop1Iterator.next();
        if (prop1Iterator.hasNext()) {
            throw new IllegalStateException("There are multiple nodes stored for strain " + genename + " for property " + prop1);
        }
        Node node = rel.getOtherNode(strain);*/
        Iterator<String> props = node.getPropertyKeys().iterator();
        Mineotaur.LOGGER.info(String.valueOf(props.hasNext()));
        while (props.hasNext()) {
            Mineotaur.LOGGER.info(props.next());
        }
        double[] prop1Arr = (double[]) node.getProperty(prop1,null);
        if (prop1Arr == null) {
            return new double[]{};
        }
        String[] filterArr = (String[]) node.getProperty("filter");

//        Mineotaur.LOGGER.info(filter.toString());
        List<Double> results = new ArrayList<>();
        for (int i = 0; i < prop1Arr.length; ++i) {
            if (filter.contains(filterArr[i])) {
                results.add(prop1Arr[i]);
            }
        }
        double[] resultArr = new double[results.size()];
        for (int i = 0; i < resultArr.length; ++i) {
            resultArr[i] = results.remove(0);
        }
        return resultArr;
    }

    protected Double getFilteredAggregatedData(Node strain, String prop1, String genename, String aggregate, List<String> filter) {
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1)).iterator();
        Node node = null;
        while (prop1Iterator.hasNext()) {
            Relationship rel = prop1Iterator.next();
            /*if (!(Boolean)rel.getProperty("aggregated",false)) {
                continue;
            }*/

            String[] filterArr = (String[]) rel.getProperty("filter");
            //Mineotaur.LOGGER.info(Arrays.toString(filterArr));
//            Mineotaur.LOGGER.info(Boolean.toString((Boolean)rel.getProperty("aggregated")));
//            Mineotaur.LOGGER.info(Arrays.toString(filterArr));
//            Mineotaur.LOGGER.info(filter.toString());

            if (filterArr.length != filter.size()) {
                continue;
            }
            boolean filterMatch = true;
            for (String f: filterArr) {
                if (!filter.contains(f)) {
                    filterMatch = false;
                    break;
                }
            }
            if (filterMatch) {
                node = rel.getOtherNode(strain);
                break;
            }

            /*if (node != null) {
                break;
            }*/
        }
        if (node == null) {
            Mineotaur.LOGGER.warning("There is no node stored for strain " + genename + " for property " + prop1);
            return null;
        }
        /*Relationship rel = prop1Iterator.next();
        if (prop1Iterator.hasNext()) {
            throw new IllegalStateException("There are multiple nodes stored for strain " + genename + " for property " + prop1);
        }
        Node node = rel.getOtherNode(strain);*/

        /*double prop1Arr = (double) node.getProperty(aggregate);

        Mineotaur.LOGGER.info(filter.toString());
        List<Double> results = new ArrayList<>();*/
        /*for (int i = 0; i < prop1Arr.length; ++i) {
            results.add(prop1Arr[i]);
            *//*if (filter.contains(filterArr[i])) {
                results.add(prop1Arr[i]);
            }*//*
        }
        double[] resultArr = new double[results.size()];
        for (int i = 0; i < resultArr.length; ++i) {
            resultArr[i] = results.remove(0);
        }*/
        return (Double) node.getProperty(aggregate,null);
    }

    protected Double getAggregatedData(Node strain, String prop1, String genename, String aggProp1) {
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1)).iterator();
        if (!prop1Iterator.hasNext()) {
            Mineotaur.LOGGER.info("There is no node stored for strain " + genename + " for property " + prop1);
            return null;
        }
        Relationship rel = prop1Iterator.next();
        if (prop1Iterator.hasNext()) {
            Mineotaur.LOGGER.info("There are multiple nodes stored for strain " + genename + " for property " + prop1);
            return null;
        }
        Node node = rel.getOtherNode(strain);
//        double prop1Agg = (double) node.getProperty(aggProp1);
        return (Double) node.getProperty(aggProp1,null);
    }

    protected List<Map<String, Object>> getCellwiseScatterplotData(String prop1, String prop2, List<String> mapValuesProp1, List<String> mapValuesProp2, Node strain, String genename) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            double[] xArr = getFilteredArrayData(strain, prop1, genename, mapValuesProp1);
            double[] yArr = getFilteredArrayData(strain, prop2, genename, mapValuesProp2);
            int length = Math.min(xArr.length, yArr.length);

            for (int i = 0; i < length; ++i) {
                Map map = new HashMap<>();
                map.put("x", xArr[i]);
                map.put("y", yArr[i]);
                map.put("logX", Math.log(xArr[i]));
                map.put("logY", Math.log(yArr[i]));
                map.put("name", genename);
                map.put("labels", actualLabels);
                dataPoints.add(map);
            }


            tx.success();
        }
        return dataPoints;
    }

    /**
     * Method to query data for a genewise scatterplot.
     * @param geneList The list of genes.
     * @param prop1 First property.
     * @param prop2 Second property.
     * @param aggProp1  First aggregation mode.
     * @param mapValuesProp1 Checked filters for the first property.
     * @param aggProp2 Second aggregation mode.
     * @param mapValuesProp2 Checked filters for the first property.
     * @param hitLabels The requested labels.
     * @return A list containing the data points. Properties for each map (data point): x,y: data values; logX, logY: logarithmic values; name: name of the group object; labels: the labels of the object
     */
    protected List<Map<String, Object>> getHitsDecoupled(String[] geneList, String prop1, String prop2, String aggProp1, List<String> mapValuesProp1, String aggProp2, List<String> mapValuesProp2, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Mineotaur.LOGGER.info(rt.toString());
            Mineotaur.LOGGER.info(groupLabel.toString());
            Mineotaur.LOGGER.info(groupName);
            Mineotaur.LOGGER.info(Arrays.toString(geneList));
            Mineotaur.LOGGER.info(prop1);
            Mineotaur.LOGGER.info(prop2);
            for (String geneName : geneList) {

                Node strain = strainMap.get(geneName);
//                Node strain = db.findNodes(groupLabel,groupName,geneName).next();
                if (strain == null) {
                    continue;
                }
//                Mineotaur.LOGGER.info(geneName);
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
//                Mineotaur.LOGGER.info(String.valueOf(smds.hasNext()));
                Object x = null, y = null;
                DescriptiveStatistics statX = new DescriptiveStatistics();
                DescriptiveStatistics statY = new DescriptiveStatistics();
                while (smds.hasNext()) {

                    Relationship rel = smds.next();
                   /* Mineotaur.LOGGER.info(rel.getType().name());
                    if (!rel.getType().name().equals(rt.name()))
                        continue;*/
                    boolean in1, in2;
                    if (hasFilter) {
                        /*String stage = (String) rel.getProperty(filterName, null);
                        in1 = mapValuesProp1.contains(stage);
                        in2 = mapValuesProp2.contains(stage);
                        if (stage == null || (!in1 && !in2)) {
                            continue;
                        }*/
                        in1 = in2 = true;
                    }
                    else {
                        in1 = in2 = true;
                    }

                    Node smd = rel.getOtherNode(strain);
                    /*Mineotaur.LOGGER.info(smd.getLabels().toString());
                    Mineotaur.LOGGER.info(smd.getPropertyKeys().toString());*/


                    if (in1) {
                        x = smd.getProperty(prop1, null);
                        addValuesToDS(x, statX);
                    }
                    if (in2) {
                        y = smd.getProperty(prop2, null);
                        addValuesToDS(y, statY);
                    }
                }
                if (x == null || y == null) {
                    continue;
                }
                double xAgg = aggregate(statX, aggProp1), yAgg = aggregate(statY, aggProp2);
                Map<String, Object> map = new HashMap<>();
                map.put("x", xAgg);
                map.put("y", yAgg);
                map.put("logX", Math.log(xAgg));
                map.put("logY", Math.log(yAgg));
                map.put("name", geneName);
                map.put("labels", actualLabels);
                dataPoints.add(map);
            }
            tx.success();
        }
        Mineotaur.LOGGER.info(dataPoints.toString());
        return dataPoints;
    }

    protected Map<String, Object> getDataPointsForGene(String geneName, String prop1, String prop2, String aggProp1, List<String> mapValuesProp1, String aggProp2, List<String> mapValuesProp2, List<Label> hitLabels) {
        Map map = new HashMap<>();
//        Mineotaur.LOGGER.info(geneName);
        try (Transaction tx = db.beginTx()) {
            Node strain = strainMap.get(geneName);
//            Node strain = db.findNodes(groupLabel, groupName, geneName).next();
            if (strain == null) {
                return map;
            }
//                Mineotaur.LOGGER.info(geneName);
            List<String> actualLabels = getActualLabels(hitLabels, strain);
            if (actualLabels.isEmpty()) {
                return map;
            }
//                Mineotaur.LOGGER.info(mapValuesProp1.toString());
            Double xAgg = getFilteredAggregatedData(strain, prop1, geneName, aggProp1,mapValuesProp1);
            Double yAgg = getFilteredAggregatedData(strain, prop2, geneName, aggProp2,mapValuesProp2);
            if (xAgg == null || yAgg == null) {
                return map;
            }
            map.put("x", xAgg);
            map.put("y", yAgg);
            map.put("logX", Math.log(xAgg));
            map.put("logY", Math.log(yAgg));
            map.put("name", geneName);
            map.put("labels", actualLabels);
        }

        return map;
    }


    protected List<Map<String, Object>> getGenewiseScatterplotData(String[] geneList, String prop1, String prop2, String aggProp1, List<String> mapValuesProp1, String aggProp2, List<String> mapValuesProp2, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {

            for (String geneName : geneList) {

                Node strain = strainMap.get(geneName);
//                Mineotaur.LOGGER.info(geneName);
//                Node strain = db.findNodes(groupLabel, groupName, geneName).next();
                //Node strain = db.findNodesByLabelAndProperty(groupLabel, groupName, geneName).iterator().next();
                if (strain == null) {
                    //Mineotaur.LOGGER.info("No gene with name: " + geneName);
                    continue;
                }
                else {
                    //Mineotaur.LOGGER.info("Gene " + geneName + " loaded.");
                }

                List<String> actualLabels = getActualLabels(hitLabels, strain);
                //Mineotaur.LOGGER.info("Labels: " + actualLabels.toString());
                if (actualLabels.isEmpty()) {
                    continue;
                }
                //Mineotaur.LOGGER.info("Filters: " + mapValuesProp1.toString());
                Double xAgg = getFilteredAggregatedData(strain, prop1, geneName, aggProp1, mapValuesProp1);
                //Mineotaur.LOGGER.info(prop1 + ": " + xAgg);
                Double yAgg = getFilteredAggregatedData(strain, prop2, geneName, aggProp2, mapValuesProp2);
                //Mineotaur.LOGGER.info(prop2 + ": " + yAgg);
                if (xAgg == null || yAgg == null) {
                    continue;
                }
                Map map = new HashMap<>();
                map.put("x", xAgg);
                map.put("y", yAgg);
                map.put("logX", Math.log(xAgg));
                map.put("logY", Math.log(yAgg));
                map.put("name", geneName);
                map.put("labels", actualLabels);
                dataPoints.add(map);
                //dataPoints.add(getDataPointsForGene(geneName, prop1, prop2, aggProp1, mapValuesProp1, aggProp2, mapValuesProp2, hitLabels));
//                double[] xArr = getArrayData(strain, prop1, geneName);
//                double[] yArr = getArrayData(strain, prop2, geneName);
//                int length = Math.min(xArr.length, yArr.length);
//
//                for (int i = 0; i < length; ++i) {
//
//                }
                /*Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
                Object x, y;
                List<double[]> xList = new ArrayList<>(), yList = new ArrayList<>();
                while (smds.hasNext()) {
                    Relationship rel = smds.next();
                    boolean in1, in2;
                    if (hasFilter) {
                        String stage = (String) rel.getProperty("stage", null);
                        in1 = mapValuesProp1.contains(stage);
                        in2 = mapValuesProp2.contains(stage);
                        if (stage == null || (!in1 && !in2))  {
                            continue;
                        }
                        in1 = in2 = true;
                    }
                    else {
                        in1 = in2 = true;
                    }

                    Node smd = rel.getOtherNode(strain);
                    if (in1) {
                        x = smd.getProperty(prop1, null);
                        if (x != null) {
                            xList.add((double[]) x);
                        }
                    }
                    if (in2) {
                        y = smd.getProperty(prop2, null);
                        if (y != null) {
                            yList.add((double[]) y);
                        }
                    }
                }
                int idx = 0;
                int size = Math.min(xList.size(), yList.size());
                while (idx < size) {
                    double[] xArr = xList.get(idx);
                    double[] yArr = yList.get(idx);
                    idx++;
                    int length = Math.min(xArr.length, yArr.length);

                    for (int i = 0; i < length; ++i) {
                        Map map = new HashMap<>();
                        map.put("x", xArr[i]);
                        map.put("y", yArr[i]);
                        map.put("logX", Math.log(xArr[i]));
                        map.put("logY", Math.log(yArr[i]));
                        map.put("name", geneName);
                        map.put("labels", actualLabels);
                        dataPoints.add(map);
                    }
                }
            }*/
            }
            //tx.success();
        }
        /*catch (IllegalStateException ie) {
            Mineotaur.LOGGER.info(ie.toString());
        }*/
        return dataPoints;
        /*List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            Mineotaur.LOGGER.info(rt.toString());
            Mineotaur.LOGGER.info(groupLabel.toString());
            Mineotaur.LOGGER.info(groupName);
            Mineotaur.LOGGER.info(Arrays.toString(geneList));
            Mineotaur.LOGGER.info(prop1);
            Mineotaur.LOGGER.info(prop2);
            for (String geneName : geneList) {

//                Node strain = strainMap.get(geneName);
                Node strain = db.findNodesByLabelAndProperty(groupLabel,groupName,geneName).iterator().next();
                if (strain == null) {
                    continue;
                }
//                Mineotaur.LOGGER.info(geneName);
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
//                Mineotaur.LOGGER.info(String.valueOf(smds.hasNext()));
                Object x = null, y = null;
                DescriptiveStatistics statX = new DescriptiveStatistics();
                DescriptiveStatistics statY = new DescriptiveStatistics();
                while (smds.hasNext()) {

                    Relationship rel = smds.next();
                   *//* Mineotaur.LOGGER.info(rel.getType().name());
                    if (!rel.getType().name().equals(rt.name()))
                        continue;*//*
                    boolean in1, in2;
                    if (hasFilter) {
                        *//*String stage = (String) rel.getProperty(filterName, null);
                        in1 = mapValuesProp1.contains(stage);
                        in2 = mapValuesProp2.contains(stage);
                        if (stage == null || (!in1 && !in2)) {
                            continue;
                        }*//*
                        in1 = in2 = true;
                    }
                    else {
                        in1 = in2 = true;
                    }

                    Node smd = rel.getOtherNode(strain);
                    *//*Mineotaur.LOGGER.info(smd.getLabels().toString());
                    Mineotaur.LOGGER.info(smd.getPropertyKeys().toString());*//*


                    if (in1) {
                        x = smd.getProperty(prop1, null);
                        addValuesToDS(x, statX);
                    }
                    if (in2) {
                        y = smd.getProperty(prop2, null);
                        addValuesToDS(y, statY);
                    }
                }
                if (x == null || y == null) {
                    continue;
                }
                double xAgg = aggregate(statX, aggProp1), yAgg = aggregate(statY, aggProp2);
                Map<String, Object> map = new HashMap<>();
                map.put("x", xAgg);
                map.put("y", yAgg);
                map.put("logX", Math.log(xAgg));
                map.put("logY", Math.log(yAgg));
                map.put("name", geneName);
                map.put("labels", actualLabels);
                dataPoints.add(map);
            }
            tx.success();
        }
        Mineotaur.LOGGER.info(dataPoints.toString());
        return dataPoints;*/
    }


    /**
     * Method to query data for a genewise distribution plot.
     * @param geneListDist The gene list.
     * @param prop1 The selected feature.
     * @param agg The aggregation mode.
     * @param mapValues  The selected filters.
     * @param hitLabels The selected labels.
     * @return A list containing the data points. Properties for each map (data point): x,y: data values; logX, logY: logarithmic values; name: name of the group object; labels: the labels of the object
     */
    protected List<Map<String, Object>> getHitsDecoupledMap(String[] geneListDist, String prop1, String agg, List<String> mapValues, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            for (String geneName : geneListDist) {
                Node strain = strainMap.get(geneName);
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
                Object x;
                DescriptiveStatistics statX = new DescriptiveStatistics();
                while (smds.hasNext()) {
                    Relationship rel = smds.next();
                    /*if (hasFilter) {
                        String stage = (String) rel.getProperty("stage", null);
                        if (stage == null || !mapValues.contains(stage)) {
                            continue;
                        }
                        if (stage.isEmpty()) {
                            continue;
                        }
                    }*/
                    Node smd = rel.getOtherNode(strain);

                    x = smd.getProperty(prop1, null);
                    if (x != null) {
                        double[] arr = (double[]) x;
                        for (int i = 0; i < arr.length; ++i) {
                            statX.addValue(arr[i]);
                        }

                    }
                }
                if (statX.getN() == 0) {
                    continue;
                }
                double xAgg = aggregate(statX, agg);
                for (Label label : hitLabels) {
                    if (strain.hasLabel(label)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("x", xAgg);
                        map.put("logX", Math.log(xAgg));
                        map.put("name", geneName);
                        map.put("label", label.toString());
                        map.put("labels", actualLabels);
                        dataPoints.add(map);
                    }
                }
            }
            tx.success();
        }
        return dataPoints;
    }

    protected List<Map<String, Object>> getGenewiseDistributionData(String[] geneListDist, String prop1, String agg, List<String> mapValues, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            for (String geneName : geneListDist) {

                Node strain = strainMap.get(geneName);
//                Node strain = db.findNodes(groupLabel, groupName, geneName).next();
                if (strain == null) {
                    continue;
                }
//                Mineotaur.LOGGER.info(geneName);
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Double xAgg = getFilteredAggregatedData(strain, prop1, geneName, agg, mapValues);
                if (xAgg == null) {
                    continue;
                }
                Map map = new HashMap<>();
                map.put("x", xAgg);
                map.put("logX", Math.log(xAgg));
                map.put("name", geneName);
                map.put("labels", actualLabels);
                dataPoints.add(map);
            }
            tx.success();
        }
        catch (IllegalStateException ie) {
            Mineotaur.LOGGER.info(ie.toString());
        }
        return dataPoints;
    }

    /**
     * Method to query data for a cellwise distribution plot.
     * @param prop1  The selected property.
     * @param mapValues Checked filters for the property.
     * @param strain The selected gene.
     * @return A list containing the data points. Properties for each map (data point): x,y: data values; logX, logY: logarithmic values; name: name of the group object; labels: the labels of the object
     */
    protected List<Map<String, Object>> getHitsDecoupled(String prop1, List<String> mapValues, Node strain) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
            Object x;
            while (smds.hasNext()) {
                Relationship rel = smds.next();
                /*if (hasFilter) {
                    String stage = (String) rel.getProperty("stage", null);
                    if (stage == null || !mapValues.contains(stage)) {
                        continue;
                    }
                }*/
                Node smd = rel.getOtherNode(strain);
                x = smd.getProperty(prop1, null);
                if (x != null) {
                    double[] arr = (double[]) x;
                    for (int i = 0; i < arr.length; ++i) {
                        Map map = new HashMap<>();
                        map.put("x", arr[i]);
                        map.put("logX", Math.log(arr[i]));
                        map.put("labels", actualLabels);
                        dataPoints.add(map);
                    }

                }
            }
            tx.success();
        }
        return dataPoints;
    }

    protected List<Map<String, Object>> getCellwiseDistributionData(String prop1, List<String> mapValues, Node strain) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            double[] xArr = getFilteredArrayData(strain, prop1, "", mapValues);
            int length = xArr.length;

            for (int i = 0; i < length; ++i) {
                Map map = new HashMap<>();
                map.put("x", xArr[i]);
                map.put("logX", Math.log(xArr[i]));
                map.put("labels", actualLabels);
                dataPoints.add(map);
            }


            tx.success();
        }
        return dataPoints;
    }

    /**
     * Method to calculate the aggregated result for a query.
     * @param stat The DescriptiveStatistics instance containing the data.
     * @param agg The aggregation mode.
     * @return The aggregated result.
     */
    protected double aggregate(DescriptiveStatistics stat, String agg) {
        switch (agg) {
            case GenericEmbeddedGraphDatabaseProvider.AVERAGE: return stat.getMean();
            case GenericEmbeddedGraphDatabaseProvider.MAXIMUM: return stat.getMax();
            case GenericEmbeddedGraphDatabaseProvider.MINIMUM: return stat.getMax();
            case GenericEmbeddedGraphDatabaseProvider.MEDIAN: return stat.getPercentile(50);
            case GenericEmbeddedGraphDatabaseProvider.STANDARD_DEVIATION: return stat.getStandardDeviation();
            case GenericEmbeddedGraphDatabaseProvider.COUNT: return stat.getN();
            default: throw new IllegalStateException("Wrong aggregation mode.");
        }
    }

    /**
     * Method to get a list of labels from the array of checkboxes.
     * @param hitCheckbox The checked filters.
     * @return The list of labels.
     */
    private List<Label> manageHitCheckbox(String[] hitCheckbox) {
        List<Label> hitLabels = new ArrayList<>();
        for (String hit : hitCheckbox) {
            hitLabels.add(hitsByName.get(hit));
        }
        return hitLabels;
    }

    /**
     * Method to provide the list of actual label names for a group object.
     * @param hitLabels The list of labels applicable.
     * @param strain The group object.
     * @return The list of label names.
     */
    private List<String> getActualLabels(List<Label> hitLabels, Node strain) {
//        Mineotaur.LOGGER.info(strain.getLabels().toString());
        List<String> actualLabels = new ArrayList<>();
        for (Label label : hitLabels) {
//            Mineotaur.LOGGER.info(label.toString());
            if (strain.hasLabel(label)) {
                actualLabels.add(hitsByLabel.get(label));
            }
        }
        return actualLabels;
    }

    /**
     * Adds double values from an array to a DescriptiveStatistics instance.
     * @param o The array.
     * @param stat The DescriptiveStatistics instance.
     */
    private void addValuesToDS(Object o, DescriptiveStatistics stat) {
        if (o != null) {
            double[] arr = (double[]) o;
            for (int i = 0; i < arr.length; ++i) {
                stat.addValue(arr[i]);
            }
        }
    }

    /**
     * Getter method for allHitLabels;
     * @return allHitLabels.
     */
    @ModelAttribute("allLabels")
    public List<String> getAllHitLabels() {
        return hitNames;
    }

    /**
     * Getter method for filters;
     * @return filters.
     */
    @ModelAttribute("filters")
    public Map<String, String> getFilters() {
        return filters;
    }

    /**
     * Getter method for menu1;
     * @return menu1.
     */
    @ModelAttribute("menu1")
    public Map<String, String> getMenu1() {
        return menu1;
    }

    /**
     * Getter method for menu2
     * @return menu2.
     */
    @ModelAttribute("menu2")
    public Map<String, String> getMenu2() {
        return menu2;
    }

    /**
     * Getter method for features;
     * @return features.
     */
    @ModelAttribute("features")
    public List<String> getFeatures() {
        return features;
    }

    /**
     * Getter method for groupNames;
     * @return groupNames.
     */
    @ModelAttribute("groupNames")
    public List<String> getGroupNames() {
        return groupNames;
    }

    /**
     * Getter method for hasFilter;
     * @return hasFilter.
     */
    @ModelAttribute("hasFilter")
    public boolean hasFilter() {
        return hasFilter;
    }

    /**
     * Getter method for hitNames;
     * @return hitNames.
     */
    @ModelAttribute("hits")
    public List<String> getHitNames() {
        return hitNames;
    }

    /**
     * Getter method for aggValues;
     * @return aggValues.
     */
    @ModelAttribute("aggValues")
    public List<String> getAggValues() {
        return aggValues;
    }

    private String decompressString(String data) {
        String decompressed = null;
        byte[] byteData = Base64.getDecoder().decode(data);
        //byte[] byteData = data.getBytes();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(byteData.length)){

            Inflater inflater = new Inflater();
            inflater.setInput(byteData);

            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            byte[] output = baos.toByteArray();
            decompressed = new String(output);
        } catch (DataFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decompressed;
    }

    private Model decodeURL(Model model, MultiValueMap<String, String> params) {

        String data = params.get("content").get(0);
        String decompressed = decompressString(data);
        Mineotaur.LOGGER.info(decompressed);
        /*byte[] byteData = Base64.getDecoder().decode(data);
        //byte[] byteData = data.getBytes();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(byteData.length);){

            Inflater inflater = new Inflater();
            inflater.setInput(byteData);

            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            byte[] output = baos.toByteArray();
            decompressed = new String(output);
        } catch (DataFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String[] terms = decompressed.split(",");
        for (String term: terms) {
            String[] parts = term.split(":");
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = parts[i].replaceAll("\"|\\{|\\}","");
            }
            String[] value = parts[1].split("\\|");
            if ("geneList".equals(parts[0]) || "geneListDist".equals(parts[0])) {
                String geneListString = decompressString(parts[1]);
                List<String> geneList = new ArrayList<>();
                char[] chars = geneListString.toCharArray();
                for (int i = 0; i < chars.length; ++i) {
                    if (chars[i] == '1') {
                        geneList.add(groupNames.get(i));
                    }
                }
                model.addAttribute(parts[0], geneList.toArray(new String[geneList.size()]));
            }
            else if (parts[0].startsWith("mapValues")) {
                model.addAttribute(parts[0], Arrays.asList(value));
            }
            else if (value.length == 1){
                model.addAttribute(parts[0], value[0]);
            }
            else {
                model.addAttribute(parts[0], value);

            }
            /*if (parts[0].equals("type")) {
                type = parts[1];
            }*/
        }
        return model;
    }

    @RequestMapping("/decode")
    public @ResponseBody List<Map<String, Object>> decodeQuery(Model model, @RequestParam MultiValueMap<String, String> params) {
        model = decodeURL(model, params);
        String type = (String) model.asMap().get("type");
//        Mineotaur.LOGGER.info(((List<String>) model.asMap().get("mapValuesGWDist")).toString());
        /*String data = params.get("content").get(0);
        String decompressed = decompressString(data);
        *//*byte[] byteData = Base64.getDecoder().decode(data);
        //byte[] byteData = data.getBytes();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(byteData.length);){

            Inflater inflater = new Inflater();
            inflater.setInput(byteData);

            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            byte[] output = baos.toByteArray();
            decompressed = new String(output);
        } catch (DataFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*//*
        Application.LOGGER.info(decompressed);
        String[] terms = decompressed.split(",");
        for (String term: terms) {
            String[] parts = term.split(":");
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = parts[i].replaceAll("\"|\\{|\\}","");
            }
            String[] value = parts[1].split("\\|");
            if ("geneList".equals(parts[0]) || "geneListDist".equals(parts[0])) {
                String geneListString = decompressString(parts[1]);
                List<String> geneList = new ArrayList<>();
                char[] chars = geneListString.toCharArray();
                for (int i = 0; i < chars.length; ++i) {
                    if (chars[i] == '1') {
                        geneList.add(geneNames.get(i));
                    }
                }
                model.addAttribute(parts[0], geneList.toArray(new String[geneList.size()]));
            }
            else if (value.length == 1) {
                model.addAttribute(parts[0], value[0]);
            }
            else {
                model.addAttribute(parts[0], value);
            }
            if (parts[0].equals("type")) {
                type = parts[1];
            }
        }*/
        switch (type) {
            case "cellwiseScatter": return getScatterPlotDataCellJSONSeparate(model);
            case "genewiseScatter": return getScatterPlotDataGeneJSONSeparate(model);
            case "genewiseDistribution": return getDistributionDataGenewiseJSON(model);
            case "cellwiseDistribution": return getDistributionDataCellwiseJSON(model);
            default: throw new UnsupportedOperationException();
        }

    }

    public @ResponseBody
    List<Map<String, Object>> getDistributionDataCellwiseJSON(Model model) {
        Map<String, Object> map = model.asMap();
        List<String> mapValuesProp1 = getMapValues(map.get("mapValuesCWDist"));

        /*String[] mapValues = (String[]) map.get("mapValuesGWDist");
        List<String> mapValuesProp1 = null;
        if (mapValues != null) {
            mapValuesProp1 = Arrays.asList(mapValues);
        }*/
        return getDistributionDataCellwiseJSON(model, (String) map.get("propCWDist"), (String) map.get("geneCWDist"), mapValuesProp1);
    }

    public @ResponseBody
    List<Map<String, Object>> getDistributionDataGenewiseJSON(Model model) {
        Map<String, Object> map = model.asMap();
//        List<String> mapValuesProp1 = getMapValues(map.get("mapValuesGWDist"));
//        Mineotaur.LOGGER.info(mapValuesProp1.toString());
        /*String[] mapValues = (String[]) map.get("mapValuesGWDist");
        List<String> mapValuesProp1 = null;
        if (mapValues != null) {
            mapValuesProp1 = Arrays.asList(mapValues);
        }*/
        String[] hitCheckbox;
        Object hit = map.get("hitCheckboxGWDist");
        if (hit instanceof String) {
            hitCheckbox = new String[]{((String) hit)};
        }
        else {
            hitCheckbox = ((String[]) hit);
        }
        return getDistributionDataGenewiseJSON(model, (String[]) map.get("geneListDist"), (String) map.get("propGWDist"), (String) map.get("aggGWDist"), (List<String>) map.get("mapValuesGWDist"), hitCheckbox);
    }

    public @ResponseBody
    List<Map<String, Object>> getScatterPlotDataCellJSONSeparate(Model model) {
        Map<String, Object> map = model.asMap();
//        List<String> mapValuesProp1 = getMapValues(map.get("mapValuesProp1"));
//        List<String> mapValuesProp2 = getMapValues(map.get("mapValuesProp2"));

        /*String[] mapValues = (String[]) map.get("mapValuesCellwiseProp1");
        List<String> mapValuesProp1 = null;
        if (mapValues != null) {
            mapValuesProp1 = Arrays.asList(mapValues);
        }
        mapValues = (String[]) map.get("mapValuesCellwiseProp2");
        List<String> mapValuesProp2 = null;
        if (mapValues != null) {
            mapValuesProp1 = Arrays.asList(mapValues);
        }*/
        return cellwiseScatterJSON(model, ((String) map.get("cellwiseProp1")), ((String) map.get("cellwiseProp2")), ((List<String>) map.get("mapValuesCellwiseProp1")), ((List<String>) map.get("mapValuesCellwiseProp2")), ((String) map.get("geneCWProp1")));
    }

    private List<String> getMapValues(Object o) {
        List mapValues = null;
        if (o instanceof String[]) {
            mapValues = Arrays.asList((String[])o);
        }
        else if (o instanceof String) {
            mapValues = new ArrayList<>();
            mapValues.add(o);
        }
        return mapValues;
    }

    public @ResponseBody
    List<Map<String, Object>> getScatterPlotDataGeneJSONSeparate(Model model) {
        Map<String, Object> map = model.asMap();
        System.out.println(map.toString());
        String[] geneList = ((String[]) map.get("geneList"));
        String prop1 = (String) map.get("prop1");
        String prop2 = (String) map.get("prop2");
        String aggProp1 = (String) map.get("aggProp1");
        String aggProp2 = (String) map.get("aggProp2");
//        String[] mapValues = (String[]) map.get("mapValuesProp1");
        List<String> mapValuesProp1 = getMapValues(map.get("mapValuesProp1"));
        /*null;
        if (mapValues != null) {
            mapValuesProp1 = Arrays.asList(mapValues);
        }*/
        Object hit =  map.get("hitCheckbox");
        String[] hitCheckbox;
        if (hit instanceof String) {
            hitCheckbox = new String[]{((String) hit)};
        }
        else {
            hitCheckbox = ((String[]) hit);
        }
        List<String> mapValuesProp2 = getMapValues(map.get("mapValuesProp2"));

        /*mapValues = (String[]) map.get("mapValuesProp2");
        List<String> mapValuesProp2 = null;
        if (mapValues != null) {
            mapValuesProp2 = Arrays.asList(mapValues);
        }*/
        return getScatterPlotDataGeneJSONSeparate(model, geneList, prop1, prop2, aggProp1, aggProp2, mapValuesProp1, hitCheckbox, mapValuesProp2);
    }

    @RequestMapping("/share")
    public String query(Model model, @RequestParam String type, @RequestParam String content) {
        /*String type = params.get("type").get(0);
        model.addAllAttributes(params);*/
        /*model = decodeURL(model, params);
        String type = (String) model.asMap().get("type");
        List<Map<String, Object>> dataPoints;
        switch (type) {
            case "cellwiseScatter": dataPoints = getScatterPlotDataCellJSONSeparate(model); break;
            case "genewiseScatter": dataPoints = getScatterPlotDataGeneJSONSeparate(model); break;
            case "genewiseDistribution": dataPoints = getDistributionDataGenewiseJSON(model); break;
            case "cellwiseDistribution": dataPoints = getDistributionDataCellwiseJSON(model); break;
            default: throw new UnsupportedOperationException();
        }
        model.addAttribute("dataPoints", dataPoints);     */
        model.addAttribute("content", content);
        model.addAttribute("type", type);
        model.addAttribute("toDecode", true);
        model.addAttribute("sharedLink", true);
        /*return "redirect:/" + type;*/
        return "index";
    }

    @RequestMapping("/embed")
    public String embed(Model model, @RequestParam MultiValueMap<String, String> params) {
        model = decodeURL(model, params);
        String type = (String) model.asMap().get("type");
        List<Map<String, Object>> dataPoints;
        switch (type) {
            case "cellwiseScatter": dataPoints = getScatterPlotDataCellJSONSeparate(model); break;
            case "genewiseScatter": dataPoints = getScatterPlotDataGeneJSONSeparate(model); break;
            case "genewiseDistribution": dataPoints = getDistributionDataGenewiseJSON(model); break;
            case "cellwiseDistribution": dataPoints = getDistributionDataCellwiseJSON(model); break;
            default: throw new UnsupportedOperationException();
        }
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("sharedLink", true);
        return "embeddedcontent";
    }
}
