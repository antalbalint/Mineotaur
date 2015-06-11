package org.mineotaur.importer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.mineotaur.application.Mineotaur;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static org.testng.Assert.*;


/**
 * Created by balintantal on 05/06/2015.
 */
public class DatabaseGeneratorTest {

    private DatabaseGenerator dg = new MockDatabaseGenerator("input/test_input/mineotaur.input");;

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterTest
    public void tearDown() throws Exception {
        new File(dg.confDir).delete();
        new File(dg.name).delete();
    }

    @Test
    public void testProcessProperties() throws Exception {
        assertNotNull(dg.properties);
        assertEquals("Test", dg.name);
        assertEquals("GENE", dg.group);
        assertEquals("GeneSymbol", dg.groupName);
        assertNotNull(dg.groupLabel);
        assertEquals("CELL", dg.descriptive);
        assertNotNull(dg.descriptiveLabel);
        assertNotNull(dg.unique);
        assertEquals(1, dg.unique.size());
        assertEquals("CELL", dg.unique.get(0));
        assertEquals("\t", dg.separator);
//        assertEquals("3G", dg.totalMemory);
        assertEquals("none", dg.cache);
        assertEquals("Test/conf/", dg.confDir);
        assertEquals("Test/db/",dg.dbPath);
        assertEquals(true, dg.toPrecompute);
        assertEquals(5000,dg.limit);
    }

    @Test
    public void testCreateDirs() throws Exception {
        System.out.println(dg.name);
        assertTrue(new File(dg.name).exists());
        assertTrue(new File(dg.confDir).exists());
    }

    @Test
    public void testCreateRelationships() throws Exception {
        assertNotNull(dg.relationships);
        assertEquals(1, dg.relationshipCount);
        String rels = dg.properties.getString("relationships");
        String[] terms = rels.split(",");
        assertEquals(2, terms.length);
        for (String term: terms) {
            String[] nodeNames = term.split("-");
            assertEquals(2, nodeNames.length);
            RelationshipType rt = dg.relationships.get(nodeNames[0]).get(nodeNames[1]);
            assertNotNull(rt);
            assertEquals(rt.name(),nodeNames[0] + "_AND_" + nodeNames[1]);
        }
    }

    @Test
    public void testStartDB() throws Exception {
        dg.startDB();
        assertNotNull(dg.db);
    }


    @Test
    public void testGenerateDatabase() throws Exception {
//        dg.generateDatabase("input/test_input/chia_sample.txt", "input/test_input/chia_labels.txt");
    }

    @Test(dependsOnMethods = {"testStartDB"})
    public void testProcessMetadata() throws Exception {
        dg.processMetadata("input/test_input/chia_sample.txt");
        assertEquals(630, dg.header.length);
        assertEquals(630, dg.nodeTypes.length);
        assertEquals(630, dg.dataTypes.length);
        assertEquals(3, dg.classCount);
        assertTrue(dg.keySet.contains("GENE"));
        assertTrue(dg.keySet.contains("CELL"));
        assertTrue(dg.keySet.contains("EXPERIMENT"));
        for (String key: dg.keySet) {
            List<Integer> indices = dg.signatures.get(key);
            if ("GENE".equals(key)) {
                assertEquals(2, indices.size());
            }
            else if ("EXPERIMENT".equals(key)) {
                assertEquals(4, indices.size());
            }
            else if ("CELL".equals(key)) {
                assertEquals(624, indices.size());
            }
        }
        assertEquals(2, dg.ids.size());
        assertEquals(0, dg.filterProps.size());
    }

    private List<Integer> indices = Arrays.asList(new Integer[] {0,2,3});
    private String[] header = {"a","b","c","d"};
    private String[] dataTypes = {"ID", "NUMBER","NUMBER","TEXT"};
    private List<String> idFields = Arrays.asList(new String[]{"a"});
    private Class testClass;

    @Test
    public void testCreateClass() throws Exception {
        /*L
        Class claz = dg.createClass(ClassPool.getDefault(), "test", indices, header, dataTypes, idFields);*/
        if (testClass == null) {
            testClass = createTestClass();
        }
        assertNotNull(testClass);
        Field[] fields = testClass.getDeclaredFields();
        assertEquals(fields.length, indices.size());
        assertEquals(fields[0].getType(), java.lang.String.class);
        assertEquals(fields[1].getType(), double.class);
        assertEquals(fields[2].getType(), java.lang.String.class);
        assertNotNull(testClass.getDeclaredMethod("equals", java.lang.Object.class));
    }

    private Class createTestClass() throws NotFoundException, CannotCompileException {
        List<String> idFields = Arrays.asList(new String[]{"a"});
        return dg.createClass(ClassPool.getDefault(), "test", indices, header, dataTypes, idFields);
    }

