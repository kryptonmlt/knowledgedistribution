package org.kryptonmlt.networkdemonstrator.visualizer;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jzy3d.maths.Coord2d;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.OnlineVarianceMean;
import org.kryptonmlt.networkdemonstrator.utils.ColorUtils;
import org.kryptonmlt.networkdemonstrator.utils.IOUtils;

/**
 * endogenous, exogenous, fusion errors study
 *
 * @author Kurt
 */
public class Display2D {

    public static final DecimalFormat df = new DecimalFormat("0.####");
    private static final Map<Integer, List<Coord2d>> pointsX = new HashMap<>();
    private static final Map<Integer, OnlineStochasticGradientDescent> sgdX = new HashMap<>();
    private static final Map<Integer, OnlineVarianceMean> ovmX = new HashMap<>();
    private static int[] colNums;
    private static int LEARN_LIMIT;

    public static void main(String args[]) throws IOException, Exception {
        if (args.length < 5) {
            System.err.println("Requred: FileName column1 column2 LEARN_LIMIT sheet1 ... sheetn");
            return;
        }
        int[] sheets = new int[args.length - 4];
        for (int i = 4; i < args.length; i++) {
            sheets[i - 4] = Integer.parseInt(args[i]);
        }
        colNums = new int[2];
        colNums[0] = Integer.parseInt(args[1]);
        colNums[1] = Integer.parseInt(args[2]);
        LEARN_LIMIT = Integer.parseInt(args[3]);

        for (int i : sheets) {
            pointsX.put(i, new ArrayList<>());
            sgdX.put(i, new OnlineStochasticGradientDescent(0.05));
            ovmX.put(i, new OnlineVarianceMean());
        }

        System.out.println("Opening file ..");
        FileInputStream file = new FileInputStream(new File(args[0]));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i : sheets) {
            int counter = 0;
            XSSFSheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            System.out.println("Reading sheet " + i + ".. ");
            while (rowIterator.hasNext()) {
                if (counter == LEARN_LIMIT) {
                    break;
                }
                Row row = rowIterator.next();
                double x = IOUtils.readCell(row, colNums[0]);
                double y = IOUtils.readCell(row, colNums[1]);
                Coord2d pointX1 = new Coord2d(x, y);
                pointsX.get(i).add(pointX1);
                sgdX.get(i).learn(pointX1.x, 0, pointX1.y);
                /*if (counter > LEARN_LIMIT) {
                    double error1 = sgdX1.get(i).predict(pointX1.x, 0) - pointX1.y;
                    double error2 = sgdX2.get(i).predict(pointX2.x, 0) - pointX2.y;
                    ovmX1.get(i).update(error1 * error1);
                    ovmX2.get(i).update(error2 * error2);
                    System.out.println("X1 - Mean: " + ovmX1.get(i).getMean() + ", Variance: " + ovmX1.get(i).getVariance());
                    System.out.println("X2 - Mean: " + ovmX2.get(i).getMean() + ", Variance: " + ovmX2.get(i).getVariance());
                }*/
                counter++;
            }
        }

        // Run tests
        for (int s1 : sheets) {
            for (int s2 : sheets) {
                compute(s1, s2, workbook);
            }
        }
        file.close();
        plotSheets(sheets);
        System.out.println("All Drawing Finished");
    }

    public static void compute(int s1, int s2, XSSFWorkbook workbook) {
        if (s1 != s2) {
            XSSFSheet sheet = workbook.getSheetAt(s1);
            int errorsSize = 3;
            List<List<Double>> errors = new ArrayList<>();
            double[] errorsAVG = new double[errorsSize];
            for (int i = 0; i < errorsSize; i++) {
                errors.add(new ArrayList<>());
            }

            XSSFRow row;
            int rowNum = LEARN_LIMIT;
            while ((row = sheet.getRow(rowNum)) != null) {
                double x = IOUtils.readCell(row, colNums[0]);
                double y = IOUtils.readCell(row, colNums[1]);

                double f1 = sgdX.get(s1).predict(x, 0);
                double f2 = sgdX.get(s2).predict(x, 0);
                double e0 = Math.pow(f1 - y, 2);
                double e1 = Math.pow(f2 - y, 2);
                double e2 = Math.pow(((f1 + f2) / 2) - y, 2);
                errors.get(0).add(e0);
                errors.get(1).add(e1);
                errors.get(2).add(e2);
                errorsAVG[0] += e0;
                errorsAVG[1] += e1;
                errorsAVG[2] += e2;
                rowNum++;
            }
            for (int i = 0; i < errorsAVG.length; i++) {
                errorsAVG[i] = errorsAVG[i] / (float) (rowNum - LEARN_LIMIT);
            }

            System.out.println("Drawing Sheet " + s1 + " vs " + s2);
            Plot2D plotErrors = new Plot2D("Column " + colNums[0] + " vs " + colNums[1], "Sheet " + s1 + " vs " + s2, "Step", "Error");
            plotErrors.addIncrementalSeries(errors.get(0), "e[1] - endogenous AVG=" + df.format(errorsAVG[0]), Color.RED);
            saveSheet(s1 + "vs" + s2 + "_" + "endogenous_AVG_=" + df.format(errorsAVG[0]), errors.get(0));
            plotErrors.addIncrementalSeries(errors.get(1), "e[2] - exogenous AVG=" + df.format(errorsAVG[1]), Color.BLUE);
            saveSheet(s1 + "vs" + s2 + "_" + "exogenous_AVG_=" + df.format(errorsAVG[1]), errors.get(1));
            plotErrors.addIncrementalSeries(errors.get(2), "e[3] - fusion AVG=" + df.format(errorsAVG[2]), Color.GREEN);
            saveSheet(s1 + "vs" + s2 + "_" + "fusion_AVG_=" + df.format(errorsAVG[2]), errors.get(2));
            plotErrors.display();
            System.out.println("Finished Sheet " + s1 + " vs " + s2);
        }
    }

    public static void saveSheet(String fileName, List<Double> errors) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("OTHER_SENSORS\\"+fileName+".txt"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                sb.append("(").append(i).append(",").append(df.format(errors.get(i))).append(")");
            }
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void plotSheets(int[] sheets) {
        Plot2D plot = new Plot2D("Column " + colNums[0] + " vs " + colNums[1], "Sheets: " + Arrays.toString(sheets), "X (Column" + colNums[0] + ")", "Y (Column" + colNums[1] + ")");
        for (int i : sheets) {
            List<Coord2d> learntPointsX = new ArrayList<>();
            for (int j = 0; j < pointsX.get(i).size(); j++) {
                learntPointsX.add(new Coord2d(pointsX.get(i).get(j).x, sgdX.get(i).predict(pointsX.get(i).get(j).x, 0)));
            }
            org.jzy3d.colors.Color x0 = ColorUtils.getInstance().getNextDarkColor();
            plot.addSeries(pointsX.get(i), "Sensor " + i + ",Points X", new Color(x0.r, x0.g, x0.b), false, false);
            plot.addSeries(learntPointsX, "Sensor " + i + ",Learnt Points X", new Color(x0.r, x0.g, x0.b), true, true);
        }
        plot.display();
    }
}
