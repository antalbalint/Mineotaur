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

package org.mineotaur.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for common file utilites.
 */
public class FileUtils {

    /**
     * Reads a text file line by line and returns a list containing the lines.
     * @param file Path to the text file.
     * @return the list containing the lines
     */
    public static List<String> processTextFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file.");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static List<String> processTextFile(String file) {
        return processTextFile(new File(file));
    }

    /**
     * Saves a list of strings as lines to a text file
     * @param file Path to the text file.
     * @param list the list containing the lines.
     */
    public static void saveList(File file, List<String> list) {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        /*if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file.");
        }*/
        if (list == null) {
            throw new IllegalArgumentException("List is null.");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List is empty.");
        }
        try (PrintWriter pw = new PrintWriter(file)) {
            for (String s: list) {
                pw.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void saveList(String file, List<String> list) {
        saveList(new File(file), list);
    }

    public static void deleteDirRecursively(File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("File is null.");

        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory.");
        }
        File[] entries = dir.listFiles();
        for (File entry: entries) {
            if (entry.isDirectory()) {
                deleteDirRecursively(entry);
            }
            entry.delete();
        }
        dir.delete();
    }

    /**
     * Method for creating a directory to a give path. If the directory exists and overwrite is true then it deletes the existing directory first.
     * @param name
     */
    public static void createDir(String name, boolean overwrite) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("The provided directory name is either null or empty.");
        }
        File dir = new File(name);
        boolean dirExists = dir.exists();
        if (!dirExists || overwrite) {
            if (dirExists) {
                deleteDirRecursively(dir);
            }
            dir.mkdir();
        }
    }

}
