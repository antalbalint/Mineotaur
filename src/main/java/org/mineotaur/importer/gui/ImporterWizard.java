
package org.mineotaur.importer.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.mineotaur.importer.DatabaseGeneratorFromFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.MouseAdapter;
import java.io.*;
import java.util.Properties;
import java.util.logging.*;


public class ImporterWizard extends JFrame {
    private JPanel panel1;
    private JButton dataButton;
    private JButton labelButton;
    private JSplitPane dataPane;
    private JLabel labelPane;
    private JLabel dataFileNameLabel;
    private JLabel labelFileNameLabel;
    private JTextField nameField;
    private JButton generateTheMineotaurInstanceButton;
    private JTextField overwriteField;
    private JTextField separatorField;
    private JTextField totalMemoryField;
    private JTextField descriptiveField;
    private JTextField groupNameField;
    private JTextField groupField;
    private JTextArea logArea;
    private String dataFile;
    private String labelFile;
    private JFrame frame = this;

    public ImporterWizard() {
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel1.addComponentListener(new ComponentAdapter() {
        });
        panel1.addMouseListener(new MouseAdapter() {
        });
        dataButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(e);
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(SwingUtilities.getAncestorOfClass(JPanel.class, fileChooser));
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    dataFile = selectedFile.getAbsolutePath();
                    dataFileNameLabel.setText(selectedFile.getName());
                    dataFileNameLabel.setVisible(true);
                    //System.out.println(selectedFile.getName());
                }
            }
        });
        labelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(e);

                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(panel1);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    labelFile = selectedFile.getAbsolutePath();

                    labelFileNameLabel.setText(selectedFile.getName());
                    labelFileNameLabel.setVisible(true);
                    //System.out.println(selectedFile.getName());
                }
            }
        });
        this.getContentPane().add(panel1);
        generateTheMineotaurInstanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dataFile == null) {
                    //Mineotaur.LOGGER.info("blah");
                    JOptionPane.showMessageDialog(null, "Please select a data file.");
                } else if (labelFile == null) {
                    JOptionPane.showMessageDialog(null, "Please select a label file.");
                } else if ("".equals(nameField.getText())) {
                    JOptionPane.showMessageDialog(null, "Please provide a name for the Mineotaur instance.");
                } else {
                    generateTheMineotaurInstanceButton.setEnabled(false);
                    generateTheMineotaurInstanceButton.setText("Generating instance...");
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Properties properties = new Properties();
                            properties.put("name", nameField.getText());
                            String s = groupField.getText();
                            if (!"".equals(s)) {
                                properties.put("group", s);
                            }
                            s = groupNameField.getText();
                            if (!"".equals(s)) {
                                properties.put("groupName", s);
                            }
                            s = descriptiveField.getText();
                            if (!"".equals(s)) {
                                properties.put("descriptive", s);
                            }
                            s = totalMemoryField.getText();
                            if (!"".equals(s)) {
                                properties.put("total_memory", s);
                            }
                            s = overwriteField.getText();
                            if (!"".equals(s)) {
                                properties.put("overwrite", s);
                            }
                            s = separatorField.getText();
                            if (!"".equals(s)) {
                                properties.put("separator", s);
                            }
                            DatabaseGeneratorFromFile dg = new DatabaseGeneratorFromFile(properties, dataFile, labelFile);
                            dg.generateDatabase();

                        }
                    });
                    frame.dispose();
                }

                /*final StreamHandler seh = new StreamHandler(System.err, Mineotaur.LOGGER.getHandlers()[0].getFormatter()) {
                    @Override
                    public synchronized void publish(final LogRecord record) {

                        flush();
                    }
                };
                Mineotaur.LOGGER.setUseParentHandlers(false);*/
                /*Mineotaur mineotaur = new Mineotaur();

                Mineotaur.LOGGER.setFilter(new TextAreaFilter(logArea));*/
                //Mineotaur.LOGGER.addHandler(seh);
                /*TextAreaHandler textAreaHandler = new TextAreaHandler();
                textAreaHandler.setTextArea(logArea);*/
                /*MessageConsole mc = new MessageConsole(logArea);
                mc.redirectOut();
                mc.redirectErr(Color.RED, null);
                mc.setMessageLines(100);*/
                /*Mineotaur.LOGGER.addHandler(textAreaHandler);
                Mineotaur.LOGGER.setUseParentHandlers(false);*/


                /*TextArea log = new TextArea();
                log.setEditable(false);

                SimpleFormatter formatter = new SimpleFormatter();

                Mineotaur.LOGGER.addHandler(new Handler() {

                    @Override
                    public void publish(LogRecord record) {
                        logArea.append(formatter.format(record));
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public void close() {
                    }
                });
*/

                /*PrintStream printStream = new PrintStream(new CustomOutputStream(logArea));
                System.setOut(printStream);
                System.setErr(printStream);*/

            }
        });
        /*System.setOut(new PrintStream(new CustomOutputStream(logArea)));
        System.setErr(new PrintStream(new CustomOutputStream(logArea)));*/
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:520px:noGrow", "center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow"));
        panel1.setPreferredSize(new Dimension(520, 520));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        CellConstraints cc = new CellConstraints();
        panel1.add(panel2, cc.xy(1, 1));
        final JLabel label1 = new JLabel();
        label1.setText("Mineotaur Import Wizard");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        dataPane = new JSplitPane();
        dataPane.setDividerLocation(300);
        dataPane.setDividerSize(0);
        dataPane.setEnabled(false);
        panel1.add(dataPane, cc.xy(1, 3));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(0);
        label2.setHorizontalTextPosition(0);
        label2.setText("Select data file *");
        dataPane.setLeftComponent(label2);
        dataButton = new JButton();
        dataButton.setActionCommand("Button");
        dataButton.setLabel("Browse...");
        dataButton.setMaximumSize(new Dimension(29, 29));
        dataButton.setMinimumSize(new Dimension(29, 29));
        dataButton.setPreferredSize(new Dimension(29, 29));
        dataButton.setText("Browse...");
        dataPane.setRightComponent(dataButton);
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setDividerLocation(300);
        splitPane1.setDividerSize(0);
        splitPane1.setEnabled(false);
        panel1.add(splitPane1, cc.xy(1, 7));
        labelPane = new JLabel();
        labelPane.setHorizontalAlignment(0);
        labelPane.setHorizontalTextPosition(0);
        labelPane.setText("Select label file *");
        splitPane1.setLeftComponent(labelPane);
        labelButton = new JButton();
        labelButton.setLabel("Browse...");
        labelButton.setMaximumSize(new Dimension(29, 29));
        labelButton.setMinimumSize(new Dimension(29, 29));
        labelButton.setPreferredSize(new Dimension(29, 29));
        labelButton.setText("Browse...");
        splitPane1.setRightComponent(labelButton);
        dataFileNameLabel = new JLabel();
        dataFileNameLabel.setEnabled(true);
        dataFileNameLabel.setText("Label");
        dataFileNameLabel.setVisible(false);
        panel1.add(dataFileNameLabel, cc.xy(1, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        labelFileNameLabel = new JLabel();
        labelFileNameLabel.setEnabled(true);
        labelFileNameLabel.setText("Label");
        labelFileNameLabel.setVisible(false);
        panel1.add(labelFileNameLabel, cc.xy(1, 9, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JSplitPane splitPane2 = new JSplitPane();
        splitPane2.setDividerLocation(150);
        splitPane2.setDividerSize(0);
        splitPane2.setEnabled(false);
        panel1.add(splitPane2, cc.xy(1, 11));
        final JLabel label3 = new JLabel();
        label3.setText("Name of the instance: *");
        splitPane2.setLeftComponent(label3);
        nameField = new JTextField();
        nameField.setToolTipText("Name of the Mineotaur instance. Required. ");
        splitPane2.setRightComponent(nameField);
        final JSplitPane splitPane3 = new JSplitPane();
        splitPane3.setDividerLocation(150);
        splitPane3.setDividerSize(0);
        splitPane3.setEnabled(false);
        panel1.add(splitPane3, cc.xy(1, 13));
        final JLabel label4 = new JLabel();
        label4.setText("Group object:");
        splitPane3.setLeftComponent(label4);
        groupField = new JTextField();
        groupField.setToolTipText("name of the group object (same as described in the data file). Default: GENE");
        splitPane3.setRightComponent(groupField);
        final JSplitPane splitPane4 = new JSplitPane();
        splitPane4.setDividerLocation(150);
        splitPane4.setDividerSize(0);
        splitPane4.setEnabled(false);
        panel1.add(splitPane4, cc.xy(1, 15));
        final JLabel label5 = new JLabel();
        label5.setText("Group name property:");
        splitPane4.setLeftComponent(label5);
        groupNameField = new JTextField();
        groupNameField.setToolTipText("group object ID (same as described in the data file). Default: geneID");
        splitPane4.setRightComponent(groupNameField);
        final JSplitPane splitPane5 = new JSplitPane();
        splitPane5.setDividerLocation(150);
        splitPane5.setDividerSize(0);
        splitPane5.setEnabled(false);
        panel1.add(splitPane5, cc.xy(1, 17));
        final JLabel label6 = new JLabel();
        label6.setText("Descriptive object");
        splitPane5.setLeftComponent(label6);
        descriptiveField = new JTextField();
        descriptiveField.setToolTipText("name of the group object (same as described in the data file). Default:  CELL");
        splitPane5.setRightComponent(descriptiveField);
        final JSplitPane splitPane6 = new JSplitPane();
        splitPane6.setDividerLocation(150);
        splitPane6.setDividerSize(0);
        splitPane6.setEnabled(false);
        panel1.add(splitPane6, cc.xy(1, 19));
        final JLabel label7 = new JLabel();
        label7.setText("Total memory");
        label7.setToolTipText("");
        splitPane6.setLeftComponent(label7);
        totalMemoryField = new JTextField();
        totalMemoryField.setToolTipText("the amount of memory can be used by Neo4J. Default: 4G");
        splitPane6.setRightComponent(totalMemoryField);
        final JSplitPane splitPane7 = new JSplitPane();
        splitPane7.setDividerLocation(150);
        splitPane7.setDividerSize(0);
        splitPane7.setEnabled(false);
        panel1.add(splitPane7, cc.xy(1, 21));
        final JLabel label8 = new JLabel();
        label8.setText("Separator");
        label8.setToolTipText("");
        splitPane7.setLeftComponent(label8);
        separatorField = new JTextField();
        separatorField.setToolTipText("character used to separate columns in the data and the label files. Default: \\\\t");
        splitPane7.setRightComponent(separatorField);
        final JSplitPane splitPane8 = new JSplitPane();
        splitPane8.setDividerLocation(150);
        splitPane8.setDividerSize(0);
        splitPane8.setEnabled(false);
        panel1.add(splitPane8, cc.xy(1, 23));
        final JLabel label9 = new JLabel();
        label9.setText("Overwrite previous");
        label9.setToolTipText("");
        splitPane8.setLeftComponent(label9);
        overwriteField = new JTextField();
        overwriteField.setToolTipText("whether to overwrite the current instance with the same name. Default: true");
        splitPane8.setRightComponent(overwriteField);
        generateTheMineotaurInstanceButton = new JButton();
        generateTheMineotaurInstanceButton.setText("Generate the Mineotaur instance");
        panel1.add(generateTheMineotaurInstanceButton, cc.xy(1, 25));
        final JLabel label10 = new JLabel();
        label10.setText("<html>* Required fields. Please see <a href=\"http://docs.mineotaur.org\">http://docs.mineotaur.org</a> for instructions.<html>");
        panel1.add(label10, cc.xy(1, 27));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    /*static class TextAreaHandler extends StreamHandler {
        private void configure() {
            setFormatter(new SimpleFormatter());
            try {
                setEncoding("UTF-8");
            } catch (IOException ex) {
                try {
                    setEncoding(null);
                } catch (IOException ex2) {
                    // doing a setEncoding with null should always work.
                    // assert false;
                    ex2.printStackTrace();
                }
            }
        }

        public TextAreaHandler(OutputStream os) {
            super();
            configure();
            setOutputStream(os);
        }

        //@see java/util/logging/ConsoleHandler.java
        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        @Override
        public void close() {
            flush();
        }
    }

    static class TextAreaOutputStream extends OutputStream {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final JTextArea textArea;

        public TextAreaOutputStream(JTextArea textArea) {
            super();
            this.textArea = textArea;
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            buf.flush();
        }

        @Override
        public void close() throws IOException {
            super.close();
            buf.close();
        }

        @Override
        public void write(int b) throws IOException {
            if (b == '\r') {
                return;
            }
            if (b == '\n') {
                final String text = buf.toString("UTF-8");
                buf.reset();
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        textArea.append(text + '\n');
                        textArea.setCaretPosition(textArea.getDocument().getLength());
                    }
                });
                return;
            }
            buf.write(b);
        }
    }*/


    /*private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public void setData(ImporterBean data) {
    }

    public void getData(ImporterBean data) {
    }

    public boolean isModified(ImporterBean data) {
        return false;
    }

    class TextAreaFilter implements Filter {

        public TextAreaFilter(JTextArea area) {
            this.area = area;
        }

        JTextArea area;

        @Override
        public boolean isLoggable(LogRecord record) {
            area.append(record.getMessage());
            return true;
        }
    }


    class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            // redirects data to the text area
            textArea.append(String.valueOf((char) b));
            // scrolls the text area to the end of data
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }*/


}

