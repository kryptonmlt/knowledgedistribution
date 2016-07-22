package org.kryptonmlt.networkdemonstrator.pojos;

import java.util.ArrayList;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.Clustering;
import org.kryptonmlt.networkdemonstrator.utils.LearningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class DevicePeer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePeer.class);

    private long id;
    private double[] weights = null;
    private int timesWeightsUpdated;
    private Clustering[] clusters;
    private final List<double[]> features = new ArrayList<>();

    public DevicePeer(long id) {
        this.id = id;
        this.timesWeightsUpdated = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double[] getWeights() {
        return weights;
    }

    public synchronized void setWeights(double[] weights) {
        this.weights = weights;
        timesWeightsUpdated++;
    }

    public int getTimesWeightsUpdated() {
        return timesWeightsUpdated;
    }

    public List<double[]> getFeatures() {
        return features;
    }

    public Clustering[] getClusters() {
        return clusters;
    }

    public void setClusters(Clustering[] clusters) {
        this.clusters = clusters;
    }

    public double predict(double x1, double x2) {
        if (weights == null) {
            LOGGER.error("THIS SHOULD NOT HAPPEN!: Peer {} is being used for prediction but has null weights", id);
            return 0.0;
        }
        return LearningUtils.hypothesis(weights, x1, x2);
    }

}
