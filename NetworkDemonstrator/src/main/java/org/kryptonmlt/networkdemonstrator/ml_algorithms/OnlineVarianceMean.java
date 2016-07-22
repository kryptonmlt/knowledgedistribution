package org.kryptonmlt.networkdemonstrator.ml_algorithms;

/**
 *
 * @author Kurt
 */
public class OnlineVarianceMean {

    private int n = 0;
    double mean = 0.0;
    double M2 = 0.0;

    public void update(double x) {
        n += 1;
        double delta = x - mean;
        mean += delta / n;
        M2 += delta * (x - mean);
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return M2 / (n - 1);
    }
}
