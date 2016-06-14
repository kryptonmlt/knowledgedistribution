package org.kryptonmlt.networkdemonstrator.pojos;

import java.net.InetAddress;
import org.kryptonmlt.networkdemonstrator.tools.LearningUtils;

/**
 *
 * @author Kurt
 */
public class DevicePeer {

    private long id;
    private InetAddress address;
    private int port;
    private double[] weights = null;

    public DevicePeer(long id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
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

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double predict(double x1, double x2) {
        return LearningUtils.hypothesis(weights, x1, x2);
    }

}
