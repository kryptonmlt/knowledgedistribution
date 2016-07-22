package org.kryptonmlt.networkdemonstrator.utils;

import java.util.List;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.Learning;

/**
 *
 * @author Kurt
 */
public class LearningUtils {
    
    private LearningUtils() {

    }

    public static double hypothesis(double[] weights, double x1, double x2) {
            return weights[0] + (weights[1] * x1) + (weights[2] * x2);
    }

    public static Coord3d[] computeLine(List<Coord3d> points, Learning gd) {
        Coord3d[] lineStrip = new Coord3d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            float z = (float) gd.predict(points.get(i).x, points.get(i).y);
            lineStrip[i] = new Coord3d(points.get(i).x, points.get(i).y, z);
        }
        return lineStrip;
    }
}
