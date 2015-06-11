package org.mineotaur.controller;

import org.mineotaur.application.Mineotaur;
import org.mineotaur.provider.GraphDatabaseProvider;
import org.mineotaur.provider.MockEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.GraphDatabaseService;
/*
import org.neo4j.test.TestGraphDatabaseFactory;
*/
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by balintantal on 05/06/2015.
 */
public class MineotaurControllerTest {

    /*private GraphDatabaseProvider provider;
    private MineotaurController mineotaurController;
    private Model model = new ExtendedModelMap();

    @BeforeMethod
    public void setUp() throws Exception {
        provider = new MockEmbeddedGraphDatabaseProvider("input/test_input/");
        mineotaurController = new MineotaurController();
        mineotaurController.setProvider(provider);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        provider.getDatabaseService().shutdown();
    }


    @Test
    public void testStart() throws Exception {
        assertEquals("index",mineotaurController.start(model));
    }*/

    @Test
    public void testGetScatterPlotDataGeneJSONSeparate() throws Exception {

    }

    @Test
    public void testGetDistributionDataGenewiseJSON() throws Exception {

    }

    @Test
    public void testCellwiseScatterJSON() throws Exception {

    }

    @Test
    public void testGetDistributionDataCellwiseJSON() throws Exception {

    }

    @Test
    public void testGetHitsDecoupled() throws Exception {

    }

    @Test
    public void testGetArrayData() throws Exception {

    }

    @Test
    public void testGetFilteredArrayData() throws Exception {

    }

    @Test
    public void testGetFilteredAggregatedData() throws Exception {

    }

    @Test
    public void testGetAggregatedData() throws Exception {

    }

    @Test
    public void testGetHitsDecoupledOptimized() throws Exception {

    }

    @Test
    public void testGetHitsDecoupled1() throws Exception {

    }

    @Test
    public void testGetHitsDecoupledOptimized1() throws Exception {

    }

    @Test
    public void testGetHitsDecoupledMap() throws Exception {

    }

    @Test
    public void testGetHitsDecoupledMapOptimized() throws Exception {

    }

    @Test
    public void testGetHitsDecoupled2() throws Exception {

    }

    @Test
    public void testGetHitsDecoupledOptimized2() throws Exception {

    }

    @Test
    public void testAggregate() throws Exception {

    }

    @Test
    public void testGetAllHitLabels() throws Exception {

    }

    @Test
    public void testGetFilters() throws Exception {

    }

    @Test
    public void testGetMenu1() throws Exception {

    }

    @Test
    public void testGetMenu2() throws Exception {

    }

    @Test
    public void testGetFeatures() throws Exception {

    }

    @Test
    public void testGetGroupNames() throws Exception {

    }

    @Test
    public void testHasFilter() throws Exception {

    }

    @Test
    public void testGetHitNames() throws Exception {

    }

    @Test
    public void testGetAggValues() throws Exception {

    }

    @Test
    public void testDecodeQuery() throws Exception {

    }

    @Test
    public void testGetDistributionDataCellwiseJSON1() throws Exception {

    }

    @Test
    public void testGetDistributionDataGenewiseJSON1() throws Exception {

    }

    @Test
    public void testGetScatterPlotDataCellJSONSeparate() throws Exception {

    }

    @Test
    public void testGetScatterPlotDataGeneJSONSeparate1() throws Exception {

    }

    @Test
    public void testQuery() throws Exception {

    }

    @Test
    public void testEmbed() throws Exception {

    }
}