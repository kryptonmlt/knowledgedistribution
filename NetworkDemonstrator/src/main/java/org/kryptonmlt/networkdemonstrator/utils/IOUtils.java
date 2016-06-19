package org.kryptonmlt.networkdemonstrator.utils;

import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author Kurt
 */
public class IOUtils {

    private IOUtils() {

    }

    public static double readCell(Row row, int i) {
        try {
            return Double.parseDouble(row.getCell(i).getStringCellValue());
        } catch (Exception e) {
            if (e.getLocalizedMessage().contains("Cannot get a text value from a numeric cell")) {
                return row.getCell(i).getNumericCellValue();
            }
            throw e;
        }
    }

}
