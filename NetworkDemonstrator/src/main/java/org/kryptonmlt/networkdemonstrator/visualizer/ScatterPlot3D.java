package org.kryptonmlt.networkdemonstrator.visualizer;

import java.util.List;
import java.util.Vector;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.AbstractDrawable;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.axes.layout.IAxeLayout;
import org.jzy3d.plot3d.rendering.canvas.Quality;

/**
 *
 * @author Kurt
 */
public class ScatterPlot3D extends AbstractAnalysis {

    private Coord3d[] points = null;
    private Color[] colors = null;
    private final String[] xyz;
    private Scatter scatter;
    final List<AbstractDrawable> memory = new Vector<>();

    @Override
    public void init() {

        chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
        IAxeLayout axeLayout = chart.getAxeLayout();
        axeLayout.setXAxeLabel(xyz[0]);
        axeLayout.setYAxeLabel(xyz[1]);
        axeLayout.setZAxeLabel(xyz[2]);

        LineStrip line = new LineStrip();
        line.setWireframeColor(Color.BLACK);
        chart.getScene().getGraph().add(line);
        memory.add(line);
    }

    //initial points
    public ScatterPlot3D(String[] xyz) {
        this.xyz = xyz;
    }

    void show() throws Exception {
        AnalysisLauncher.open(this);
    }

    public void addPoint(Coord3d point, Color c) {
        if (points == null) {
            points = new Coord3d[1];
            points[0] = point;
            colors = new Color[1];
            colors[0] = c;
        } else {
            Coord3d[] tempp = new Coord3d[points.length + 1];
            Color[] tempc = new Color[colors.length + 1];
            System.arraycopy(points, 0, tempp, 0, points.length);
            System.arraycopy(colors, 0, tempc, 0, colors.length);
            tempp[points.length] = point;
            tempc[colors.length] = c;
            points = tempp;
            colors = tempc;
        }
        if (points.length == 2) {
            scatter = new Scatter(points, colors, 3f);
            chart.getScene().add(scatter);
        } else if (points.length > 2) {
            scatter.setColors(colors);
            scatter.setData(points);
        }
    }

    public void updateLine(Coord3d[] linePoints, Color color, int currentLine) {
        if (scatter != null) {
            if (memory.size() > currentLine) {
                chart.getScene().getGraph().remove(memory.get(currentLine));
                memory.remove(currentLine);
            }
            LineStrip line = new LineStrip();
            line.setWireframeColor(color);
            line.setWidth(2f);
            for (Coord3d c : linePoints) {
                line.add(new Point(c));
            }
            chart.getScene().getGraph().add(line);
            memory.add(currentLine, line);
        }
    }
}
