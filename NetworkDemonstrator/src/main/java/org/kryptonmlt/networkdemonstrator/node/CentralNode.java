package org.kryptonmlt.networkdemonstrator.node;

import com.jogamp.opengl.math.VectorUtil;
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

/**
 *
 * @author Kurt
 */
public class CentralNode implements Runnable {

    private final long id;
    private final int numberOfFeatures;
    private final DatagramSocket socket;
    private final DatagramPacket dataPacket;
    private final Map<Long, DevicePeer> peers = new HashMap<>();
    private final List<SimpleEntry<Long, double[]>> quantizedNodes = new ArrayList<>();
    private final int closestK;

    public CentralNode(int serverPort, int numberOfFeatures, int closestK) throws IOException {
        this.numberOfFeatures = numberOfFeatures;
        byte[] recieveBuffer = new byte[1 + Long.BYTES + numberOfFeatures * Double.BYTES];
        this.dataPacket = new DatagramPacket(recieveBuffer, recieveBuffer.length);
        this.socket = new DatagramSocket(serverPort);
        this.closestK = closestK;
        id = IdGenerator.getInstance().getNextId();
        System.out.println("Central Node starting up with id " + id + ", listening on port " + serverPort + "..");
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
        //closest K nodes selected now compute prediction based on them.
        double[] predictions = new double[closestK];
        for (int i = 0; i < distance.length; i++) {
            if (nodeIds[i] != -1l) {
                predictions[i] = peers.get(nodeIds[i]).predict(x[0], x[1]);
            }
        }
        //average predictions and return it.
        return VectorUtils.average(predictions);
    }

    @Override
    public void run() {
        try {
            while (true) {
                socket.receive(dataPacket);
                byte[] data = dataPacket.getData();
                short request = ConversionUtils.btos(data[0]);
                byte[] actualData = new byte[data.length - 1];
                System.arraycopy(data, 1, actualData, 0, actualData.length);
                System.out.println("Received request: " + request);

                byte[] peerIdBytes = new byte[Long.BYTES];
                long peerId;
                switch (request) {
                    case 0: //add new peer
                        int requestFeatures = ConversionUtils.btoi(actualData);
                        if (requestFeatures == numberOfFeatures) {//match
                            System.out.println("Features to learn -  MATCH");
                            //send id back
                            peerId = IdGenerator.getInstance().getNextId();
                            peers.put(peerId, new DevicePeer(peerId, dataPacket.getAddress(), dataPacket.getPort()));
                            peerIdBytes = ConversionUtils.longToBytes(peerId);
                            socket.send(new DatagramPacket(peerIdBytes, peerIdBytes.length, dataPacket.getAddress(), dataPacket.getPort()));

                        } else {
                            // features dont match... ignore
                            System.out.println("Features to learn - DO NOT match: " + numberOfFeatures + " " + request);
                        }
                        break;
                    case 1: //receiving features
                        byte[] f = new byte[numberOfFeatures * Double.BYTES];
                        System.arraycopy(actualData, 0, peerIdBytes, 0, peerIdBytes.length);
                        peerId = ConversionUtils.batol(peerIdBytes);
                        System.arraycopy(actualData, Long.BYTES, f, 0, f.length);
                        double[] features = ConversionUtils.toDouble(f);
                        System.out.println("Request from peer: " + peerId + " " + Arrays.toString(features));
                        break;
                    case 2: //receiving weights
                        byte[] w = new byte[actualData.length - peerIdBytes.length];
                        System.arraycopy(actualData, 0, peerIdBytes, 0, peerIdBytes.length);
                        peerId = ConversionUtils.batol(peerIdBytes);
                        System.arraycopy(actualData, Long.BYTES, w, 0, w.length);
                        double[] updatedWeights = ConversionUtils.toDouble(w);
                        DevicePeer p = peers.get(peerId);
                        if (p != null) {
                            System.out.println("Updating peer " + peerId + " - " + Arrays.toString(updatedWeights));
                            p.setWeights(updatedWeights);

                        } else {
                            System.out.println("PEER NOT REGISTERED: " + peerId + " - " + Arrays.toString(updatedWeights));
                        }
                        break;
                    default:
                        System.err.println("Incorrect request: " + request);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
