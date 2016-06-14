package org.kryptonmlt.networkdemonstrator.tools;

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
}
