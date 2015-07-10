package org.mineotaur.common;

import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by balintantal on 10/07/2015.
 */
public class StringUtils {

    public static String decompressString(String data) {
        String decompressed = null;
        byte[] byteData = Base64.getDecoder().decode(data);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(byteData.length)){

            Inflater inflater = new Inflater();
            inflater.setInput(byteData);

            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            byte[] output = baos.toByteArray();
            decompressed = new String(output);
        } catch (DataFormatException | IOException e) {
            e.printStackTrace();
        }
        return decompressed;
    }

    public static  Model decodeURL(Model model, MultiValueMap<String, String> params, List<String> groupNames) {

        String data = params.get("content").get(0);
        String decompressed = decompressString(data);
        String[] terms = decompressed.split(",");
        for (String term: terms) {
            String[] parts = term.split(":");
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = parts[i].replaceAll("\"|\\{|\\}","");
            }
            String[] value = parts[1].split("\\|");
            if ("geneList".equals(parts[0]) || "geneListDist".equals(parts[0])) {
                String geneListString = decompressString(parts[1]);
                List<String> geneList = new ArrayList<>();
                char[] chars = geneListString.toCharArray();
                for (int i = 0; i < chars.length; ++i) {
                    if (chars[i] == '1') {
                        geneList.add(groupNames.get(i));
                    }
                }
                model.addAttribute(parts[0], geneList.toArray(new String[geneList.size()]));
            }
            else if (parts[0].startsWith("mapValues")) {
                model.addAttribute(parts[0], Arrays.asList(value));
            }
            else if (value.length == 1){
                model.addAttribute(parts[0], value[0]);
            }
            else {
                model.addAttribute(parts[0], value);

            }
        }
        return model;
    }

}
