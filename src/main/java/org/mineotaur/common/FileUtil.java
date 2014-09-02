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
public class FileUtil {

    /**
     * Reads a text file line by line and returns a list containing the lines.
     * @param file Path to the text file.
     * @return the list containing the lines
     */
    public static List<String> processTextFile(String file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    /**
     * Saves a list of strings as lines to a text file
     * @param file Path to the text file.
     * @param list the list containing the lines.
     */
    public static void saveList(String file, List<String> list) {
        try (PrintWriter pw = new PrintWriter(file)) {
            for (String s: list) {
                pw.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
