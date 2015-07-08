package org.mineotaur.importer;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;

/**
 * Created by balintantal on 07/07/2015.
 */
public enum DefaultLabels {
    SCREEN, EXPERIMENT, GROUP, DESCRIPTIVE;

    private Label label;

    DefaultLabels() {
        this.label = DynamicLabel.label(name());
    }

    public Label getLabel() {
        return label;
    }
}
