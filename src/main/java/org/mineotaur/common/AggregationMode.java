package org.mineotaur.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by balintantal on 28/07/2015.
 */
public enum AggregationMode {
    AVERAGE("Average"), MAXIMUM("Maximum"), MINIMUM("Minimum"), MEDIAN("Median"), STANDARD_DEVIATION("Standard deviation"), COUNT("Count");

    private String value;

    AggregationMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }


}
