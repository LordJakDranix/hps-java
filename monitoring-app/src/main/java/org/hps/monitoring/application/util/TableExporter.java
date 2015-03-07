package org.hps.monitoring.application.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * This is a utility for exporting a JTable's model data to a text file.
 * Non-numeric fields are all contained in double quotes.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class TableExporter {
    
    private TableExporter() {
    }
    
    /**
     * Export the given table to a text file.
     * @param table The JTable component.
     * @param path The output file path.
     * @param fieldDelimiter The field delimiter to use.
     * @throws IOException if there are errors writing the file.
     */
    public static void export(JTable table, String path, char fieldDelimiter) throws IOException {
        
        StringBuffer buffer = new StringBuffer();
        TableModel model = table.getModel();
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();
        
        // Column headers.
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            buffer.append("\"" + model.getColumnName(columnIndex) + "\"" + fieldDelimiter);
        }        
        buffer.setLength(buffer.length() - 1);
        buffer.append('\n');
        
        // Row data.
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                Object value = model.getValueAt(rowIndex, columnIndex);
                if (Number.class.isAssignableFrom(model.getColumnClass(columnIndex))) {
                    buffer.append(value);
                } else {
                    buffer.append("\"" + value + "\"" + fieldDelimiter);
                }
            }    
            buffer.setLength(buffer.length() - 1);
            buffer.append('\n');
        }
                        
        // Write string buffer to file.
        BufferedWriter out = new BufferedWriter(new FileWriter(path));
        out.write(buffer.toString());
        out.flush();
        out.close();
    }    
}