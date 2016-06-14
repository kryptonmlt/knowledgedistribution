package org.kryptonmlt.networkdemonstrator.visualizer;

import com.opencsv.CSVReader;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A demonstration application showing a time series chart where you can
 * dynamically add (random) data by clicking on a button.
 *
 */
public class GraphVisualizer extends ApplicationFrame {

    private final List<XYSeries> series = new ArrayList<>();

    private final XYSeries onlineHypothesis = new XYSeries("Online Hypothesis");
    private final XYSeries batchHypothesis = new XYSeries("Batch Hypothesis");

    public static final double MIN_RANGE = -0.6;
    public static final double MAX_RANGE = 0.6;

    private double alpha = 0.05;
    private double batch_error_converge = 0.000000001;

    double[] batchWeights = {0, 0};
    double[] onlineWeights = {0, 0};

    public GraphVisualizer(final String title, String[] seriesNames) {

        super(title);
        for (String seriesName : seriesNames) {
            series.add(new XYSeries(seriesName));
        }

        XYPlot plot = new XYPlot();
        ValueAxis data = new NumberAxis("Data");
        Range r = new Range(MIN_RANGE, MAX_RANGE);
        data.setRange(r);
        ValueAxis quality = new NumberAxis("Quality");
        quality.setRange(r);
        plot.setDomainAxis(0, data);
        plot.setRangeAxis(0, quality);

        /* SETUP Online LINE */
        XYDataset onlineLineDataset = new XYSeriesCollection(onlineHypothesis);
        XYItemRenderer onlinelineRenderer = new XYLineAndShapeRenderer(true, false);   // Lines only
        plot.setDataset(0, onlineLineDataset);
        plot.setRenderer(0, onlinelineRenderer);
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        /* SETUP Batch LINE */
        XYDataset batchLineDataset = new XYSeriesCollection(batchHypothesis);
        XYItemRenderer batchLineRenderer = new XYLineAndShapeRenderer(true, false);   // Lines only
        plot.setDataset(1, batchLineDataset);
        plot.setRenderer(1, batchLineRenderer);
        plot.mapDatasetToDomainAxis(1, 0);
        plot.mapDatasetToRangeAxis(1, 0);

        /* SETUP SCATTER */
        for (int i = 0; i < series.size(); i++) {
            XYDataset scatterDataSet = new XYSeriesCollection(this.series.get(i));
            XYItemRenderer scatterRenderer = new XYLineAndShapeRenderer(false, true);   // Shapes only
            plot.setDataset(i + 2, scatterDataSet);
            plot.setRenderer(i + 2, scatterRenderer);
            plot.mapDatasetToDomainAxis(i + 2, 0);
            plot.mapDatasetToRangeAxis(i + 2, 0);
        }

        // Create the chart with the plot and a legend
        JFreeChart chart = new JFreeChart("Quality Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        final ChartPanel chartPanel = new ChartPanel(chart);
        final JPanel content = new JPanel(new BorderLayout());
        content.add(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(content);

    }

    private void addRandomData(int amount) {
        Random r = new Random();
        for (int i = 0; i < amount; i++) {
            double xr = r.nextDouble() - 0.5;
            double yr = xr;
            this.addDataPoint(0, xr, yr);
        }
        setBatchHypothesis();
    }

    public void addData(int startCol, int numberOfSeries, String filename) {
        try {
            File inputFile = new File(filename);
            FileReader fileReader = new FileReader(inputFile);
            CSVReader reader = new CSVReader(fileReader, ',');
            String[] rowHolder;
            //reader.readNext();
            while ((rowHolder = reader.readNext()) != null) {
                int counter = 0;
                switch (rowHolder.length) {
                    case 12:
                        for (int i = startCol; i < startCol + numberOfSeries; i++) {
                            this.addDataPoint(counter, Double.parseDouble(rowHolder[i]), Double.parseDouble(rowHolder[rowHolder.length - 1]));
                            counter++;
                        }
                        break;
                    case 2:
                        for (int i = startCol; i < startCol + numberOfSeries; i++) {
                            this.addDataPoint(counter, Double.parseDouble(rowHolder[i]), Double.parseDouble(rowHolder[rowHolder.length - 1]));
                            counter++;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Incorrect Data Point: " + rowHolder.length);
                }
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(GraphVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        setBatchHypothesis();
        setOnlineHypothesis();
    }

    public void addDataPoint(int series, double x, double y) {
        this.series.get(series).add(new XYDataItem(x, y));
        onlineWeights = onlineSGD(onlineWeights, x, y);
        setOnlineHypothesis();
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public double predictTarget(double[] weights, double x) {
        return weights[0] + (x * weights[1]);
    }

    public void setOnlineHypothesis() {
        this.onlineHypothesis.clear();
        this.onlineHypothesis.add(new XYDataItem(-0.5, predictTarget(onlineWeights, -0.5)));
        this.onlineHypothesis.add(new XYDataItem(0.5, predictTarget(onlineWeights, 0.5)));
    }

    public void setBatchHypothesis() {
        batchWeights = batchSGD(batchWeights);
        this.batchHypothesis.clear();
        this.batchHypothesis.add(new XYDataItem(-0.5, predictTarget(batchWeights, -0.5)));
        this.batchHypothesis.add(new XYDataItem(0.5, predictTarget(batchWeights, 0.5)));
    }

    public double hypothesis(double[] theta, double x) {
        double value = 0;
        for (int i = 0; i < theta.length; i++) {
            value += theta[i] * Math.pow(x, i);
        }
        return value;
    }

    public double[] batchSGD(double[] theta) {
        double[] thetaTemp = theta;
        double currentError = calculateError(theta);
        double lastError;
        int runC = 0;
        do {
            System.out.println(runC + ": " + currentError);
            for (int i = 0; i < theta.length; i++) {
                double summation = 0;
                for (Object item : series.get(0).getItems()) {
                    XYDataItem point = (XYDataItem) item;
                    double value = point.getYValue() - hypothesis(thetaTemp, point.getXValue());
                    summation += value * Math.pow(point.getXValue(), i);
                }
                summation = summation / ((double) series.get(0).getItemCount());
                theta[i] = thetaTemp[i] + (alpha * summation);
            }
            thetaTemp = theta;
            lastError = currentError;
            currentError = calculateError(theta);
            runC++;
        } while (currentError != lastError && (lastError - currentError) > batch_error_converge);
        return theta;
    }

    public double calculateError(double[] theta) {
        double error = 0;
        for (Object item : series.get(0).getItems()) {
            XYDataItem point = (XYDataItem) item;
            double value = hypothesis(theta, point.getXValue()) - point.getYValue();
            error += value * value;
        }
        error = error / ((double) series.get(0).getItemCount());
        return error;
    }

    public double[] onlineSGD(double[] theta, double x, double y) {
        double[] thetaTemp = new double[theta.length];
        for (int i = 0; i < theta.length; i++) {
            double value = (hypothesis(theta, x) - y) * Math.pow(x, i);
            thetaTemp[i] = theta[i] - (alpha * value);
        }
        return thetaTemp;
    }

    private final String[] wineDataSet = {"fixed acidity", "volatile acidity",
        "citric acid", "residual sugar", "chlorides", "free sulfur dioxide",
        "total sulfur dioxide", "density", "pH", "sulphates", "alcohol"};

    public static void main(final String[] args) throws InterruptedException {
        String[] series = {"fixed acidity"};
        final GraphVisualizer gv = new GraphVisualizer("Graph Visualization", series);
        gv.pack();
        RefineryUtilities.centerFrameOnScreen(gv);
        gv.setVisible(true);
        //gv.addData(series.length, "testd.csv");
        //gv.addData(5, series.length, "nwinequality-red.csv");
        Thread.sleep(5000);
        gv.addRandomData(500);
    }

}
