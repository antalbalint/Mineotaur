package org.mineotaur.provider;

import org.mineotaur.application.Mineotaur;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created by balintantal on 03/08/2015.
 */
public class HTHCSEmbeddedGraphDatabaseProvider implements GraphDatabaseProvider   {

    private DataDescriptor dataDescriptor;

    public HTHCSEmbeddedGraphDatabaseProvider(DataDescriptor dataDescriptor) {
        this.dataDescriptor = dataDescriptor;
    }

    public HTHCSEmbeddedGraphDatabaseProvider() {
        try {
            this.dataDescriptor = new DataDescriptor(Mineotaur.name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GraphDatabaseService getDatabaseService() {
        return dataDescriptor.getDatabase();
    }

    @Override
    public DataDescriptor getContext() {
        return dataDescriptor;
    }
}
