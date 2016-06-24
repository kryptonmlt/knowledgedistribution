package org.kryptonmlt.automatedvisualizer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 *
 * @author Kurt
 */
public class HistogramDisplay extends ApplicationFrame {

    public HistogramDisplay(String name, double[] data) {
        super(name + " Histogram");
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries(name, data, 20); // Number of bins is 50
        String plotTitle = name + " Histogram";
        String xAxis = "Error";
        String yAxis = "Probability";
        PlotOrientation orientation = PlotOrientation.VERTICAL;

        boolean show = false;
        boolean toolTips = false;
        boolean urls = false;
        JFreeChart chart = ChartFactory.createHistogram(plotTitle, xAxis, yAxis,
                dataset, orientation, show, toolTips, urls);

        chart.setBackgroundPaint(java.awt.Color.WHITE);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        this.setContentPane(chartPanel);
    }

    public void display() {
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }
}
