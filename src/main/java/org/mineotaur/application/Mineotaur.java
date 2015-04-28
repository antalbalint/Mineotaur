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

import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.apache.commons.cli.*;
import org.mineotaur.importer.DatabaseGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
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
    private static void parseArguments(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("start", true, "Starts Mineotaur with the specified database. Parameters: name of the folder containing the Mineotaur data.");
        options.addOption("import", true, "Generates the database from the specified file. Parameters: property file input file label file.");
        options.addOption("help", false, "Prints this help message.");
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("start")) {
                name = args[1];
                SpringApplication.run(Mineotaur.class, args);
            }
            else if (line.hasOption("import")) {
                DatabaseGenerator gen = new DatabaseGenerator(args[1]);
                gen.generateDatabase(args[2], args[3]);
            }
            else {
                formatter.printHelp("Mineotaur", options);
            }
        } catch (IllegalAccessException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (CannotCompileException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (NotFoundException e) {
            System.err.println("For the current usage use the -help handle.");
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
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
