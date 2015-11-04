package org.mineotaur.importer.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * Created by Balint on 2015-10-29.
 */
public class InputWizard {

    public InputWizard(String inputFileName, String separator) {
        this.inputFileName = inputFileName;
        this.separator = separator;
        loadFile();
    }

    private String inputFileName;
    private File inputFile;
    private JFrame frame = new JFrame();
    private String[] header/* = "ddffd,fdff,fdff,fdf,fdfdf,fddf,fdf"*/;
    private String separator/* = ","*/;
    private String[] dataOptions = {"NUMBER", "TEXT", "ID"};
    private String[] guiHeader ={"Column","Object type","Data type"};
    private String[] objectTypes, dataTypes;
    private JTextField fileNameField;
    private boolean succesfullSave=true;

    private void loadFile() {
        inputFile = new File(inputFileName);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));) {
            header = br.readLine().split(separator);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHeader(JPanel panel) {
        for (int i = 0; i < guiHeader.length; ++i) {
            JLabel label = new JLabel(guiHeader[i],JLabel.CENTER);
            panel.add(label);
        }
    }

    private void addLine(JPanel panel, String column, String[] dataTypes) {
        JLabel label = new JLabel(column, JLabel.TRAILING);
        panel.add(label);
        JTextField textField = new JTextField(10);
        panel.add(textField);
        JComboBox comboBox = new JComboBox(dataTypes);
        panel.add(comboBox);
    }

    private ActionListener chooseFileActionListener(){
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new java.io.File("."));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Choose Folder");
                int returnValue = fileChooser.showOpenDialog(SwingUtilities.getAncestorOfClass(JPanel.class, fileChooser));
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String path = selectedFile.getAbsolutePath();
                    String[] terms = fileNameField.getText().split("\\\\");
                    fileNameField.setText(path + File.separator + terms[terms.length-1]);
//                    dataFile = selectedFile.getAbsolutePath();
//                    dataFileNameLabel.setText(selectedFile.getName());
//                    dataFileNameLabel.setVisible(true);
                    //System.out.println(selectedFile.getName());
                }
            }
        };
    }

    private void saveFile(String fileName, String[] objectTypes, String[] dataTypes) {
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile)); PrintWriter pw = new PrintWriter(new File(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (lineCount == 1) {
                    String[][] toBeWritten = {objectTypes, dataTypes};
                    for (String[] lineToBeWritten: toBeWritten) {
                        String s = lineToBeWritten[0] + separator;
                        for (int i = 1; i < lineToBeWritten.length; ++i) {
                            s += lineToBeWritten[i] + separator;
                        }
                        s.substring(0, s.length()-2);
                        pw.println(s);
                    }
                    lineCount += toBeWritten.length;
                }
                pw.println(line);
                lineCount++;
            }

        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(frame,
                    e.getMessage());
            succesfullSave=false;

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    e.getMessage());
            succesfullSave=false;
        }
    }

        private ActionListener saveActionListener(){
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JPanel panel = (JPanel) frame.getContentPane();
                int n = panel.getComponentCount();
                int lineSize = 3;
                int lines = n / lineSize;
                dataTypes = new String[header.length];
                objectTypes = new String[header.length];
                for (int i = 1; i < lines-1; ++i) {
//                    for (int j = 1; j < 2; ++j) {
                        int idx = i*lineSize;
                        JTextField textField = (JTextField) panel.getComponent(idx+1);
//                        String text = textField.getText();
                        objectTypes[i-1] = textField.getText();
                        JComboBox comboBox = (JComboBox) panel.getComponent(idx + 2);
//                        String choice = dataOptions[comboBox.getSelectedIndex()];
                        dataTypes[i-1] = dataOptions[comboBox.getSelectedIndex()];
//                    }

                }
                /*System.out.println(Arrays.toString(objectTypes));
                System.out.println(Arrays.toString(dataTypes));*/
                saveFile(fileNameField.getText(), objectTypes, dataTypes);
                if (succesfullSave) {
                    JOptionPane.showMessageDialog(frame,
                            "Succesfully saved.");
                }

            }
        };
    }

    private ActionListener resetActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                show();
            }
        };
    }

    public void show() {
        frame.setTitle("Mineotaur metadata wizard");
//        String[] terms = header.split(separator);
        int numLines = header.length;
        objectTypes = new String[numLines];
        dataTypes = new String[numLines];
        JPanel panel = new JPanel(new SpringLayout());
        addHeader(panel);
        for (int i = 0; i < numLines; ++i) {
            addLine(panel, header[i], dataOptions);
        }
//        JPanel dirPanel = new JPanel();
        JButton chooseDirButton = new JButton("Choose directory");
        chooseDirButton.addActionListener(chooseFileActionListener());
//        dirPanel.add(chooseDirButton);
        panel.add(chooseDirButton);
        fileNameField = new JTextField(10);
        fileNameField.setText("output.txt");
        panel.add(fileNameField);
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(saveActionListener());
        buttonPanel.add(saveButton);
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(resetActionListener());
        buttonPanel.add(resetButton);
//        panel.add(new JPanel());
//        panel.add(new JPanel());
        panel.add(buttonPanel);
        SpringUtilities.makeCompactGrid(panel, numLines + 2, 3, 6, 6, 6, 6);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

//    public static void main(String[] args) {
//        InputWizard iw = new InputWizard("input\\metadata_test.txt","\t");
//        iw.show();
//    }

}
