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
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.LoggerFactory;

/**
 * Loads data from an excel sheet.
 * @author Kurt
 */
public class DataLoader {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    private final List<double[]> features = new ArrayList<>();
    private final List<double[]> sampledData = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

    public DataLoader(XSSFSheet sheet, XSSFSheet querySheet, int startCol, int numberOfFeatures, String filename, double samplingRate) {

        //check if query sheet is empty
        Iterator<Row> queryIter = querySheet.iterator();
        boolean querySheetFull = queryIter.hasNext();
        // populate sampledData if file is not empty
        while (queryIter.hasNext()) {
            Row queryRow = queryIter.next();
            double[] f = new double[3];
            f[0] = queryRow.getCell(0).getNumericCellValue();
            f[1] = queryRow.getCell(1).getNumericCellValue();
            f[2] = queryRow.getCell(2).getNumericCellValue();
            sampledData.add(f);
        }
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
                    if (!querySheetFull) {
                        double gen = r.nextDouble();
                        if (gen < samplingRate) {
                            XSSFRow queryRow = querySheet.createRow(sampledData.size());
                            queryRow.createCell(0).setCellValue(f[0]);
                            queryRow.createCell(1).setCellValue(f[1]);
                            queryRow.createCell(2).setCellValue(f[2]);
                            sampledData.add(f);
                        }
                    }
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
        System.out.println("Features: " + features.size());
    }

    public List<double[]> getFeatures() {
        return features;
    }

    public List<double[]> getSampledData() {
        return sampledData;
    }
}
