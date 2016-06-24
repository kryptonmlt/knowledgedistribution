package org.kryptonmlt.automatedvisualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jzy3d.maths.Coord3d;

/**
 *
 * @author Kurt
 */
public class Plot2D extends ApplicationFrame {

    private final XYSeriesCollection dataset = new XYSeriesCollection();
    private final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    private int counter = 0;
    private final float STROKE_SIZE = 1.0f;

    public Plot2D(String applicationTitle, String chartTitle, String xName, String yName) {
        super(applicationTitle);
        JFreeChart xylineChart = ChartFactory.createXYLineChart(
                chartTitle, xName, yName, dataset,
                PlotOrientation.VERTICAL,
                true, false, false);

        ChartPanel chartPanel = new ChartPanel(xylineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        final XYPlot plot = xylineChart.getXYPlot();
        plot.setRenderer(renderer);
        this.setContentPane(chartPanel);
    }

    public void display() {
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }

    public void addIncrementalSeries(List<Double> data, String name, Color c) {
        final XYSeries series = new XYSeries(name);
        for (int i = 0; i < data.size(); i++) {
            series.add(i, data.get(i));
        }
        renderer.setSeriesPaint(counter, c);
        renderer.setSeriesStroke(counter, new BasicStroke(this.STROKE_SIZE));
        renderer.setSeriesShapesVisible(counter, false);
        dataset.addSeries(series);
        counter++;
    }

    public void addSeries(List<Coord3d> xy, String name, Color c) {
        final XYSeries series = new XYSeries(name);
        for (int i = 0; i < xy.size(); i++) {
            series.add(xy.get(i).x, xy.get(i).y);
        }
        renderer.setSeriesPaint(counter, c);
        renderer.setSeriesStroke(counter, new BasicStroke(this.STROKE_SIZE));
        renderer.setSeriesShapesVisible(counter, false);
        dataset.addSeries(series);
        counter++;
    }

}
