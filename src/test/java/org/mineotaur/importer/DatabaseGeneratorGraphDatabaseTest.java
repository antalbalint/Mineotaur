package org.mineotaur.importer;

import org.mineotaur.provider.MockEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by balintantal on 05/08/2015.
 */
public class DatabaseGeneratorGraphDatabaseTest {

    protected DatabaseGenerator dg = new MockDatabaseGenerator();

    protected int limit = (int) DefaultProperty.LIMIT.getValue();
    protected Label groupLabel = DynamicLabel.label("GROUP");
    protected Label descriptiveLabel = DynamicLabel.label("DESCRIPTIVE");
    protected String filterProp = "filter";


    @DataProvider(name="testCreateFiltersExceptionDataProvider")
    public Object[][] testCreateFiltersExceptionDataProvider() {
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        GlobalGraphOperations ggo = mock(GlobalGraphOperations.class);
        Label groupLabel = mock(Label.class);
        Label descriptiveLabel = mock(Label.class);
        RelationshipType relationshipType = mock(RelationshipType.class);
        List<String> filterProps = mock(List.class);
        List zeroList = mock(List.class);
        when(zeroList.size()).thenReturn(0);
        Map zeroMap = mock(Map.class);
        when(zeroMap.size()).thenReturn(0);
        Object[][] params = {
                {null, ggo, groupLabel, descriptiveLabel, relationshipType, filterProps, limit},
                {db, null, groupLabel, descriptiveLabel, relationshipType, filterProps, limit},
                {db, ggo, null, descriptiveLabel, relationshipType, filterProps, limit},
                {db, ggo, groupLabel, null, relationshipType, filterProps, limit},
                {db, ggo, groupLabel, descriptiveLabel, null, filterProps, limit},
                {db, ggo, groupLabel, descriptiveLabel, zeroMap, filterProps, limit},
                {db, ggo, groupLabel, descriptiveLabel, relationshipType, null, limit},
                {db, ggo, groupLabel, descriptiveLabel, relationshipType, zeroList, limit},
        };
        return params;
    }

    @Test(dataProvider = "testCreateFiltersExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testCreateFiltersException(Object db, Object ggo, Object groupLabel, Object descriptiveLabel, RelationshipType relationshipType, List<String> filterProps, int limit) throws Exception {
        dg.createFilters((GraphDatabaseService)db, (GlobalGraphOperations)ggo, (Label)groupLabel, (Label)descriptiveLabel, relationshipType, filterProps, limit);
    }

    @DataProvider(name="testCreateFiltersDataProvider")
    public Object[][] testCreateFiltersDataProvider() {
        GraphDatabaseService db = MockEmbeddedGraphDatabaseProvider.newDatabaseService();
        GlobalGraphOperations ggo = GlobalGraphOperations.at(db);

        String[] values = {"1", "2", "3", null};
        RelationshipType relationshipType = DynamicRelationshipType.withName("RT");
        try (Transaction tx = db.beginTx()) {
            Node node = db.createNode(groupLabel);
            for (String s: values) {
                Node desc = db.createNode(descriptiveLabel);
                if (s!= null) {
                    desc.setProperty(filterProp, s);
                }
                desc.createRelationshipTo(node, relationshipType);
            }
            tx.success();
        }
        List<String> filterProps = new ArrayList<>();
        filterProps.add(filterProp);

        Object[][] params = {
                {db, ggo, groupLabel, descriptiveLabel, relationshipType, filterProps, limit, Arrays.asList(values)},

        };
        return params;
    }

    @Test(dataProvider = "testCreateFiltersDataProvider")
    public void testCreateFilters(Object _db, Object _ggo, Object _groupLabel, Object _descriptiveLabel, RelationshipType relationshipType, List<String> filterProps, int limit, List<String> values) throws Exception {
        GraphDatabaseService db = (GraphDatabaseService) _db;
        GlobalGraphOperations ggo = (GlobalGraphOperations) _ggo;
        Label groupLabel = (Label) _groupLabel;
        Label descriptiveLabel = (Label) _descriptiveLabel;

        dg.createFilters(db, ggo, groupLabel, descriptiveLabel, relationshipType, filterProps, limit);
        int groupCount = 1;
        int descriptiveCount = values.size();
        try (Transaction tx = db.beginTx()) {
            Iterator<Node> groups = ggo.getAllNodesWithLabel(groupLabel).iterator();
            while (groups.hasNext()) {
                Node group = groups.next();
                groupCount--;
                Iterator<Relationship> relationshipIterator = group.getRelationships(relationshipType).iterator();
                while (relationshipIterator.hasNext()) {
                    Relationship rel = relationshipIterator.next();
                    descriptiveCount--;
                    Node descriptive = rel.getOtherNode(group);
                    assertTrue(descriptive.hasLabel(descriptiveLabel));
                    String filterProp = (String) rel.getProperty(filterProps.get(0), null);
                    assertTrue(values.contains(filterProp));
                }
            }
        }
        assertEquals(groupCount, 0);
        assertEquals(descriptiveCount, 0);
    }

    @DataProvider(name="testPrecomputeOptimizedExceptionDataProvider")
     public Object[][] testPrecomputeOptimizedExceptionDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testPrecomputeOptimizedExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testPrecomputeOptimizedException() {
    }

    @DataProvider(name="testPrecomputeOptimizedDataProvider")
    public Object[][] testPrecomputeOptimizedDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testPrecomputeOptimizedDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testPrecomputeOptimized() {
    }

    @DataProvider(name="testGenerateGroupnameListExceptionDataProvider")
    public Object[][] testGenerateGroupnameListExceptionDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGenerateGroupnameListExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGenerateGroupnameListException() {
    }

    @DataProvider(name="testGenerateGroupnameListDataProvider")
    public Object[][] testGenerateGroupnameListDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGenerateGroupnameListDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGenerateGroupnameList() {
    }

    @DataProvider(name="testGenerateFilterListExceptionDataProvider")
    public Object[][] testGenerateFilterListExceptionDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGenerateFilterListExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGenerateFilterListException() {
    }

    @DataProvider(name="testGenerateFilterListDataProvider")
    public Object[][] testGenerateFilterListDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGenerateFilterListDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGenerateFilterList() {
    }

    @DataProvider(name="testGeneratePropertyFileExceptionDataProvider")
    public Object[][] testGeneratePropertyFileExceptionDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGeneratePropertyFileExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGeneratePropertyFileException() {
    }

    @DataProvider(name="testGeneratePropertyFileDataProvider")
    public Object[][] testGeneratePropertyFileDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGeneratePropertyFileDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGeneratePropertyFile() {
    }

    @DataProvider(name="testGetImageIDsExceptionDataProvider")
    public Object[][] testGetImageIDsExceptionDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGetImageIDsExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGetImageIDsException() {
    }

    @DataProvider(name="testGetImageIDsDataProvider")
    public Object[][] testGetImageIDsDataProvider() {
        return new Object[][] {{}};
    }

    @Test(dataProvider="testGetImageIDsDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testGetImageIDs() {
    }
    
}
