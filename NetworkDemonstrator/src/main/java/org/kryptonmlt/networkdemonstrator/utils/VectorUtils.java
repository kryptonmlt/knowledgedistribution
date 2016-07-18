package org.kryptonmlt.networkdemonstrator.utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Kurt
 */
public class VectorUtils {

    private VectorUtils() {

    }

    /**
     * Distance between two vectors
     *
     * @param p1
     * @param p2
     * @return
     */
    public static double distance(double[] p1, double[] p2) {
        double dist = 0;
        for (int i = 0; i < p1.length; i++) {
            dist += (p1[i] - p2[i]) * (p1[i] - p2[i]);
        }
        return Math.sqrt(dist);
    }

    /**
     * Finds the closest cluster to this point
     *
     * @param point
     * @param centroids
     * @return
     */
    public static int classify(double[] point, List<double[]> centroids) {
        double minDist = Float.MAX_VALUE;
        int ans = -1;
        for (int i = 0; i < centroids.size(); i++) {
            double tempDist = VectorUtils.distance(point, centroids.get(i));
            if (tempDist < minDist) {
                minDist = tempDist;
                ans = i;
            }
        }
        return ans;
    }

    public static double[] multiply(double[] update, double scalar) {
        double[] result = new double[update.length];
        for (int i = 0; i < update.length; i++) {
            result[i] = update[i] * scalar;
        }
        return result;
    }

    public static double[] subtract(double[] point, double[] point2) {
        double[] result = new double[point.length];
        for (int i = 0; i < point.length; i++) {
            result[i] = point[i] - point2[i];
        }
        return result;
    }

    public static double[] add(double[] vector1, double[] vector2) {
        double[] result = new double[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] = vector1[i] + vector2[i];
        }
        return result;
    }

    public static double[] moveCentroid(double[] point, double[] centroid, double alpha) {
        double[] update = VectorUtils.subtract(point, centroid);
        update = VectorUtils.multiply(update, alpha);
        return VectorUtils.add(centroid, update);
    }

    public static double summation(double[] v) {
        double result = 0;
        for (int i = 0; i < v.length; i++) {
            result += v[i];
        }
        return result;
    }

    public static double[] abs(double[] v) {
        double[] result = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = Math.abs(v[i]);
        }
        return result;
    }

    public static double average(double[] v) {
        double result = 0;
        for (int i = 0; i < v.length; i++) {
            result += v[i];
        }
        return result / (double) v.length;
    }

    /**
     * Distance between two vectors
     *
     * @param p1
     * @param p2
     * @return
     */
    public static float distance(float[] p1, float[] p2) {
        float dist = 0;
        for (int i = 0; i < p1.length; i++) {
            dist += (p1[i] - p2[i]) * (p1[i] - p2[i]);
        }
        return (float) Math.sqrt(dist);
    }

    /**
     * Finds the closest cluster to this point
     *
     * @param point
     * @param centroids
     * @return
     */
    public static int classify(float[] point, List<float[]> centroids) {
        float minDist = Float.MAX_VALUE;
        int ans = -1;
        for (int i = 0; i < centroids.size(); i++) {
            float tempDist = VectorUtils.distance(point, centroids.get(i));
            if (tempDist < minDist) {
                minDist = tempDist;
                ans = i;
            }
        }
        return ans;
    }

    public static float[] multiply(float[] update, float scalar) {
        float[] result = new float[update.length];
        for (int i = 0; i < update.length; i++) {
            result[i] = update[i] * scalar;
        }
        return result;
    }

    public static float[] subtract(float[] point, float[] point2) {
        float[] result = new float[point.length];
        for (int i = 0; i < point.length; i++) {
            result[i] = point[i] - point2[i];
        }
        return result;
    }

    public static float[] add(float[] vector1, float[] vector2) {
        float[] result = new float[vector1.length];
        for (int i = 0; i < vector1.length; i++) {
            result[i] = vector1[i] + vector2[i];
        }
        return result;
    }

    public static float[] moveCentroid(float[] point, float[] centroid, float alpha) {
        float[] update = VectorUtils.subtract(point, centroid);
        update = VectorUtils.multiply(update, alpha);
        return VectorUtils.add(centroid, update);
    }

    public static List<Double> normalizeList(List<Double> source) {
        List<Double> normalized = new ArrayList<>();
        double total = 0;
        for (Double d : source) {
            total += d;
        }
        for (Double d : source) {
            if (total == 0) {
                normalized.add(0.0);
            } else {
                normalized.add(d / (double) total);
            }
        }
        return normalized;
    }
}
