package org.kryptonmlt.networkdemonstrator.learning;

import org.kryptonmlt.networkdemonstrator.tools.LearningUtils;

/**
 *
 * @author Kurt
 */
public class OnlineStochasticGradientDescent {

    private double[] weights = new double[3];
    private double alpha = 0.05;

    public OnlineStochasticGradientDescent(double alpha) {
        this.alpha = alpha;
    }

    public double predict(double x1, double x2) {
        return LearningUtils.hypothesis(weights, x1, x2);
    }

    public void onlineSGD(double x1, double x2, double y) {
        double[] thetaTemp = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            double value = (predict(x1, x2) - y) * (Math.pow(x1, i) + Math.pow(x2, i));
            thetaTemp[i] = weights[i] - (alpha * value);
        }
        weights = thetaTemp;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

}
