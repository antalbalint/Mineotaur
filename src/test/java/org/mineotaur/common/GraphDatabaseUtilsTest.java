package org.mineotaur.common;

import org.mineotaur.provider.MockEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;
/**
 * Created by balintantal on 23/07/2015.
 */
public class GraphDatabaseUtilsTest {

    @DataProvider(name="testCreateNewGraphDatabaseServiceExcpetionDataProvider")
    public Object[][] testCreateNewGraphDatabaseServiceExcpetionDataProvider() throws Exception {
        String test = "test";
        String emptyString = "";
        return new Object[][]{
                {null, test, test},
                {emptyString, test, test},
                {test, null, test},
                {test, emptyString, test},
                {test, test, null},
                {test, test, emptyString},
        };
    }

    @Test(dataProvider = "testCreateNewGraphDatabaseServiceExcpetionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testCreateNewGraphDatabaseServiceExcpetion(String dbPath, String totalMemory, String cache) throws Exception {
        GraphDatabaseUtils.createNewGraphDatabaseService(dbPath, totalMemory, cache);
    }
    
    @DataProvider(name="testCreateNewGraphDatabaseServiceDataProvider")
    public Object[][] testCreateNewGraphDatabaseServiceDataProvider() throws Exception {
        return new Object[][]{
                {"testDB", "1G", "none"}
        };

    }
    
    @Test(dataProvider = "testCreateNewGraphDatabaseServiceDataProvider")
    public void testCreateNewGraphDatabaseService(String dbPath, String totalMemory, String cache) throws Exception {
        assertNotNull(GraphDatabaseUtils.createNewGraphDatabaseService(dbPath, totalMemory, cache));
        FileUtils.deleteDirRecursively(new File(dbPath));
    }

    @DataProvider(name="testCreateIndexServiceExcpetionDataProvider")
    public Object[][] testCreateIndexServiceExcpetionDataProvider() throws Exception {
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        Label label = mock(Label.class);
        String name = "test";
        return new Object[][]{
                {null, label, name},
                {db, null, name},
                {db, label, null},
                {db, label, ""}
        };
    }

    @Test(dataProvider = "testCreateIndexServiceExcpetionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testCreateIndexServiceExcpetion(GraphDatabaseService db, Label label, String name) throws Exception {
        GraphDatabaseUtils.createIndex(db, label, name);
    }

    @DataProvider(name="testCreateIndexServiceDataProvider")
    public Object[][] testCreateIndexServiceDataProvider() throws Exception {
        GraphDatabaseService db = MockEmbeddedGraphDatabaseProvider.newDatabaseService();
        Label label= DynamicLabel.label("test");
        String name = "test";
        return new Object[][]{
                {db, label, name}
        };

    }

    @Test(dataProvider = "testCreateIndexServiceDataProvider")
    public void testCreateIndexService(GraphDatabaseService db, Label label, String name) throws Exception {
        GraphDatabaseUtils.createIndex(db, label, name);
        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            Iterator<IndexDefinition> iterator = schema.getIndexes(label).iterator();
            assertTrue(iterator.hasNext());
            boolean equalsToName = false;
            while (iterator.hasNext()) {
                IndexDefinition id = iterator.next();
                Iterator<String> propertyKeys = id.getPropertyKeys().iterator();
                while (propertyKeys.hasNext()) {
                    if (propertyKeys.next().equals(name)) {
                        equalsToName = true;
                        break;
                    }
                }
            }
            assertTrue(equalsToName);
        }
    }
}