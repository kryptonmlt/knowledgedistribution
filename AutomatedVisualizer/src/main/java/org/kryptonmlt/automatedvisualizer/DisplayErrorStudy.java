package org.kryptonmlt.automatedvisualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final static int PDF_MIN = -200;
    private final static int PDF_MAX = 200;

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

        //Theta Mean and Variance
        List<Coord3d> E_DASH_thetaMeanVariance = new ArrayList<>();
        List<Coord3d> E_thetaMeanVariance = new ArrayList<>();
        List<Coord3d> Y_thetaMeanVariance = new ArrayList<>();

        //Query stuff
        Map<Float, Map<Integer, List<Coord3d>>> clusterParameterQuantizedError = new HashMap<>();
        Map<Float, List<Coord3d>> clusterParameterGeneralError = new HashMap<>();

        //2D Plot
        List<Double> E_DASH = new ArrayList<>();
        List<Double> E = new ArrayList<>();
        List<Double> Y = new ArrayList<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        float pastTheta = -1;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("AND")) {
                BufferedReader br = new BufferedReader(new FileReader(files[i].getPath()));
                int peersCount = Integer.parseInt(br.readLine());
                float theta = Float.parseFloat(br.readLine());
                String w = br.readLine();
                if (peersCount == validPeers && worth_type.equals(w)) {
                    System.out.println("File " + files[i].getName() + " matches criteria");
                    float dd = Float.parseFloat(br.readLine());
                    float messagesSentPercent = Float.parseFloat(br.readLine());
                    float m = Float.parseFloat(br.readLine());
                    float v = Float.parseFloat(br.readLine());
                    float p = Float.parseFloat(br.readLine());
                    String[] mVData = br.readLine().split(",");
                    if (pastTheta != theta) {
                        messagesErrorInfo.add(new Coord3d(messagesSentPercent, dd, 0f));
                        thetaMessagesInfo.add(new Coord3d(theta, messagesSentPercent, 0f));
                        thetaErrorInfo.add(new Coord3d(theta, dd, 0f));
                        thetaMean.add(new Coord3d(theta, m, 0f));
                        thetaVariance.add(new Coord3d(theta, v, 0f));
                        thetaP.add(new Coord3d(theta, p, 0f));
                        E_DASH_thetaMeanVariance.add(new Coord3d(theta, Float.parseFloat(mVData[0]), Float.parseFloat(mVData[1])));
                        E_thetaMeanVariance.add(new Coord3d(theta, Float.parseFloat(mVData[2]), Float.parseFloat(mVData[3])));
                        Y_thetaMeanVariance.add(new Coord3d(theta, Float.parseFloat(mVData[4]), Float.parseFloat(mVData[5])));
                    }

                    String t = br.readLine();
                    float clusterParameter; //k or row
                    if (t.contains(".")) {
                        clusterParameter = Float.parseFloat(t);
                    } else {
                        clusterParameter = Integer.parseInt(t);
                    }
                    // closest k
                    String[] closestK = br.readLine().split(",");
                    if (clusterParameterQuantizedError.get(theta) == null) {
                        clusterParameterQuantizedError.put(theta, new HashMap<>());
                        clusterParameterGeneralError.put(theta, new ArrayList<>());
                    }
                    for (String tempK : closestK) {
                        int k = Integer.parseInt(tempK);
                        if (clusterParameterQuantizedError.get(theta).get(k) == null) {
                            clusterParameterQuantizedError.get(theta).put(k, new ArrayList<>());
                        }
                    }
                    // quantized errors
                    String[] queryErrors = br.readLine().split(",");
                    for (int j = 0; j < closestK.length; j++) {
                        int k = Integer.parseInt(closestK[j]);
                        clusterParameterQuantizedError.get(theta).get(k).add(new Coord3d(clusterParameter, Float.parseFloat(queryErrors[j]), 0f));
                    }
                    // general error
                    String generalError = br.readLine();
                    clusterParameterGeneralError.get(theta).add(new Coord3d(clusterParameter, Float.parseFloat(generalError), 0f));

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
                pastTheta = theta;
            }
        }
        System.out.println("Displaying Statistics Graph");
        /*Plot2D plot2D = new Plot2D("Sensor Histogram", "Histogram 1-1000", "Steps", "Error");
        plot2D.addIncrementalSeries(Y, "Y", java.awt.Color.BLUE);
        plot2D.addIncrementalSeries(E_DASH, "E'", java.awt.Color.RED);
        plot2D.addIncrementalSeries(E, "E", java.awt.Color.GREEN);
        plot2D.display();
        drawHistogram("E'", E_DASH);
        drawHistogram("E", E);
        drawHistogram("Y", Y);*/

        drawPDFs("Theta vs E'", E_DASH_thetaMeanVariance);
        drawPDFs("Theta vs E", E_thetaMeanVariance);
        drawPDFs("Theta vs Y", Y_thetaMeanVariance);
        plot2D("", clusterParameterQuantizedError, clusterParameterGeneralError);

        if (messagesErrorInfo.size() > 2) {
            //showGraph(messagesErrorInfo, MESSAGES_ERROR);
            plot2D(messagesErrorInfo, MESSAGES_ERROR, "Messages Sent % vs Difference Error", worth_type + " " + validPeers + " devices");
            //showGraph(thetaMessagesInfo, THETA_MESSAGES);
            plot2D(thetaMessagesInfo, THETA_MESSAGES, "Theta vs Messages Sent %", worth_type + " " + validPeers + " devices");
            //showGraph(thetaErrorInfo, THETA_ERROR);
            plot2D(thetaErrorInfo, THETA_ERROR, "Theta vs Difference Error", worth_type + " " + validPeers + " devices");
            //showGraph(thetaMean, THETA_MEAN);
            plot2D(thetaMean, THETA_MEAN, "Theta vs Mean", worth_type + " " + validPeers + " devices");
            //showGraph(thetaVariance, THETA_VARIANCE);
            plot2D(thetaVariance, THETA_VARIANCE, "Theta vs Variance", worth_type + " " + validPeers + " devices");
            //showGraph(thetaP, THETA_P);
            plot2D(thetaP, THETA_P, "Theta vs P", worth_type + " " + validPeers + " devices");
        }
    }

    public static void drawPDFs(String name, List<Coord3d> data) {
        Plot2D pdfPlot = new Plot2D("Probablity Distribution Function", name, "Error", "P");
        for (Coord3d thetaPDF : data) {
            PDF pdf = new PDF(thetaPDF.y, thetaPDF.z, DisplayErrorStudy.PDF_MIN, DisplayErrorStudy.PDF_MAX);
            Color tc = ColorUtils.getInstance().getNextDarkColor();
            pdfPlot.addSeries(pdf.getNextThousand3D(), "" + thetaPDF.x, new java.awt.Color(tc.r, tc.g, tc.b), false);
        }
        pdfPlot.display();
    }

    public static void plot2D(String name, Map<Float, Map<Integer, List<Coord3d>>> series1, Map<Float, List<Coord3d>> series2) {
        Plot2D pdfPlot = new Plot2D("K/row vs Error", "Query Error", "Clusters", "Error");
        for (Float theta : series1.keySet()) {
            for (Integer k : series1.get(theta).keySet()) {
                List<Coord3d> quantizedData = series1.get(theta).get(k);
                Color tc = ColorUtils.getInstance().getNextDarkColor();
                pdfPlot.addSeries(quantizedData, "Theta: " + theta + ",KNN=" + k + " Quantized Error", new java.awt.Color(tc.r, tc.g, tc.b), false);
            }
            List<Coord3d> generalData = series2.get(theta);
            Color tc = ColorUtils.getInstance().getNextDarkColor();
            pdfPlot.addSeries(generalData, "Theta: " + theta + " KNN=ALL", new java.awt.Color(tc.r, tc.g, tc.b), true);
        }
        pdfPlot.display();
    }

    public static void plot2D(List<Coord3d> points, String[] names, String title, String seriesName) {
        Plot2D pdfPlot = new Plot2D(title, title, names[0], names[1]);
        pdfPlot.addSeries(points, seriesName, java.awt.Color.RED, false);
        pdfPlot.display();
    }

    public static void drawHistogram(String name, List<Double> data) {
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
