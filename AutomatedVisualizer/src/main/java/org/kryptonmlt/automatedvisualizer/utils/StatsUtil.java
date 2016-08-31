package org.kryptonmlt.automatedvisualizer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Kurt
 */
public class StatsUtil {

    private StatsUtil() {

    }

    /**
     * Calculates the Kullback-Leibler Divergence of 2 PDFs
     * @param mean1
     * @param variance1
     * @param mean2
     * @param variance2
     * @return 
     */
    public static double klDivergence2PDFs(double mean1, double variance1, double mean2, double variance2) {
        return 0.5 * (Math.log(variance2 / variance1) - 1 + ((variance1 + Math.pow(mean1 - mean2, 2)) / variance2));
    }

    /**
     * Normalizes a list of double
     * @param p
     * @return the same list values 0..1
     */
    public static List<Double> normalize_min_max(List<Double> p) {
        List<Double> normalized = new ArrayList<>();
        double min = Collections.min(p);
        double max = Collections.max(p);
        for (Double x : p) {
            normalized.add((x - min) / (max - min));
        }
        return normalized;
    }
}
