package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.OnlineVarianceMean;
import org.kryptonmlt.networkdemonstrator.utils.ColorUtils;
import org.kryptonmlt.networkdemonstrator.utils.IOUtils;
import org.kryptonmlt.networkdemonstrator.utils.LearningUtils;
import org.kryptonmlt.networkdemonstrator.utils.VisualizationUtils;

public class DisplayExcelFile3D {

    public static void main(String args[]) throws IOException, Exception {
        int[] colNums = {1, 2, 3};
        String[] names = {"PM25_AQI", "PM10_AQI", "NO2"};
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        int sheets = 36;
        int startSheet = 0;

        FileInputStream file = new FileInputStream(new File("NormalizedData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("temp_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("pm25_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("TestData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("SimpleNormalization.xlsx"));
        ScatterPlot3D plot = new ScatterPlot3D(names, false);
        plot.show();
        Map<Integer, List<Coord3d>> points = new HashMap<>();
        Map<Integer, OnlineStochasticGradientDescent> sgds = new HashMap<>();
        Map<Integer, OnlineVarianceMean> ovms = new HashMap<>();
        for (int i = startSheet; i < sheets; i++) {
            points.put(i, new ArrayList<>());
            sgds.put(i, new OnlineStochasticGradientDescent(0.07));
            ovms.put(i, new OnlineVarianceMean());
        }
        OnlineStochasticGradientDescent general = new OnlineStochasticGradientDescent(0.07);
        System.out.println("Opening file ..");
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = startSheet; i < sheets; i++) {
            int counter = 0;
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            System.out.println("Starting the plot for sheet" + i + ".. ");
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Coord3d point;
                try {
                    double x = IOUtils.readCell(row, colNums[0]);
                    double y = IOUtils.readCell(row, colNums[1]);
                    double z = IOUtils.readCell(row, colNums[2]);
                    point = new Coord3d(x, y, z);
                } catch (Exception e) {
                    point = new Coord3d(sdf.parse(row.getCell(colNums[0]).getStringCellValue()).getTime(),
                            IOUtils.readCell(row, colNums[1]), IOUtils.readCell(row, colNums[2]));

                }
                //VisualizationUtils.drawLine(LearningUtils.computeLine(points.get(i), sgds.get(i)), plot, i - startSheet, ColorUtils.getInstance().getDarkColor(i));
                points.get(i).add(point);
                plot.addPoint(point, ColorUtils.getInstance().getLightColor(i));
                sgds.get(i).learn(point.x, point.y, point.z);
                general.learn(point.x, point.y, point.z);
                if (counter > 1000) {
                    double error = sgds.get(i).predict(point.x, point.y) - point.z;
                    ovms.get(i).update(error * error);
                    //System.out.println("Mean: " + ovms.get(i).getMean() + ", Variance: " + ovms.get(i).getVariance());
                }
                counter++;
            }
        }
        file.close();
        System.out.println("Drawing Lines..");
        for (int i = startSheet; i < sheets; i++) {
            /*BatchGradientDescent bgd = new BatchGradientDescent(0.07);
            bgd.learn(points);*/
            VisualizationUtils.drawLine(LearningUtils.computeLine(points.get(i), sgds.get(i)), plot, i - startSheet, ColorUtils.getInstance().getDarkColor(i));
            System.out.println("Sheet " + i + ": ");
            System.out.println("Weights: " + Arrays.toString(sgds.get(i).getWeights()));
            System.out.println("Mean: " + ovms.get(i).getMean() + ", Variance: " + ovms.get(i).getVariance());
        }
        //VisualizationUtils.drawLine(LearningUtils.computeLine(points.get(0), general), plot, 0, Color.BLACK);
        System.out.println("Finished ..");
    }
}
