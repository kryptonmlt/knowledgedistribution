package org.kryptonmlt.networkdemonstrator.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
import org.kryptonmlt.networkdemonstrator.visualizer.ScatterPlot3D;

/**
 *
 * @author Kurt
 */
public class VisualizationUtils {

    private VisualizationUtils() {

    }

    public static void drawLine(Coord3d[] points, ScatterPlot3D plot, int sheetNum, Color color) {
        Arrays.sort(points, coord3dComparator);
        plot.updateLine(points, color, sheetNum);
    }

    /**
     * Showing only clusters[0]
     * @param quantizedNodes
     * @return 
     */
    public static SimpleEntry<Coord3d[], Color[]> getPointsAndColors(Map<Long, DevicePeer> quantizedNodes) {
        List<Coord3d> points = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (Long peerId : quantizedNodes.keySet()) {
            for (double[] centroid : quantizedNodes.get(peerId).getClusters()[0].getCentroids()) {
                points.add(new Coord3d(centroid[0], centroid[1], 0.0));
                colors.add(ColorUtils.getInstance().getLightColor(peerId.intValue()));
            }
        }
        Coord3d[] tempPoints = new Coord3d[points.size()];
        Color[] tempColors = new Color[colors.size()];
        return new SimpleEntry<>(points.toArray(tempPoints), colors.toArray(tempColors));
    }

    public static Comparator<Coord3d> coord3dComparator = (Coord3d c1, Coord3d c2) -> {
        Float x1 = c1.x;
        Float x2 = c2.x;
        return x1.compareTo(x2);
    };
}
