/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jdbc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.SwingConstants;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

/**
 *
 * @author Robert
 */
public class GraphView extends JDialog {

    private static final long serialVersionUID = 1L;
    private Connection connection;
    private ArrayList<String> referenced;
    private ArrayList<String> references;
    private ArrayList<Object> objectList;
    private ArrayList<String> alreadyCreated;

    public GraphView(Connection connection) {
        referenced = new ArrayList<>();
        references = new ArrayList<>();
        objectList = new ArrayList<>();
        alreadyCreated = new ArrayList<>();
        this.connection = connection;
        getForeingKeys();
        createGraph();
    }

    public void createGraph() {
        ArrayList<String> auxReferences = new ArrayList<String>();
        for (String aux : references) {
            auxReferences.add(aux);
        }

        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setBackground(Color.getHSBColor(0, 0, 100));
        graphComponent.getViewport().setOpaque(true);
        add(BorderLayout.CENTER, graphComponent);

        graph.getModel().beginUpdate();
        try {
            int x = 20, y = 20;
            for (String ref1 : references) {
                Object v1 = null, v2 = null;
                if (!alreadyCreated.contains(ref1)) {
                    v1 = graph.insertVertex(parent, null, ref1, x, y, 80, 30);
                    x += 100;
                    alreadyCreated.add(ref1);
                    objectList.add(v1);
                } else {
                    v1 = objectList.get(alreadyCreated.indexOf(ref1));
                }
                String ref2 = referenced.get(auxReferences.indexOf(ref1));
                if (!alreadyCreated.contains(ref2)) {
                    v2 = graph.insertVertex(parent, null, ref2, x, y, 80, 30);
                    x += 100;
                    alreadyCreated.add(ref2);
                    objectList.add(v2);
                } else {
                    v2 = objectList.get(alreadyCreated.indexOf(ref2));
                }
                auxReferences.remove(0);
                referenced.remove(0);
                graph.insertEdge(parent, null, null, v1, v2);
            }
            x += 50;
            y += 75;
            for (String ref : referenced) {
                graph.insertVertex(parent, null, ref, x, y, 80, 30);
                x += 50;
                y += 75;
            }
        } finally {
            graph.getModel().endUpdate();
        }

        graph.getModel().beginUpdate();
        try {
            //Hierarchical Layout
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setIntraCellSpacing(10);
            layout.setOrientation(SwingConstants.WEST);
            layout.execute(graph.getDefaultParent());
        } finally {
            graph.getModel().endUpdate();
        }

    }

    public ArrayList<String> getTablesNames() {
        ArrayList<String> relationNames = new ArrayList<String>();
        DatabaseMetaData meta;
        try {
            meta = connection.getMetaData();

            String[] types = {"TABLE"};
            ResultSet tables = meta.getTables(null, null, "%", types);
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                relationNames.add(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return relationNames;
    }

    public void getForeingKeys() {
        DatabaseMetaData meta;
        try {
            meta = connection.getMetaData();
            ArrayList<String> tablesList = getTablesNames();
            for (String table : tablesList) {
                ResultSet rs = null;
                rs = meta.getExportedKeys(connection.getCatalog(), null, table);
                while (rs.next()) {
                    String fkTableName = rs.getString("FKTABLE_NAME");
                    referenced.add(table);
                    references.add(fkTableName);
                }
            }
            for (String table : tablesList) {
                if (!referenced.contains(table) && !references.contains(table)) {
                    referenced.add(table);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
