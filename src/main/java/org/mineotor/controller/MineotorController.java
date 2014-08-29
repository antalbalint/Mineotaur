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

package org.mineotor.controller;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.mineotor.application.Mineotor;
import org.mineotor.provider.GraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created by balintantal on 28/05/2014.
 */
@Controller
public class MineotorController {


    private GraphDatabaseProvider provider;
    private GraphDatabaseService db;
    private GlobalGraphOperations ggo;
    private Map<String, Node> strainMap = null;
    private Map<String, Object> context;
    private List<String> aggregationModes;
    private Map<String, Label> hitsByName = null;
    private Map<Label, String> hitsByLabel = null;
    private List<String> hitNames = null;
    private List<Label> allHitLabels = null;
    private RelationshipType rt = null;
    private boolean hasFilter;
    private List<String> filters;
    private Map<String, String> texts;

    @Autowired
    public void setProvider (GraphDatabaseProvider provider) {
        this.provider = provider;
        this.db = provider.getDatabaseService();
        this.ggo = provider.getGlobalGraphOperations();
        this.context = provider.getContext();
        this.aggregationModes = provider.getAggregationModes();
        this.hitsByName = (Map<String, Label>) context.get("hitLabels");
        this.hitsByLabel = (Map<Label, String>) context.get("hitsByLabel");
        this.hitNames = (List<String>) context.get("hitNames");
        this.allHitLabels = new ArrayList<>();
        allHitLabels.addAll(hitsByLabel.keySet());
        this.strainMap = provider.getStrainsByName();
        this.rt = (RelationshipType) context.get("rel");
        this.texts = provider.getTexts();
    }

    private double getAggregation(DescriptiveStatistics stat, String agg) {
        if (agg == null) {
            throw new IllegalStateException("Wrong aggregation mode.");
        }
        else if (agg.equals(texts.get("avg"))) {
            return stat.getMean();
        }
        else if (agg.equals(texts.get("max"))) {
            return stat.getMax();
        }
        else if (agg.equals(texts.get("min"))) {
            return stat.getMin();
        }
        else if (agg.equals(texts.get("median"))) {
            return stat.getPercentile(50);
        }
        else if (agg.equals(texts.get("stdev"))) {
            return stat.getStandardDeviation();
        }
        else if (agg.equals(texts.get("number"))) {
            return stat.getN();
        }
        throw new IllegalStateException("Wrong aggregation mode.");
    }

    private List<Label> manageHitCheckbox(String[] hitCheckbox) {
        List<Label> hitLabels = new ArrayList<>();
        for (String hit : hitCheckbox) {
            hitLabels.add(hitsByName.get(hit));
        }
        return hitLabels;
    }

    private List<String> getActualLabels(List<Label> hitLabels, Node strain) {
        List<String> actualLabels = new ArrayList<>();
        for (Label label : hitLabels) {

            if (strain.hasLabel(label)) {
                actualLabels.add(hitsByLabel.get(label));
            }
        }
        return actualLabels;
    }

