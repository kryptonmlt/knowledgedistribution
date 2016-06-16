package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.learning.BatchGradientDescent;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;

public class DisplayExcelFile3D {

    public static void main(String args[]) throws IOException, Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        int sheets = 1;
        Color[] choice = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.MAGENTA, Color.GRAY, Color.YELLOW};
        OnlineStochasticGradientDescent sgd = new OnlineStochasticGradientDescent(0.1);
        //FileInputStream file = new FileInputStream(new File("NormalizedData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("temp_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("pm25_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("TestData.xlsx"));
        FileInputStream file = new FileInputStream(new File("SimpleNormalization.xlsx"));
        String[] names = {"Time", "PM25_AQI", "PM10_AQI"};
        ScatterPlot3D plot = new ScatterPlot3D(names);
        plot.show();
        List<Coord3d> points = new ArrayList<>();

        System.out.println("Opening file ..");
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = 0; i < sheets; i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            System.out.println("Starting the plot for sheet" + i + ".. ");
            //Thread.sleep(1000);
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Coord3d point;
                try {
                    double x = readCell(row, 0);
                    double y = readCell(row, 1);
                    double z = readCell(row, 2);
                    point = new Coord3d(x, y, z);
                } catch (Exception e) {
                    point = new Coord3d(sdf.parse(row.getCell(0).getStringCellValue()).getTime(),
                            Double.parseDouble(row.getCell(1).getStringCellValue()), Double.parseDouble(row.getCell(2).getStringCellValue()));

                }
                //recomputeLine(points, plot, sgd);
                points.add(point);
                Color color = choice[i % choice.length];
                plot.addPoint(point, color);
                sgd.onlineSGD(point.x, point.y, point.z);
            }
        }
        file.close();
        Collections.sort(points, coord3dComparator);
        recomputeLine(points, plot, sgd);
        /*BatchGradientDescent bgd = new BatchGradientDescent(0.07);
        bgd.learn(points);
        recomputeLine(points, plot, bgd);*/
        System.out.println("Finished ..");
    }

    public static void recomputeLine(List<Coord3d> points, ScatterPlot3D plot, OnlineStochasticGradientDescent sgd) {
        Coord3d[] lineStrip = new Coord3d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            float z = (float) sgd.predict(points.get(i).x, points.get(i).y);
            lineStrip[i] = new Coord3d(points.get(i).x, points.get(i).y, z);
        }
        plot.updateLine(lineStrip);
    }

    public static void recomputeLine(List<Coord3d> points, ScatterPlot3D plot, BatchGradientDescent bgd) {
        Coord3d[] lineStrip = new Coord3d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            float z = (float) bgd.predict(points.get(i).x, points.get(i).y);
            lineStrip[i] = new Coord3d(points.get(i).x, points.get(i).y, z);
        }
        plot.updateLine(lineStrip);
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

    public static Comparator<Coord3d> coord3dComparator = (Coord3d c1, Coord3d c2) -> {
        Float x1 = c1.x;
        Float x2 = c2.x;
        return x1.compareTo(x2);
    };
}
