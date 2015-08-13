package org.mineotaur.importer;

import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * Created by balintantal on 22/07/2015.
 */
public class MockImportFromFile extends DatabaseGeneratorFromFile {
    public MockImportFromFile(String prop, String dataFile, String labelFile) {
        super(prop, dataFile, labelFile);
    }

    public void startDB() {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        ggo = GlobalGraphOperations.at(db);
    }
}
