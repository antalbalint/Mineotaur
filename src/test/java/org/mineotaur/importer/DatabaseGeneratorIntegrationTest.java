package org.mineotaur.importer;

import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by balintantal on 24/07/2015.
 */
public class DatabaseGeneratorIntegrationTest {

    protected DatabaseGenerator dg;

    @Test
    public void testCreateRelationships() throws Exception {
        assertNotNull(dg.relationships);
        //assertEquals(1, dg.relationshipCount);
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
        dg.startDB(dg.dbPath, dg.totalMemory, dg.cache);
        assertNotNull(dg.db);
    }

    @Test
    public void testPrecomputeOptimized() throws Exception {
        //dg.precomputeOptimized(dg.limit);
        try (Transaction tx = dg.db.beginTx()) {
            Iterator<Node> nodes = dg.ggo.getAllNodesWithLabel(dg.precomputedLabel).iterator();
            assertTrue(nodes.hasNext());
            Node node = nodes.next();
            Iterator<Relationship> rels = node.getRelationships().iterator();
            assertTrue(rels.hasNext());
            String groupObject = null;
            while (rels.hasNext()) {
                Relationship rel = rels.next();
                Node other = rel.getOtherNode(node);
                assertTrue(other.hasLabel(dg.groupLabel));
                String name = (String) other.getProperty(dg.groupName, "");
                if (groupObject == null) {
                    groupObject = name;
                }
                else {
                    assertEquals(name, groupObject);
                }
            }
        }
    }

    @Test(dependsOnMethods = {"testProcessData"})
    public void testStoreGroupnames() throws Exception {
        //dg.generateGroupnameList(dg.db);
        assertTrue(new File(dg.confDir + "mineotaur.groupNames").exists());
    }

    @Test
    public void testStoreFilters() throws Exception {
        //dg.generateFilterList();
        assertTrue(new File(dg.confDir + "mineotaur.filters").exists());
    }

    @Test
    public void testGeneratePropertyFile() throws Exception {
        //dg.generatePropertyFile();
        assertTrue(new File(dg.confDir + "mineotaur.properties").exists());
    }

    @Test
    public void testGetImageIDs() throws Exception {

    }


}
