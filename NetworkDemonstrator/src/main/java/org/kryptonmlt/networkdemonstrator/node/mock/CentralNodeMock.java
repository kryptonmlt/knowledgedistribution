package org.kryptonmlt.networkdemonstrator.node.mock;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeerMock;
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
public class CentralNodeMock implements CentralNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralNodeMock.class);

    private final int numberOfFeatures;
    private final Map<Long, Peer> peers = new HashMap<>();
    private final int closestK;
    private ScatterPlot3D plot;

    public CentralNodeMock(int numberOfFeatures, int closestK, String[] columnNames, boolean showVisualization) throws IOException {
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
    public double query(double[] x) {
        //select closest K nodes
        Long[] nodeIds = new Long[this.closestK];
        double[] distance = new double[this.closestK];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = Double.MAX_VALUE;
            nodeIds[i] = -1l;
        }
        for (Long peerId : peers.keySet()) {
            for (double[] centroid : peers.get(peerId).getQuantizedNodes()) {
                double d = VectorUtils.distance(centroid, x);
                int idMax = 0;
                for (int i = 1; i < distance.length; i++) {
                    if (distance[i] > distance[idMax]) {
                        idMax = i;
                    }
                }
                if (d < distance[idMax]) {
                    distance[idMax] = d;
                    nodeIds[idMax] = peerId;
                }

            }
        }
        LOGGER.debug("Received Query: {}, ClosestIds: {}, Distance: {}", Arrays.toString(x), Arrays.toString(nodeIds), Arrays.toString(distance));
        //closest K nodes selected now compute prediction based on them.
        double[] predictions = new double[this.closestK];
        for (int i = 0; i < distance.length; i++) {
            if (nodeIds[i] != -1l) {
                predictions[i] = peers.get(nodeIds[i]).predict(x[0], x[1]);
            }
        }
        //average predictions and return it.
        double result = VectorUtils.average(predictions);
        return result;
    }

    @Override
    public double queryAll(double[] x) {
        //closest K nodes selected now compute prediction based on them.
        double[] predictions = new double[peers.keySet().size()];
        int i = 0;
        for (Long l : peers.keySet()) {
            predictions[i] = peers.get(l).predict(x[0], x[1]);
            i++;
        }
        //average predictions and return it.
        double result = VectorUtils.average(predictions);
        return result;
    }

    @Override
    public Map<Long, Peer> getPeers() {
        return peers;
    }

    public void addKnowledge(long id, double[] weights, List<double[]> centroids) {
        synchronized (peers) {
            LOGGER.debug("Updating peer {} - {} and {} centroids", id, Arrays.toString(weights), centroids.size());
            Peer peer = this.getPeer(id);
            peer.setWeights(weights);
            List<double[]> centroidsCopy = new ArrayList<>();
            centroids.stream().forEach((centroid) -> {
                centroidsCopy.add(centroid.clone());
            });
            peer.setQuantizedNodes(centroidsCopy);
            SimpleEntry<Coord3d[], Color[]> plotInfo = VisualizationUtils.getPointsAndColors(peers);
            if (plot != null) {
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
