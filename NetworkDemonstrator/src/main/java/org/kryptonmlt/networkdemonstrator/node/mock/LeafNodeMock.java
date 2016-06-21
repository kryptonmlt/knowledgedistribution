package org.kryptonmlt.networkdemonstrator.node.mock;

import java.io.IOException;
import java.util.Arrays;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.learning.ART;
import org.kryptonmlt.networkdemonstrator.learning.Clustering;
import org.kryptonmlt.networkdemonstrator.learning.OnlineKmeans;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.sensors.SensorManager;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class LeafNodeMock implements LeafNode, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeafNodeMock.class);

    private final long id;
    private final int delayMillis;
    private final WorthType worth;
    private final double allowed_error;

    private double total_local_error;
    private double total_central_node_error;
    private int timesErrorExceeded = 0;
    private int timesErrorAcceptable = 0;

    //Sensor Learning
    private final int numberOfFeatures;
    private final SensorManager sensorManager;
    private final OnlineStochasticGradientDescent localModel;
    private final OnlineStochasticGradientDescent centralNodeModel;
    private final Clustering clustering;
    private final int maxLearnPoints;
    private int dataCounter = 0;
    private int currentGeneration = 0;

    private final CentralNodeMock centralNode;
    private boolean finished;
    private final int maximumGenerations;
    private final double degrade_alpha;
    private final double minimum_alpha;

    public LeafNodeMock(CentralNodeMock centralNode, int delayMillis,
            String datafile, int sheetNum, XSSFSheet sheet, int startFeature, int numberOfFeatures,
            double alpha, int maxLearnPoints, WorthType worth, double error, Integer k, double row, int generations,
            double degrade_alpha, double minimum_alpha) throws IOException {
        this.maxLearnPoints = maxLearnPoints;
        this.numberOfFeatures = numberOfFeatures;
        this.centralNode = centralNode;
        this.worth = worth;
        this.allowed_error = error;
        this.id = sheetNum;

        localModel = new OnlineStochasticGradientDescent(alpha);
        centralNodeModel = new OnlineStochasticGradientDescent(alpha);

        this.delayMillis = delayMillis;
        sensorManager = new SensorManager(sheet, startFeature, numberOfFeatures, datafile);

        if (k != null) {
            clustering = new OnlineKmeans(k, alpha);
        } else {
            clustering = new ART(row, alpha);
        }
        this.maximumGenerations = generations;
        this.degrade_alpha = degrade_alpha;
        this.minimum_alpha = minimum_alpha;
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
            LOGGER.info("Device {} starting up collecting {} feature, Registering to Central Node..", id, numberOfFeatures);
            //starts sending data
            sendData();
            LOGGER.info("Device {} finished sending data ..", id);
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
        double tempLocalPredict;
        double tempCentralNodePredict;
        for (int i = 0; i < maximumGenerations; i++) {
            currentGeneration++;
            while (sensorManager.isReadyForRead()) {
                double[] dataGathered = sensorManager.requestData();
                learnFromData(dataGathered);
                if (dataCounter > maxLearnPoints) {
                    // IS IT WORTH IT TO SEND IT ?
                    switch (worth) {
                        case ALL:
                            sendKnowledge();
                            tempLocalPredict = localModel.predict(dataGathered[0], dataGathered[1]);
                            total_local_error += Math.abs(tempLocalPredict - dataGathered[2]);
                            total_central_node_error += Math.abs(tempLocalPredict - dataGathered[2]);
                            break;
                        case CHANGE_IN_WEIGHT:
                            double change = VectorUtils.summation(VectorUtils.abs(VectorUtils.subtract(centralNodeModel.getWeights(), localModel.getWeights())));
                            tempLocalPredict = localModel.predict(dataGathered[0], dataGathered[1]);
                            if (change > allowed_error) {
                                LOGGER.debug("Discrepancy in weights between device {} and Central Node is {} which is greater than {}", id, change, allowed_error);
                                sendKnowledge();
                                total_central_node_error += Math.abs(tempLocalPredict - dataGathered[2]);
                            } else {
                                //local model not sent
                                total_central_node_error += Math.abs(centralNodeModel.predict(dataGathered[0], dataGathered[1]) - dataGathered[2]);
                                timesErrorAcceptable++;
                            }
                            total_local_error += Math.abs(tempLocalPredict - dataGathered[2]);
                            break;
                        case THETA:
                            tempLocalPredict = localModel.predict(dataGathered[0], dataGathered[1]);
                            tempCentralNodePredict = centralNodeModel.predict(dataGathered[0], dataGathered[1]);
                            double difference = Math.abs(tempLocalPredict - tempCentralNodePredict);
                            if (difference > allowed_error) {
                                LOGGER.debug("Discrepancy in error between device {} and Central Node is {} which is greater than {}", id, difference, allowed_error);
                                sendKnowledge();
                                total_central_node_error += Math.abs(tempLocalPredict - dataGathered[2]);
                            } else {
                                //local model not sent
                                total_central_node_error += Math.abs(tempCentralNodePredict - dataGathered[2]);
                                timesErrorAcceptable++;
                            }
                            total_local_error += Math.abs(tempLocalPredict - dataGathered[2]);
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
            // START SENSOR FROM START
            sensorManager.reset();

            // DEGRADE LEARNING RATE
            double alphaUpdate = localModel.getAlpha() - degrade_alpha;
            if (minimum_alpha >= alphaUpdate) {
                this.localModel.setAlpha(alphaUpdate);
            }
        }
        finished = true;
    }

    private void sendKnowledge() throws IOException {
        LOGGER.debug("Device {} sending: {} and {} centroids", id, Arrays.toString(localModel.getWeights()), clustering.getCentroids().size());
        centralNode.addKnowledge(id, localModel.getWeights(), clustering.getCentroids());
        centralNodeModel.setWeights(localModel.getWeights());
        LOGGER.debug("Device {} sent data successfully", id);
        timesErrorExceeded++;
    }

    private void sendFeatures(double[] dataGathered) throws IOException {
        centralNode.addFeatures(id, dataGathered);
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
        return timesErrorExceeded;
    }

    @Override
    public int getTimesErrorAcceptable() {
        return timesErrorAcceptable;
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
