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

package org.mineotaur.provider;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.List;
import java.util.Map;

/**
 * Created by balintantal on 14/01/2014.
 */
public interface GraphDatabaseProvider {

    GraphDatabaseService getDatabaseService();

    GlobalGraphOperations getGlobalGraphOperations();

    List<String> getGeneNames();

    List<String> getCellProperties();

    List<String> getAggregationModes();

    Map<String, String> getTimePoints();

    Map<String, org.neo4j.graphdb.Label> getHitLabels();

    Map<org.neo4j.graphdb.Label, String> getHitNames();

    Map<String, Node> getStrainsByName();

    Node findStrainByGenename(String name);

    Map<String, Object> getContext();

    Map<String, String> getTexts();
}

