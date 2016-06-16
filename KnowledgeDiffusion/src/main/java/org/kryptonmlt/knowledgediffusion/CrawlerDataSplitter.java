package org.kryptonmlt.knowledgediffusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kryptonmlt.knowledgediffusion.pojos.StationWriter;
import org.kryptonmlt.knowledgediffusion.tools.Tools;

public class CrawlerDataSplitter {

    public static void main(String args[]) throws IOException, InvalidFormatException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        System.out.println("Reading Stations");
        XSSFWorkbook workbook = new XSSFWorkbook();
        String tempLine;
        BufferedReader stationReader = new BufferedReader(new FileReader("Air Quality Data/Beijing/Station.txt"));
        stationReader.readLine();
        HashMap<String, StationWriter> stations = new HashMap<>();//stores station info
        String normal_type = "simple";

        while ((tempLine = stationReader.readLine()) != null) {
            String[] station = tempLine.split(",");
            if (station.length == 4) {
                stations.put(station[0], new StationWriter(station[0], station[1], Double.parseDouble(station[2]), Double.parseDouble(station[3]), workbook.createSheet(station[0])));
            } else {
                throw new IllegalArgumentException("To populate stations expected size 4 but got " + station.length);
            }
        }
        stationReader.close();
        System.out.println(stations.size() + " Stations successfuly Read");
        System.out.println("Reading sensors..");

        BufferedReader dataReader = new BufferedReader(new FileReader("Air Quality Data/Beijing/CrawledData.txt"));
        String names = dataReader.readLine();
        int cols = Tools.properSplit(names, ",").length - 1;
        ArrayList<String[]> allData = new ArrayList<>();
        String[] total = new String[cols];
        Tools.setAllStringArray(total, "0");

        System.out.println("Calculating max and min..");
        String[][] columnProperties = new String[cols][2];
        for (int i = 0; i < columnProperties.length; i++) {
            columnProperties[i][0] = "-999999999";
            columnProperties[i][1] = "9000000000000";
        }

        int numberOfRows = 0;
        double[] mean = new double[cols];
        double[] variance = new double[cols];
        while ((tempLine = dataReader.readLine()) != null) {
            int loc = tempLine.indexOf(',');
            String data = tempLine.substring(loc + 1);
            String[] values = Tools.properSplit(data, ",");
            allData.add(values);
            for (int i = 0; i < values.length; i++) {
                if ("".equals(values[i])) {
                    values[i] = "0";
                } else {
                    try {
                        double d = Double.parseDouble(values[i]);
                        total[i] = "" + (Double.parseDouble(total[i]) + d);

                        if (d > Double.parseDouble(columnProperties[i][0])) {
                            columnProperties[i][0] = "" + d;
                        }
                        if (d < Double.parseDouble(columnProperties[i][1])) {
                            columnProperties[i][1] = "" + d;
                        }
                    } catch (Exception e) {
                        long l = sdf.parse(values[i]).getTime();
                        total[i] = "" + (Long.parseLong(total[i]) + l);

                        if (l > Long.parseLong(columnProperties[i][0])) {
                            columnProperties[i][0] = "" + l;
                        }
                        if (l < Long.parseLong(columnProperties[i][1])) {
                            columnProperties[i][1] = "" + l;
                        }
                    }
                }
            }
            //add data to correct station using its id.
            stations.get(tempLine.substring(0, loc).trim()).addRow(values);
            numberOfRows++;
        }
        dataReader.close();
        System.out.println("Finished Reading sensors..writing to file");

        FileOutputStream fos = new FileOutputStream(new File("Data.xlsx"));
        workbook.write(fos);
        fos.flush();
        fos.close();

        //reset station sheets
        workbook = new XSSFWorkbook();
        for (String name : stations.keySet()) {
            stations.get(name).resetSheet(workbook.createSheet(name));
        }

        System.out.println("Finished!\nCalculating Mean..");
        //calculate mean
        for (int i = 0; i < total.length; i++) {
            if (total[i].contains(".")) {
                mean[i] = Double.parseDouble(total[i]) / (float) numberOfRows;
            } else {
                mean[i] = Long.parseLong(total[i]) / (float) numberOfRows;
            }
        }
        System.out.println("Mean Calculated..");
        Tools.setAllStringArray(total, "0");
        FileWriter simpleNormalization = new FileWriter(new File("SimpleNormalization.csv"));
        //calculate variance
        for (String[] row : allData) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                double d;
                double v;
                try {
                    v = Double.parseDouble(row[i]);
                    d = v - mean[i];
                    d = d - mean[i];

                    double n = (v - Double.parseDouble(columnProperties[i][0]))
                            / (Double.parseDouble(columnProperties[i][1]) - Double.parseDouble(columnProperties[i][0]));
                    v = n - 0.5;
                } catch (Exception e) {
                    long time = sdf.parse(row[i]).getTime();
                    d = time - mean[i];
                    double n = (time - Long.parseLong(columnProperties[i][0]))
                            / (double) (Long.parseLong(columnProperties[i][1]) - Long.parseLong(columnProperties[i][0]));
                    v = n - 0.5;
                }
                d = d * d;
                total[i] = "" + (Double.parseDouble(total[i]) + d);
                sb.append(v);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("\n");
            simpleNormalization.write(sb.toString());
        }
        simpleNormalization.flush();
        simpleNormalization.close();
        allData = null;
        for (int i = 0; i < total.length; i++) {
            variance[i] = Double.parseDouble(total[i]) / (float) numberOfRows;
        }
        System.out.println("Variance Calculated..");
        System.out.println("Re reading File ..");

        dataReader = new BufferedReader(new FileReader("Air Quality Data/Beijing/CrawledData.txt"));
        dataReader.readLine();
        while ((tempLine = dataReader.readLine()) != null) {
            int loc = tempLine.indexOf(',');
            String data = tempLine.substring(loc + 1);
            String[] values = Tools.properSplit(data, ",");
            for (int i = 0; i < values.length; i++) {
                if ("".equals(values[i])) {
                    values[i] = "0";
                } else {
                    double d;
                    try {
                        d = Double.parseDouble(values[i]) - mean[i];
                        d = d - mean[i];
                    } catch (Exception e) {
                        long time = sdf.parse(values[i]).getTime();
                        d = time - mean[i];
                    }
                    values[i] = "" + (d / variance[i]);
                }
            }
            //add data to correct station using its id.
            stations.get(tempLine.substring(0, loc).trim()).addRow(values);
        }
        dataReader.close();

        System.out.println("Writing to Normalization Excel File..");
        FileOutputStream normalizedFos = new FileOutputStream(new File("NormalizedData.xlsx"));
        workbook.write(normalizedFos);
        normalizedFos.flush();
        normalizedFos.close();
        System.out.println("Finished Normalization Process");
    }
}
