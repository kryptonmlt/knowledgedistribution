package org.kryptonmlt.networkdemonstrator.pojos;

/**
 *
 * @author Kurt
 */
public class MinMax {

    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public void update(double value) {
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
    }
}
