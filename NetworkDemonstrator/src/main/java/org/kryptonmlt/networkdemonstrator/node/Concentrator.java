package org.kryptonmlt.networkdemonstrator.node;

import java.util.Map;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
import org.kryptonmlt.networkdemonstrator.visualizer.ScatterPlot3D;

/**
 *
 * @author Kurt
 */
public interface Concentrator {

    public Map<Long, DevicePeer> getPeers();

    public double queryAll(double[] query);

    public double[][] query(double[] query, boolean useError);

    public ScatterPlot3D getPlot();

    public double queryLeafNode(long peerId, double[] x);

    public double queryBaseLineSolution(double[] x);

    public OnlineStochasticGradientDescent getFeatureModel();

    public void setFeaturesReceived(int featuresReceived);

}
