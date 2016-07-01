package org.kryptonmlt.networkdemonstrator.pojos;

import java.util.ArrayList;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.utils.LearningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class DevicePeerMock implements Peer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePeerMock.class);

    private long id;
    private double[] weights = null;
    private int timesWeightsUpdated;
    private List<double[]> quantizedNodes;
    private List<Double> quantizedErrors;
    private final List<double[]> features = new ArrayList<>();

    public DevicePeerMock(long id) {
        this.id = id;
        this.timesWeightsUpdated = 0;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public double[] getWeights() {
        return weights;
    }

    @Override
    public List<double[]> getQuantizedNodes() {
        return quantizedNodes;
    }

    @Override
    public void setQuantizedNodes(List<double[]> quantizedNodes) {
        this.quantizedNodes = quantizedNodes;
    }

    @Override
    public synchronized void setWeights(double[] weights) {
        this.weights = weights;
        timesWeightsUpdated++;
    }

    @Override
    public int getTimesWeightsUpdated() {
        return timesWeightsUpdated;
    }

    @Override
    public List<double[]> getFeatures() {
        return features;
    }

    @Override
    public double predict(double x1, double x2) {
        if (weights == null) {
            LOGGER.error("THIS SHOULD NOT HAPPEN!: Peer {} is being used for prediction but has null weights", id);
            return 0.0;
        }
        return LearningUtils.hypothesis(weights, x1, x2);
    }

    @Override
    public List<Double> getQuantizedErrors() {
        return quantizedErrors;
    }

    @Override
    public void setQuantizedErrors(List<Double> quantizedErrors) {
        this.quantizedErrors = quantizedErrors;
    }

}
