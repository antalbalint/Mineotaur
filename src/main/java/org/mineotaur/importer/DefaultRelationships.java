package org.mineotaur.importer;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;

/**
 * Created by balintantal on 07/07/2015.
 */
public enum DefaultRelationships {
    GROUP_EXPERIMENT, GROUP_CELL;

    private RelationshipType relationshipType;

    DefaultRelationships() {
        this.relationshipType = DynamicRelationshipType.withName(name());
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }
}
