/*
 * Mineotaur: a visual analytics tool for high-throughput microscopy screens
 * Copyright (C) 2014  Bálint Antal (University of Cambridge)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mineotaur.model;

/**
 * Created by balintantal on 29/05/2014.
 */
public enum AggregationMode {
    AVERAGE("Average"), MIN("Min"), MAX("Max"), MEDIAN("Median"), STDEV("Standard deviation"), NUMBER("Number");

    private String name;

    AggregationMode(String name) {
        this.name = name;
    }

    public static AggregationMode byName(String _name) {
        AggregationMode[] values = values();
        for (AggregationMode mode: values) {
            if (mode.name.equals(_name)) {
                return mode;
            }
        }
        throw new EnumConstantNotPresentException(AggregationMode.class, "There is no aggregation mode with the name: " + _name);
    }

    @Override
    public String toString() {
        return name;
    }
}
