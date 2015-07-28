package org.mineotaur.common;

import javassist.ClassPool;
import javassist.CtClass;
import org.mineotaur.provider.MockEmbeddedGraphDatabaseProvider;
import org.neo4j.graphdb.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.lang.reflect.Field;
import java.util.*;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by balintantal on 22/07/2015.
 */
@PrepareForTest(ClassUtils.class)
public class ClassUtilsTest {

    protected class TestClass {
        public TestClass(double d, String s) {
            this.d = d;
            this.s = s;
        }

        public double d;
        public String s;

    };

    @DataProvider(name = "testCreateClassExceptionDataProvider")
    public Object[][] testCreateClassExceptionDataProvider() throws Exception {
        ClassPool pool = mock(ClassPool.class);
        String string = "test";
        List list = mock(List.class);
        String[] stringArray = new String[]{string};
        String emptyString = "";
        List emptyList = mock(List.class);
        when(emptyList.isEmpty()).thenReturn(true);
        String[] emptyStringArray = new String[]{};
        return new Object[][] {
                {null, string, list, stringArray, stringArray, list, string, string},
                {pool, null, list, stringArray, stringArray, list, string, string},
                {pool, emptyString, list, stringArray, stringArray, list, string, string},
                {pool, string, null, stringArray, stringArray, list, string, string},
                {pool, string, emptyList, stringArray, stringArray, list, string, string},
                {pool, string, list, null, stringArray, list, string, string},
                {pool, string, list, emptyStringArray, stringArray, list, string, string},
                {pool, string, list, stringArray, null,  list, string, string},
                {pool, string, list, stringArray, emptyStringArray,  list, string, string},
                {pool, string, list, stringArray, stringArray,  null, string, string},
                {pool, string, list, stringArray, stringArray,  emptyList, string, string},
                {pool, string, list, stringArray, stringArray,  list, null, string},
                {pool, string, list, stringArray, stringArray,  list, emptyString, string},
                {pool, string, list, stringArray, stringArray,  list, string, null},
                {pool, string, list, stringArray, stringArray,  list, string, emptyString}
        };
    }

    @Test(dataProvider = "testCreateClassExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testCreateClassException(ClassPool pool,
                                         String className,
                                         List<Integer> indices,
                                         String[] header,
                                         String[] dataTypes,
                                         List<String> idFields,
                                         String dataType,
                                         String methodBody) throws Exception {
        ClassUtils.createClass(pool, className, indices, header, dataTypes, dataType, methodBody);
    }
    @DataProvider(name = "testCreateClassDataProvider")
    public Object[][] testCreateClassDataProvider() throws Exception {
        ClassPool pool = spy(ClassPool.getDefault());
        String string = "test";
        List list = mock(List.class);
        List indices = new ArrayList<>();
        indices.add(0);
        when(list.size()).thenReturn(1);
        when(list.get(anyInt())).thenReturn(0);
        String[] stringArray = new String[]{string};
        String methodBody = "public void nothing() {}";
        return new Object[][] {
                {pool, "test", indices,  stringArray, stringArray, string, methodBody, CtClass.doubleType, "test",pool.makeClass("test")},
                {pool, "test2", indices, new String[]{"test2"}, stringArray, "somethingElse", methodBody, pool.get(String.class.getTypeName()), "test2",pool.makeClass("test2")},

        };
    }

    @Test(dataProvider = "testCreateClassDataProvider")
    public void testCreateClass(ClassPool pool,
                                String className,
                                List<Integer> indices,
                                String[] header,
                                String[] dataTypes,
                                String dataType,
                                String methodBody,
                                CtClass type,
                                String fieldName,
                                CtClass claz) throws Exception {

        Class cls = ClassUtils.createClass(ClassPool.getDefault(), className, indices, header, dataTypes, dataType, methodBody);
        verify(pool).makeClass(className);
        assertEquals(cls.getName(), claz.getName());
        Field field = cls.getDeclaredField(fieldName);
        assertTrue(field!=null);
        assertTrue(field.getType()!=null);
        assertEquals(field.getType().getName(), type.getName());
    }

