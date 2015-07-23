package org.mineotaur.common;

import static org.mockito.Mockito.*;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.*;


/**
 * Created by balintantal on 01/09/2014.
 */
public class FileUtilsTest {

    private String dirName = "testDir";

    @Test
    public void testProcessTextFile() throws Exception {
        List<String> lines = FileUtils.processTextFile("input/mineotaur.input");
        assertNotNull(lines);
        System.out.println(lines.toString());
        assertEquals(lines.size(), 14);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProcessTextFileNull() throws Exception {
        FileUtils.processTextFile((File) null);
        File fileMock = mock(File.class);
        when(fileMock.isFile()).thenReturn(false);
        FileUtils.processTextFile(fileMock);
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSaveListNull() throws Exception {
        List listMock = mock(List.class);
        FileUtils.saveList((File)null,listMock);
        File fileMock = mock(File.class);
        when(fileMock.isFile()).thenReturn(false);
        FileUtils.saveList(fileMock, listMock);
        when(fileMock.isFile()).thenReturn(true);
        when(listMock.isEmpty()).thenReturn(true);
        FileUtils.saveList(fileMock, listMock);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateDirNull()  throws Exception {
        FileUtils.createDir(null, false);
    }

    @Test
    public void testCreateDir() throws Exception {
        FileUtils.createDir(dirName, false);
        File dir = new File(dirName);
        assertTrue(dir.exists());
        String subDirName = dirName+File.separator+dirName;
        File subDir = new File(subDirName);
        FileUtils.createDir(subDirName, false);
        assertTrue(subDir.exists());
        FileUtils.createDir(dirName, true);
        assertTrue(dir.exists());
        assertFalse(subDir.exists());
        FileUtils.deleteDirRecursively(dir);
        assertFalse(dir.exists());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDeleteDirRecursivelyNull() throws Exception {
        FileUtils.deleteDirRecursively(null);
        File fileMock = mock(File.class);
        when(fileMock.isDirectory()).thenReturn(false);
        FileUtils.deleteDirRecursively(fileMock);
    }
}
