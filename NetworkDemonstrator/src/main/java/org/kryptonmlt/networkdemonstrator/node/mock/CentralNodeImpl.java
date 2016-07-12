package org.kryptonmlt.networkdemonstrator.node.mock;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.learning.Clustering;
import org.kryptonmlt.networkdemonstrator.learning.DummyClustering;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
import org.kryptonmlt.networkdemonstrator.pojos.NodeDistanceError;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;
import org.kryptonmlt.networkdemonstrator.utils.VisualizationUtils;
import org.kryptonmlt.networkdemonstrator.visualizer.ScatterPlot3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class CentralNodeImpl implements CentralNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralNodeImpl.class);

    private final int numberOfFeatures;
    private final Map<Long, DevicePeer> peers = new HashMap<>();
    private final int[] closestK;
    private final int allK;
    private ScatterPlot3D plot;
    private int featuresReceived = 0;
    private final OnlineStochasticGradientDescent featureModel;

    public CentralNodeImpl(int numberOfFeatures, int[] closestK, int allK, String[] columnNames, double alpha, boolean showVisualization) throws IOException {
        this.numberOfFeatures = numberOfFeatures;
        this.closestK = closestK;
        this.allK = allK;
        if (showVisualization) {
            try {
                plot = new ScatterPlot3D(columnNames, true);
                plot.show();
            } catch (Exception ex) {
                LOGGER.error("Error when trying to show Central Node Visualization", ex);
            }
        }
        featureModel = new OnlineStochasticGradientDescent(alpha);
        LOGGER.info("Central Node started up.. listening for deivces with {} features", this.numberOfFeatures);
    }

    @Override
    public double[][] query(double[] x, boolean error) {
        double[][] result = new double[this.closestK.length][];
        for (int i = 0; i < this.closestK.length; i++) {
            result[i] = queryK(x, this.closestK[i], error);
        }
        return result;
    }

    @Override
    public double queryLeafNode(long peerId, double[] x) {
        return peers.get(peerId).predict(x[0], x[1]);
    }

    @Override
    public double queryBaseLineSolution(double[] x) {
        return featureModel.predict(x[0], x[1]);
    }

    public double[] queryK(double[] x, int k, boolean useError) {
        //sort nodes
        double[] kResults = new double[allK];
        for (int j = 0; j < kResults.length; j++) {
            List<NodeDistanceError> nd = new ArrayList<>();
            synchronized (peers) {
                for (Long peerId : peers.keySet()) {
                    for (int i = 0; i < peers.get(peerId).getClusters()[j].getCentroids().size(); i++) {
                        double d = VectorUtils.distance(peers.get(peerId).getClusters()[j].getCentroids().get(i), x);
                        Double e = null;
                        if (useError) {
                            e = peers.get(peerId).getClusters()[j].getErrors().get(i);
                        }
                        nd.add(new NodeDistanceError(peerId, d, e));
                    }
                }
            }
            Collections.sort(nd);
            int tempSize = k;
            if (k > nd.size()) {
                tempSize = nd.size();
            }

            double totalWeight = 0;
            for (int i = 0; i < tempSize; i++) {
                totalWeight += nd.get(i).getWeight();
            }
            //closest K nodes selected now compute prediction based on them.
            double[] predictions = new double[tempSize];
            double result = 0;
            double totalNormalizedWeight = 0;
            for (int i = 0; i < tempSize; i++) {
                double normalizedWeight = nd.get(i).getWeight() / totalWeight;
                totalNormalizedWeight += normalizedWeight;
                nd.get(i).setWeight(normalizedWeight);
                predictions[i] = peers.get(nd.get(i).getId()).predict(x[0], x[1]) * nd.get(i).getWeight();
                result += predictions[i];
            }
            LOGGER.debug("Received Query: {}, KNN={}, Result: {}", Arrays.toString(x), k, result);
            kResults[j] = result;
        }
        return kResults;
    }

    @Override
    public double queryAll(double[] x) {
        //closest K nodes selected now compute prediction based on them.
        double[] predictions;
        synchronized (peers) {
            predictions = new double[peers.keySet().size()];
            int i = 0;
            for (Long l : peers.keySet()) {
                predictions[i] = peers.get(l).predict(x[0], x[1]);
                i++;
            }
        }
        //average predictions and return it.
        double result = VectorUtils.average(predictions);
        return result;
    }

    @Override
    public Map<Long, DevicePeer> getPeers() {
        return peers;
    }

    public void addKnowledge(long id, double[] weights, Clustering[] clusters) {
        synchronized (peers) {
            LOGGER.debug("Updating peer {} - {}", id, Arrays.toString(weights));
            DevicePeer peer = this.getPeer(id);
            peer.setWeights(weights);
            Clustering[] clustersCopy = new Clustering[clusters.length];
            for (int i = 0; i < clusters.length; i++) {
                List<double[]> centroidsCopy = new ArrayList<>();
                for (double[] centroid : clusters[i].getCentroids()) {
                    centroidsCopy.add(centroid.clone());
                }
                List<Double> errorsCopy = new ArrayList<>();
                for (Double error : clusters[i].getErrors()) {
                    errorsCopy.add(error.doubleValue());
                }
                clustersCopy[i] = new DummyClustering(centroidsCopy, errorsCopy);
            }
            peer.setClusters(clustersCopy);
            if (plot != null) {
                SimpleEntry<Coord3d[], Color[]> plotInfo = VisualizationUtils.getPointsAndColors(peers);
                plot.setPoints(plotInfo.getKey(), plotInfo.getValue());
            }
        }
    }

    public synchronized void addFeatures(long id, double[] dataGathered) {
        featureModel.learn(dataGathered[0], dataGathered[1], dataGathered[2]);
        featuresReceived++;
    }

    private DevicePeer getPeer(long id) {
        DevicePeer peer = peers.get(id);
        if (peer == null) {
            peers.put(id, new DevicePeer(id));
        }
        return peers.get(id);
    }

    @Override
    public ScatterPlot3D getPlot() {
        return plot;
    }

    @Override
    public OnlineStochasticGradientDescent getFeatureModel() {
        return featureModel;
    }

    @Override
    public void setFeaturesReceived(int featuresReceived) {
        this.featuresReceived = featuresReceived;
    }

}