    @DataProvider(name = "testBuildEqualsExceptionDataProvider")
    public Object[][] testBuildEqualsExceptionDataProvider() throws Exception {
        String name="test";
        String emptyString="";
        List<String> idFields = new ArrayList<>();
        idFields.add("testField");
        List emptyList = mock(List.class);
        when(emptyList.isEmpty()).thenReturn(true);
        return new Object[][] {
                {null, idFields},
                {emptyString, idFields},
                {name, null},
                {name, emptyList}
        };
    }

    @Test(dataProvider = "testBuildEqualsExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testBuildEqualsException(String name, List<String> idFields) throws Exception {
        ClassUtils.buildEquals(name, idFields);
    }

    @DataProvider(name = "testBuildEqualsDataProvider")
    public Object[][] testBuildEqualsDataProvider() throws Exception {
        String name="test";
        List<String> idFields = new ArrayList<>();
        idFields.add("testField");

        return new Object[][] {
                {name, idFields},
        };
    }


    @Test(dataProvider = "testBuildEqualsDataProvider")
    public void testBuildEquals(String name, List<String> idFields) throws Exception {
        assertNotNull(ClassUtils.buildEquals(name, idFields));
    }

    @DataProvider(name = "testCreateNodeExceptionDataProvider")
    public Object[][] testCreateNodeExceptionDataProvider() throws Exception {
        Object o = mock(Object.class);
        Label label = mock(Label.class);
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        return new Object[][] {
                {null, label, db},
                {o, null, db},
                {o, label, null}
        };
    }

    @Test(dataProvider = "testCreateNodeExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testCreateNodeException(Object o, Label label, GraphDatabaseService db) throws Exception {
        ClassUtils.createNode(o, label, db);
    }



    @DataProvider(name = "testCreateNodeDataProvider")
    public Object[][] testCreateNodeDataProvider() throws Exception {
        double dVal = 1;
        String sVal = "test";

        Object o = new TestClass(dVal, sVal);
        Label label = DynamicLabel.label("test");
        GraphDatabaseService db = MockEmbeddedGraphDatabaseProvider.newDatabaseService();
        Map<String, Object> expectedProperties = new HashMap<>();
        expectedProperties.put("d", dVal);
        expectedProperties.put("s", sVal);
        return new Object[][] {
                {o, label, db, expectedProperties, label},
        };
    }


    @Test(dataProvider = "testCreateNodeDataProvider")
    public void testCreateNode(Object o, Label label, GraphDatabaseService db, Map<String, Object> expectedProperties, Label expectedLabel) throws Exception {
        try (Transaction tx = db.beginTx()) {
            Node node = ClassUtils.createNode(o, label, db);
            for (String s: expectedProperties.keySet()) {
                assertEquals(node.getProperty(s), expectedProperties.get(s));
            }
            assertTrue(node.hasLabel(expectedLabel));
        }

    }

    @DataProvider(name = "testLookupObjectExceptionDataProvider")
    public Object[][] testLookupObjectExceptionDataProvider() throws Exception {
        Object o = mock(Object.class);
        List list = mock(List.class);
        List emptyList = mock(List.class);
        when(emptyList.isEmpty()).thenReturn(true);
        GraphDatabaseService db = mock(GraphDatabaseService.class);
        return new Object[][] {
                {null, list, db},
                {o, null, db},
                {o, emptyList, db},
                {o, list, null}
        };
    }

    @Test(dataProvider = "testLookupObjectExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testLookupObjectException(Object o, List<Node> storedNodes, GraphDatabaseService db) throws Exception {
        ClassUtils.lookupObject(o, storedNodes, db);
    }

    @DataProvider(name = "testLookupObjectDataProvider")
    public Object[][] testLookupObjectDataProvider() throws Exception {

        Object o1 = new TestClass(1, "test");
        Object o2 = new TestClass(2, "test");
        Label label = DynamicLabel.label("test");
        GraphDatabaseService db = MockEmbeddedGraphDatabaseProvider.newDatabaseService();
        Node node1 = ClassUtils.createNode(o1, label, db);
        List<Node> nodes = new ArrayList<>();
        nodes.add(node1);
        return new Object[][] {
                {o1, nodes, db, node1},
                {o2, nodes, db, null},

        };
    }

    @Test(dataProvider = "testLookupObjectDataProvider")
    public void testLookupObject(Object o, List<Node> storedNodes, GraphDatabaseService db, Node expectedNode) throws Exception {
        assertEquals(ClassUtils.lookupObject(o, storedNodes, db), expectedNode);
    }
}