    private Object createTestObject(Class claz, String idField, String id) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Object o = claz.newInstance();
        Field f = claz.getDeclaredField(idField);
        f.set(o, id);
        return o;
    }

    @Test(dependsOnMethods = {"testProcessMetadata"})
    public void testGenerateClasses() throws Exception {
        dg.generateClasses();
        assertEquals(dg.classes.size(), 3);
        assertTrue(dg.classes.containsKey("GENE"));
        assertTrue(dg.classes.containsKey("CELL"));
        assertTrue(dg.classes.containsKey("EXPERIMENT"));
    }

    @Test
    public void testBuildEquals() throws Exception {
        if (testClass == null) {
            testClass = createTestClass();
        }
        String idField = idFields.get(0);
        Object o1 = createTestObject(testClass, idField, "1");
        assertFalse(o1.equals(null));
        assertTrue(o1.equals(o1));
        Object o2 = createTestObject(testClass, idField, "2");
        assertFalse(o2.equals(null));
        assertFalse(o1.equals(o2));
        assertFalse(o2.equals(o1));
        Object o3 = createTestObject(testClass, idField, "1");
        assertFalse(o3.equals(null));
        assertTrue(o1.equals(o3));
        assertTrue(o3.equals(o1));
        assertFalse(o2.equals(o3));
        assertFalse(o3.equals(o2));
        String s ="s";
        assertFalse(o1.equals(s));
        assertFalse(s.equals(o1));
        assertFalse(o2.equals(s));
        assertFalse(s.equals(o2));
        assertFalse(o2.equals(s));
        assertFalse(s.equals(o2));
    }

    @Test(dependsOnMethods = {"testGenerateClasses"})
    public void testGenerateObjectsFromLine() throws Exception {
        String line = "Rep1\t1\tO03\t-1\tcontrol\t3\t3.00E+00\t1.00E+00\t8.34E+03\t1.40E+03\t1.24E+03\t3.08E+02\t1.23E+02\t1.35E+03\t1.46E+03\t1.19E+03\t1.29E+03\t2.14E+00\t4.54E+02\t1.73E+04\t-2.78E+02\t2.12E+03\t1.77E+07\t4.41E+03\t2.37E+00\t8.54E-02\t1.75E+00\t2.30E-01\t6.18E-02\t2.26E-01\t2.14E-01\t2.47E-01\t9.96E-03\t1.10E+00\t1.54E-01\t3.57E-01\t4.61E-02\t6.92E-03\t2.36E-01\t1.61E-01\t1.39E-01\t8.24E-02\t1.72E-02\t4.29E-03\t7.42E-01\t7.83E-02\t2.66E-01\t1.15E-01\t5.02E-02\t1.02E-02\t2.76E-03\t5.71E-01\t1.39E-01\t3.65E-01\t1.69E-01\t8.63E-02\t9.98E-03\t7.24E-03\t3.07E-03\t3.31E-04\t9.42E+02\t6.71E-01\t1.43E+03\t8.39E-02\t1.26E+02\t1.87E+04\t7.83E+00\t1.19E+01\t4.46E+02\t5.85E+00\t-1.93E-01\t9.60E-01\t-2.49E+00\t6.75E-02\t6.65E+01\t-8.36E+00\t4.89E-02\t3.70E+01\t-2.33E+01\t7.90E-02\t8.61E+01\t-3.86E+01\t1.36E-01\t1.74E+02\t-1.68E+01\t2.26E-01\t3.97E+02\t4.19E+01\t2.50E-01\t3.74E+02\t-5.93E+01\t1.04E-01\t6.89E+01\t2.23E+03\t1.66E-02\t1.63E+00\t2.38E+00\t3.33E+00\t1.40E+00\t8.43E-01\t1.26E+00\t5.67E-01\t2.27E+00\t4.03E+00\t5.67E-01\t4.39E+00\t9.49E+00\t5.67E-01\t1.19E+01\t2.07E+01\t5.67E-01\t1.72E+01\t3.21E+01\t1.40E+00\t2.63E-01\t1.06E+00\t5.67E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.11E+02\t2.16E+04\t-6.87E+02\t7.28E+02\t6.07E+06\t2.17E+03\t2.12E+00\t3.63E-01\t3.50E-01\t1.19E+00\t2.00E-01\t1.34E+00\t9.38E-01\t7.69E-01\t6.20E-02\t4.59E-01\t3.09E-01\t1.38E+00\t3.53E-01\t1.25E-01\t1.18E+00\t1.09E+00\t1.13E+00\t8.77E-01\t1.85E-01\t4.05E-02\t1.05E+00\t5.53E-01\t2.45E-01\t1.07E+00\t4.84E-01\t5.30E-02\t6.19E-02\t5.84E-01\t6.88E-01\t1.25E+00\t5.56E-01\t6.59E-01\t8.27E-02\t2.51E-02\t8.73E-02\t3.13E-04\t3.74E+02\t8.62E-01\t1.35E+03\t9.89E-02\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.60E+02\t5.32E+00\t-2.31E-01\t9.80E-01\t-1.67E-01\t9.97E-02\t4.87E+01\t-3.63E-01\t5.35E-02\t4.73E+01\t-1.13E+00\t9.02E-02\t1.30E+02\t-5.10E+00\t1.31E-01\t1.83E+02\t-1.43E+01\t1.81E-01\t1.77E+02\t-1.33E+01\t1.69E-01\t8.99E+01\t-9.48E+01\t4.50E-02\t6.72E+00\t8.57E+02\t1.18E-02\t4.19E-01\t1.65E+00\t2.29E+00\t1.40E+00\t9.48E-01\t1.41E+00\t9.59E-01\t3.60E+00\t7.09E+00\t5.67E-01\t5.96E+00\t1.30E+01\t5.67E-01\t5.77E+00\t8.39E+00\t1.40E+00\t3.27E+00\t4.52E+00\t1.00E+00\t3.78E-02\t8.38E-02\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t3.54E+02\t7.26E+03\t-2.90E+02\t1.58E+02\t1.32E+06\t7.32E+02\t2.05E+00\t5.56E-01\t2.06E-01\t1.90E+00\t2.93E-01\t1.92E+00\t1.94E+00\t1.27E+00\t7.30E-02\t9.39E-01\t2.47E-01\t2.39E+00\t5.85E-01\t3.41E-02\t1.65E+00\t2.13E+00\t1.94E+00\t1.50E+00\t2.57E-01\t4.33E-03\t1.89E+00\t1.45E+00\t3.37E-01\t1.97E+00\t7.61E-01\t5.35E-02\t2.93E-02\t6.18E-01\t9.87E-01\t2.06E+00\t1.25E+00\t1.18E+00\t2.45E-01\t4.27E-02\t4.06E-02\t5.28E-04\t1.84E+03\t3.30E-01\t1.38E+03\t7.00E-02\t1.27E+02\t1.79E+04\t7.74E+00\t1.13E+01\t7.27E+02\t6.39E+00\t-9.73E-02\t8.28E-01\t-3.31E-02\t1.28E-01\t2.73E+01\t-5.98E-02\t5.61E-02\t1.76E+01\t-1.82E-01\t8.91E-02\t5.27E+01\t-1.75E+00\t1.32E-01\t7.82E+01\t-1.57E+01\t1.71E-01\t6.03E+01\t-5.56E+01\t1.58E-01\t2.68E+01\t-8.79E+01\t3.97E-02\t2.03E+00\t3.19E+02\t1.93E-02\t3.73E-01\t9.85E-01\t1.41E+00\t1.40E+00\t3.27E-01\t5.67E-01\t5.67E-01\t1.39E+00\t2.98E+00\t5.67E-01\t2.64E+00\t5.73E+00\t5.67E-01\t2.08E+00\t3.43E+00\t5.67E-01\t1.57E+00\t2.63E+00\t9.59E-01\t1.40E-01\t6.46E-01\t1.00E+00\t0.00E+00\t0.00E+00\t1.00E+00\t1.22E+03\t4.20E+04\t-1.05E+03\t1.34E+03\t1.11E+07\t3.77E+03\t2.11E+00\t3.26E-01\t4.26E-01\t1.07E+00\t1.78E-01\t1.24E+00\t8.77E-01\t6.88E-01\t7.59E-02\t5.10E-01\t2.23E-01\t1.28E+00\t3.19E-01\t1.32E-01\t1.03E+00\t9.84E-01\t1.10E+00\t7.88E-01\t1.52E-01\t5.11E-02\t1.04E+00\t6.08E-01\t1.40E-01\t9.84E-01\t4.22E-01\t5.42E-02\t7.58E-02\t4.50E-01\t5.12E-01\t1.13E+00\t5.58E-01\t5.73E-01\t9.88E-02\t2.91E-02\t8.50E-02\t3.29E-04\t3.06E+02\t8.87E-01\t1.35E+03\t1.10E-01\t1.28E+02\t1.96E+04\t7.96E+00\t1.22E+01\t1.32E+02\t5.18E+00\t-2.43E-01\t9.83E-01\t3.17E-01\t9.36E-02\t8.12E+01\t1.56E-01\t5.45E-02\t8.82E+01\t1.53E-01\t8.92E-02\t2.17E+02\t9.22E-01\t1.29E-01\t3.01E+02\t1.76E+01\t1.81E-01\t2.95E+02\t8.44E+01\t1.73E-01\t1.62E+02\t-9.63E+01\t4.09E-02\t8.98E+00\t1.33E+03\t1.15E-02\t6.90E-01\t2.69E+00\t3.77E+00\t1.40E+00\t2.00E+00\t3.18E+00\t1.40E+00\t6.14E+00\t1.13E+01\t5.67E-01\t9.86E+00\t2.10E+01\t5.67E-01\t9.45E+00\t1.42E+01\t1.40E+00\t5.61E+00\t7.69E+00\t1.00E+00\t4.72E-02\t1.34E-01\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.45E-01\t7.97E-01\t1.84E+02\t1.60E+03\t3.00E+01\t3.00E+01\t3.00E+01\t9.83E+00\t1.23E+01\t0.00E+00\t1.00E+00\t4.45E-01\t6.48E-01\t1.46E+02\t2.96E+02\t1.88E+01\t1.33E+01\t2.06E+01\t6.61E+00\t1.02E+01\t1.50E+00\t3.00E+00\t2.68E-01\t3.08E-01\t9.20E+01\t1.20E+02\t2.19E+01\t2.19E+01\t2.30E+01\t2.07E+00\t6.73E+00\t1.96E-01\t1.00E+00\t5.52E-01\t3.03E-01\t7.60E+01\t1.62E+02\t2.11E+01\t2.11E+01\t2.22E+01\t1.95E+00\t6.44E+00\t2.18E-01\t1.00E+00\t2.48E-01\t1.86E-01\t5.40E+01\t5.70E+01\t2.10E+01\t2.10E+01\t2.21E+01\t1.27E+00\t6.83E+00\t1.77E-01\t2.00E+00\t4.24E-01\t5.79E-01\t1.48E+02\t3.08E+02\t1.76E+01\t1.18E+01\t2.02E+01\t6.56E+00\t1.13E+01\t1.48E+00\t3.00E+00\t2.56E-01\t8.51E-01\t1.28E+02\t1.31E+02\t2.05E+01\t1.87E+01\t2.13E+01\t5.76E+00\t6.77E+00\t2.37E-01\t6.00E+00\t0.00E+00\t5.70E-02\t5.01E-03\t2.13E-02\t5.01E-03\t6.33E-02\t1.57E-02\t3.07E-01\t0.00E+00\t4.02E-01\t5.47E-01\t1.93E-01\t9.09E-01\t4.29E-01\t6.67E-02\t9.92E-01\t0.00E+00\t8.92E-01\t4.67E-01\t9.92E-01\t8.33E-01\t2.10E-01\t1.00E+00\t6.60E-01\t0.00E+00\t3.52E-01\t9.88E-01\t6.30E-01\t1.40E-01\t1.00E+00\t9.82E-01\t1.00E+00\t0.00E+00\t1.00E+00\t8.77E-01\t3.28E-01\t8.73E-01\t3.86E-01\t5.19E-01\t1.85E-01\t0.00E+00\t4.16E-01\t1.91E-01\t9.69E-01\t7.63E-01\t7.79E-01\t3.82E-01\t9.77E-01\t0.00E+00\t1.78E-04\t9.94E+02\t6.44E-01\t1.39E+03\t4.53E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.98E+02\t6.02E+00\t-1.60E-01\t9.43E-01\t2.00E-04\t1.95E+03\t3.07E-01\t1.40E+03\t3.19E-02\t1.26E+02\t1.77E+04\t7.83E+00\t1.25E+01\t7.08E+02\t6.49E+00\t-1.17E-01\t8.89E-01\t1.83E-04\t9.47E+02\t6.60E-01\t1.39E+03\t4.70E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.86E+02\t5.98E+00\t-1.64E-01\t9.46E-01\t2.69E-04\t1.50E+03\t4.49E-01\t1.36E+03\t6.38E-02\t1.28E+02\t1.83E+04\t7.86E+00\t1.25E+01\t6.07E+02\t6.29E+00\t-1.59E-01\t9.40E-01\t3.08E-04\t3.48E+02\t8.71E-01\t1.35E+03\t1.01E-01\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.49E+02\t5.27E+00\t-2.33E-01\t9.80E-01\t2.77E-04\t1.49E+03\t4.53E-01\t1.36E+03\t6.65E-02\t1.28E+02\t1.83E+04\t7.87E+00\t1.25E+01\t6.05E+02\t6.28E+00\t-1.63E-01\t9.44E-01\t\t\t\t\t\t\t\t\t";
        String[] terms = line.split(dg.separator);
        Map<String, Object> data = dg.generateObjectsFromLine(terms);
        assertEquals(data.size(), dg.classCount);
        for (String key: dg.keySet) {
            Object o = data.get(key);
            Class c = o.getClass();
            List<Integer> signature = dg.signatures.get(key);
            for (Integer i: signature) {
                String h = dg.header[i];
                String d = dg.dataTypes[i];
                Class fieldType;
                if (dg.NUMBER.equals(d)) {
                    fieldType = double.class;
                }
                else {
                    fieldType = String.class;
                }
                Field f = c.getField(h);
                assertNotNull(f);
                assertEquals(f.getType(),fieldType);
            }
        }
    }

    @Test(dependsOnMethods = {"testCreateNode","testLookupNode"})
    public void testGetNodesForObjects() throws Exception {
        String line = "Rep1\t1\tO03\t-1\tcontrol\t3\t3.00E+00\t1.00E+00\t8.34E+03\t1.40E+03\t1.24E+03\t3.08E+02\t1.23E+02\t1.35E+03\t1.46E+03\t1.19E+03\t1.29E+03\t2.14E+00\t4.54E+02\t1.73E+04\t-2.78E+02\t2.12E+03\t1.77E+07\t4.41E+03\t2.37E+00\t8.54E-02\t1.75E+00\t2.30E-01\t6.18E-02\t2.26E-01\t2.14E-01\t2.47E-01\t9.96E-03\t1.10E+00\t1.54E-01\t3.57E-01\t4.61E-02\t6.92E-03\t2.36E-01\t1.61E-01\t1.39E-01\t8.24E-02\t1.72E-02\t4.29E-03\t7.42E-01\t7.83E-02\t2.66E-01\t1.15E-01\t5.02E-02\t1.02E-02\t2.76E-03\t5.71E-01\t1.39E-01\t3.65E-01\t1.69E-01\t8.63E-02\t9.98E-03\t7.24E-03\t3.07E-03\t3.31E-04\t9.42E+02\t6.71E-01\t1.43E+03\t8.39E-02\t1.26E+02\t1.87E+04\t7.83E+00\t1.19E+01\t4.46E+02\t5.85E+00\t-1.93E-01\t9.60E-01\t-2.49E+00\t6.75E-02\t6.65E+01\t-8.36E+00\t4.89E-02\t3.70E+01\t-2.33E+01\t7.90E-02\t8.61E+01\t-3.86E+01\t1.36E-01\t1.74E+02\t-1.68E+01\t2.26E-01\t3.97E+02\t4.19E+01\t2.50E-01\t3.74E+02\t-5.93E+01\t1.04E-01\t6.89E+01\t2.23E+03\t1.66E-02\t1.63E+00\t2.38E+00\t3.33E+00\t1.40E+00\t8.43E-01\t1.26E+00\t5.67E-01\t2.27E+00\t4.03E+00\t5.67E-01\t4.39E+00\t9.49E+00\t5.67E-01\t1.19E+01\t2.07E+01\t5.67E-01\t1.72E+01\t3.21E+01\t1.40E+00\t2.63E-01\t1.06E+00\t5.67E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.11E+02\t2.16E+04\t-6.87E+02\t7.28E+02\t6.07E+06\t2.17E+03\t2.12E+00\t3.63E-01\t3.50E-01\t1.19E+00\t2.00E-01\t1.34E+00\t9.38E-01\t7.69E-01\t6.20E-02\t4.59E-01\t3.09E-01\t1.38E+00\t3.53E-01\t1.25E-01\t1.18E+00\t1.09E+00\t1.13E+00\t8.77E-01\t1.85E-01\t4.05E-02\t1.05E+00\t5.53E-01\t2.45E-01\t1.07E+00\t4.84E-01\t5.30E-02\t6.19E-02\t5.84E-01\t6.88E-01\t1.25E+00\t5.56E-01\t6.59E-01\t8.27E-02\t2.51E-02\t8.73E-02\t3.13E-04\t3.74E+02\t8.62E-01\t1.35E+03\t9.89E-02\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.60E+02\t5.32E+00\t-2.31E-01\t9.80E-01\t-1.67E-01\t9.97E-02\t4.87E+01\t-3.63E-01\t5.35E-02\t4.73E+01\t-1.13E+00\t9.02E-02\t1.30E+02\t-5.10E+00\t1.31E-01\t1.83E+02\t-1.43E+01\t1.81E-01\t1.77E+02\t-1.33E+01\t1.69E-01\t8.99E+01\t-9.48E+01\t4.50E-02\t6.72E+00\t8.57E+02\t1.18E-02\t4.19E-01\t1.65E+00\t2.29E+00\t1.40E+00\t9.48E-01\t1.41E+00\t9.59E-01\t3.60E+00\t7.09E+00\t5.67E-01\t5.96E+00\t1.30E+01\t5.67E-01\t5.77E+00\t8.39E+00\t1.40E+00\t3.27E+00\t4.52E+00\t1.00E+00\t3.78E-02\t8.38E-02\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t3.54E+02\t7.26E+03\t-2.90E+02\t1.58E+02\t1.32E+06\t7.32E+02\t2.05E+00\t5.56E-01\t2.06E-01\t1.90E+00\t2.93E-01\t1.92E+00\t1.94E+00\t1.27E+00\t7.30E-02\t9.39E-01\t2.47E-01\t2.39E+00\t5.85E-01\t3.41E-02\t1.65E+00\t2.13E+00\t1.94E+00\t1.50E+00\t2.57E-01\t4.33E-03\t1.89E+00\t1.45E+00\t3.37E-01\t1.97E+00\t7.61E-01\t5.35E-02\t2.93E-02\t6.18E-01\t9.87E-01\t2.06E+00\t1.25E+00\t1.18E+00\t2.45E-01\t4.27E-02\t4.06E-02\t5.28E-04\t1.84E+03\t3.30E-01\t1.38E+03\t7.00E-02\t1.27E+02\t1.79E+04\t7.74E+00\t1.13E+01\t7.27E+02\t6.39E+00\t-9.73E-02\t8.28E-01\t-3.31E-02\t1.28E-01\t2.73E+01\t-5.98E-02\t5.61E-02\t1.76E+01\t-1.82E-01\t8.91E-02\t5.27E+01\t-1.75E+00\t1.32E-01\t7.82E+01\t-1.57E+01\t1.71E-01\t6.03E+01\t-5.56E+01\t1.58E-01\t2.68E+01\t-8.79E+01\t3.97E-02\t2.03E+00\t3.19E+02\t1.93E-02\t3.73E-01\t9.85E-01\t1.41E+00\t1.40E+00\t3.27E-01\t5.67E-01\t5.67E-01\t1.39E+00\t2.98E+00\t5.67E-01\t2.64E+00\t5.73E+00\t5.67E-01\t2.08E+00\t3.43E+00\t5.67E-01\t1.57E+00\t2.63E+00\t9.59E-01\t1.40E-01\t6.46E-01\t1.00E+00\t0.00E+00\t0.00E+00\t1.00E+00\t1.22E+03\t4.20E+04\t-1.05E+03\t1.34E+03\t1.11E+07\t3.77E+03\t2.11E+00\t3.26E-01\t4.26E-01\t1.07E+00\t1.78E-01\t1.24E+00\t8.77E-01\t6.88E-01\t7.59E-02\t5.10E-01\t2.23E-01\t1.28E+00\t3.19E-01\t1.32E-01\t1.03E+00\t9.84E-01\t1.10E+00\t7.88E-01\t1.52E-01\t5.11E-02\t1.04E+00\t6.08E-01\t1.40E-01\t9.84E-01\t4.22E-01\t5.42E-02\t7.58E-02\t4.50E-01\t5.12E-01\t1.13E+00\t5.58E-01\t5.73E-01\t9.88E-02\t2.91E-02\t8.50E-02\t3.29E-04\t3.06E+02\t8.87E-01\t1.35E+03\t1.10E-01\t1.28E+02\t1.96E+04\t7.96E+00\t1.22E+01\t1.32E+02\t5.18E+00\t-2.43E-01\t9.83E-01\t3.17E-01\t9.36E-02\t8.12E+01\t1.56E-01\t5.45E-02\t8.82E+01\t1.53E-01\t8.92E-02\t2.17E+02\t9.22E-01\t1.29E-01\t3.01E+02\t1.76E+01\t1.81E-01\t2.95E+02\t8.44E+01\t1.73E-01\t1.62E+02\t-9.63E+01\t4.09E-02\t8.98E+00\t1.33E+03\t1.15E-02\t6.90E-01\t2.69E+00\t3.77E+00\t1.40E+00\t2.00E+00\t3.18E+00\t1.40E+00\t6.14E+00\t1.13E+01\t5.67E-01\t9.86E+00\t2.10E+01\t5.67E-01\t9.45E+00\t1.42E+01\t1.40E+00\t5.61E+00\t7.69E+00\t1.00E+00\t4.72E-02\t1.34E-01\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.45E-01\t7.97E-01\t1.84E+02\t1.60E+03\t3.00E+01\t3.00E+01\t3.00E+01\t9.83E+00\t1.23E+01\t0.00E+00\t1.00E+00\t4.45E-01\t6.48E-01\t1.46E+02\t2.96E+02\t1.88E+01\t1.33E+01\t2.06E+01\t6.61E+00\t1.02E+01\t1.50E+00\t3.00E+00\t2.68E-01\t3.08E-01\t9.20E+01\t1.20E+02\t2.19E+01\t2.19E+01\t2.30E+01\t2.07E+00\t6.73E+00\t1.96E-01\t1.00E+00\t5.52E-01\t3.03E-01\t7.60E+01\t1.62E+02\t2.11E+01\t2.11E+01\t2.22E+01\t1.95E+00\t6.44E+00\t2.18E-01\t1.00E+00\t2.48E-01\t1.86E-01\t5.40E+01\t5.70E+01\t2.10E+01\t2.10E+01\t2.21E+01\t1.27E+00\t6.83E+00\t1.77E-01\t2.00E+00\t4.24E-01\t5.79E-01\t1.48E+02\t3.08E+02\t1.76E+01\t1.18E+01\t2.02E+01\t6.56E+00\t1.13E+01\t1.48E+00\t3.00E+00\t2.56E-01\t8.51E-01\t1.28E+02\t1.31E+02\t2.05E+01\t1.87E+01\t2.13E+01\t5.76E+00\t6.77E+00\t2.37E-01\t6.00E+00\t0.00E+00\t5.70E-02\t5.01E-03\t2.13E-02\t5.01E-03\t6.33E-02\t1.57E-02\t3.07E-01\t0.00E+00\t4.02E-01\t5.47E-01\t1.93E-01\t9.09E-01\t4.29E-01\t6.67E-02\t9.92E-01\t0.00E+00\t8.92E-01\t4.67E-01\t9.92E-01\t8.33E-01\t2.10E-01\t1.00E+00\t6.60E-01\t0.00E+00\t3.52E-01\t9.88E-01\t6.30E-01\t1.40E-01\t1.00E+00\t9.82E-01\t1.00E+00\t0.00E+00\t1.00E+00\t8.77E-01\t3.28E-01\t8.73E-01\t3.86E-01\t5.19E-01\t1.85E-01\t0.00E+00\t4.16E-01\t1.91E-01\t9.69E-01\t7.63E-01\t7.79E-01\t3.82E-01\t9.77E-01\t0.00E+00\t1.78E-04\t9.94E+02\t6.44E-01\t1.39E+03\t4.53E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.98E+02\t6.02E+00\t-1.60E-01\t9.43E-01\t2.00E-04\t1.95E+03\t3.07E-01\t1.40E+03\t3.19E-02\t1.26E+02\t1.77E+04\t7.83E+00\t1.25E+01\t7.08E+02\t6.49E+00\t-1.17E-01\t8.89E-01\t1.83E-04\t9.47E+02\t6.60E-01\t1.39E+03\t4.70E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.86E+02\t5.98E+00\t-1.64E-01\t9.46E-01\t2.69E-04\t1.50E+03\t4.49E-01\t1.36E+03\t6.38E-02\t1.28E+02\t1.83E+04\t7.86E+00\t1.25E+01\t6.07E+02\t6.29E+00\t-1.59E-01\t9.40E-01\t3.08E-04\t3.48E+02\t8.71E-01\t1.35E+03\t1.01E-01\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.49E+02\t5.27E+00\t-2.33E-01\t9.80E-01\t2.77E-04\t1.49E+03\t4.53E-01\t1.36E+03\t6.65E-02\t1.28E+02\t1.83E+04\t7.87E+00\t1.25E+01\t6.05E+02\t6.28E+00\t-1.63E-01\t9.44E-01\t\t\t\t\t\t\t\t\t";
        String[] terms = line.split(dg.separator);
        Map<String, Object> data = dg.generateObjectsFromLine(terms);
        try (Transaction tx = dg.db.beginTx()) {
            Map<String, Node> map = dg.getNodesForObjects(data);
            assertEquals(map.size(), 3);
            assertNotNull(map.get("GENE"));
            assertNotNull(map.get("CELL"));
            assertNotNull(map.get("EXPERIMENT"));
            tx.success();
        }
    }

    @Test(dependsOnMethods = {"testGetNodesForObjects"})
    public void testConnectNodes() throws Exception {
        String line = "Rep1\t1\tO03\t-1\tcontrol\t3\t3.00E+00\t1.00E+00\t8.34E+03\t1.40E+03\t1.24E+03\t3.08E+02\t1.23E+02\t1.35E+03\t1.46E+03\t1.19E+03\t1.29E+03\t2.14E+00\t4.54E+02\t1.73E+04\t-2.78E+02\t2.12E+03\t1.77E+07\t4.41E+03\t2.37E+00\t8.54E-02\t1.75E+00\t2.30E-01\t6.18E-02\t2.26E-01\t2.14E-01\t2.47E-01\t9.96E-03\t1.10E+00\t1.54E-01\t3.57E-01\t4.61E-02\t6.92E-03\t2.36E-01\t1.61E-01\t1.39E-01\t8.24E-02\t1.72E-02\t4.29E-03\t7.42E-01\t7.83E-02\t2.66E-01\t1.15E-01\t5.02E-02\t1.02E-02\t2.76E-03\t5.71E-01\t1.39E-01\t3.65E-01\t1.69E-01\t8.63E-02\t9.98E-03\t7.24E-03\t3.07E-03\t3.31E-04\t9.42E+02\t6.71E-01\t1.43E+03\t8.39E-02\t1.26E+02\t1.87E+04\t7.83E+00\t1.19E+01\t4.46E+02\t5.85E+00\t-1.93E-01\t9.60E-01\t-2.49E+00\t6.75E-02\t6.65E+01\t-8.36E+00\t4.89E-02\t3.70E+01\t-2.33E+01\t7.90E-02\t8.61E+01\t-3.86E+01\t1.36E-01\t1.74E+02\t-1.68E+01\t2.26E-01\t3.97E+02\t4.19E+01\t2.50E-01\t3.74E+02\t-5.93E+01\t1.04E-01\t6.89E+01\t2.23E+03\t1.66E-02\t1.63E+00\t2.38E+00\t3.33E+00\t1.40E+00\t8.43E-01\t1.26E+00\t5.67E-01\t2.27E+00\t4.03E+00\t5.67E-01\t4.39E+00\t9.49E+00\t5.67E-01\t1.19E+01\t2.07E+01\t5.67E-01\t1.72E+01\t3.21E+01\t1.40E+00\t2.63E-01\t1.06E+00\t5.67E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.11E+02\t2.16E+04\t-6.87E+02\t7.28E+02\t6.07E+06\t2.17E+03\t2.12E+00\t3.63E-01\t3.50E-01\t1.19E+00\t2.00E-01\t1.34E+00\t9.38E-01\t7.69E-01\t6.20E-02\t4.59E-01\t3.09E-01\t1.38E+00\t3.53E-01\t1.25E-01\t1.18E+00\t1.09E+00\t1.13E+00\t8.77E-01\t1.85E-01\t4.05E-02\t1.05E+00\t5.53E-01\t2.45E-01\t1.07E+00\t4.84E-01\t5.30E-02\t6.19E-02\t5.84E-01\t6.88E-01\t1.25E+00\t5.56E-01\t6.59E-01\t8.27E-02\t2.51E-02\t8.73E-02\t3.13E-04\t3.74E+02\t8.62E-01\t1.35E+03\t9.89E-02\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.60E+02\t5.32E+00\t-2.31E-01\t9.80E-01\t-1.67E-01\t9.97E-02\t4.87E+01\t-3.63E-01\t5.35E-02\t4.73E+01\t-1.13E+00\t9.02E-02\t1.30E+02\t-5.10E+00\t1.31E-01\t1.83E+02\t-1.43E+01\t1.81E-01\t1.77E+02\t-1.33E+01\t1.69E-01\t8.99E+01\t-9.48E+01\t4.50E-02\t6.72E+00\t8.57E+02\t1.18E-02\t4.19E-01\t1.65E+00\t2.29E+00\t1.40E+00\t9.48E-01\t1.41E+00\t9.59E-01\t3.60E+00\t7.09E+00\t5.67E-01\t5.96E+00\t1.30E+01\t5.67E-01\t5.77E+00\t8.39E+00\t1.40E+00\t3.27E+00\t4.52E+00\t1.00E+00\t3.78E-02\t8.38E-02\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t3.54E+02\t7.26E+03\t-2.90E+02\t1.58E+02\t1.32E+06\t7.32E+02\t2.05E+00\t5.56E-01\t2.06E-01\t1.90E+00\t2.93E-01\t1.92E+00\t1.94E+00\t1.27E+00\t7.30E-02\t9.39E-01\t2.47E-01\t2.39E+00\t5.85E-01\t3.41E-02\t1.65E+00\t2.13E+00\t1.94E+00\t1.50E+00\t2.57E-01\t4.33E-03\t1.89E+00\t1.45E+00\t3.37E-01\t1.97E+00\t7.61E-01\t5.35E-02\t2.93E-02\t6.18E-01\t9.87E-01\t2.06E+00\t1.25E+00\t1.18E+00\t2.45E-01\t4.27E-02\t4.06E-02\t5.28E-04\t1.84E+03\t3.30E-01\t1.38E+03\t7.00E-02\t1.27E+02\t1.79E+04\t7.74E+00\t1.13E+01\t7.27E+02\t6.39E+00\t-9.73E-02\t8.28E-01\t-3.31E-02\t1.28E-01\t2.73E+01\t-5.98E-02\t5.61E-02\t1.76E+01\t-1.82E-01\t8.91E-02\t5.27E+01\t-1.75E+00\t1.32E-01\t7.82E+01\t-1.57E+01\t1.71E-01\t6.03E+01\t-5.56E+01\t1.58E-01\t2.68E+01\t-8.79E+01\t3.97E-02\t2.03E+00\t3.19E+02\t1.93E-02\t3.73E-01\t9.85E-01\t1.41E+00\t1.40E+00\t3.27E-01\t5.67E-01\t5.67E-01\t1.39E+00\t2.98E+00\t5.67E-01\t2.64E+00\t5.73E+00\t5.67E-01\t2.08E+00\t3.43E+00\t5.67E-01\t1.57E+00\t2.63E+00\t9.59E-01\t1.40E-01\t6.46E-01\t1.00E+00\t0.00E+00\t0.00E+00\t1.00E+00\t1.22E+03\t4.20E+04\t-1.05E+03\t1.34E+03\t1.11E+07\t3.77E+03\t2.11E+00\t3.26E-01\t4.26E-01\t1.07E+00\t1.78E-01\t1.24E+00\t8.77E-01\t6.88E-01\t7.59E-02\t5.10E-01\t2.23E-01\t1.28E+00\t3.19E-01\t1.32E-01\t1.03E+00\t9.84E-01\t1.10E+00\t7.88E-01\t1.52E-01\t5.11E-02\t1.04E+00\t6.08E-01\t1.40E-01\t9.84E-01\t4.22E-01\t5.42E-02\t7.58E-02\t4.50E-01\t5.12E-01\t1.13E+00\t5.58E-01\t5.73E-01\t9.88E-02\t2.91E-02\t8.50E-02\t3.29E-04\t3.06E+02\t8.87E-01\t1.35E+03\t1.10E-01\t1.28E+02\t1.96E+04\t7.96E+00\t1.22E+01\t1.32E+02\t5.18E+00\t-2.43E-01\t9.83E-01\t3.17E-01\t9.36E-02\t8.12E+01\t1.56E-01\t5.45E-02\t8.82E+01\t1.53E-01\t8.92E-02\t2.17E+02\t9.22E-01\t1.29E-01\t3.01E+02\t1.76E+01\t1.81E-01\t2.95E+02\t8.44E+01\t1.73E-01\t1.62E+02\t-9.63E+01\t4.09E-02\t8.98E+00\t1.33E+03\t1.15E-02\t6.90E-01\t2.69E+00\t3.77E+00\t1.40E+00\t2.00E+00\t3.18E+00\t1.40E+00\t6.14E+00\t1.13E+01\t5.67E-01\t9.86E+00\t2.10E+01\t5.67E-01\t9.45E+00\t1.42E+01\t1.40E+00\t5.61E+00\t7.69E+00\t1.00E+00\t4.72E-02\t1.34E-01\t2.19E-01\t0.00E+00\t0.00E+00\t1.00E+00\t8.45E-01\t7.97E-01\t1.84E+02\t1.60E+03\t3.00E+01\t3.00E+01\t3.00E+01\t9.83E+00\t1.23E+01\t0.00E+00\t1.00E+00\t4.45E-01\t6.48E-01\t1.46E+02\t2.96E+02\t1.88E+01\t1.33E+01\t2.06E+01\t6.61E+00\t1.02E+01\t1.50E+00\t3.00E+00\t2.68E-01\t3.08E-01\t9.20E+01\t1.20E+02\t2.19E+01\t2.19E+01\t2.30E+01\t2.07E+00\t6.73E+00\t1.96E-01\t1.00E+00\t5.52E-01\t3.03E-01\t7.60E+01\t1.62E+02\t2.11E+01\t2.11E+01\t2.22E+01\t1.95E+00\t6.44E+00\t2.18E-01\t1.00E+00\t2.48E-01\t1.86E-01\t5.40E+01\t5.70E+01\t2.10E+01\t2.10E+01\t2.21E+01\t1.27E+00\t6.83E+00\t1.77E-01\t2.00E+00\t4.24E-01\t5.79E-01\t1.48E+02\t3.08E+02\t1.76E+01\t1.18E+01\t2.02E+01\t6.56E+00\t1.13E+01\t1.48E+00\t3.00E+00\t2.56E-01\t8.51E-01\t1.28E+02\t1.31E+02\t2.05E+01\t1.87E+01\t2.13E+01\t5.76E+00\t6.77E+00\t2.37E-01\t6.00E+00\t0.00E+00\t5.70E-02\t5.01E-03\t2.13E-02\t5.01E-03\t6.33E-02\t1.57E-02\t3.07E-01\t0.00E+00\t4.02E-01\t5.47E-01\t1.93E-01\t9.09E-01\t4.29E-01\t6.67E-02\t9.92E-01\t0.00E+00\t8.92E-01\t4.67E-01\t9.92E-01\t8.33E-01\t2.10E-01\t1.00E+00\t6.60E-01\t0.00E+00\t3.52E-01\t9.88E-01\t6.30E-01\t1.40E-01\t1.00E+00\t9.82E-01\t1.00E+00\t0.00E+00\t1.00E+00\t8.77E-01\t3.28E-01\t8.73E-01\t3.86E-01\t5.19E-01\t1.85E-01\t0.00E+00\t4.16E-01\t1.91E-01\t9.69E-01\t7.63E-01\t7.79E-01\t3.82E-01\t9.77E-01\t0.00E+00\t1.78E-04\t9.94E+02\t6.44E-01\t1.39E+03\t4.53E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.98E+02\t6.02E+00\t-1.60E-01\t9.43E-01\t2.00E-04\t1.95E+03\t3.07E-01\t1.40E+03\t3.19E-02\t1.26E+02\t1.77E+04\t7.83E+00\t1.25E+01\t7.08E+02\t6.49E+00\t-1.17E-01\t8.89E-01\t1.83E-04\t9.47E+02\t6.60E-01\t1.39E+03\t4.70E-02\t1.27E+02\t1.88E+04\t7.86E+00\t1.27E+01\t3.86E+02\t5.98E+00\t-1.64E-01\t9.46E-01\t2.69E-04\t1.50E+03\t4.49E-01\t1.36E+03\t6.38E-02\t1.28E+02\t1.83E+04\t7.86E+00\t1.25E+01\t6.07E+02\t6.29E+00\t-1.59E-01\t9.40E-01\t3.08E-04\t3.48E+02\t8.71E-01\t1.35E+03\t1.01E-01\t1.29E+02\t1.96E+04\t7.95E+00\t1.23E+01\t1.49E+02\t5.27E+00\t-2.33E-01\t9.80E-01\t2.77E-04\t1.49E+03\t4.53E-01\t1.36E+03\t6.65E-02\t1.28E+02\t1.83E+04\t7.87E+00\t1.25E+01\t6.05E+02\t6.28E+00\t-1.63E-01\t9.44E-01\t\t\t\t\t\t\t\t\t";
        String[] terms = line.split(dg.separator);
        Map<String, Object> data = dg.generateObjectsFromLine(terms);
        try (Transaction tx = dg.db.beginTx()) {
            Map<String, Node> map = dg.getNodesForObjects(data);
            dg.connectNodes(map);
            for (String key : dg.relKeySet) {
                Map<String, RelationshipType> rels = dg.relationships.get(key);
                Node n1 = map.get(key);
                Set<String> innerKeySet = rels.keySet();
                for (String s : innerKeySet) {
                    Node n2 = map.get(s);
                    RelationshipType rt = rels.get(s);
                    assertTrue(n1.hasRelationship(rt));
                    Iterator<Relationship> rel = n1.getRelationships(rt).iterator();
                    boolean isConnected = false;
                    while (rel.hasNext()) {
                        Node node = rel.next().getOtherNode(n1);
                        if (node.equals(n2)) {
                            isConnected = true;
                            break;
                        }
                    }
                    assertTrue(isConnected);
                }
            }
            tx.success();
        }

    }

    private int countObjects(Label label) {
        if (dg.db == null) {
            dg.startDB();
        }
        int count = 0;
        try (Transaction tx = dg.db.beginTx()) {
            Iterator<Node> nodes = dg.db.findNodes(label);
            while (nodes.hasNext()) {
                nodes.next();
                count++;
            }
        }
        return count;
    }

    private void deleteObjects(Label label) {
        if (dg.db == null) {
            dg.startDB();
        }
        try (Transaction tx = dg.db.beginTx()) {
            Iterator<Node> nodes = dg.db.findNodes(label);
            while (nodes.hasNext()) {
                Node node = nodes.next();
                Iterator<Relationship> rels = node.getRelationships().iterator();
                while (rels.hasNext()) {
                    rels.next().delete();
                }
                node.delete();
            }
            tx.success();
        }
    }

