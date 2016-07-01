package org.kryptonmlt.networkdemonstrator.pojos;

import java.util.List;

/**
 *
 * @author Kurt
 */
public interface Peer {

    long getId();

    void setId(long id);

    double[] getWeights();

    List<double[]> getQuantizedNodes();
    
    List<Double> getQuantizedErrors();

    void setQuantizedNodes(List<double[]> quantizedNodes);

    void setQuantizedErrors(List<Double> quantizedErrors);

    void setWeights(double[] weights);

    int getTimesWeightsUpdated();

    List<double[]> getFeatures();

    double predict(double x1, double x2);
}
