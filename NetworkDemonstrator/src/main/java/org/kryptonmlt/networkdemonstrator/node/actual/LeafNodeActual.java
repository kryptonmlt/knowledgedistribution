package org.kryptonmlt.networkdemonstrator.node.actual;

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
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.sensors.SensorManager;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;
import org.kryptonmlt.networkdemonstrator.utils.MessageUtils;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class LeafNodeActual implements LeafNode, Runnable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LeafNodeActual.class);

    private long id;
    private final int delayMillis;
    private final WorthType worth;
    private final double error;
    private boolean connected = false;
    private double total_local_error;
    private double total_central_node_error;

    //Sensor Learning
    private final int numberOfFeatures;
    private final SensorManager sensorManager;
    private final OnlineStochasticGradientDescent localModel;
    private final OnlineStochasticGradientDescent centralNodeModel;
    private final Clustering clustering;
    private final int maxLearnPoints;
    private int dataCounter = 0;
    private boolean finished;

    private final int serverPort;
    private final DatagramSocket socket;
    private final InetAddress toSendAddress;

    public LeafNodeActual(String hostname, int serverPort, int delayMillis,
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

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Device starting up collecting {} feature, Registering to Central Node..", numberOfFeatures);
            byte[] toSend = MessageUtils.constructRegistrationMessage(numberOfFeatures);
            DatagramPacket connectPacket = new DatagramPacket(toSend, toSend.length,
                    toSendAddress, serverPort);
            socket.send(connectPacket);
            byte[] idBuf = new byte[Long.BYTES];
            DatagramPacket receivePacket = new DatagramPacket(idBuf, idBuf.length);
            socket.receive(receivePacket);
            id = ConversionUtils.batol(idBuf);
            LOGGER.info("Device contacted central node and is now id {}", id);
            connected = true;
            //starts sending data
            sendData();
        } catch (IOException ex) {
            LOGGER.error("Error when communicating with CentralNode", ex);
        }
    }

    /**
     * Sends sensor data to server using the delay assigned
     *
     * @throws java.io.IOException
     */
    public void sendData() throws IOException {
        double tempLocalError = 0;
        double tempCentralNodeError = 0;
        while (sensorManager.isReadyForRead()) {
            double[] dataGathered = sensorManager.requestData();
            learnFromData(dataGathered);
            if (dataCounter > maxLearnPoints) {
                // IS IT WORTH IT TO SEND IT ?
                switch (worth) {
                    case ALL:
                        sendKnowledge();
                        tempLocalError = localModel.predict(dataGathered[0], dataGathered[1]);
                        total_local_error += tempLocalError;
                        total_central_node_error += tempLocalError;
                        break;
                    case CHANGE_IN_WEIGHT:
                        double change = VectorUtils.summation(VectorUtils.abs(VectorUtils.subtract(centralNodeModel.getWeights(), localModel.getWeights())));
                        tempLocalError = localModel.predict(dataGathered[0], dataGathered[1]);
                        if (change > error) {
                            LOGGER.debug("Discrepancy in weights between device {} and Central Node is {} which is greater than {}", id, change, error);
                            sendKnowledge();
                            total_central_node_error += tempLocalError;
                        } else {
                            //local model not sent
                            total_central_node_error += centralNodeModel.predict(dataGathered[0], dataGathered[1]);
                        }
                        total_local_error += tempLocalError;
                        break;
                    case THETA:
                        tempLocalError = localModel.predict(dataGathered[0], dataGathered[1]);
                        tempCentralNodeError = centralNodeModel.predict(dataGathered[0], dataGathered[1]);
                        double difference = Math.abs(tempLocalError - tempCentralNodeError);
                        if (difference > error) {
                            LOGGER.debug("Discrepancy in error between device {} and Central Node is {} which is greater than {}", id, difference, error);
                            sendKnowledge();
                            total_central_node_error += tempLocalError;
                        } else {
                            //local model not sent
                            total_central_node_error += tempCentralNodeError;
                        }
                        total_local_error += tempLocalError;
                        break;
                    case STOPPING_RULE:
                        LOGGER.error("Still to be implemented !");
                        break;
                    default:
                        throw new AssertionError(worth.name());
                }

                if (delayMillis != 0) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ex) {
                        LOGGER.error("Error when waiting to read from sensor", ex);
                    }
                }
            } else if (dataCounter == maxLearnPoints) {
                LOGGER.info("Device {} finished learning stage now sending knowledge.. ", id);
                sendKnowledge();
            }
            dataCounter++;
        }
        finished = true;
    }

    private void sendKnowledge() throws IOException {
        LOGGER.debug("Device {} sending: {} and {} centroids", id, Arrays.toString(localModel.getWeights()), clustering.getCentroids().size());
        byte[] dataToSend = MessageUtils.constructKnowledgeMessage(id, localModel.getWeights(), ConversionUtils.convert2DListToArray(clustering.getCentroids()));
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length,
                toSendAddress, serverPort);
        socket.send(packet);
        centralNodeModel.setWeights(localModel.getWeights());
        LOGGER.debug("Device {} sent data successfully", id);
    }

    private void sendFeatures(double[] dataGathered) throws IOException {
        byte[] dataToSend = MessageUtils.constructFeaturesMessage(id, dataGathered);
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length,
                toSendAddress, serverPort);
        socket.send(packet);
    }

    private void learnFromData(double[] dataGathered) {
        localModel.learn(dataGathered[0], dataGathered[1], dataGathered[2]);
        double[] inputSpace = {dataGathered[0], dataGathered[1]};
        clustering.update(inputSpace);
    }

    @Override
    public int getTotalMessagesToBeSentSoFar() {
        int amount = dataCounter - maxLearnPoints;
        if (amount < 0) {
            return 0;
        }
        return amount;
    }

    @Override
    public double getAverageLocalError() {
        return total_local_error / (float) getTotalMessagesToBeSentSoFar();
    }

    @Override
    public double getAverageCentralNodeError() {
        return total_central_node_error / (float) getTotalMessagesToBeSentSoFar();
    }

    @Override
    public int getTimesErrorExceeded() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getTimesErrorAcceptable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
