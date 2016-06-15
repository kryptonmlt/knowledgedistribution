package org.kryptonmlt.networkdemonstrator.visualizer;

import java.util.ArrayList;
import java.util.List;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.CroppableLineStrip;
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
    private Coord3d[] linePoints = null;
    private Color[] colors = null;
    private String[] xyz;

    @Override
    public void init() {
        Scatter scatter;
        if (colors == null) {
            scatter = new Scatter(points);
        } else {
            scatter = new Scatter(points, colors);
        }
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
        IAxeLayout axeLayout = chart.getAxeLayout();
        axeLayout.setXAxeLabel(xyz[0]);
        axeLayout.setYAxeLabel(xyz[1]);
        axeLayout.setZAxeLabel(xyz[2]);
        chart.getScene().add(scatter);
        if (linePoints != null) {
            List<CroppableLineStrip> lineStrips = new ArrayList<>();
            CroppableLineStrip lineStrip = new CroppableLineStrip();
            lineStrip.setWireframeColor(Color.BLACK);
            for (Coord3d c : linePoints) {
                lineStrip.add(new Point(c));
            }
            lineStrips.add(lineStrip);
            chart.getScene().getGraph().add(lineStrips);
        }
    }

    public ScatterPlot3D(Coord3d[] points, Color[] colors, String[] xyz, Coord3d[] linePoints) {
        this.points = points;
        this.colors = colors;
        this.linePoints = linePoints;
        this.xyz = xyz;
    }

    void show() throws Exception {
        AnalysisLauncher.open(this);
    }
}
