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
import org.mineotaur.provider.GenericEmbeddedGraphDatabaseProvider;
import org.mineotaur.provider.GraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

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
    private List<String> filters;
    private Map<String, String> menu1;
    private Map<String, String> menu2;
    private List<String> features;
    private List<String> groupNames;

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
        this.strainMap = (Map<String, Node>) context.get("groupByGroupName");;
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
        filters = (List<String>) context.get("filters");
        groupNames = (List<String>) context.get("groupNames");
        hasFilter = (boolean) context.get("hasFilter");
    }

    /**
     * Method for mapping the starting page.
     * @param model The model.
     * @return The requested page.
     */
    @RequestMapping("/mineotaur")
    public String start(Model model) {
        return "mineotaur";
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
        List<Map<String, Object>> dataPoints = getHitsDecoupled(geneList, prop1, prop2, aggProp1, mapValuesProp1, aggProp2, mapValuesProp2, hitLabels);
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
        List<Map<String, Object>> dataPoints = getHitsDecoupledMap(geneListDist, propGWDist, aggGWDist, mapValuesGWDist, hitLabels);
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
        List<Map<String, Object>> dataPoints = getHitsDecoupled(cellwiseProp1, cellwiseProp2, mapValuesCellwiseProp1, mapValuesCellwiseProp2, strain1, geneCWProp1);
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
        List<Map<String, Object>> dataPoints = getHitsDecoupled(propCWDist, mapValuesCWDist, strain);
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
     * @return
     */
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
                    String stage = (String) rel.getProperty("stage", null);
                    in1 = mapValuesProp1.contains(stage);
                    in2 = mapValuesProp2.contains(stage);
                    if (stage == null || (!in1 && !in2))  {
                        continue;
                    }
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
            for (String geneName : geneList) {

                Node strain = strainMap.get(geneName);
                if (strain == null) {
                    continue;
                }
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Iterator<Relationship> smds = strain.getRelationships(rt).iterator();
                Object x = null, y = null;
                DescriptiveStatistics statX = new DescriptiveStatistics();
                DescriptiveStatistics statY = new DescriptiveStatistics();
                while (smds.hasNext()) {
                    Relationship rel = smds.next();
                    boolean in1, in2;
                    if (hasFilter) {
                        String stage = (String) rel.getProperty("stage", null);
                        in1 = mapValuesProp1.contains(stage);
                        in2 = mapValuesProp2.contains(stage);
                        if (stage == null || (!in1 && !in2)) {
                            continue;
                        }
                    }
                    else {
                        in1 = in2 = true;
                    }

                    Node smd = rel.getOtherNode(strain);
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
        return dataPoints;
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
                    if (hasFilter) {
                        String stage = (String) rel.getProperty("stage", null);
                        if (stage == null || !mapValues.contains(stage)) {
                            continue;
                        }
                        if (stage.isEmpty()) {
                            continue;
                        }
                    }
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
                if (hasFilter) {
                    String stage = (String) rel.getProperty("stage", null);
                    if (stage == null || !mapValues.contains(stage)) {
                        continue;
                    }
                }
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
        List<String> actualLabels = new ArrayList<>();
        for (Label label : hitLabels) {

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
    public List<Label> getAllHitLabels() {
        return allHitLabels;
    }

    /**
     * Getter method for filters;
     * @return filters.
     */
    @ModelAttribute
    public List<String> getFilters() {
        return filters;
    }

    /**
     * Getter method for menu1;
     * @return menu1.
     */
    @ModelAttribute
    public Map<String, String> getMenu1() {
        return menu1;
    }

    /**
     * Getter method for menu2
     * @return menu2.
     */
    @ModelAttribute
    public Map<String, String> getMenu2() {
        return menu2;
    }

    /**
     * Getter method for features;
     * @return features.
     */
    @ModelAttribute
    public List<String> getFeatures() {
        return features;
    }

    /**
     * Getter method for groupNames;
     * @return groupNames.
     */
    @ModelAttribute
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
    @ModelAttribute
    public List<String> getAggValues() {
        return aggValues;
    }
}
