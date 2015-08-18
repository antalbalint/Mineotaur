package org.mineotaur.importer;

import org.junit.runner.RunWith;
import org.mineotaur.common.FileUtils;
import org.mineotaur.common.GraphDatabaseUtils;
import org.mineotaur.provider.MockEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Created by balintantal on 22/07/2015.
 */
@Test(groups = {"unit"})
@PrepareForTest(value = {FileUtils.class, GraphDatabaseUtils.class, GlobalGraphOperations.class})
public class DatabaseGeneratorTest extends PowerMockTestCase {

    protected DatabaseGenerator dg = new MockDatabaseGenerator();
    protected String name = "test";
    protected String confDir = "conf";
    protected boolean overwrite = false;
    protected String dbPath = name+File.separator+"db";
    protected String totalMemory = "4G";
    protected String cache = "none";
    protected int limit = (int) DefaultProperty.LIMIT.getValue();


    @DataProvider(name="testCreateDirsException")
    public String[][] testCreateDirsExceptionDataProvider() {
        String[][] params = {{name, null},  {name, ""}, {null, confDir}, {"", confDir}, {null, null}};
        return params;
    }

    @Test(dataProvider = "testCreateDirsException", expectedExceptions = IllegalArgumentException.class)
    public void testCreateDirsException(String name, String confDir) throws Exception {
        dg.createDirs(name, confDir, overwrite);
    }

    @Test
    public void testCreateDirs() throws Exception {
        PowerMockito.mockStatic(FileUtils.class);
        dg.createDirs(name, confDir, overwrite);
        PowerMockito.verifyStatic();
        FileUtils.createDir(name, overwrite);
        PowerMockito.verifyStatic();
        FileUtils.createDir(confDir, overwrite);
    }

    @DataProvider(name="testCreateRelationshipTypesException")
    public String[][] testCreateRelationshipTypesExceptionDataProvider() {
        String[][] params = {{""}, {null}, {"XXXXYY"}, {"-XXX"}, {"YYYY-"}, {"XXX--YYYY"}, {"XXX-YYY,ZZZ-"}, {"XXX-YYY,-ZZZ"}};
        return params;
    }

    @Test(dataProvider = "testCreateRelationshipTypesException", expectedExceptions = IllegalArgumentException.class)
    public void testCreateRelationshipTypesException(String rels) throws Exception {
        dg.createRelationshipTypes(rels);
    }

    @DataProvider(name="testCreateRelationshipTypes")
    public Object[][] testCreateRelationshipTypesDataProvider() {
        Object[][] params = {{"XXX-XXX",1}, {"XXX-XXX,YYY-YYY",2}, {"XXX-ZZZ,XXX-YYY",1}};
        return params;
    }

    @Test(dataProvider = "testCreateRelationshipTypes")
    public void testCreateRelationshipTypes(String rels, int number) throws Exception {
        assertEquals(number, dg.createRelationshipTypes(rels).size());
    }

    @DataProvider(name="testStartDBException")
    public Object[][] testStartDBException() {
        Object[][] params = {{null, totalMemory, cache}, {"", totalMemory, cache}, {dbPath, null, cache},{dbPath, "", cache},{dbPath, totalMemory, null},{dbPath, totalMemory, ""},{dbPath, "4", cache},{dbPath, "G", cache},{dbPath, "4M", cache},{dbPath, "10G", cache},};
        return params;
    }

    @Test(dataProvider = "testStartDBException", expectedExceptions = IllegalArgumentException.class)
    public void testStartDBException(String dbPath, String totalMemory, String cache) throws Exception {
        dg.startDB(dbPath, totalMemory, cache);
    }


    @DataProvider(name="testStartDB")
    public Object[][] testStartDB() {
        Object[][] params = {{dbPath, totalMemory, cache}};
        return params;
    }

    @Test(dataProvider = "testStartDB")
    public void testStartDB(String dbPath, String totalMemory, String cache) throws Exception {
        PowerMockito.mockStatic(GraphDatabaseUtils.class);
        PowerMockito.mockStatic(GlobalGraphOperations.class);
        dg.startDB(dbPath, totalMemory, cache);
        PowerMockito.verifyStatic();
        GraphDatabaseService db = GraphDatabaseUtils.createNewGraphDatabaseService(dbPath, totalMemory, cache);
        PowerMockito.verifyStatic();
        GlobalGraphOperations.at(db);
    }


}