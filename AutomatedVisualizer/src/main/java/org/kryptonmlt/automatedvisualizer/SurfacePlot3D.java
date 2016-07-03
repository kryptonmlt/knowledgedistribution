package org.kryptonmlt.automatedvisualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axes.layout.IAxeLayout;
import org.jzy3d.plot3d.rendering.canvas.Quality;

/**
 *
 * @author Kurt
 */
public class SurfacePlot3D extends AbstractAnalysis {

    private final String[] xyz;
    private final Map<Float, Map<Integer, List<Coord3d>>> errors;

    @Override
    public void init() {

        double[][] distDataProp = convertToArray(convertToList(errors));
        List<Polygon> polygons = new ArrayList<>();
        for (int i = 0; i < distDataProp.length - 1; i++) {
            for (int j = 0; j < distDataProp[i].length - 1; j++) {
                Polygon polygon = new Polygon();
                /*polygon.add(new Point(new Coord3d(i, j, distDataProp[i][j])));
                polygon.add(new Point(new Coord3d(i, j + 1, distDataProp[i][j + 1])));
                polygon.add(new Point(new Coord3d(i + 1, j + 1, distDataProp[i + 1][j + 1])));
                polygon.add(new Point(new Coord3d(i + 1, j, distDataProp[i + 1][j])));*/
                polygon.add(new Point(new Coord3d(i, j, distDataProp[i][j])));
                polygon.add(new Point(new Coord3d(i, j + 1, distDataProp[i][j + 1])));
                polygon.add(new Point(new Coord3d(i + 1, j + 1, distDataProp[i + 1][j + 1])));
                polygon.add(new Point(new Coord3d(i + 1, j, distDataProp[i + 1][j])));
                polygons.add(polygon);
            }
        }
        Shape surface = new Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new org.jzy3d.colors.Color(1, 1, 1, 1f)));
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(org.jzy3d.colors.Color.BLACK);

        chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
        chart.getScene().getGraph().add(surface);
        IAxeLayout axeLayout = chart.getAxeLayout();
        axeLayout.setXAxeLabel(xyz[0]);
        axeLayout.setYAxeLabel(xyz[1]);
        axeLayout.setZAxeLabel(xyz[2]);
    }

    public SurfacePlot3D(String[] xyzNames, Map<Float, Map<Integer, List<Coord3d>>> errors) {
        this.errors = errors;
        this.xyz = xyzNames;

    }

    public static Map<Float, Map<Integer, List<Coord3d>>> convertKNN0(Map<Float, List<Coord3d>> errors) {
        Map<Float, Map<Integer, List<Coord3d>>> map = new HashMap<>();
        for (Float theta : errors.keySet()) {
            Map<Integer, List<Coord3d>> added = new HashMap<>();
            added.put(0, errors.get(theta));
            map.put(theta, added);
        }
        return map;
    }

    public static List<Coord3d> convertToList(Map<Float, Map<Integer, List<Coord3d>>> errors) {
        List<Coord3d> errorConversion = new ArrayList<>();
        for (Float theta : errors.keySet()) {
            for (Integer knn : errors.get(theta).keySet()) {
                for (Coord3d c : errors.get(theta).get(knn)) {
                    errorConversion.add(new Coord3d(theta, knn.floatValue(), c.y));
                }
            }
        }
        return errorConversion;
    }

    public static double[][] convertToArray(List<Coord3d> errorConversion) {
        double[][] distDataProp = new double[errorConversion.size()][3];
        for (int i = 0; i < distDataProp.length; i++) {
            double[] d = {errorConversion.get(i).x, errorConversion.get(i).y, errorConversion.get(i).z};
            distDataProp[i] = d;
        }
        return distDataProp;
    }

    public void show() throws Exception {
        AnalysisLauncher.open(this);
    }
}