    private List<Map<String, Object>> getHitsDecoupled(String prop1, String prop2, List<String> mapValuesProp1, List<String> mapValuesProp2, Node strain, String genename) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            /*Iterator<Relationship> rts = strain.getRelationships().iterator();
            while (rts.hasNext()) {
                Application.LOGGER.info(rts.next().getType().toString());
            }*/
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
                    //dataPoints.add(new DataPoint(xArr[i], yArr[i], Math.log(xArr[i]), Math.log(yArr[i]), genename, Texts.WILD_TYPE));
                }
            }

            tx.success();
        }
        return dataPoints;
    }

    private List<Map<String, Object>> getHitsDecoupled(String[] geneList, String prop1, String prop2, String aggProp1, List<String> mapValuesProp1, String aggProp2, List<String> mapValuesProp2, List<Label> hitLabels) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            for (String geneName : geneList) {

                Node strain = strainMap.get(geneName);
                if (strain == null) {
                    /*Application.LOGGER.info(geneName);*/
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
                        if (stage == null || (!in2 && !in2)) {
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
                double xAgg = getAggregation(statX, aggProp1), yAgg = getAggregation(statY, aggProp2);
                /*for (HitLabels label: hitLabels) {
                    if (strain.hasLabel(label)) {*/
                Map<String, Object> map = new HashMap<>();
                map.put("x", xAgg);
                map.put("y", yAgg);
                map.put("logX", Math.log(xAgg));
                map.put("logY", Math.log(yAgg));
                map.put("name", geneName);
                map.put("labels", actualLabels);
                //map.put("label", label.toString());
                dataPoints.add(map);
                //dataPoints.add(new DataPoint(xAgg, yAgg, Math.log(xAgg), Math.log(yAgg), geneName, label.toString()));
                /*    }
                }*/
            }
            tx.success();
        }
        return dataPoints;
    }

    private List<Map<String, Object>> getHitsDecoupledMap(String[] geneListDist, String prop1, String agg, List<String> mapValues, List<Label> hitLabels) {
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
                    /*for (HitLabels label: hitLabels) {
                        if (strain.hasLabel(label)) {
                            actualLabels.add(hitNames.get(label));
                        }
                    }
                    }
                    */

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
                double xAgg = getAggregation(statX, agg);
                for (Label label : hitLabels) {
                    if (strain.hasLabel(label)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("x", xAgg);
                        map.put("logX", Math.log(xAgg));
                        map.put("name", geneName);
                        map.put("label", label.toString());
                        map.put("labels", actualLabels);
                        /*for (String s: actualLabels) {
                            map.put(s, true);
                        }*/
                        dataPoints.add(map);
                        /*// TODO: toString-> Texts
                        DataPoint dp = new DataPoint(xAgg, geneName, label.toString());
                        // TODO: precompute
                        dp.setLogX(Math.log(xAgg));
                        dataPoints.add(dp);*/
                    }
                }
            }
            tx.success();
        }
        return dataPoints;
    }

    private List<Map<String, Object>> getHitsDecoupled(String prop1, List<String> mapValues, Node strain) {
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        try (Transaction tx = db.beginTx()) {
            List<String> actualLabels = getActualLabels(allHitLabels, strain);
            /*List<String> actualLabels = new ArrayList<>();
            actualLabels.add("Wild type");*/
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


    private void addValuesToDS(Object o, DescriptiveStatistics stat) {
        if (o != null) {
            double[] arr = (double[]) o;
            for (int i = 0; i < arr.length; ++i) {
                stat.addValue(arr[i]);
            }
        }
    }

    @RequestMapping("/mineotaur")
    public String start(Model model) {
        addBasicsToModel(model);
        //model.addAttribute("labels", context.get("labels"));
        return "mineotaur";
    }

    @RequestMapping("/genewiseScatterJSON")
    public
    @ResponseBody
    List<Map<String, Object>> getScatterPlotDataGeneJSONSeparate(Model model,
                                                                 @RequestParam String[] geneList,
                                                                 @RequestParam String prop1,
                                                                 @RequestParam String prop2,
                                                                 @RequestParam String aggProp1,
                                                                 @RequestParam String aggProp2,
                                                                 @RequestParam(required = false) List<String> mapValuesProp1,
                                                                 @RequestParam String[] hitCheckbox,
                                                                 @RequestParam(required = false) List<String> mapValuesProp2) {
        //Application.LOGGER.info(Arrays.toString(geneList));
        long time = System.currentTimeMillis();
        List<Label> hitLabels = manageHitCheckbox(hitCheckbox);
        //Application.LOGGER.info(mapValuesProp1.toString());
        List<Map<String, Object>> dataPoints = getHitsDecoupled(geneList, prop1, prop2, aggProp1, mapValuesProp1, aggProp2, mapValuesProp2, hitLabels);
        /*Application.LOGGER.info(String.valueOf(System.currentTimeMillis() - time));*/
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", prop1);
        model.addAttribute("prop2", prop2);
        model.addAttribute("selectedGenes", geneList);
        return dataPoints;
    }

    @RequestMapping("/genewiseDistributionJSON")
    public
    @ResponseBody /*List<DataPoint>*/ List<Map<String, Object>> getDistributionDataGenewiseJSON(Model model,
                                                                                                @RequestParam String[] geneListDist,
                                                                                                @RequestParam String propGWDist,
                                                                                                @RequestParam String aggGWDist,
                                                                                                @RequestParam(required = false) List<String> mapValuesGWDist,
                                                                                                @RequestParam String[] hitCheckboxGWDist) {
        //Application.LOGGER.info(Arrays.toString(geneListDist));
        List<Label> hitLabels = manageHitCheckbox(hitCheckboxGWDist);
        //long time = System.currentTimeMillis();
        List<Map<String, Object>> dataPoints = getHitsDecoupledMap(geneListDist, propGWDist, aggGWDist, mapValuesGWDist, hitLabels);
        //Application.LOGGER.info(String.valueOf(System.currentTimeMillis() - time));
        model.addAttribute("prop1", propGWDist);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("selectedGenes", geneListDist);
        //Application.LOGGER.info(model.toString());
        return dataPoints;
    }

    @RequestMapping("/cellwiseScatterJSON")
    public
    @ResponseBody
    List<Map<String, Object>> cellwiseScatterJSON(Model model,
                                                                 @RequestParam String cellwiseProp1,
                                                                 @RequestParam String cellwiseProp2,
                                                                 @RequestParam(required = false) List<String> mapValuesCellwiseProp1,
                                                                 @RequestParam(required = false) List<String> mapValuesCellwiseProp2,
                                                                 @RequestParam String geneCWProp1) {



        /*String cellwiseProp1 = (String) map.get("cellwiseProp1");
        String cellwiseProp2 = (String) map.get("cellwiseProp2");
        List<String> mapValuesCellwiseProp1 = (List<String>) map.get("mapValuesCellwiseProp1");
        List<String> mapValuesCellwiseProp2 = (List<String>) map.get("mapValuesCellwiseProp2");
        String geneCWProp1 = (String) map.get("geneCWProp1");*/
        Node strain1 = strainMap.get(geneCWProp1);

        List<Map<String, Object>> dataPoints = getHitsDecoupled(cellwiseProp1, cellwiseProp2, mapValuesCellwiseProp1, mapValuesCellwiseProp2, strain1, geneCWProp1);

        model.addAttribute("genename", geneCWProp1);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", cellwiseProp1);
        model.addAttribute("prop2", cellwiseProp2);
        return dataPoints;
    }

    @RequestMapping("/cellwiseDistributionJSON")
    public
    @ResponseBody
    List<Map<String, Object>> getDistributionDataCellwiseJSON(Model model,
                                                              @RequestParam String propCWDist,
                                                              @RequestParam String geneCWDist,
                                                              @RequestParam(required = false) List<String> mapValuesCWDist
    ) {
        Node strain = strainMap.get(geneCWDist);
        List<Map<String, Object>> dataPoints = getHitsDecoupled(propCWDist, mapValuesCWDist, strain);
        model.addAttribute("genename", geneCWDist);
        model.addAttribute("dataPoints", dataPoints);
        model.addAttribute("prop1", propCWDist);
        return dataPoints;
    }

    private void addBasicsToModel(Model model) {

        Map<String, String> menu1 = new HashMap<>();
        menu1.put("cellwiseScatter", "Cell-wise scatter plot");
        menu1.put("cellwiseHistogramX", "Histogram (X axis)");
        menu1.put("cellwiseHistogramY", "Histogram (Y axis)");
        menu1.put("cellwiseKDEX", "Kernel Density Estimation (X axis)");
        menu1.put("cellwiseKDEY", "Kernel Density Estimation (Y axis)");
        Map<String, String> menu2 = new HashMap<>();
        menu2.put("analyze", "Analyze");

        model.addAttribute("menu1", menu1);
        model.addAttribute("menu2", menu2);
        List<String> features = (List<String>) context.get("features");
        Mineotor.LOGGER.info(features.toString());
        filters = (List<String>) context.get("filters");
        hasFilter = filters != null && !filters.isEmpty();
        if (hasFilter) {
            model.addAttribute("cellcycle", filters);
            Mineotor.LOGGER.info(filters.toString());
        }

            model.addAttribute("hasFilter", hasFilter);


        model.addAttribute("cellProperties", features);
        model.addAttribute("geneNames", context.get("geneNames"));
        model.addAttribute("hits", context.get("hitNames"));
        model.addAttribute("aggValues", aggregationModes);
    }


}
