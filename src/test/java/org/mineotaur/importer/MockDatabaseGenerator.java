package org.mineotaur.importer;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.List;

/**
 * Created by balintantal on 09/06/2015.
 */
class MockDatabaseGenerator extends DatabaseGenerator {


    public void startDB() {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        ggo = GlobalGraphOperations.at(db);
    }

    protected void generateFeatureNameList(List<Integer> numericData, String[] header) {

    }

    @Override
    protected List<String> getHits() {
        return null;
    }

    @Override
    protected List<String> generateFeatureNameList() {
        return null;
    }

    @Override
    public void generateDatabase() {

    }

    @Override
    protected void processData(GraphDatabaseService db) {

    }

    @Override
    protected void labelGenes() {

    }

    @Override
    protected void processMetadata() {

    }
}
