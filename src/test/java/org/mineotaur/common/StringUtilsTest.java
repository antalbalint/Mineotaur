package org.mineotaur.common;

import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.zip.Deflater;


import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by balintantal on 23/07/2015.
 */
@Test(groups = {"unit"})
public class StringUtilsTest {

    @DataProvider(name = "testDecompressStringExceptionDataProvider") 
    public Object[][] testDecompressStringExceptionDataProvider() throws Exception {
        return new Object[][] {{null},{""}};
    }
    
    @Test(dataProvider = "testDecompressStringExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testDecompressStringException(String data) throws Exception {
        StringUtils.decompressString(data);
    }

    private String encodeString(String s) throws UnsupportedEncodingException {
        byte[] input = s.getBytes();
        byte[] output = new byte[1024];
        Deflater compresser = new Deflater();
        compresser.setInput(input);
        compresser.finish();
        compresser.deflate(output);
        compresser.end();
        return new String(Base64.getEncoder().encode(output));
    }

    @DataProvider(name = "testDecompressStringDataProvider")
    public Object[][] testDecompressStringDataProvider() throws Exception {
        String s = "test";
        String encoded = encodeString(s);
        return new Object[][] {
                {encoded, s}
        };

    }

    @Test(dataProvider = "testDecompressStringDataProvider")
    public void testDecompressString(String data, String expected) throws Exception {
        assertEquals(StringUtils.decompressString(data), expected);
    }

    @DataProvider(name = "testDecodeURLExceptionDataProvider")
    public Object[][] testDecodeURLExceptionDataProvider() throws Exception {
        Model model = mock(Model.class);
        MultiValueMap map = mock(MultiValueMap.class);
        List list = mock(List.class);
        List emptyList = mock(List.class);
        when(emptyList.isEmpty()).thenReturn(true);
        MultiValueMap emptyMap = mock(MultiValueMap.class);
        when(emptyMap.isEmpty()).thenReturn(true);
        MultiValueMap noContentMap = mock(MultiValueMap.class);
        when(noContentMap.containsKey("content")).thenReturn(false);
        return new Object[][] {
                {null, map, list},
                {model, null, list},
                {model, emptyMap, list},
                {model, noContentMap, list},
                {model, map, null},
                {model, map, emptyList}
        };
    }

    @Test(dataProvider = "testDecodeURLExceptionDataProvider", expectedExceptions = IllegalArgumentException.class)
    public void testDecodeURLException(Model model, MultiValueMap<String, String> params, List<String> groupNames) throws Exception {
        StringUtils.decodeURL(model, params, groupNames);
    }

    @DataProvider(name = "testDecodeURLDataProvider")
    public Object[][] testDecodeURLDataProvider() throws Exception {
        String term = "test";
        Object value = "1";
        Model model = mock(Model.class);
        MultiValueMap map = mock(MultiValueMap.class);
        List<String> values = new ArrayList<>();
        values.add(encodeString(term + ":" + value));
        when(map.get("content")).thenReturn(values);
        when(map.containsKey("content")).thenReturn(true);
        List list = mock(List.class);
        return new Object[][] {
                {model, map, list,term, value}
        };

    }

    @Test(dataProvider = "testDecodeURLDataProvider")
    public void testDecodeURL(Model model, MultiValueMap<String, String> params, List<String> groupNames, String term, Object value) throws Exception {
        StringUtils.decodeURL(model, params, groupNames);
        verify(params).get("content");
        verify(model).addAttribute(term, value);
    }
}