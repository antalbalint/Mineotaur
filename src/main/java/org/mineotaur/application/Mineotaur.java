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

import org.apache.commons.cli.*;

import org.mineotaur.importer.DatabaseGenerator;
import org.mineotaur.importer.DatabaseGeneratorFromFile;
import org.mineotaur.importer.DatabaseGeneratorFromOmero;
import org.mineotaur.importer.gui.ImporterWizard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Main class for mineotaur.
 * Parses command line arguments and either start the Mineotaur, imports a dataset or prints out the usage help.
 */
@ComponentScan(basePackages = {"org.mineotaur.controller", "org.mineotaur.application"})
@EnableAutoConfiguration
public class Mineotaur extends WebMvcConfigurerAdapter {

    /**
     * Logger instance for Mineotaur.
     */
    public static Logger LOGGER = Logger.getLogger(Mineotaur.class.getName());

    /**
     * Name of the current Mineotaur instance.
     */
    public static String name;
    /**
     * Method to parse command line arguments using Apache Commons CLI.
     * @param args command-line arguments.
     */
    protected static void parseArguments(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("start", true, "Starts Mineotaur with the specified database. Parameters: name of the folder containing the Mineotaur data.");
        options.addOption("import", true, "Generates the database from the specified file or from omero. Parameters: from file: property file input file label file; from omero: hostname username password screenID.");
        options.addOption("wizard", true, "Starts the Mineotaur import wizard.");
        options.addOption("help", false, "Prints this help message.");
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("start")) {
                name = args[1];
                SpringApplication.run(Mineotaur.class, args);
            }
            else if (line.hasOption("import")) {
                DatabaseGenerator gen;
                Mineotaur.LOGGER.info(Arrays.toString(args));
                if (args.length == 4) {
                    gen = new DatabaseGeneratorFromFile(args[1], args[2], args[3]);
                }
                else if (args.length == 5) {
                    gen = new DatabaseGeneratorFromOmero(args[1], args[2], args[3], Long.valueOf(args[4]));
                }
                else {
                    throw new UnsupportedOperationException("The number of arguments must be 3 (if importing from files) or 4 (if importing from Omero).");
                }
                gen.generateDatabase();
            }
            else if (line.hasOption("wizard")) {
                ImporterWizard iw = new ImporterWizard();
                iw.pack();
                iw.setVisible(true);
            }
            else {
                formatter.printHelp("Mineotaur", options);
            }

        } catch (ParseException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (Exception e) {

            e.printStackTrace();
        }


    }

    /**
     * Main entry point for the application.
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        parseArguments(args);
    }

}
