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
 * Created by balintantal on 28/05/2014.
 */
@ComponentScan(basePackages = {"org.mineotaur.controller", "org.mineotaur.application"})
@EnableAutoConfiguration
public class Mineotaur extends WebMvcConfigurerAdapter {

    public static Logger LOGGER = Logger.getLogger(Mineotaur.class.getName());

    private static void parseArguments(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("start", true, "Starts Mineotaur with the specified database.");
        options.addOption("import", true, "Generates the database from the specified file.");
        options.addOption("help", false, "Prints this help message.");
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("start")) {
                //TODO set mine path
                SpringApplication.run(Mineotaur.class, args);
            }
            else if (line.hasOption("import")) {
                DatabaseGenerator gen = new DatabaseGenerator(args[1]);
                gen.generateDatabase(args[2], args[3]);
            }
            else if (line.hasOption("help")) {
                formatter.printHelp("Mineotaur", options);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            formatter.printHelp("Mineotaur", options);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        parseArguments(args);
    }

}
