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
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeerMock;
import org.kryptonmlt.networkdemonstrator.pojos.NodeDistanceError;
import org.kryptonmlt.networkdemonstrator.pojos.Peer;
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
    private final Map<Long, Peer> peers = new HashMap<>();
    private final int[] closestK;
    private ScatterPlot3D plot;

    public CentralNodeImpl(int numberOfFeatures, int[] closestK, String[] columnNames, boolean showVisualization) throws IOException {
        this.numberOfFeatures = numberOfFeatures;
        this.closestK = closestK;
        if (showVisualization) {
            try {
                plot = new ScatterPlot3D(columnNames, true);
                plot.show();
            } catch (Exception ex) {
                LOGGER.error("Error when trying to show Central Node Visualization", ex);
            }
        }
        LOGGER.info("Central Node started up.. listening for deivces with {} features", this.numberOfFeatures);
    }

    @Override
    public double[] query(double[] x, boolean error) {
        double[] result = new double[this.closestK.length];
        for (int i = 0; i < this.closestK.length; i++) {
            result[i] = queryK(x, this.closestK[i], error);
        }
        return result;
    }

    @Override
    public double queryLeafNode(long peerId, double[] x) {
        return peers.get(peerId).predict(x[0], x[1]);
    }

    public double queryK(double[] x, int k, boolean useError) {
        //select closest K nodes
        List<NodeDistanceError> nd = new ArrayList<>();
        synchronized (peers) {
            for (Long peerId : peers.keySet()) {
                for (int i = 0; i < peers.get(peerId).getQuantizedNodes().size(); i++) {
                    double d = VectorUtils.distance(peers.get(peerId).getQuantizedNodes().get(i), x);
                    double e = 1.0;
                    if (useError) {
                        e = peers.get(peerId).getQuantizedErrors().get(i);
                    }
                    nd.add(new NodeDistanceError(peerId, d, e));
                }
            }
        }
        Collections.sort(nd);
        //closest K nodes selected now compute prediction based on them.
        int tempSize = k;
        if (k > nd.size()) {
            tempSize = nd.size();
        }
        double[] predictions = new double[tempSize];
        for (int i = 0; i < tempSize; i++) {
            predictions[i] = peers.get(nd.get(i).getId()).predict(x[0], x[1]);
        }
        //average predictions and return it.
        double result = VectorUtils.average(predictions);
        LOGGER.debug("Received Query: {}, KNN={}, Result: {}", Arrays.toString(x), k, result);
        return result;
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
    public Map<Long, Peer> getPeers() {
        return peers;
    }

    public void addKnowledge(long id, double[] weights, List<double[]> centroids, List<Double> errors) {
        synchronized (peers) {
            LOGGER.debug("Updating peer {} - {} and {} centroids", id, Arrays.toString(weights), centroids.size());
            Peer peer = this.getPeer(id);
            peer.setWeights(weights);
            List<double[]> centroidsCopy = new ArrayList<>();
            centroids.stream().forEach((centroid) -> {
                centroidsCopy.add(centroid.clone());
            });
            peer.setQuantizedNodes(centroidsCopy);
            List<Double> errorsCopy = new ArrayList<>();
            for (Double e : errors) {
                errorsCopy.add(e.doubleValue());
            }
            peer.setQuantizedErrors(errorsCopy);
            if (plot != null) {
                SimpleEntry<Coord3d[], Color[]> plotInfo = VisualizationUtils.getPointsAndColors(peers);
                plot.setPoints(plotInfo.getKey(), plotInfo.getValue());
            }
        }
    }

    public synchronized void addFeatures(long id, double[] dataGathered) {
        synchronized (peers) {
            this.getPeer(id).getFeatures().add(dataGathered);
        }
    }

    private Peer getPeer(long id) {
        Peer peer = peers.get(id);
        if (peer == null) {
            peers.put(id, new DevicePeerMock(id));
        }
        return peers.get(id);
    }

    @Override
    public ScatterPlot3D getPlot() {
        return plot;
    }
}
