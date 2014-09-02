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
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.Map;

/**
 * Interface for the graph database providers.
 *
 * Created by balintantal on 14/01/2014.
 */
public interface GraphDatabaseProvider {

    /**
     * Method to access the database instance started.
     * @return The graph database service instance.
     */
    GraphDatabaseService getDatabaseService();

    /**
     * Method to access the GlobalGraphOperations instance.
     * @return the GlobalGraphOperations instance.
     */
    GlobalGraphOperations getGlobalGraphOperations();

    /**
     * Method to access the context variables.
     * @return A map containing all the context vaiables.
     */
    Map<String, Object> getContext();

}

