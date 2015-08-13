package org.mineotaur.importer;

/**
 * Created by balintantal on 23/07/2015.
 */
public enum DefaultProperty {
    GROUP("GENE"),
    GROUP_NAME("geneID"),
    DESCRIPTIVE("CELL"),
    SEPARATOR("\t"),
    TOTAL_MEMORY("4G"),
    LIMIT(10000),
    OVERWRITE(false);

    private Object value;

    DefaultProperty(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
