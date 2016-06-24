package org.kryptonmlt.automatedvisualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;

/**
 *
 * @author Kurt
 */
public class DisplayErrorStudy {

    private final static String DIFFERENCE_ERROR = "Difference Error";
    private final static String MESSAGES_SENT = "Messages Sent %";
    private final static String THETA = "Theta";
    private final static String MEAN = "Mean";
    private final static String P = "P";
    private final static String VARIANCE = "Variance";
    private final static String[] MESSAGES_ERROR = {MESSAGES_SENT, DIFFERENCE_ERROR, ""};
    private final static String[] THETA_MESSAGES = {THETA, MESSAGES_SENT, ""};
    private final static String[] THETA_ERROR = {THETA, DIFFERENCE_ERROR, ""};
    private final static String[] THETA_MEAN = {THETA, MEAN, ""};
    private final static String[] THETA_VARIANCE = {THETA, VARIANCE, ""};
    private final static String[] THETA_P = {THETA, P, ""};

    public static void main(String args[]) throws Exception, FileNotFoundException {
        if (args.length != 3) {
            System.err.println("3 parameters needed: FOLDER_PATH WORTH_TYPE VALID_PEERS\n Example: AUTOMATED_ERRORS_STUDY THETA 36");
            return;
        }
        String folderPath = args[0];
        String worth_type = args[1];
        int validPeers = Integer.parseInt(args[2]);

        List<Coord3d> messagesErrorInfo = new ArrayList<>();
        List<Coord3d> thetaMessagesInfo = new ArrayList<>();
        List<Coord3d> thetaErrorInfo = new ArrayList<>();
        List<Coord3d> thetaMean = new ArrayList<>();
        List<Coord3d> thetaVariance = new ArrayList<>();
        List<Coord3d> thetaP = new ArrayList<>();

        //2D Plot
        List<Double> E_DASH = new ArrayList<>();
        List<Double> E = new ArrayList<>();
        List<Double> Y = new ArrayList<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("AND")) {
                BufferedReader br = new BufferedReader(new FileReader(files[i].getPath()));
                int peersCount = Integer.parseInt(br.readLine());
                float t = Float.parseFloat(br.readLine());
                String w = br.readLine();
                if (peersCount == validPeers && worth_type.equals(w)) {
                    System.out.println("File " + files[i].getName() + " matches criteria");
                    float dd = Float.parseFloat(br.readLine());
                    float messagesSentPercent = Float.parseFloat(br.readLine());
                    float m = Float.parseFloat(br.readLine());
                    float v = Float.parseFloat(br.readLine());
                    float p = Float.parseFloat(br.readLine());
                    messagesErrorInfo.add(new Coord3d(messagesSentPercent, dd, 0f));
                    thetaMessagesInfo.add(new Coord3d(t, messagesSentPercent, 0f));
                    thetaErrorInfo.add(new Coord3d(t, dd, 0f));
                    thetaMean.add(new Coord3d(t, m, 0f));
                    thetaVariance.add(new Coord3d(t, v, 0f));
                    thetaP.add(new Coord3d(t, p, 0f));

                    //START STATISTICS
                    String tempLine;
                    while ((tempLine = br.readLine()) != null) {
                        String[] data = tempLine.split(",");
                        E_DASH.add(Double.parseDouble(data[0]));
                        E.add(Double.parseDouble(data[1]));
                        Y.add(Double.parseDouble(data[2]));
                    }
                } else {
                    System.out.println("File " + files[i].getName() + " does not match criteria");
                }
            }
        }
        if (messagesErrorInfo.size() > 2) {
            showGraph(messagesErrorInfo, MESSAGES_ERROR);
            showGraph(thetaMessagesInfo, THETA_MESSAGES);
            showGraph(thetaErrorInfo, THETA_ERROR);
            showGraph(thetaMean, THETA_MEAN);
            showGraph(thetaVariance, THETA_VARIANCE);
            showGraph(thetaP, THETA_P);
        }
        System.out.println("Displaying Statistics Graph");
        Plot2D plot2D = new Plot2D("Sensor Histogram", "Histogram 1-1000", "Steps", "Error");
        plot2D.addIncrementalSeries(Y, "Y", java.awt.Color.BLUE);
        plot2D.addIncrementalSeries(E_DASH, "E'", java.awt.Color.RED);
        plot2D.addIncrementalSeries(E, "E", java.awt.Color.GREEN);
        plot2D.display();
        drawHistogram("E'", E_DASH);
        drawHistogram("E", E);
        drawHistogram("Y", Y);
        
        PDF E_DASH_PDF = new PDF(0.1794, 0.2199);
        drawHistogram("E' GENERATED", E_DASH_PDF.getNextThousand());
        PDF E_PDF = new PDF(2.0243, 4.1247);
        drawHistogram("E GENERATED", E_PDF.getNextThousand());
        PDF Y_PDF = new PDF(1.8816, 3.7859);
        drawHistogram("Y GENERATED", Y_PDF.getNextThousand());
    }

    public static void drawHistogram(String name, List<Double> data) {

        Histogram h = new Histogram(name, 10, Collections.min(data), Collections.max(data));
        for (int i = 0; i < data.size(); i++) {
            h.fill(data.get(i));
        }
        double[] temp = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            temp[i] = data.get(i);
        }
        HistogramDisplay hd = new HistogramDisplay(name, temp);
        hd.display();

    }

    public static void showGraph(List<Coord3d> points, String[] names) throws Exception {
        System.out.println("Displaying graph " + Arrays.toString(names));
        ScatterPlot3D plot = new ScatterPlot3D(names, false);
        plot.show();
        Coord3d[] tempC = new Coord3d[points.size()];
        Color[] colors = new Color[points.size()];
        tempC = points.toArray(tempC);
        Arrays.sort(tempC, coord3dComparator);
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.BLUE;
        }
        plot.setPoints(tempC, colors);
        plot.updateLine(tempC, Color.BLUE, 0);
    }

    public static Comparator<Coord3d> coord3dComparator = (Coord3d c1, Coord3d c2) -> {
        Float x1 = c1.x;
        Float x2 = c2.x;
        return x1.compareTo(x2);
    };
}
