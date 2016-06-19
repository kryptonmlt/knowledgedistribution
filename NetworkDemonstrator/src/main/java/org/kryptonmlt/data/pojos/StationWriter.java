package org.kryptonmlt.data.pojos;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 *
 * @author Kurt
 */
public class StationWriter extends Station {

    private XSSFSheet sheet;
    private int rowNum = 0;

    public StationWriter(String id, String name, double latitude, double longtitude, XSSFSheet sheet) {
        super(id, name, latitude, longtitude);
        this.sheet = sheet;
    }

    public synchronized void addRow(String[] values) {
        XSSFRow row = sheet.createRow(rowNum);
        int cellNum = 0;
        for (String value : values) {
            XSSFCell cell = row.createCell(cellNum);
            cell.setCellValue(value);
            cellNum++;
        }
        rowNum++;
    }

    public void resetSheet(XSSFSheet sheet) {
        this.sheet = sheet;
        this.rowNum = 0;
    }

}
