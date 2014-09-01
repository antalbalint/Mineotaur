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

package org.mineotaur.importer;

/**
 * Created by balintantal on 14/07/2014.
 */
public class GeneData {

    private String id;
    private int[] counts;
    private double[] values;
    private int n;

    public GeneData(String id, int n) {
        this.id = id;
        this.n = n;
        this.counts = new int[n];
        this.values = new double[n];
    }

    public String getId() {
        return id;
    }

    public int[] getCounts() {
        return counts;
    }

    public double[] getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeneData geneData = (GeneData) o;

        if (id != null ? !id.equals(geneData.id) : geneData.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        String sep="\t";
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(sep);
        for (int i = 0; i < n; ++i) {

            if (values[i] / (double)counts[i] >= 0.5) {
                sb.append(1);
            }
            else {
                sb.append(0);
            }
            sb.append(sep);
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
