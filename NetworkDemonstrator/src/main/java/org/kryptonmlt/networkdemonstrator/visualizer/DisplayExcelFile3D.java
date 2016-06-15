package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;

public class DisplayExcelFile3D {

    public static void main(String args[]) throws IOException, Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        int sheets = 1;
        ArrayList<Coord3d> coords = new ArrayList<>();
        ArrayList<Color> c = new ArrayList<>();
        Color[] choice = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.MAGENTA, Color.GRAY, Color.YELLOW};
        OnlineStochasticGradientDescent sgd = new OnlineStochasticGradientDescent(0.01);
        FileInputStream file = new FileInputStream(new File("NormalizedData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("Data.xlsx"));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = 0; i < sheets; i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    double x = Double.parseDouble(row.getCell(0).getStringCellValue());
                    double y = Double.parseDouble(row.getCell(1).getStringCellValue());
                    double z = Double.parseDouble(row.getCell(2).getStringCellValue());
                    coords.add(new Coord3d(x, y, z));
                    sgd.onlineSGD(x, y, z);
                } catch (Exception e) {
                    coords.add(new Coord3d(sdf.parse(row.getCell(0).getStringCellValue()).getTime(),
                            Double.parseDouble(row.getCell(1).getStringCellValue()), Double.parseDouble(row.getCell(2).getStringCellValue())));
                }
                c.add(choice[i % choice.length]);
            }
        }
        file.close();
        Coord3d[] points = new Coord3d[coords.size()];
        points = coords.toArray(points);
        Arrays.sort(points, coord3dComparator);
        Coord3d[] lineStrip = new Coord3d[coords.size()];
        for (int i = 0; i < lineStrip.length; i++) {
            float z = (float) sgd.predict(points[i].x, points[i].y);
            lineStrip[i] = new Coord3d(points[i].x, points[i].y, z);
        }
        Color[] colors = new Color[c.size()];
        String[] names = {"Time", "PM25_AQI", "PM10_AQI"};
        ScatterPlot3D plot = new ScatterPlot3D(points, c.toArray(colors), names, lineStrip);
        plot.show();
    }

    public static Comparator<Coord3d> coord3dComparator = (Coord3d c1, Coord3d c2) -> {
        Float x1 = c1.x;
        Float x2 = c2.x;
        return x1.compareTo(x2);
    };
}
