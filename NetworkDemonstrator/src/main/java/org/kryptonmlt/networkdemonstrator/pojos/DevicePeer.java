package org.kryptonmlt.networkdemonstrator.pojos;

import java.net.InetAddress;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.tools.LearningUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class DevicePeer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePeer.class);

    private long id;
    private InetAddress address;
    private int port;
    private double[] weights = null;
    private int timesWeightsUpdated;
    private List<double[]> quantizedNodes;

    public DevicePeer(long id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.timesWeightsUpdated = 0;
    }

    public long getId() {
        return id;
    }

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

    public double[] getWeights() {
        return weights;
    }

    public List<double[]> getQuantizedNodes() {
        return quantizedNodes;
    }

    public void setQuantizedNodes(List<double[]> quantizedNodes) {
        this.quantizedNodes = quantizedNodes;
    }

    public synchronized void setWeights(double[] weights) {
        this.weights = weights;
        timesWeightsUpdated++;
    }

    public int getTimesWeightsUpdated() {
        return timesWeightsUpdated;
    }

    public double predict(double x1, double x2) {
        if (weights == null) {
            LOGGER.error("THIS SHOULD NOT HAPPEN!: Peer {} is being used for prediction but has null weights", id);
            return 0.0;
        }
        return LearningUtils.hypothesis(weights, x1, x2);
    }

}
