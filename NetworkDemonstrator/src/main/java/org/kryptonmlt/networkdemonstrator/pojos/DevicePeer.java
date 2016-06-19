package org.kryptonmlt.networkdemonstrator.pojos;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.utils.LearningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class DevicePeer implements Peer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePeer.class);

    private long id;
    private InetAddress address;
    private int port;
    private double[] weights = null;
    private int timesWeightsUpdated;
    private List<double[]> quantizedNodes;
    private final List<double[]> features = new ArrayList<>();

    public DevicePeer(long id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
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

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

}
