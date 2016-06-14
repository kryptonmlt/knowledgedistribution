package org.kryptonmlt.networkdemonstrator.sensors;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Kurt
 */
public class DataLoader {

    private final List<double[]> features = new ArrayList<>();

    public DataLoader(int sheetNumber, int startCol, int numberOfFeatures, String filename) {
        try {
            if (filename.endsWith(".xlsx")) {
                FileInputStream file = new FileInputStream(new File(filename));
                XSSFWorkbook workbook = new XSSFWorkbook(file);
                XSSFSheet sheet = workbook.getSheetAt(sheetNumber);
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    double[] f = new double[numberOfFeatures];
                    for (int i = startCol, c = 0; i < startCol + numberOfFeatures; i++, c++) {
                        f[c] = Double.parseDouble(row.getCell(i).getStringCellValue());
                    }
                    features.add(f);
                }
                file.close();
            } else {
                File inputFile = new File(filename);
                FileReader fileReader = new FileReader(inputFile);
                CSVReader reader = new CSVReader(fileReader, ',');
                String[] rowHolder;
                while ((rowHolder = reader.readNext()) != null) {
                    double[] f = new double[numberOfFeatures];
                    for (int i = startCol, c = 0; i < startCol + numberOfFeatures; i++, c++) {
                        f[c] = Double.parseDouble(rowHolder[i]);
                    }
                    features.add(f);
                }
                reader.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public double[] getFeaturesInstance(int i) {
        return features.get(i);
    }

    public float getNumberOfSamplesLoaded() {
        return features.size();
    }
}
