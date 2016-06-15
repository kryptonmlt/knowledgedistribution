package org.kryptonmlt.networkdemonstrator.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.learning.ART;
import org.kryptonmlt.networkdemonstrator.learning.Clustering;
import org.kryptonmlt.networkdemonstrator.learning.OnlineKmeans;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.sensors.SensorManager;
import org.kryptonmlt.networkdemonstrator.tools.ConversionUtils;
import org.kryptonmlt.networkdemonstrator.tools.MessageUtils;
import org.kryptonmlt.networkdemonstrator.tools.VectorUtils;

/**
 *
 * @author Kurt
 */
public class LeafNode {

    private long id;
    private final int delayMillis;
    private final WorthType worth;
    private final double error;

    //Sensor Learning
    private final int numberOfFeatures;
    private final SensorManager sensorManager;
    private final OnlineStochasticGradientDescent localModel;
    private final OnlineStochasticGradientDescent centralNodeModel;
    private final Clustering clustering;
    private final int maxLearnPoints;
    private int dataCounter = 0;

    private final int serverPort;
    private final DatagramSocket socket;
    private final InetAddress toSendAddress;

    public LeafNode(String hostname, int serverPort, int delayMillis,
            String datafile, XSSFSheet sheet, int startFeature, int numberOfFeatures,
            double alpha, int maxLearnPoints, WorthType worth, double error, Integer k, double row) throws IOException {
        this.maxLearnPoints = maxLearnPoints;
        this.numberOfFeatures = numberOfFeatures;
        this.serverPort = serverPort;
        this.worth = worth;
        this.error = error;

        localModel = new OnlineStochasticGradientDescent(alpha);
        centralNodeModel = new OnlineStochasticGradientDescent(alpha);

        this.socket = new DatagramSocket();
        this.delayMillis = delayMillis;
        this.toSendAddress = InetAddress.getByName(hostname);
        sensorManager = new SensorManager(sheet, startFeature, numberOfFeatures, datafile);

        if (k != null) {
            clustering = new OnlineKmeans(k, alpha);
        } else {
            clustering = new ART(row, alpha);
        }
    }

    /**
     * Creates local Datagram Server and connects with Server to tell him its
     * alive.
     *
     * @param initializeServer
     * @throws IOException
     */
    public void initializeCommunication(boolean initializeServer) throws IOException {
        System.out.println("Device starting up collecting " + numberOfFeatures + " feature\nRegistering to Central Node..");
        byte[] toSend = MessageUtils.constructRegistrationMessage(numberOfFeatures);
        DatagramPacket connectPacket = new DatagramPacket(toSend, toSend.length,
                toSendAddress, serverPort);
        socket.send(connectPacket);
        byte[] idBuf = new byte[Long.BYTES];
        DatagramPacket receivePacket = new DatagramPacket(idBuf, idBuf.length);
        socket.receive(receivePacket);
        id = ConversionUtils.batol(idBuf);
        System.out.println("Device contacted central node and is now id " + id);

        //starts sending data
        sendData();
    }

    /**
     * Sends sensor data to server using the delay assigned
     *
     * @throws java.io.IOException
     */
    public void sendData() throws IOException {
        while (sensorManager.isReadyForRead()) {
            double[] dataGathered = sensorManager.requestData();
            learnFromData(dataGathered);
            if (dataCounter > maxLearnPoints) {
                // IS IT WORTH IT TO SEND IT ?
                switch (worth) {
                    case ALL:
                        sendKnowledge();
                        break;
                    case CHANGE_IN_WEIGHT:
                        double change = VectorUtils.summation(VectorUtils.abs(VectorUtils.subtract(centralNodeModel.getWeights(), localModel.getWeights())));
                        if (change > error) {
                            System.err.println("Discrepancy in weights between device " + id + " and Central Node is " + change + " which is greater than " + error);
                            sendKnowledge();
                        }
                        break;
                    case THETA:
                        double difference = Math.abs(localModel.predict(dataGathered[0], dataGathered[1]) - centralNodeModel.predict(dataGathered[0], dataGathered[1]));
                        if (difference > error) {
                            System.err.println("Discrepancy in error between device " + id + " and Central Node is " + difference + " which is greater than " + error);
                            sendKnowledge();
                        }
                        break;
                    case STOPPING_RULE:
                        System.err.println("Still to be implemented !");
                        break;
                    default:
                        throw new AssertionError(worth.name());
                }
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else if (dataCounter == maxLearnPoints) {
                System.err.println("Learning Stage finished sending knowledge.. ");
                sendKnowledge();
            }
            dataCounter++;
        }
    }

    private void sendKnowledge() throws IOException {
        System.out.println("Device " + id + " sending: " + Arrays.toString(localModel.getWeights()) + " and " + clustering.getCentroids().size() + " centroids");
        byte[] dataToSend = MessageUtils.constructKnowledgeMessage(id, localModel.getWeights(), ConversionUtils.convert2DListToArray(clustering.getCentroids()));
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length,
                toSendAddress, serverPort);
        socket.send(packet);
        centralNodeModel.setWeights(localModel.getWeights());
        System.out.println("Device " + id + " data sent");
    }

    private void sendFeatures(double[] dataGathered) throws IOException {
        byte[] dataToSend = MessageUtils.constructFeaturesMessage(id, dataGathered);
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length,
                toSendAddress, serverPort);
        socket.send(packet);
    }

    private void learnFromData(double[] dataGathered) {
        localModel.onlineSGD(dataGathered[0], dataGathered[1], dataGathered[2]);
        double[] inputSpace = {dataGathered[0], dataGathered[1]};
        clustering.update(inputSpace);
    }
}
