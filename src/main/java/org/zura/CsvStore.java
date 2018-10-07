package org.zura.JournalFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public class CsvStore implements IRowStore {
    private OutputStreamWriter writer;
    private CSVPrinter printer;
    CsvStore(OutputStream outStream, List<String> columnHeaders, Properties prop) throws IOException {
        this.writer = new OutputStreamWriter(outStream, prop.getProperty("CSV_CHARSET"));
	    createPrinter(columnHeaders);
    }
    private void createPrinter(List<String> columnHeaders) {
        try {
            printer = CSVFormat
                .EXCEL
                .withHeader(columnHeaders.get(0), columnHeaders.get(1), columnHeaders.get(2), columnHeaders.get(3), columnHeaders.get(4), columnHeaders.get(5))
                .withCommentMarker('#')
                .print(writer);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    public void storeRow(Integer no, String timeStamp, String fileName, String fullPath, String eventInfo, String fileAttr) {
        try {
		    printer.printRecord(no.toString(), timeStamp, fileName, fullPath, eventInfo, fileAttr);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    public void close() {
        try {
            printer.flush();
            printer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        printer = null;
    }
}
