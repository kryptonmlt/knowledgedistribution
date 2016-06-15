package org.kryptonmlt.networkdemonstrator.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
import org.kryptonmlt.networkdemonstrator.tools.ConversionUtils;
import org.kryptonmlt.networkdemonstrator.tools.IdGenerator;
import org.kryptonmlt.networkdemonstrator.tools.VectorUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class CentralNode implements Runnable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CentralNode.class);

    private final long id;
    private final int numberOfFeatures;
    private final DatagramSocket socket;
    private final DatagramPacket dataPacket;
    private final Map<Long, DevicePeer> peers = new HashMap<>();
    private final List<SimpleEntry<Long, double[]>> quantizedNodes = new ArrayList<>();
    private final int closestK;

    public CentralNode(int serverPort, int numberOfFeatures, int closestK) throws IOException {
        this.numberOfFeatures = numberOfFeatures;
        byte[] recieveBuffer = new byte[1 + Long.BYTES + numberOfFeatures * Double.BYTES + (closestK * 2 * Double.BYTES)];
        this.dataPacket = new DatagramPacket(recieveBuffer, recieveBuffer.length);
        this.socket = new DatagramSocket(serverPort);
        this.closestK = closestK;
        id = IdGenerator.getInstance().getNextId();
        LOGGER.info("Central Node starting up with id {}, listening on port {}..", id, serverPort);
    }

    public double query(double[] x) {
        //select closest K nodes
        Long[] nodeIds = new Long[closestK];
        double[] distance = new double[closestK];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = Double.MAX_VALUE;
            nodeIds[i] = -1l;
        }
        quantizedNodes.stream().forEach((centroid) -> {
            double d = VectorUtils.distance(centroid.getValue(), x);
            int idMax = 0;
            for (int i = 1; i < distance.length; i++) {
                if (distance[i] > distance[idMax]) {
                    idMax = i;
                }
            }
            if (d < distance[idMax]) {
                distance[idMax] = d;
                nodeIds[idMax] = centroid.getKey();
            }
        });
        LOGGER.debug("Received Query: {}, ClosestIds: {}, Distance: {}", Arrays.toString(x), Arrays.toString(nodeIds), Arrays.toString(distance));
        //closest K nodes selected now compute prediction based on them.
        double[] predictions = new double[closestK];
        for (int i = 0; i < distance.length; i++) {
            if (nodeIds[i] != -1l) {
                predictions[i] = peers.get(nodeIds[i]).predict(x[0], x[1]);
            }
        }
        //average predictions and return it.
        double result = VectorUtils.average(predictions);
        return result;
    }

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
    public void run() {
        Thread.currentThread().setName("CentralNode");
        while (true) {
            try {
                //Receive Request
                socket.receive(dataPacket);
                byte[] data = dataPacket.getData();
                short request = ConversionUtils.btos(data[0]);
                byte[] actualData = new byte[data.length - 1];
                System.arraycopy(data, 1, actualData, 0, actualData.length);
                LOGGER.debug("Received request: {}", request);

                byte[] peerIdBytes = new byte[Long.BYTES];
                long peerId;
                // Process Request
                switch (request) {
                    case 0: //add new peer
                        int requestFeatures = ConversionUtils.btoi(actualData);
                        if (requestFeatures == numberOfFeatures) {//match
                            LOGGER.debug("Features to learn -  MATCH");
                            //send id back
                            peerId = IdGenerator.getInstance().getNextId();
                            peers.put(peerId, new DevicePeer(peerId, dataPacket.getAddress(), dataPacket.getPort()));
                            peerIdBytes = ConversionUtils.longToBytes(peerId);
                            socket.send(new DatagramPacket(peerIdBytes, peerIdBytes.length, dataPacket.getAddress(), dataPacket.getPort()));

                        } else {
                            // features dont match... ignore
                            LOGGER.debug("Features to learn - DO NOT match: {} != {}", numberOfFeatures, request);
                        }
                        break;
                    case 1: //receiving features
                        byte[] f = new byte[numberOfFeatures * Double.BYTES];
                        System.arraycopy(actualData, 0, peerIdBytes, 0, peerIdBytes.length);
                        peerId = ConversionUtils.batol(peerIdBytes);
                        System.arraycopy(actualData, Long.BYTES, f, 0, f.length);
                        double[] features = ConversionUtils.toDouble(f);
                        LOGGER.debug("Receiving features from peer: {}, {} ", peerId, Arrays.toString(features));
                        break;
                    case 2: //receiving weights
                        // Get peer Id
                        System.arraycopy(actualData, 0, peerIdBytes, 0, peerIdBytes.length);
                        peerId = ConversionUtils.batol(peerIdBytes);
                        // Get Weights
                        byte[] weightsBytes = new byte[numberOfFeatures * Double.BYTES];
                        System.arraycopy(actualData, Long.BYTES, weightsBytes, 0, weightsBytes.length);
                        double[] updatedWeights = ConversionUtils.toDouble(weightsBytes);
                        // Get Centroids
                        byte[] centroidsBytes = new byte[actualData.length - Long.BYTES - weightsBytes.length];
                        System.arraycopy(actualData, Long.BYTES + weightsBytes.length, centroidsBytes, 0, centroidsBytes.length);
                        double[] centroidsArr = ConversionUtils.toDouble(centroidsBytes);
                        DevicePeer p = peers.get(peerId);
                        if (p != null) {
                            LOGGER.debug("Updating peer {} - {} and {} centroids", peerId, Arrays.toString(updatedWeights), (centroidsArr.length / 2));
                            p.setWeights(updatedWeights);
                            for (int i = 0; i < centroidsArr.length; i += 2) {
                                double[] c = {centroidsArr[i], centroidsArr[i + 1]};
                                quantizedNodes.add(new SimpleEntry<>(peerId, c));
                            }
                        } else {
                            LOGGER.error("PEER {} NOT REGISTERED sent: {}", peerId, Arrays.toString(updatedWeights));
                        }
                        break;
                    default:
                        LOGGER.error("Incorrect request: {}", request);
                }
            } catch (Exception ex) {
                LOGGER.error("Error in CentralNode run method", ex);
            }
        }
    }
}
