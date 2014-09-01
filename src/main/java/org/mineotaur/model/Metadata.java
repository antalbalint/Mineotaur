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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by balintantal on 30/06/2014.
 */
public class Metadata {

    private List<String> nodeTypeNames = new ArrayList<>();
    private List<String> labelNames;
    private Map<String, List<String>> ids = new HashMap<>();
    private String[] header;
    private String[] nodeTypes;
    private String[] dataTypes;
    private static final String ID = "ID";
    private List<String> relationships;
    private List<String> geneNames;

    public Metadata(String[] header, String[] nodeTypes, String[] dataTypes) {
        this.header = header;
        this.nodeTypes = nodeTypes;
        this.dataTypes = dataTypes;
        convertHeadersToMetadata();
    }

    private void convertHeadersToMetadata() {
        addNodeTypeNamesFromArray();
    }

    private void addNodeTypeNamesFromArray() {
        for (String s: nodeTypes) {
            if (!nodeTypeNames.contains(s)) {
                nodeTypeNames.add(s);
            }
        }
    }

    private void addId(String type, String property) {
        List<String> propertyList = ids.get(type);
        if (propertyList == null) {
            propertyList = new ArrayList<>();
        }
        propertyList.add(property);
        ids.put(type, propertyList);
    }

    private void getIdsForNodes() {
        for (int i = 0 ; i < dataTypes.length; ++i) {
            if (ID.equals(dataTypes[i])) {
                addId(dataTypes[i], header[i]);
            }
        }
    }
}
