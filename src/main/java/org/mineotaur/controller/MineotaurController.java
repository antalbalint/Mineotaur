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

import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.StringUtils;
import org.mineotaur.provider.DataDescriptor;
import org.mineotaur.provider.GenericEmbeddedGraphDatabaseProvider;
import org.mineotaur.provider.GraphDatabaseProvider;
import org.mineotaur.provider.HTHCSEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
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
    //private List<String> aggValues;
    private Map<String, Label> hitsByName = null;
    private Map<Label, String> hitsByLabel = null;
    //private List<String> hitNames = null;
    private List<Label> allHitLabels = null;
    private boolean hasFilter;
    /*private Map<String, String> filters;
    private Map<String, String> menu1;
    private Map<String, String> menu2;
    private List<String> features;*/
    private List<String> groupNames;
    private boolean multiScreen;
    //private String omeroURL;

    /**
     * Method to initalize the controller. The database provider is injected by Spring.
     * @param provider The database provider.
     */
    @Autowired
    public void setProvider (GraphDatabaseProvider provider) {
        this.provider = provider;
        this.db = provider.getDatabaseService();
        manageDataDescriptor(provider);


    }

    private void manageDataDescriptor(GraphDatabaseProvider provider) {
        if (provider instanceof GenericEmbeddedGraphDatabaseProvider) {
            manageGenericDataDescriptor((Map<String, Object>) provider.getContext());
        }
        else if (provider instanceof HTHCSEmbeddedGraphDatabaseProvider) {
            manageHTHCSDataDescriptor((DataDescriptor) provider.getContext());
        }
    }

    private void manageGenericDataDescriptor(Map<String, Object> context) {
        //this.aggValues = (List<String>) context.get("aggValues");
        this.hitsByName = (Map<String, Label>) context.get("hitLabels");
        this.hitsByLabel = (Map<Label, String>) context.get("hitsByLabel");
        //this.hitNames = (List<String>) context.get("hitNames");
        this.allHitLabels = new ArrayList<>();
        allHitLabels.addAll(hitsByLabel.keySet());
        this.strainMap = (Map<String, Node>) context.get("groupByGroupName");
        /*menu1 = new HashMap<>();
        menu1.put("cellwiseScatter", "Cell-wise scatter plot");
        menu1.put("cellwiseHistogramX", "Histogram (X axis)");
        menu1.put("cellwiseHistogramY", "Histogram (Y axis)");
        menu1.put("cellwiseKDEX", "Kernel Density Estimation (X axis)");
        menu1.put("cellwiseKDEY", "Kernel Density Estimation (Y axis)");
        menu2 = new HashMap<>();
        menu2.put("analyze", "Analyze");*/
        //features = (List<String>) context.get("features");
        //filters = (Map<String, String>) context.get("filters");
        groupNames = (List<String>) context.get("groupNames");
        hasFilter = (boolean) context.get("hasFilter");
        /*if (context.containsKey("omeroURL")) {
            omeroURL = (String) context.get("omeroURL");
        }*/
        multiScreen = (boolean) context.get("multiscreen");
    }

    private void manageHTHCSDataDescriptor(DataDescriptor dataDescriptor) {

    }

    @ModelAttribute("dataDescriptor")
    public Object getDataDescriptor() {
        return provider.getContext();
    }


        /**
         * Getter method for allHitLabels;
         * @return allHitLabels.
         */
    /*@ModelAttribute("allLabels")
    public List<String> getAllHitLabels() {
        return hitNames;
    }*/

    /**
     * Getter method for filters;
     * @return filters.
     */
    /*@ModelAttribute("filters")
    public Map<String, String> getFilters() {
        return filters;
    }*/

    /**
     * Getter method for menu1;
     * @return menu1.
     */
    /*@ModelAttribute("menu1")
    public Map<String, String> getMenu1() {
        return menu1;
    }*/

    /**
     * Getter method for menu2
     * @return menu2.
     */
    /*@ModelAttribute("menu2")
    public Map<String, String> getMenu2() {
        return menu2;
    }*/

    /**
     * Getter method for features;
     * @return features.
     */
    /*@ModelAttribute("features")
    public List<String> getFeatures() {
        return features;
    }*/

    /**
     * Getter method for groupNames;
     * @return groupNames.
     */
    /*@ModelAttribute("groupNames")
    public List<String> getGroupNames() {
        return groupNames;
    }*/

    /**
     * Getter method for hasFilter;
     * @return hasFilter.
     */
    /*@ModelAttribute("hasFilter")
    public boolean hasFilter() {
        return hasFilter;
    }*/

    /**
     * Getter method for hitNames;
     * @return hitNames.
     */
    /*@ModelAttribute("hits")
    public List<String> getHitNames() {
        return hitNames;
    }*/

    /**
     * Getter method for aggValues;
     * @return aggValues.
     */
    /*@ModelAttribute("aggValues")
    public List<String> getAggValues() {
        return aggValues;
    }*/
    /*@ModelAttribute("omeroURL")
    public String getOmeroURL() {
        return omeroURL;
    }*/

    /**
     * Method for mapping the starting page.
     * @param model The model.
     * @return The requested page.
     */
    @RequestMapping("/")
    protected String start(Model model) {

        return "index";
    }

    /*@RequestMapping("/cleanup")
    protected String cleanup(Model model) {
        List<String> features = (List<String>) ((Map<String, Object>) provider.getContext()).get("features");
        Set<String> filters = (Set<String>) ((Map<String, String>)((Map<String, Object>) provider.getContext()).get("filters")).keySet();
        *//*Mineotaur.LOGGER.info(features.toString());
        Mineotaur.LOGGER.info(filters.toString());*//*
        int count = 0;
        Transaction tx = null;
        try  {
            tx = db.beginTx();
            for (String geneName: groupNames) {
                Mineotaur.LOGGER.info(geneName);
                Node strain = strainMap.get(geneName);

                for (String feature: features) {
//                    Mineotaur.LOGGER.info(feature);
                    RelationshipType aggRt = DynamicRelationshipType.withName(feature);
                    Iterator<Relationship> preRels = strain.getRelationships(aggRt).iterator();
                    while (preRels.hasNext()) {
                        Relationship preRel = preRels.next();
                        Node node = preRel.getOtherNode(strain);
                        preRel.delete();
//                        Mineotaur.LOGGER.info(String.valueOf(node.getId()));
                        node.delete();
                    }
                    RelationshipType rt = DynamicRelationshipType.withName(feature+"_ARRAY");
                    if (strain.hasRelationship(rt)) {
                        Iterator<Relationship> arrayRels = strain.getRelationships(rt).iterator();
                        while (arrayRels.hasNext()) {
                            Node node = arrayRels.next().getOtherNode(strain);
                            *//*Iterator<String> properties = node.getPropertyKeys().iterator();
                            while (properties.hasNext()) {
                                String key = properties.next();
                                Mineotaur.LOGGER.info(key);
                            }*//*
                            double[] array = (double[]) node.getProperty(feature);
                            String[] filtArr = (String[]) node.getProperty("filter");
                            Map<String, List<Double>> valuesByfilter = new HashMap<>();
                            for (String filter: filters) {
                                valuesByfilter.put(filter, new ArrayList<>());
                            }
                            for (int i = 0; i < array.length; ++i) {
//                                if (!Double.isNaN(array[i])) {
//                                    stat.addValue(array[i]);
//                                }
                                if (!Double.isNaN(array[i]) && !filtArr[i].equals("NaN")) {
                                    valuesByfilter.get(filtArr[i]).add(array[i]);
                                }
                            }
                            for (String filter: valuesByfilter.keySet()) {
                                List<Double> values = valuesByfilter.get(filter);

                            }
                            List<String> filterValues = new ArrayList<>();
                            filterValues.addAll(filters);
                            Map<String[], List<Double>> powerSet = new HashMap<>();
                            double maxSize = Math.pow(2, filters.size());
                            int set = 1;
                            while (set < maxSize) {
                                List<String> combination = new ArrayList<>();
                                List<Double> values = new ArrayList<>();
                                for (int i = 0; i < filters.size(); ++i) {
                                    if (((set >> i) & 1) == 1) {
                                        String filter = filterValues.get(i);
                                        combination.add(filter);
                                        values.addAll(valuesByfilter.get(filter));
//                                        System.out.print(i + ", ");
                                    }
                                }

//                                powerSet.put(combination.toArray(new String[combination.size()]), values);
//                                System.out.println(combination);
                                DescriptiveStatistics stat = new DescriptiveStatistics();
                                for (Double val:values) {

                                    if (!Double.isNaN(val))
                                        stat.addValue(val);
                                }
                                Node precomputedAgg = db.createNode();
                                precomputedAgg.setProperty("Average", stat.getMean());
                                precomputedAgg.setProperty("Minimum", stat.getMin());
                                precomputedAgg.setProperty("Maximum", stat.getMax());
                                precomputedAgg.setProperty("Standard deviation", stat.getStandardDeviation());
                                precomputedAgg.setProperty("Median", stat.getPercentile(50));
                                precomputedAgg.setProperty("Count", stat.getN());
                                Relationship rel = precomputedAgg.createRelationshipTo(strain, aggRt);
                                rel.setProperty("filter", combination.toArray(new String[combination.size()]));
                                count++;
//                                Mineotaur.LOGGER.info(feature + "(" + combination + "): " + stat.getMean());
                                set += 1;
                            }
                        }
                    }
                    if (count > 5000) {
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                        count = 0;
                    }
                }

//                break;

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            tx.success();
            tx.close();
        }
//        listData(strainMap.get(groupNames.get(0)));

        return "index";
    }

    public void listData(Node strain) {
        try (Transaction tx = db.beginTx()) {
            Iterator<Relationship> rels = strain.getRelationships().iterator();
            while (rels.hasNext()) {
                Relationship rel = rels.next();
//                Mineotaur.LOGGER.info(rel.getType().name());
                String rtName = rel.getType().name();
                if (rtName.equals("length") || rtName.equals("length_ARRAY")) {
                    Node node = rel.getOtherNode(strain);
                    Iterator<String> properties = node.getPropertyKeys().iterator();
                    while (properties.hasNext()) {
                        String key = properties.next();

                        if (key.equals("length")) {
                            Mineotaur.LOGGER.info(key + ": " + (Arrays.toString((double[])node.getProperty(key))));

                        }
                        else if (key.equals("filter")) {
                            Mineotaur.LOGGER.info(key + ": " + (Arrays.toString((String[])node.getProperty(key))));

                        }
                        else {
                            Mineotaur.LOGGER.info(key + ": " + node.getProperty(key));
                        }
                    }
                }

            }
        }

    }*/
    @RequestMapping("/share")
    protected String query(Model model, @RequestParam MultiValueMap<String, String> params, @RequestParam String type, @RequestParam String content) {
        if (model == null || type == null || content == null || "".equals(type) || "".equals(content)) {
            throw new IllegalArgumentException();
        }
        Map<String, Object> model2 = StringUtils.decodeURL(model, params, groupNames).asMap();
        model.addAttribute("prop1", model2.get("prop1"));
        model.addAttribute("prop2", model2.get("prop2"));
        model.addAttribute("mapValuesProp1", model2.get("mapValuesProp1"));
        model.addAttribute("mapValuesProp2", model2.get("mapValuesProp2"));

        model.addAttribute("content", content);
        model.addAttribute("type", type);
        model.addAttribute("toDecode", true);
        model.addAttribute("sharedLink", true);
        return "index";
    }

    @RequestMapping("/embed")
    protected String embed(Model model, @RequestParam MultiValueMap<String, String> params) {
        if (model == null || params == null || params.size() == 0) {
            throw new IllegalArgumentException();
        }
        model = StringUtils.decodeURL(model, params, groupNames);
        String type = (String) model.asMap().get("type");
        List<Map<String, Object>> dataPoints;
        switch (type) {
            case "cellwiseScatter": dataPoints = getScatterPlotDataCellJSONSeparate(model); break;
            case "groupwiseScatterplot": dataPoints = getScatterPlotDataGeneJSONSeparate(model); break;
            case "groupwiseDistributionForm": dataPoints = getDistributionDataGenewiseJSON(model); break;
            case "cellwiseDistribution": dataPoints = getDistributionDataCellwiseJSON(model); break;
            default: throw new UnsupportedOperationException();
        }
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("sharedLink", true);
        return "embeddedcontent";
    }

    @RequestMapping("/decode")
    protected @ResponseBody List<Map<String, Object>> decodeQuery(Model model, @RequestParam MultiValueMap<String, String> params) {
        if (model == null || params == null || params.size() == 0) {
            throw new IllegalArgumentException();
        }
        model = StringUtils.decodeURL(model, params, groupNames);
        String type = (String) model.asMap().get("type");
        switch (type) {
            case "cellwiseScatter": return getScatterPlotDataCellJSONSeparate(model);
            case "groupwiseScatterplot": return getScatterPlotDataGeneJSONSeparate(model);
            case "groupwiseDistributionForm": return getDistributionDataGenewiseJSON(model);
            case "cellwiseDistribution": return getDistributionDataCellwiseJSON(model);
            default: throw new UnsupportedOperationException();
        }

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
    @RequestMapping("/groupwiseScatterplotJSON")
    protected @ResponseBody List<Map<String, Object>> getScatterPlotDataGeneJSONSeparate(Model model,
                                                                                      @RequestParam String[] geneList,
                                                                                      @RequestParam String prop1,
                                                                                      @RequestParam String prop2,
                                                                                      @RequestParam String aggProp1,
                                                                                      @RequestParam String aggProp2,
                                                                                      @RequestParam(required = false) List<String> mapValuesProp1,
                                                                                      @RequestParam String[] hitCheckbox,
                                                                                      @RequestParam(required = false) List<String> mapValuesProp2) {
        if (model == null || geneList == null || geneList.length == 0 || prop1 == null || "".equals(prop1) || prop2 == null || "".equals(prop2) || aggProp1 == null || "".equals(aggProp1) || aggProp2 == null || "".equals(aggProp2)|| hitCheckbox == null || hitCheckbox.length == 0) {
            throw new IllegalArgumentException();
        }
        List<Label> hitLabels = manageHitCheckbox(hitCheckbox);
        Map<String, Object> map = model.asMap();
        // TODO: fix it!
        List<Map<String, Object>> dataPoints = getGroupwiseScatterplotData(geneList, prop1, prop2, aggProp1, (List<String>) map.get("mapValuesProp1"), aggProp2, (List<String>) map.get("mapValuesProp2"), hitLabels);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", prop1);
        model.addAttribute("prop2", prop2);
        model.addAttribute("selectedGenes", geneList);
        return dataPoints;
    }
    protected List<Map<String, Object>> getGroupwiseScatterplotData(String[] geneList, String prop1, String prop2, String aggProp1, List<String> mapValuesProp1, String aggProp2, List<String> mapValuesProp2, List<Label> hitLabels) {

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {

            for (String geneName : geneList) {
//                Mineotaur.LOGGER.info("Gene name: " + geneName);
                Node strain = strainMap.get(geneName);
                if (strain == null) {
                    continue;
                }

                List<String> actualLabels = getActualLabels(hitLabels, strain);
//                Mineotaur.LOGGER.info(actualLabels.toString());
                /*if (actualLabels.isEmpty()) {
                    continue;
                }*/
                Double xAgg, yAgg;
                if (hasFilter) {
//                    Mineotaur.LOGGER.info(mapValuesProp1.toString());
//                    Mineotaur.LOGGER.info(mapValuesProp2.toString());
//                    listData(strain);
                    xAgg = getFilteredAggregatedData(strain, prop1, geneName, aggProp1, mapValuesProp1);
                    yAgg = getFilteredAggregatedData(strain, prop2, geneName, aggProp2, mapValuesProp2);
//                    getFilteredAggregatedData(strain, "length", geneName, aggProp2, mapValuesProp2);
//                    getFilteredAggregatedData(strain, "width", geneName, aggProp2, mapValuesProp2);

                }
                else {
                    xAgg = getAggregatedData(strain, prop1, geneName, aggProp1);
                    yAgg = getAggregatedData(strain, prop2, geneName, aggProp2);
                }

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
                String[] imageIDs = (String[]) strain.getProperty("imageIDs",null);
                map.put("imageIDs", imageIDs);
                dataPoints.add(map);

            }
        }
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
    @RequestMapping("/groupwiseDistributionFormJSON")
    protected @ResponseBody List<Map<String, Object>> getDistributionDataGenewiseJSON(Model model,
                                                                                   @RequestParam String[] geneListDist,
                                                                                   @RequestParam String propGWDist,
                                                                                   @RequestParam String aggGWDist,
                                                                                   @RequestParam(required = false) List<String> mapValuesGWDist,
                                                                                   @RequestParam String[] hitCheckboxGWDist) {

        List<Label> hitLabels = manageHitCheckbox(hitCheckboxGWDist);
        //TODO: fix it!
        List<Map<String, Object>> dataPoints = getgroupwiseDistributionFormData(geneListDist, propGWDist, aggGWDist, mapValuesGWDist, hitLabels);
        model.addAttribute("prop1", propGWDist);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("selectedGenes", geneListDist);
        return dataPoints;
    }

    protected List<Map<String, Object>> getgroupwiseDistributionFormData(String[] geneListDist, String prop1, String agg, List<String> mapValues, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            for (String geneName : geneListDist) {

                Node strain = strainMap.get(geneName);
                if (strain == null) {
                    continue;
                }
                List<String> actualLabels = getActualLabels(hitLabels, strain);
                if (actualLabels.isEmpty()) {
                    continue;
                }
                Double xAgg;
                if (hasFilter) {
                    xAgg = getFilteredAggregatedData(strain, prop1, geneName, agg, mapValues);
                }
                else {
                    xAgg = getAggregatedData(strain, prop1, geneName, agg);
                }
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
    protected @ResponseBody List<Map<String, Object>> cellwiseScatterJSON(Model model,
                                                                       @RequestParam String cellwiseProp1,
                                                                       @RequestParam String cellwiseProp2,
                                                                       @RequestParam(required = false) List<String> mapValuesCellwiseProp1,
                                                                       @RequestParam(required = false) List<String> mapValuesCellwiseProp2,
                                                                       @RequestParam String geneCWProp1) {

        Node strain1 = strainMap.get(geneCWProp1);
        List<Map<String, Object>> dataPoints = getCellwiseScatterplotData(cellwiseProp1, cellwiseProp2, mapValuesCellwiseProp1, mapValuesCellwiseProp2, strain1, geneCWProp1);
        model.addAttribute("genename", geneCWProp1);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", cellwiseProp1);
        model.addAttribute("prop2", cellwiseProp2);
        return dataPoints;
    }

    protected List<Map<String, Object>> getCellwiseScatterplotData(String prop1, String prop2, List<String> mapValuesProp1, List<String> mapValuesProp2, Node strain, String genename) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            double[] xArr, yArr;
            if (hasFilter) {
                xArr = getFilteredArrayData(strain, prop1, genename, mapValuesProp1);
                yArr = getFilteredArrayData(strain, prop2, genename, mapValuesProp2);
            }
            else {
                xArr = getArrayData(strain, prop1, genename);
                yArr = getArrayData(strain, prop2, genename);
            }
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
     * Method for providing a JSON response to a cellwise distribution plot request.
     * @param model The model.
     * @param propCWDist The selected property.
     * @param geneCWDist The selected gene.
     * @param mapValuesCWDist Checked filters for the property.
     * @return A JSON object containing the data points.
     */
    @RequestMapping("/cellwiseDistributionJSON")
    protected @ResponseBody List<Map<String, Object>> getDistributionDataCellwiseJSON(Model model,
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

    protected List<Map<String, Object>> getCellwiseDistributionData(String prop1, List<String> filter, Node strain) {
        if (strain == null || prop1 == null || "".equals(prop1))  {
            throw new IllegalArgumentException();
        }
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            double[] xArr;
            if (hasFilter) {
                xArr = getFilteredArrayData(strain, prop1, "", filter);
            }
            else {
                xArr = getArrayData(strain, prop1, "");
            }
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

    protected double[] getArrayData(Node strain, String prop1, String genename) {
        if (strain == null || prop1 == null || "".equals(prop1))  {
            throw new IllegalArgumentException();
        }
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1+"_ARRAY")).iterator();
        if (!prop1Iterator.hasNext()) {
            throw new IllegalStateException("There is no node stored for strain " + genename + " for property " + prop1);
        }
        Relationship rel = prop1Iterator.next();
        if (prop1Iterator.hasNext()) {
            throw new IllegalStateException("There are multiple nodes stored for strain " + genename + " for property " + prop1);
        }
        Node node = rel.getOtherNode(strain);
        return (double[]) node.getProperty(prop1);
    }

    protected double[] getFilteredArrayData(Node strain, String prop1, String genename, List<String> filter) {
        if (strain == null || prop1 == null || "".equals(prop1) ||  filter == null || filter.isEmpty())  {
            throw new IllegalArgumentException();
        }
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1+"_ARRAY")).iterator();
        Mineotaur.LOGGER.info(prop1 + ": " + prop1Iterator.hasNext());
        Node node = null;
        while (prop1Iterator.hasNext()) {
            Relationship rel = prop1Iterator.next();
            node = rel.getOtherNode(strain);
        }
        if (node == null) {

            throw new IllegalStateException("There is no node stored for strain " + genename + " for property " + prop1);

        }
        /*Iterator<String> props = node.getPropertyKeys().iterator();
        Mineotaur.LOGGER.info(String.valueOf(props.hasNext()));
        while (props.hasNext()) {
            Mineotaur.LOGGER.info(props.next());
        }*/
        double[] prop1Arr = (double[]) node.getProperty(prop1,null);
        if (prop1Arr == null) {
            return new double[]{};
        }
        String[] filterArr = (String[]) node.getProperty("filter");
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
        if (strain == null || prop1 == null || "".equals(prop1) || genename == null /*|| "".equals(genename)*/ || aggregate == null || "".equals(aggregate) || filter == null || filter.isEmpty())  {
            throw new IllegalArgumentException();
        }
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1)).iterator();
        Node node = null;
        while (prop1Iterator.hasNext()) {
            Relationship rel = prop1Iterator.next();
            String[] filterArr = (String[]) rel.getProperty("filter");
//            Mineotaur.LOGGER.info("filters: " + Arrays.toString(filterArr));
            if (filterArr.length != filter.size()) {
                continue;
            }
            boolean filterMatch = true;
            int count = 0;
            for (String f: filterArr) {
                if (f.equalsIgnoreCase("NaN")) {
                    continue;
                }
                if (!filter.contains(f)) {
                    filterMatch = false;
                    break;
                }
                count++;
            }
//            Mineotaur.LOGGER.info("filter size: " + count + " vs " + filter.size());
            /*boolean filterMatch = false;
            for (String f: filterArr) {
                if (filter.contains(f)) {
                    filterMatch = true;
                    break;
                }
            }*/
            if (count == filter.size() && filterMatch) {
//                Mineotaur.LOGGER.info("Filter match: " + Arrays.toString(filterArr) + " vs " + filter);
                node = rel.getOtherNode(strain);
                break;
            }
        }
        if (node == null) {
            /*prop1Iterator = strain.getRelationships().iterator();
            while (prop1Iterator.hasNext()) {
                Mineotaur.LOGGER.info(prop1Iterator.next().getType().name());
            }*/
            Mineotaur.LOGGER.warning("There is no node stored for strain " + genename + " for property " + prop1);
            return null;
        }
        Object value = node.getProperty(aggregate,null);
//        Mineotaur.LOGGER.info(prop1 + ": " + value);
        if (value instanceof Double) {
            return (Double) value;
        }
        else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        throw new UnsupportedOperationException();
    }

    protected Double getAggregatedData(Node strain, String prop1, String genename, String aggProp1) {
        if (strain == null || prop1 == null || "".equals(prop1) || genename == null || "".equals(genename) || aggProp1 == null || "".equals(aggProp1)) {
            throw new IllegalArgumentException();
        }
        Iterator<Relationship> prop1Iterator = strain.getRelationships(DynamicRelationshipType.withName(prop1)).iterator();
        //Iterator<Relationship> prop1Iterator = strain.getRelationships().iterator();

        if (!prop1Iterator.hasNext()) {
//            Mineotaur.LOGGER.info("There is no node stored for strain " + genename + " for property " + prop1);
            return null;
        }

        Relationship rel = prop1Iterator.next();
        RelationshipType rt = rel.getType();
        /*if (prop1Iterator.hasNext()) {
//            Mineotaur.LOGGER.info("There are multiple nodes stored for strain " + genename + " for property " + prop1);
            return null;
        }*/
        Node node = rel.getOtherNode(strain);
        return (Double) node.getProperty(aggProp1,null);
    }

    /**
     * Method to get a list of labels from the array of checkboxes.
     * @param hitCheckbox The checked filters.
     * @return The list of labels.
     */
    private List<Label> manageHitCheckbox(String[] hitCheckbox) {
        if (hitCheckbox == null || hitCheckbox.length == 0) {
            throw new IllegalArgumentException();
        }
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
        if (hitLabels == null || hitLabels.size() == 0 || strain == null) {
            throw new IllegalArgumentException();
        }
        List<String> actualLabels = new ArrayList<>();
        for (Label label : hitLabels) {
            if (strain.hasLabel(label)) {
                actualLabels.add(hitsByLabel.get(label));
            }
        }
        return actualLabels;
    }


    protected @ResponseBody
    List<Map<String, Object>> getDistributionDataCellwiseJSON(Model model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        Map<String, Object> map = model.asMap();
        List<String> mapValuesProp1 = null;
        if ((hasFilter)) {
            mapValuesProp1 = getMapValues(map.get("mapValuesCWDist"));
        }

        return getDistributionDataCellwiseJSON(model, (String) map.get("propCWDist"), (String) map.get("geneCWDist"), mapValuesProp1);
    }

    protected @ResponseBody
    List<Map<String, Object>> getDistributionDataGenewiseJSON(Model model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        Map<String, Object> map = model.asMap();
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

    protected @ResponseBody
    List<Map<String, Object>> getScatterPlotDataCellJSONSeparate(Model model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        Map<String, Object> map = model.asMap();
        return cellwiseScatterJSON(model, ((String) map.get("cellwiseProp1")), ((String) map.get("cellwiseProp2")), ((List<String>) map.get("mapValuesCellwiseProp1")), ((List<String>) map.get("mapValuesCellwiseProp2")), ((String) map.get("geneCWProp1")));
    }

    private List<String> getMapValues(Object o) {
        if (o == null) {
            throw new IllegalArgumentException();
            //return null;
        }
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

    protected @ResponseBody
    List<Map<String, Object>> getScatterPlotDataGeneJSONSeparate(Model model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        Map<String, Object> map = model.asMap();
        //System.out.println(map.toString());
        String[] geneList = ((String[]) map.get("geneList"));
        String prop1 = (String) map.get("prop1");
        String prop2 = (String) map.get("prop2");
        String aggProp1 = (String) map.get("aggProp1");
        String aggProp2 = (String) map.get("aggProp2");
        List<String> mapValuesProp1 = null;
        List<String> mapValuesProp2 = null;

        if (hasFilter) {
            mapValuesProp1 =  getMapValues(map.get("mapValuesProp1"));
            mapValuesProp2 =  getMapValues(map.get("mapValuesProp2"));
        }
        Object hit =  map.get("hitCheckbox");
        String[] hitCheckbox;
        if (hit instanceof String) {
            hitCheckbox = new String[]{((String) hit)};
        }
        else {
            hitCheckbox = ((String[]) hit);
        }
        return getScatterPlotDataGeneJSONSeparate(model, geneList, prop1, prop2, aggProp1, aggProp2, mapValuesProp1, hitCheckbox, mapValuesProp2);
    }


}
