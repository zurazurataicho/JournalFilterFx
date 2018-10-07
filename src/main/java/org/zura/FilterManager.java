package org.zura.JournalFilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Exception;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;


public class FilterManager {
	private String CSV_CHARSET; // = "MS932";    // CSVファイルはShift_JIS(cp932/MS932)
    private Boolean writeOneFile = false;
    private BufferedReader csvIn;
    private IRowStore store;
    private Integer afterFiltered = 0;
    private Integer beforeFiltered = 0;
    private List<String> columnHeaders = new ArrayList<>();
    private List<String> outColumnHeaders = new ArrayList<>();
    private OutputStream outStream = null;
    private Properties prop;
    private String outFilename;
    FilterManager(Properties prop) throws IOException {
        this.prop = prop;
        setProperties();
        setHeaders();
        setOutputHeaders();
    }
    FilterManager(String inFilename, String outFilename, Properties prop) throws IOException {
        this.prop = prop;
        setProperties();
        setHeaders();
        setOutputHeaders();
        setCsvInFile(inFilename);
        this.outFilename = outFilename;
    }
    private void setProperties() {
        CSV_CHARSET = prop.getProperty("CSV_CHARSET");
    }
    private void setCsvInFile(String inFilename) throws IOException {
        csvIn = Files.newBufferedReader(Paths.get(inFilename), Charset.forName(CSV_CHARSET));
    }
    private void setHeaders() {
        columnHeaders.add(prop.getProperty("COL_TEXT_TIMESTAMP"));
        columnHeaders.add(prop.getProperty("COL_TEXT_USN"));
        columnHeaders.add(prop.getProperty("COL_TEXT_FILENAME"));
        columnHeaders.add(prop.getProperty("COL_TEXT_FULLPATH"));
        columnHeaders.add(prop.getProperty("COL_TEXT_EVENTINFO"));
        columnHeaders.add(prop.getProperty("COL_TEXT_SOURCEINFO"));
        columnHeaders.add(prop.getProperty("COL_TEXT_FILEATTRIBUTE"));
    }
    private void setOutputHeaders() {
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_NO"));
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_TIMESTAMP"));
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_FILENAME"));
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_FULLPATH"));
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_EVENTINFO"));
        outColumnHeaders.add(prop.getProperty("OUTPUT_COL_TEXT_FILEATTRIBUTE"));
    }
    private List<CSVRecord> read() throws IOException {
        CSVParser parser = CSVFormat
            .RFC4180
			.withHeader()
            .withIgnoreEmptyLines(true)
            .withIgnoreSurroundingSpaces(true)
            .parse(csvIn);
        return parser.getRecords();
    }
    private void filter(IRowStore store, String regexExt, String regexEvent) throws IOException {
        beforeFiltered = 0;
        afterFiltered = 0;
        for (CSVRecord record : read()) {
            String fileName = record.get(columnHeaders.get(2));
            beforeFiltered += 1;
			Pattern p = Pattern.compile(regexExt, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(fileName);
            if (!m.find()) {
                // オフィスファイルではなかったらスキップ
				continue;
			}
            String eventInfo = record.get(columnHeaders.get(4));
			p = Pattern.compile(regexEvent, Pattern.CASE_INSENSITIVE);
			m = p.matcher(eventInfo);
            if (!m.find()) {
                // 対象イベントではなかった
				continue;
			}

            String timeStamp = record.get(columnHeaders.get(0));
            String usn = record.get(columnHeaders.get(1));
            String fullPath = record.get(columnHeaders.get(3));
            String sourceInfo = record.get(columnHeaders.get(5));
            String fileAttr = record.get(columnHeaders.get(6));
            afterFiltered += 1;
			store.storeRow(afterFiltered, timeStamp, fileName, fullPath, eventInfo, fileAttr);
        }
    }
    private void removePrivacyInformation() throws IOException {
        // 一旦closeしてから再オープンしないと処理できない(ぽい)
        InputStream poiIs = new FileInputStream(outFilename);
        POIFSFileSystem poiFs = new POIFSFileSystem(poiIs);
        DirectoryEntry poiDir = poiFs.getRoot();
        SummaryInformation info = PropertySetFactory.newSummaryInformation();
        info.removeAuthor();
        info.removeLastAuthor();
        // TODO: implementation

        poiFs.close();
        poiIs.close();
    }
    public void close() throws IOException {
        if (outStream != null) {
		    store.close();
            outStream.flush();
            outStream.close();
            outStream = null;
        }
    }
    public void run(StoreType type, String regexExt, String regexEvent) throws IOException {
        if (outStream == null) {
            outStream = Files.newOutputStream(Paths.get(outFilename));
            if (type == StoreType.Xlsx) {
                Integer pos = outFilename.lastIndexOf("/");
                String outFilenameForHeader = outFilename.substring(pos + 1);
                store = new XlsxStore(outStream, outColumnHeaders, prop, Arrays.asList(outFilenameForHeader, prop.getProperty("XLSX_SHEET_NAME")));
            } else {
                store = new CsvStore(outStream, outColumnHeaders, prop);
            }
        }
        filter(store, regexExt, regexEvent);
        if (!writeOneFile) {
            close();
        }
/*
        if (type == StoreType.Xlsx) {
            removePrivacyInformation();
        }
*/
/*
        System.out.println("フィルタ前: " + beforeFiltered + "行");
        System.out.println("フィルタ後: " + afterFiltered + "行");
*/
    }
    public void run(StoreType storeType, String inFilename, String outFilename, Boolean writeOneFile, String regexExt, String regexEvent) throws IOException {
        setCsvInFile(inFilename);
        this.outFilename = outFilename;
        this.writeOneFile = writeOneFile;
        run(storeType, regexExt, regexEvent);
    }
}
