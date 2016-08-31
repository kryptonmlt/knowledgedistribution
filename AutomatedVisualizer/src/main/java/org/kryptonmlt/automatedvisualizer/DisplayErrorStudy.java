package org.kryptonmlt.automatedvisualizer;

import org.kryptonmlt.automatedvisualizer.utils.ColorUtils;
import org.kryptonmlt.automatedvisualizer.utils.PDF;
import org.kryptonmlt.automatedvisualizer.utils.StatsUtil;
import org.kryptonmlt.automatedvisualizer.plots.ScatterPlot3D;
import org.kryptonmlt.automatedvisualizer.plots.SurfacePlot3D;
import org.kryptonmlt.automatedvisualizer.plots.Plot2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final static int PDF_MIN = -20;
    private final static int PDF_MAX = 20;

    public static void main(String args[]) throws Exception, FileNotFoundException {
        if (args.length < 4) {
            System.err.println("4 parameters needed: FOLDER_PATH WORTH_TYPE VALID_PEERS SHOW_LABELS\n Example: AUTOMATED_ERRORS_STUDY THETA 36 false");
            return;
        }
        String folderPath = args[0];
        String worth_type = args[1];
        int validPeers = Integer.parseInt(args[2]);
        boolean showLabels = Boolean.parseBoolean(args[3]);
        Float thetaToShow = null;
        if (args.length > 4) {
            thetaToShow = Float.parseFloat(args[4]);
        }

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
        Map<Float, Map<Integer, List<Coord3d>>> clusterParameterQuantizedErrorDistanceOnly = new HashMap<>();
        Map<Float, List<Coord3d>> clusterParameterGeneralError = new HashMap<>();
        Map<Float, List<Coord3d>> clusterParameterIdealError = new HashMap<>();
        Map<Float, List<Coord3d>> clusterParameterBaseLineError = new HashMap<>();

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
                if (thetaToShow == null || (thetaToShow != null && theta == thetaToShow)) {
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

                            String[] t = br.readLine().split(",");
                            //k or row
                            float[] clusterParameter = new float[t.length];
                            for (int j = 0; j < t.length; j++) {
                                if (t[j].contains(".")) {
                                    clusterParameter[j] = Float.parseFloat(t[j]);
                                } else {
                                    clusterParameter[j] = Integer.parseInt(t[j]);
                                }
                            }
                            // closest k
                            String[] closestK = br.readLine().split(",");
                            if (clusterParameterQuantizedError.get(theta) == null) {
                                clusterParameterQuantizedError.put(theta, new HashMap<>());
                                clusterParameterQuantizedErrorDistanceOnly.put(theta, new HashMap<>());
                                clusterParameterGeneralError.put(theta, new ArrayList<>());
                                clusterParameterIdealError.put(theta, new ArrayList<>());
                                clusterParameterBaseLineError.put(theta, new ArrayList<>());
                            }
                            for (String tempK : closestK) {
                                int k = Integer.parseInt(tempK);
                                if (clusterParameterQuantizedError.get(theta).get(k) == null) {
                                    clusterParameterQuantizedError.get(theta).put(k, new ArrayList<>());
                                    clusterParameterQuantizedErrorDistanceOnly.get(theta).put(k, new ArrayList<>());
                                }
                            }
                            // quantized errors
                            for (int j = 0; j < closestK.length; j++) {
                                String[] queryErrors = br.readLine().split(",");
                                int k = Integer.parseInt(closestK[j]);
                                for (int l = 0; l < clusterParameter.length; l++) {
                                    clusterParameterQuantizedError.get(theta).get(k).add(new Coord3d(clusterParameter[l], Float.parseFloat(queryErrors[l]), 0f));
                                }
                            }
                            for (int j = 0; j < closestK.length; j++) {
                                String[] queryErrorsDistanceOnly = br.readLine().split(",");
                                int k = Integer.parseInt(closestK[j]);
                                for (int l = 0; l < clusterParameter.length; l++) {
                                    clusterParameterQuantizedErrorDistanceOnly.get(theta).get(k).add(new Coord3d(clusterParameter[l], Float.parseFloat(queryErrorsDistanceOnly[l]), 0f));
                                }
                            }
                            // general error
                            String generalError = br.readLine();
                            for (int l = 0; l < clusterParameter.length; l++) {
                                clusterParameterGeneralError.get(theta).add(new Coord3d(clusterParameter[l], Float.parseFloat(generalError), 0f));
                            }

                            // ideal error
                            String idealError = br.readLine();
                            for (int l = 0; l < clusterParameter.length; l++) {
                                clusterParameterIdealError.get(theta).add(new Coord3d(clusterParameter[l], Float.parseFloat(idealError), 0f));
                            }
                            String baseLineError = br.readLine();
                            for (int l = 0; l < clusterParameter.length; l++) {
                                clusterParameterBaseLineError.get(theta).add(new Coord3d(clusterParameter[l], Float.parseFloat(baseLineError), 0f));
                            }

                            //START STATISTICS
                            String tempLine;
                            while ((tempLine = br.readLine()) != null) {
                                String[] data = tempLine.split(",");
                                E_DASH.add(Double.parseDouble(data[0]));
                                E.add(Double.parseDouble(data[1]));
                                Y.add(Double.parseDouble(data[2]));
                            }
                        }
                        pastTheta = theta;
                    } else {
                        System.out.println("File " + files[i].getName() + " does not match criteria");
                    }
                }
            }
        }

        drawPDFs("Theta vs E'", E_DASH_thetaMeanVariance, false);
        drawPDFs("Theta vs E", E_thetaMeanVariance, false);
        drawPDFs("Theta vs Y", Y_thetaMeanVariance, false);
        
        List<Coord3d> klDiv = new ArrayList<>();
        for (int i = 0; i < E_DASH_thetaMeanVariance.size(); i++) {
            Coord3d tempPoint = new Coord3d(E_thetaMeanVariance.get(i).x,
                    StatsUtil.klDivergence2PDFs(E_DASH_thetaMeanVariance.get(i).y, E_DASH_thetaMeanVariance.get(i).z,
                            E_thetaMeanVariance.get(i).y, E_thetaMeanVariance.get(i).z), 0.0);
            klDiv.add(tempPoint);
        }
        String[] KLDIV_NAMES = {"THETA", "Kullback Leibler Divergence"};
        plot2D(klDiv, KLDIV_NAMES, "Concentrator - Local PDF Analysis", "KL Divergance vs THETA", false);
        
        plot2D("", clusterParameterQuantizedError, clusterParameterQuantizedErrorDistanceOnly, clusterParameterGeneralError, clusterParameterIdealError, showLabels);
        String[] xyzNames = {"Clusters", "KNN", "Error"};
        Set<Integer> knn = clusterParameterQuantizedErrorDistanceOnly.get(clusterParameterQuantizedErrorDistanceOnly.keySet().iterator().next()).keySet();
        showErrors(clusterParameterQuantizedError, clusterParameterQuantizedErrorDistanceOnly,
                SurfacePlot3D.convertKNN0(clusterParameterGeneralError, knn), SurfacePlot3D.convertKNN0(clusterParameterIdealError, knn),
                SurfacePlot3D.convertKNN0(clusterParameterBaseLineError, knn), xyzNames);

        if (messagesErrorInfo.size() > 2) {
            //showGraph(messagesErrorInfo, MESSAGES_ERROR);
            plot2D(messagesErrorInfo, MESSAGES_ERROR, "Messages Sent % vs Difference Error", worth_type + " " + validPeers + " devices", false);
            //showGraph(thetaMessagesInfo, THETA_MESSAGES);
            plot2D(thetaMessagesInfo, THETA_MESSAGES, "Theta vs Messages Sent %", worth_type + " " + validPeers + " devices", false);
            //showGraph(thetaErrorInfo, THETA_ERROR);
            plot2D(thetaErrorInfo, THETA_ERROR, "Theta vs Difference Error", worth_type + " " + validPeers + " devices", false);
            //showGraph(thetaMean, THETA_MEAN);
            plot2D(thetaMean, THETA_MEAN, "Theta vs Mean", worth_type + " " + validPeers + " devices", false);
            //showGraph(thetaVariance, THETA_VARIANCE);
            plot2D(thetaVariance, THETA_VARIANCE, "Theta vs Variance", worth_type + " " + validPeers + " devices", false);
            //showGraph(thetaP, THETA_P);
            plot2D(thetaP, THETA_P, "Theta vs P", worth_type + " " + validPeers + " devices", false);
        }
        System.out.println("Finished!");
    }

    public static void drawPDFs(String name, List<Coord3d> data, boolean showLabels) {
        Plot2D pdfPlot = new Plot2D("Probablity Distribution Function", name, "Error", "P", showLabels);
        for (Coord3d thetaPDF : data) {
            PDF pdf = new PDF(thetaPDF.y, thetaPDF.z, DisplayErrorStudy.PDF_MIN, DisplayErrorStudy.PDF_MAX);
            Color tc = ColorUtils.getInstance().getNextDarkColor();
            pdfPlot.addSeries(pdf.getNextThousand3D(), "" + thetaPDF.x, new java.awt.Color(tc.r, tc.g, tc.b), false);
        }
        pdfPlot.display();
    }

    public static void plot2D(String name, Map<Float, Map<Integer, List<Coord3d>>> series1, Map<Float, Map<Integer, List<Coord3d>>> series2,
            Map<Float, List<Coord3d>> series3, Map<Float, List<Coord3d>> series4, boolean showLabels) {
        Plot2D pdfPlot = new Plot2D("K/row vs Error", "Query Error", "Clusters", "Error", showLabels);
        for (Float theta : series1.keySet()) {
            for (Integer k : series1.get(theta).keySet()) {
                List<Coord3d> quantizedData = series1.get(theta).get(k);
                Color tc = ColorUtils.getInstance().getNextDarkColor();
                pdfPlot.addSeries(quantizedData, "Theta: " + theta + ",KNN=" + k + " Quantized Error (DistanceError)", new java.awt.Color(tc.r, tc.g, tc.b), false);
            }
            for (Integer k : series2.get(theta).keySet()) {
                List<Coord3d> quantizedData = series2.get(theta).get(k);
                Color tc = ColorUtils.getInstance().getNextDarkColor();
                pdfPlot.addSeries(quantizedData, "Theta: " + theta + ",KNN=" + k + " Quantized Error Distance", new java.awt.Color(tc.r, tc.g, tc.b), false);
            }
            Color tc = ColorUtils.getInstance().getNextDarkColor();
            pdfPlot.addSeries(series3.get(theta), "Theta: " + theta + " Average Error", new java.awt.Color(tc.r, tc.g, tc.b), true);
            tc = ColorUtils.getInstance().getNextDarkColor();
            pdfPlot.addSeries(series4.get(theta), "Theta: " + theta + " Ideal Error", new java.awt.Color(tc.r, tc.g, tc.b), true);
        }
        pdfPlot.display();
    }

    public static void plot2D(List<Coord3d> points, String[] names, String title, String seriesName, boolean showLabels) {
        Plot2D plot = new Plot2D(title, title, names[0], names[1], showLabels);
        plot.addSeries(points, seriesName, java.awt.Color.RED, false);
        plot.display();
    }

    private static void showErrors(Map<Float, Map<Integer, List<Coord3d>>> clusterParameterQuantizedError,
            Map<Float, Map<Integer, List<Coord3d>>> clusterParameterQuantizedErrorDistanceOnly,
            Map<Float, Map<Integer, List<Coord3d>>> clusterParameterGeneralError,
            Map<Float, Map<Integer, List<Coord3d>>> clusterParameterIdealError,
            Map<Float, Map<Integer, List<Coord3d>>> clusterParameterBaseLineError, String[] names) throws Exception {

        for (Float theta : clusterParameterQuantizedError.keySet()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("VISUALIZE_QUERIES/" + theta + "_study.csv"));
            bw.write(names[0] + "," + names[1] + "," + names[2] + ",Algorithm" + "\n");
            for (Integer knn : clusterParameterQuantizedError.get(theta).keySet()) {
                for (Coord3d c : clusterParameterQuantizedError.get(theta).get(knn)) {
                    float clusterParameter = c.x;
                    float error = c.y;
                    bw.write(clusterParameter + "," + knn + "," + error + ",0" + "\n");
                }
            }
            for (Integer knn : clusterParameterQuantizedErrorDistanceOnly.get(theta).keySet()) {
                for (Coord3d c : clusterParameterQuantizedErrorDistanceOnly.get(theta).get(knn)) {
                    float clusterParameter = c.x;
                    float error = c.y;
                    bw.write(clusterParameter + "," + knn + "," + error + ",1" + "\n");
                }
            }
            for (Integer knn : clusterParameterGeneralError.get(theta).keySet()) {
                for (Coord3d c : clusterParameterGeneralError.get(theta).get(knn)) {
                    float clusterParameter = c.x;
                    float error = c.y;
                    bw.write(clusterParameter + "," + knn + "," + error + ",2" + "\n");
                }
            }
            for (Integer knn : clusterParameterIdealError.get(theta).keySet()) {
                for (Coord3d c : clusterParameterIdealError.get(theta).get(knn)) {
                    float clusterParameter = c.x;
                    float error = c.y;
                    bw.write(clusterParameter + "," + knn + "," + error + ",3" + "\n");
                }
            }
            for (Integer knn : clusterParameterBaseLineError.get(theta).keySet()) {
                for (Coord3d c : clusterParameterBaseLineError.get(theta).get(knn)) {
                    float clusterParameter = c.x;
                    float error = c.y;
                    bw.write(clusterParameter + "," + knn + "," + error + ",4" + "\n");
                }
            }
            bw.flush();
            bw.close();
        }
    }

    public static void showGraph(List<Coord3d> points, List<Color> tempC, String[] names) throws Exception {
        System.out.println("Displaying graph " + Arrays.toString(names));
        ScatterPlot3D plot = new ScatterPlot3D(names, true);
        plot.show();
        Coord3d[] coords = new Coord3d[points.size()];
        Color[] colors = new Color[points.size()];
        coords = points.toArray(coords);
        colors = tempC.toArray(colors);
        plot.setPoints(coords, colors);
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

    public static void writeToFile(String name, List<Coord3d> plot) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(name));
        for (Coord3d p : plot) {
            bw.write(p.x + "," + p.y + "," + p.z + "\n");
        }
        bw.flush();
        bw.close();
    }
}
