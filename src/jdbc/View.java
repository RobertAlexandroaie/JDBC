/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jdbc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Robert
 */
public class View extends JFrame {

    private static final long serialVersionUID = 1L;
    private Connection connection = null;
    private JLabel queryLabel;
    private JTextField queryField;
    private JButton submitButton, reportButton, getMetaData, createGraph;
    private JPanel gui, northPanel, westPanel, eastPanel, southPanel;
    private JTextArea resultArea;
    private JTable table;

    public View() {
        connectToDatabase();
        createGui();
    }

    public void createGui() {

        gui = new JPanel(new BorderLayout(20, 20));
        westPanel = new JPanel(new BorderLayout(20, 20));
        northPanel = new JPanel();
        eastPanel = new JPanel();
        southPanel = new JPanel(new GridLayout(1, 2, 20, 20));

        queryLabel = new JLabel("Insert query:");
        queryLabel.setFont(new java.awt.Font("Tahoma", 1, 16));
        queryField = new JTextField(10);

        northPanel = new JPanel(new GridLayout(1, 0, 20, 20));

        submitButton = new JButton("Submit");
        submitButton.setFont(new java.awt.Font("Tahoma", 1, 16));
        submitButton.setContentAreaFilled(false);

        northPanel.add(queryLabel);
        northPanel.add(queryField);
        northPanel.add(submitButton);

        reportButton = new JButton("Report");
        reportButton.setFont(new java.awt.Font("Tahoma", 1, 16));
        reportButton.setContentAreaFilled(false);

        getMetaData = new JButton("Meta data information");
        getMetaData.setFont(new java.awt.Font("Tahoma", 1, 16));
        getMetaData.setContentAreaFilled(false);

        createGraph = new JButton("Create database graph");
        createGraph.setFont(new java.awt.Font("Tahoma", 1, 16));
        createGraph.setContentAreaFilled(false);

        southPanel.add(createGraph);
        southPanel.add(getMetaData);

        resultArea = new JTextArea();

        eastPanel.add(resultArea);

        gui.add(northPanel, BorderLayout.NORTH);
        gui.add(eastPanel, BorderLayout.EAST);
        gui.add(southPanel, BorderLayout.SOUTH);
        gui.add(westPanel, BorderLayout.WEST);
        add(gui);

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String query = queryField.getText();
                if (query.isEmpty()) {
                    JOptionPane.showMessageDialog(View.this,
                            "Insert query first", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    executeQuery(query);
                }
            }
        });

        getMetaData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resultArea.setText("");
                eastPanel.remove(resultArea);
                eastPanel.validate();
                resultArea.append(getTables());
                resultArea.append(getProcedures());
                resultArea.append(getInfo());
                eastPanel.add(resultArea);
                eastPanel.validate();
                validate();
            }
        });

        createGraph.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                GraphView graph = new GraphView(connection);
                graph.setModal(true);
                graph.pack();
                graph.setVisible(true);
            }
        });

        reportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                DefaultTableModel de = (DefaultTableModel) table.getModel();
                JRTableModelDataSource datasource = new JRTableModelDataSource(de);
                String reportSource = "example.jrxml";

                JasperReport jr;
                try {
                    jr = JasperCompileManager.compileReport(reportSource);

                    Map<String, Object> params = new HashMap<>();
                    params.put("title1", "title 1");

                    JasperPrint jp = JasperFillManager.fillReport(jr, params, datasource);

                    JasperViewer.viewReport(jp, false);

                } catch (JRException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void executeQuery(String query) {
        westPanel.removeAll();
        westPanel.validate();
        remove(westPanel);
        validate();
        Statement s;
        int counter = 1;
        try {
            s = connection.createStatement();

            ResultSet r = s.executeQuery(query);
            int columns = r.getMetaData().getColumnCount();
            int rows;
            int currentRow = r.getRow();
            rows = r.last() ? r.getRow() : 0;

            if (currentRow == 0) {
                r.beforeFirst();
            } else {
                r.absolute(currentRow);
            }

            Object data[][] = new Object[rows][columns];

            String[] columnNames = new String[columns];

            while (r.next()) {
                for (int i = 1; i <= columns; i++) {
                    data[counter - 1][i - 1] = r.getObject(i);
                    columnNames[i - 1] = r.getMetaData().getColumnName(i);
                }
                counter++;
            }

            table = null;
            table = new JTable();
            table.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));

            JScrollPane scrollPane = new JScrollPane(table);
            JInternalFrame frame = new JInternalFrame();
            frame.add(scrollPane);
            add(frame, BorderLayout.SOUTH);
            frame.add(reportButton, BorderLayout.NORTH);
            frame.setVisible(true);
            westPanel.add(frame, BorderLayout.CENTER);
            gui.add(westPanel, BorderLayout.CENTER);
            validate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(View.this,
                    "Query error, please try again", "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public String getInfo() {
        StringBuilder returnValue = new StringBuilder();
        DatabaseMetaData dbmd;
        try {
            dbmd = connection.getMetaData();
            returnValue.append(("URL in use: " + dbmd.getURL())).append("\n");
            returnValue.append("User name: " + dbmd.getUserName()).append("\n");
            returnValue.append("DBMS name: " + dbmd.getDatabaseProductName())
                    .append("\n");
            returnValue.append(
                    "DBMS version: " + dbmd.getDatabaseProductVersion())
                    .append("\n");
            returnValue.append("Driver name: " + dbmd.getDriverName()).append(
                    "\n");
            returnValue.append("Driver version: " + dbmd.getDriverVersion())
                    .append("\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return returnValue.toString();
    }

    public String getTables() {
        StringBuilder returnValue = new StringBuilder();
        DatabaseMetaData dbmd;
        try {
            dbmd = connection.getMetaData();

            ResultSet rs = dbmd.getTables(null, null, null, null);
            returnValue.append("Tables: \n");
            while (rs.next()) {
                returnValue.append(rs.getString("TABLE_NAME")).append(" ");
            }
            returnValue.append("\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return returnValue.toString();
    }

    public String getProcedures() {
        StringBuilder returnValue = new StringBuilder();
        DatabaseMetaData dbmd;
        try {
            dbmd = connection.getMetaData();

            ResultSet rs;
            returnValue.append("Procedures: \n");
            rs = dbmd.getProcedures(null, null, null);
            while (rs.next()) {
                returnValue.append(rs.getString("TABLE_NAME")).append(" ");
            }
            returnValue.append("\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return returnValue.toString();

    }

    public void connectToDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Error, MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }

        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/testdb", "root", "");

        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return;
        }

        if (connection == null) {
            System.out.println("Failed to make connection!");
        }
    }
}
