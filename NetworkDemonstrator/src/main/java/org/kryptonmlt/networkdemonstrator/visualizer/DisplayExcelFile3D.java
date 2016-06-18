package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.learning.Learning;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;

public class DisplayExcelFile3D {

    public static void main(String args[]) throws IOException, Exception {
        int[] colNums = {1, 2, 3};
        String[] names = {"PM25_AQI", "PM10_AQI", "NO2"};
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        int sheets = 1;
        int startSheet = 0;
        //Color[] choice = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.MAGENTA, Color.GRAY, Color.YELLOW};
        List<Color> tempColors = generateColors();
        Color[] choice = tempColors.toArray(new Color[tempColors.size()]);
        Color[] darkColors = new Color[choice.length];
        Color[] lightColors = new Color[choice.length];
        for (int i = 0; i < choice.length; i++) {
            darkColors[i] = darkenColor(choice[i]);
            lightColors[i] = lightenColor(choice[i]);
            lightColors[i].a = 0.8f;
            //darkColors[i].a = 0.3f;
        }

        FileInputStream file = new FileInputStream(new File("NormalizedData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("temp_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("pm25_pm10.xlsx"));
        //FileInputStream file = new FileInputStream(new File("TestData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("SimpleNormalization.xlsx"));
        ScatterPlot3D plot = new ScatterPlot3D(names);
        plot.show();
        Map<Integer, List<Coord3d>> points = new HashMap<>();
        Map<Integer, OnlineStochasticGradientDescent> sgds = new HashMap<>();
        for (int i = startSheet; i < sheets; i++) {
            points.put(i, new ArrayList<>());
            sgds.put(i, new OnlineStochasticGradientDescent(0.07));
        }
        System.out.println("Opening file ..");
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = startSheet; i < sheets; i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            System.out.println("Starting the plot for sheet" + i + ".. ");
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Coord3d point;
                try {
                    double x = readCell(row, colNums[0]);
                    double y = readCell(row, colNums[1]);
                    double z = readCell(row, colNums[2]);
                    point = new Coord3d(x, y, z);
                } catch (Exception e) {
                    point = new Coord3d(sdf.parse(row.getCell(colNums[0]).getStringCellValue()).getTime(),
                            Double.parseDouble(row.getCell(colNums[1]).getStringCellValue()), Double.parseDouble(row.getCell(colNums[2]).getStringCellValue()));

                }
                //Collections.sort(points.get(i), coord3dComparator);
                //recomputeLine(points.get(i), plot, sgds.get(i), i - startSheet, darkColors[i % choice.length]);
                points.get(i).add(point);
                plot.addPoint(point, lightColors[i % choice.length]);
                sgds.get(i).learn(point.x, point.y, point.z);
            }
        }
        file.close();
        System.out.println("Drawing Lines..");
        for (int i = startSheet; i < sheets; i++) {            
            /*BatchGradientDescent bgd = new BatchGradientDescent(0.07);
            bgd.learn(points);*/
            Collections.sort(points.get(i), coord3dComparator);
            Color color = darkColors[i % choice.length];
            recomputeLine(points.get(i), plot, sgds.get(i), i - startSheet, color);
            System.out.println("Weights for sheet " + i + ": " + Arrays.toString(sgds.get(i).getWeights()));
        }
        System.out.println("Finished ..");
    }

    public static void recomputeLine(List<Coord3d> points, ScatterPlot3D plot, Learning gd, int sheetNum, Color color) {
        Coord3d[] lineStrip = new Coord3d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            float z = (float) gd.predict(points.get(i).x, points.get(i).y);
            lineStrip[i] = new Coord3d(points.get(i).x, points.get(i).y, z);
        }
        plot.updateLine(lineStrip, color, sheetNum);
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

    public static Color darkenColor(Color c) {
        return new Color(c.r * 0.7f, c.g * 0.6f, c.b * 0.5f, c.a);
    }

    public static Color lightenColor(Color c) {
        return new Color(c.r * 1.3f, c.g * 1.2f, c.b * 1.4f, c.a);
    }

    public static List<Color> generateColors() {
        List<Color> colors = new ArrayList<>();
        for (int r = 0; r < 100; r++) {
            colors.add(new Color(r * 255 / 100, 255, 0));
        }
        for (int g = 100; g > 0; g--) {
            colors.add(new Color(255, g * 255 / 100, 0));
        }
        for (int b = 0; b < 100; b++) {
            colors.add(new Color(255, 0, b * 255 / 100));
        }
        for (int r = 100; r > 0; r--) {
            colors.add(new Color(r * 255 / 100, 0, 255));
        }
        for (int g = 0; g < 100; g++) {
            colors.add(new Color(0, g * 255 / 100, 255));
        }
        for (int b = 100; b > 0; b--) {
            colors.add(new Color(0, 255, b * 255 / 100));
        }
        colors.add(new Color(0, 255, 0));
        Collections.shuffle(colors);
        return colors;
    }
}
