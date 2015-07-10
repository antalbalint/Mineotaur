package org.mineotaur.common;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.concurrent.TimeUnit;

/**
 * Created by balintantal on 10/07/2015.
 */
public class GraphDatabaseUtils {

    /**
     * Creates a new graph database instance.
     * @param dbPath Path to the Database.
     * @param totalMemory Total memory can be used by Neo4J.
     * @param cache the caching mechanism used by Neo4J. See Neo4J documentation for details. For import use 'none', otherwise use 'soft'.
     * @return the GraphDatabaseService instance.
     */
    public static GraphDatabaseService createNewGraphDatabaseService(String dbPath, String totalMemory, String cache) {
        GraphDatabaseBuilder gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath);
        gdb.setConfig(GraphDatabaseSettings.all_stores_total_mapped_memory_size, totalMemory);
        gdb.setConfig(GraphDatabaseSettings.cache_type, cache);
        GraphDatabaseService db = gdb.newGraphDatabase();
        registerShutdownHook(db);
        return db;
    }

    /**
     * Registers a shutdown hook for the database (from neo4j.com).
     * @param graphDb The database.
     */
    public static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    /**
     * Method to create indices on the groupName property of the group nodes.
     * @param db The GraphDatabaseService instance.
     * @param label The node type to be indexed
     * @param name The property ot be indexed
     */
    public static void createIndex(GraphDatabaseService db, Label label, String name) {
        IndexDefinition indexDefinition;
        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            indexDefinition = schema.indexFor(label).on(name).create();
            tx.success();
        }

        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
            tx.success();
        }
    }

}
