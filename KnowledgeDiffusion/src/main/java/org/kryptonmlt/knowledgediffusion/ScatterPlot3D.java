package org.kryptonmlt.knowledgediffusion;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

/**
 *
 * @author Kurt
 */
public class ScatterPlot3D extends AbstractAnalysis {

    private Coord3d[] points = null;
    private Color[] colors = null;

    @Override
    public void init() {
        Scatter scatter = new Scatter(points, colors);
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
        chart.getScene().add(scatter);
    }

    public ScatterPlot3D(Coord3d[] points, Color[] colors) {
        this.points = points;
        this.colors = colors;
    }

    void show() throws Exception {
        AnalysisLauncher.open(this);
    }
}
