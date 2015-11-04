package org.mineotaur.application;

import org.apache.commons.cli.*;
import org.mineotaur.importer.DatabaseGenerator;
import org.mineotaur.importer.DatabaseGeneratorFromFile;
import org.mineotaur.importer.DatabaseGeneratorFromOmero;
import org.mineotaur.importer.gui.ImporterWizard;
import org.mineotaur.importer.gui.InputWizard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.FileReader;
import java.util.PropertyResourceBundle;

/**
 * Created by Balint on 2015-09-21.
 */
@ComponentScan(basePackages = {"org.mineotaur.controller", "org.mineotaur.application"})
@EnableAutoConfiguration
public class MineotaurStandalone extends WebMvcConfigurerAdapter {

    /**
     * Method to parse command line arguments using Apache Commons CLI.
     * @param args command-line arguments.
     */
    protected static void parseArguments(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("start", true, "Starts Mineotaur with the specified database. Parameters: name of the folder containing the Mineotaur data.");
        options.addOption("import", true, "Generates the database from the specified file or from omero. Parameters: from file: property file input file label file; from omero: hostname username password screenID.");
        options.addOption("wizard", false, "Starts the Mineotaur import wizard.");
        options.addOption("metadata", false, "Starts the Mineotaur metadata wizard. The first parameter is the original data file to be loaded and the second parameter is the separator character used in the file.");

        options.addOption("help", false, "Prints this help message.");
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("start")) {
                Mineotaur.name = args[1];
                SpringApplication.run(MineotaurStandalone.class, args);
            }
            else if (line.hasOption("import")) {
                DatabaseGenerator gen;
                if (args.length == 4) {
                    gen = new DatabaseGeneratorFromFile(args[1], args[2], args[3]);
                }
                else if (args.length == 2) {
                    gen = new DatabaseGeneratorFromOmero(new PropertyResourceBundle(new FileReader((args[1]))));
                }
                /*else if (args.length == 5) {
                    gen = new DatabaseGeneratorFromOmero(args[1], args[2], args[3], Long.valueOf(args[4]));
                }*/
                else {
                    throw new UnsupportedOperationException("The number of arguments must be 3 (if importing from files) or 1 (if importing from Omero).");
                }
                gen.generateDatabase();
            }
            else if (line.hasOption("wizard")) {

                ImporterWizard iw = new ImporterWizard();
                iw.pack();
                iw.setVisible(true);
            }
            else if (line.hasOption("metadata")) {
                if (args.length == 3) {
                    InputWizard iw = new InputWizard(args[1], args[2]);
                    iw.show();
                }
                else {
                    throw new UnsupportedOperationException("The number of arguments must be 3");
                }

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
