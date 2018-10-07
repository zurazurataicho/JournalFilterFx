package org.zura.JournalFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.poi.ooxml.POIXMLProperties.CoreProperties;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class XlsxStore implements IRowStore {
    private Properties prop;
    private OutputStream outStream;
    private List<String> columnHeaders;
    private List<String> headerText;
    private Integer currentLine = 0;
    //private Workbook xlsxBook = new SXSSFWorkbook();
    private XSSFWorkbook xlsxBook = new XSSFWorkbook();
    private Sheet xlsxSheet;
    private Font xlsxHeaderFont;
    private Font xlsxBodyFont;
    private DataFormat xlsxFormat;
    private CellStyle styleHeader;
    private CellStyle styleDateTime;
    private CellStyle styleNumeric;
    private CellStyle styleString;

    // 構造体
    private class CellContent {
        private String content;
        private CellStyle style;
        private DataType dataType;
        CellContent(String content, CellStyle style, DataType dataType) {
            this.content = content;
            this.style = style;
            this.dataType = dataType;
        }
        public String getContent() {
            return content;
        }
        public CellStyle getStyle() {
            return style;
        }
        public DataType getDataType() {
            return dataType;
        }
    }

    XlsxStore(OutputStream outStream, List<String> columnHeaders, Properties prop, List<String> headerText) {
        this.outStream = outStream;
        this.columnHeaders = columnHeaders;
        this.prop = prop;
        this.headerText = headerText;
        setSheetStyles();
        setCellStyles();
    }
    private void setSheetStyles() {
        xlsxSheet = xlsxBook.createSheet();
        if (xlsxSheet instanceof SXSSFSheet) {
            ((SXSSFSheet)xlsxSheet).trackAllColumnsForAutoSizing();
        }
        // ヘッダ用
        xlsxHeaderFont = xlsxBook.createFont();
        xlsxHeaderFont.setFontName(prop.getProperty("XLSX_SHEET_FONT"));
        xlsxHeaderFont.setFontHeightInPoints(Short.parseShort(prop.getProperty("XLSX_SHEET_FONT_POINT")));
        xlsxHeaderFont.setBold(true);
        // 本体
        xlsxBodyFont = xlsxBook.createFont();
        xlsxBodyFont.setFontName(prop.getProperty("XLSX_SHEET_FONT"));
        xlsxBodyFont.setFontHeightInPoints(Short.parseShort(prop.getProperty("XLSX_SHEET_FONT_POINT")));
        xlsxFormat = xlsxBook.createDataFormat();
        xlsxBook.setSheetName(0, prop.getProperty("XLSX_SHEET_NAME"));
        // カラム幅
        Integer column = 0;
        String[] widthList = prop.getProperty("XLSX_SHEET_COLUMNS_WIDTH").split(",");
        for (String width : widthList) {
            xlsxSheet.setColumnWidth(column, Integer.parseInt(width) * 256);
            column += 1;
        }
        // ページ設定
        PrintSetup printSetup = xlsxSheet.getPrintSetup();
        printSetup.setLandscape(true);  // 印刷の向き
        // 横幅が1ページに収まるように印刷
        // TODO: 機能しない。要調査 20180926
        printSetup.setFitWidth((short)1);
        printSetup.setFitHeight((short)0);
        // ヘッタ・フッタ
        Header header = xlsxSheet.getHeader();
        //header.setCenter(prop.getProperty("XLSX_SHEET_HEADER_CENTER_TEXT"));
        String totalHeaderText = new String();
        for (String text : headerText) {
            totalHeaderText += text;
        }
        header.setCenter(totalHeaderText);
        Footer footer = xlsxSheet.getFooter();
        footer.setCenter(prop.getProperty("XLSX_SHEET_FOOTER_CENTER_TEXT"));
        // 印刷タイトル
        xlsxSheet.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
    }
    private void setCellStyles() {
        // ヘッダ
        styleHeader = xlsxBook.createCellStyle();
        setBorders(styleHeader);
        setHeaderStyles(styleHeader);
        setHeaderText(styleHeader);
        // 日時表示用スタイル
        styleDateTime = xlsxBook.createCellStyle();
        setBorders(styleDateTime);
        setCellContents(styleDateTime, prop.getProperty("XLSX_SHEET_FORMAT_DATETIME"));
        // 文字列用スタイル
        styleString = xlsxBook.createCellStyle();
        setBorders(styleString);
        styleString.setWrapText(true);
        setCellContents(styleString, BuiltinFormats.getBuiltinFormat(0));
        // 整数用スタイル
        styleNumeric = xlsxBook.createCellStyle();
        setBorders(styleNumeric);
        setCellContents(styleNumeric, prop.getProperty("XLSX_SHEET_FORMAT_DATA"));
    }
    private void setHeaderStyles(CellStyle style) {
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFont(xlsxHeaderFont);
    }
    private void setHeaderText(CellStyle style) {
        Integer column = 0;
        Row row = xlsxSheet.createRow(0);
        for (String columnName : columnHeaders) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(styleHeader);
            cell.setCellType(CellType.STRING);
            cell.setCellValue(columnName);
            //xlsxSheet.autoSizeColumn(column, true); // 列幅自動調整
            column += 1;
        }
        // 先頭行固定
        xlsxSheet.createFreezePane(1, 1);
        // ヘッダ行のオートフィルタ設定
        xlsxSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, column - 1));

        currentLine += 1;
    }
    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
    private void setCellContents(CellStyle style, String format) {
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFont(xlsxBodyFont);
        style.setDataFormat(xlsxFormat.getFormat(format));
    }
    private void setBodyRow(List<CellContent> cellContents) {
        Integer column = 0;
        Row row = xlsxSheet.createRow(currentLine);
        for (CellContent cellContent : cellContents) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(cellContent.getStyle());

            DataType dataType = cellContent.getDataType();
            switch (dataType) {
            case Numeric:
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(Double.parseDouble(cellContent.getContent()));
                break;
            case Datetime:
                cell.setCellType(CellType.STRING);
                cell.setCellValue(convertStringDateTimeToExcelDate(cellContent.getContent()));
                break;
            default:
                cell.setCellType(CellType.STRING);
                cell.setCellValue(cellContent.getContent());
            }
            column += 1;
        }
        currentLine += 1;
    }
    private double convertStringDateTimeToExcelDate(String dateTime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return DateUtil.getExcelDate(dateFormat.parse(dateTime));
        } catch (ParseException e) {
            System.out.println(e);
        }
        return 0.0;
    }
    public void storeRow(Integer no, String timeStamp, String fileName, String fullPath, String eventInfo, String fileAttr) {
        List<CellContent> bodyRow = Arrays.asList(
            new CellContent(no.toString(), styleNumeric, DataType.Numeric),
            new CellContent(timeStamp, styleDateTime, DataType.Datetime),
            new CellContent(fileName, styleString, DataType.String),
            new CellContent(fullPath, styleString, DataType.String),
            new CellContent(eventInfo, styleString, DataType.String),
            new CellContent(fileAttr, styleString, DataType.String)
        );
        setBodyRow(bodyRow);
    }
    public void close() {
        try {
            xlsxBook.write(outStream);
            xlsxBook.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
