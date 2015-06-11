package org.mineotaur.provider;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;


/**
 * Created by balintantal on 05/06/2015.
 */
public class MockEmbeddedGraphDatabaseProvider extends GenericEmbeddedGraphDatabaseProvider {


    public MockEmbeddedGraphDatabaseProvider(String _baseDir) {
        baseDir = _baseDir;
    }

    @Override
    protected GraphDatabaseService newDatabaseService() {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    @Override
    protected void preFecthGroupNames() {

    }
}
