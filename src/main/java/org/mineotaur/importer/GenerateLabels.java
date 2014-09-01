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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by balintantal on 14/07/2014.
 */
public class GenerateLabels {

    public static void main(String[] args) {
        String file = "input/chia_sample.txt";
        List<GeneData> geneData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split("\t");

            String[] labels = br.readLine().split("\t");
            String[] types = br.readLine().split("\t");
            List<Integer> indices = new ArrayList<>();
            int id = -1;
            for (int i = 0; i < labels.length; ++i) {
                if ("HIT".equals(labels[i])) {
                    indices.add(i);
                }
                else if ("GeneID".equals(header[i])) {
                    id = i;
                }
            }
            int numberOfHits = indices.size();
            String line;

            while ((line = br.readLine()) != null) {
                String[] data = line.split("\t");
                String currId = data[id];
                GeneData g = null;
                for (GeneData gd: geneData) {
                    if (currId.equals(gd.getId())) {
                        g = gd;
                        break;
                    }
                }
                if (g==null) {
                    g = new GeneData(currId, numberOfHits);
                    geneData.add(g);
                }
                int[] counts = g.getCounts();
                double[] values = g.getValues();
                for (int i = 0; i < numberOfHits; ++i) {
                    int index = indices.get(i);
                    counts[i]++;
                    if (Double.parseDouble(data[index]) == 1.0) {
                        values[i]++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (GeneData g: geneData) {
            System.out.println(g);
        }
    }
}
