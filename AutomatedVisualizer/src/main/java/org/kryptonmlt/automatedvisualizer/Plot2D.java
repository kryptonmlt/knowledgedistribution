package org.kryptonmlt.automatedvisualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
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
    private final List<String> names = new ArrayList<>();
    private int counter = 0;
    private final float STROKE_SIZE = 1.0f;
    private boolean showLabels;

    public Plot2D(String applicationTitle, String chartTitle, String xName, String yName, boolean showLabels) {
        super(applicationTitle);
        JFreeChart xylineChart = ChartFactory.createXYLineChart(
                chartTitle, xName, yName, dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(xylineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        final XYPlot plot = xylineChart.getXYPlot();
        plot.setRenderer(renderer);
        this.setContentPane(chartPanel);
        this.showLabels = showLabels;
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

    public void addSeries(List<Coord3d> xy, String name, Color c, boolean increaseSize) {
        final XYSeries series = new XYSeries(name);
        for (int i = 0; i < xy.size(); i++) {
            series.add(xy.get(i).x, xy.get(i).y);
        }
        renderer.setSeriesPaint(counter, c);
        if (increaseSize) {
            renderer.setSeriesStroke(counter, new BasicStroke(this.STROKE_SIZE + 2f));
        } else {
            renderer.setSeriesStroke(counter, new BasicStroke(this.STROKE_SIZE));
        }
        renderer.setSeriesShapesVisible(counter, false);
        dataset.addSeries(series);
        names.add(name);
        renderer.setSeriesItemLabelGenerator(counter, new XYItemLabelGenerator() {
            @Override
            public String generateLabel(XYDataset xyd, int series, int item) {
                return names.get(series);
            }
        });
        renderer.setSeriesItemLabelsVisible(counter, showLabels);
        counter++;
    }

}
