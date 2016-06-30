package org.kryptonmlt.networkdemonstrator.learning;

/**
 *
 * @author Kurt
 */
public interface Learning {

    double predict(double x1, double x2);

    double[] getWeights();

    void setWeights(double[] weights);

    double getAlpha();

    void setAlpha(double alpha);

}
