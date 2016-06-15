package org.kryptonmlt.networkdemonstrator.sensors;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 *
 * @author Kurt
 */
public class DataLoader {

    private final List<double[]> features = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

    public DataLoader(XSSFSheet sheet, int startCol, int numberOfFeatures, String filename) {
        try {
            if (filename.endsWith(".xlsx")) {
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    double[] f = new double[numberOfFeatures];
                    for (int i = startCol, c = 0; i < startCol + numberOfFeatures; i++, c++) {
                        try {
                            f[c] = Double.parseDouble(row.getCell(i).getStringCellValue());
                        } catch (Exception e) {
                            try {
                                f[c] = sdf.parse(row.getCell(i).getStringCellValue()).getTime();
                            } catch (ParseException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    features.add(f);
                }
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
