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
import java.util.Random;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class DataLoader {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    private final List<double[]> features = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

    public DataLoader(XSSFSheet sheet, int startCol, int numberOfFeatures, String filename) {
        try {
            Random r = new Random();
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
                                LOGGER.error("Error when trying to parse date", ex);
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
            LOGGER.error("Error when trying to read from datafile..", ex);
        }
        System.out.println("Features: "+features.size());
    }

    public List<double[]> getFeatures() {
        return features;
    }
}
