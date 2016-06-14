package org.kryptonmlt.networkdemonstrator.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;

public class DisplayExcelFile3D {

    public static void main(String args[]) throws IOException, Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        int sheets = 5;
        ArrayList<Coord3d> coords = new ArrayList<>();
        ArrayList<Color> c = new ArrayList<>();
        
        FileInputStream file = new FileInputStream(new File("NormalizedData.xlsx"));
        //FileInputStream file = new FileInputStream(new File("Data.xlsx"));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        Random r = new Random();
        for (int i = 0; i < sheets; i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            float red = r.nextFloat();
            float green = r.nextFloat();
            float blue = r.nextFloat();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    coords.add(new Coord3d(Double.parseDouble(row.getCell(0).getStringCellValue()),
                            Double.parseDouble(row.getCell(1).getStringCellValue()), Double.parseDouble(row.getCell(2).getStringCellValue())));
                } catch (Exception e) {
                    coords.add(new Coord3d(sdf.parse(row.getCell(0).getStringCellValue()).getTime(),
                            Double.parseDouble(row.getCell(1).getStringCellValue()), Double.parseDouble(row.getCell(2).getStringCellValue())));
                }
                c.add(new Color(red, green, blue));
            }
        }
        file.close();
        Coord3d[] points = new Coord3d[coords.size()];
        Color[] colors = new Color[c.size()];
        ScatterPlot3D plot = new ScatterPlot3D(coords.toArray(points), c.toArray(colors));
        plot.show();
    }
}
