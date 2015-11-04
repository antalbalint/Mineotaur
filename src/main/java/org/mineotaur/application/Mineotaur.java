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

package org.mineotaur.application;

import java.util.logging.Logger;

/**
 * Main class for mineotaur.
 * Parses command line arguments and either start the Mineotaur, imports a dataset or prints out the usage help.
 */
/*
@ComponentScan(basePackages = {"org.mineotaur.controller", "org.mineotaur.application"})
@EnableAutoConfiguration
public class Mineotaur extends WebMvcConfigurerAdapter {
*/
public class Mineotaur {
    /**
     * Logger instance for Mineotaur.
     */
    public static Logger LOGGER = Logger.getLogger(Mineotaur.class.getName());

    /**
     * Name of the current Mineotaur instance.
     */
    public static String name;



}