//    @Test(dependsOnMethods = {"testProcessMetadata","testGenerateObjectsFromLine","testGetNodesForObjects","testConnectNodes"})
    @Test(dependsOnMethods = {"testProcessMetadata"})
    public void testProcessData() throws Exception {
       /* deleteObjects(dg.labels.get("GENE"));
        deleteObjects(dg.labels.get("EXPERIMENT"));
        deleteObjects(dg.labels.get("CELL"));
        assertEquals(countObjects(dg.labels.get("GENE")), 0);
        assertEquals(countObjects(dg.labels.get("EXPERIMENT")),0);
        assertEquals(countObjects(dg.labels.get("CELL")),0);*/
        dg.processData();
        assertEquals(countObjects(dg.labels.get("GENE")), 3);
        assertEquals(countObjects(dg.labels.get("EXPERIMENT")),3);
        assertEquals(countObjects(dg.labels.get("CELL")),795);

    }

    // TODO: later
    @Test
    public void testCreateFilters() throws Exception {

    }

    @Test(dependsOnMethods = {"testStartDB","testProcessMetadata"})
    public void testCreateNode() throws Exception {
        String className = testClass.getName();
        dg.labels.put(className, DynamicLabel.label(className));
        try (Transaction tx = dg.db.beginTx()) {
            Object o = createTestObject(testClass, idFields.get(0), "1");
            Node node = dg.createNode(o);
            assertNotNull(node);
            Field[] fields = testClass.getFields();

            for (Field f: fields) {
                String name = f.getName();
                Object value = f.get(o);
                if (value != null) {
                    assertEquals(value, node.getProperty(name));
                }
                else {
                    assertNull(node.getProperty(name, null));
                }
            }
            tx.success();
        }
        dg.labels.remove(className);
    }

    @Test(dependsOnMethods = {"testStartDB","testCreateNode","testProcessMetadata"})
    public void testLookupNode() throws Exception {
        List<Node> testNodes = new ArrayList<>();
        String className = testClass.getName();
        dg.labels.put(className, DynamicLabel.label(className));
        try (Transaction tx = dg.db.beginTx()) {
            Object o = createTestObject(testClass, idFields.get(0), "1");
            Node testNode = dg.createNode(o);
            testNodes.add(testNode);
            dg.nonUniqueNodes.put(className, testNodes);
            Node result = dg.lookupObject(o);
            assertEquals(testNode, result);
            Object o2 = createTestObject(testClass, idFields.get(0), "2");
            Node result2 = dg.lookupObject(o2);
            assertNotEquals(o2, result2);
            int size = dg.nonUniqueNodes.get(className).size();
            assertEquals(size, 2);
            Node result3 = dg.lookupObject(o2);
            assertEquals(dg.nonUniqueNodes.get(className).get(size-1), result3);
            tx.success();
        }
        dg.nonUniqueNodes.remove(className);
        dg.labels.remove(className);
    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testLabelGenes() throws Exception {
        dg.labelGenes("input/test_input/chia_labels.txt");
        assertTrue(new File(dg.confDir + "mineotaur.hitLabels").exists());
        assertEquals(countObjects(dg.wildTypeLabel), 1);
        assertEquals(countObjects(DynamicLabel.label("CisDiffuse")), 0);
        assertEquals(countObjects(DynamicLabel.label("CisFragmented")),1);
        assertEquals(countObjects(DynamicLabel.label("CisCondensed")),1);
        assertEquals(countObjects(DynamicLabel.label("MedialDiffuse")),0);
        assertEquals(countObjects(DynamicLabel.label("MedialFragmented")),0);
        assertEquals(countObjects(DynamicLabel.label("MedialCondensed")),1);
        assertEquals(countObjects(DynamicLabel.label("TransDiffuse")),0);
        assertEquals(countObjects(DynamicLabel.label("TransFragmented")),1);
        assertEquals(countObjects(DynamicLabel.label("TransCondensed")),0);
    }


    @Test
    public void testPrecomputeOptimized() throws Exception {

    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testStoreFeatureNames() throws Exception {
        dg.storeFeatureNames();
        assertTrue(new File(dg.confDir + "mineotaur.features").exists());

    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testStoreGroupnames() throws Exception {
        dg.storeGroupnames(dg.db);
        assertTrue(new File(dg.confDir + "mineotaur.groupNames").exists());
    }

    @Test
    public void testStoreFilters() throws Exception {

    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testGeneratePropertyFile() throws Exception {
        dg.generatePropertyFile();
        assertTrue(new File(dg.confDir + "mineotaur.properties").exists());
    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testCreateIndex() throws Exception {
        dg.createIndex(dg.db);
        try (Transaction tx = dg.db.beginTx()) {
            Schema schema = dg.db.schema();
            assertTrue(schema.getIndexes().iterator().hasNext());
        }
    }
}