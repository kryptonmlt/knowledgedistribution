package org.kryptonmlt.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;
import org.kryptonmlt.networkdemonstrator.utils.IOUtils;

/**
 *
 * @author Kurt
 */
public class BaseLineModelGenerator {

    public static void main(String[] args) throws IOException {
        int[] columns = {1, 2, 3};
        int maxEpoch = 100;
        List<double[]> dataset = new ArrayList<>();
        String dataFileName = "NormalizedData.xlsx";
        String outputFileName = "featureModel.txt";
        OnlineStochasticGradientDescent learning = new OnlineStochasticGradientDescent(0.05);
        Random r = new Random();
        System.out.println("Reading excel file " + dataFileName);
        FileInputStream file = new FileInputStream(new File(dataFileName));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        Iterator<XSSFSheet> sheetsIterator = workbook.iterator();
        while (sheetsIterator.hasNext()) {
            XSSFSheet sheet = sheetsIterator.next();
            Iterator<Row> rows = sheet.iterator();
            while (rows.hasNext()) {
                Row row = rows.next();
                double[] rowData = new double[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    rowData[i] = IOUtils.readCell(row, columns[i]);
                }
                dataset.add(rowData);
            }
        }
        file.close();
        System.out.println("Starting Learning process ..");

        for (int i = 0; i < maxEpoch; i++) {
            System.out.println("Epoch: " + i + " ..");
            for (int j = 0; j < dataset.size(); j++) {
                double[] features = dataset.get(r.nextInt(dataset.size()));
                learning.learn(features[0], features[1], features[2]);
            }
        }
        System.out.println("Finished Learning process ..");
        //compute weights
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
        bw.write(ConversionUtils.cleanDoubleArrayToString(learning.getWeights()) + "\n");
        bw.write("" + dataset.size());
        bw.flush();
        bw.close();
        System.out.println("Finished Writing to file !");
    }
}
