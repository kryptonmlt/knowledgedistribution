package org.kryptonmlt.networkdemonstrator.node.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.learning.ART;
import org.kryptonmlt.networkdemonstrator.learning.Clustering;
import org.kryptonmlt.networkdemonstrator.learning.OnlineKmeans;
import org.kryptonmlt.networkdemonstrator.learning.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.learning.OnlineVarianceMean;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.sensors.SensorManager;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class LeafNodeImpl implements LeafNode, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeafNodeImpl.class);

    private final long id;
    private final int delayMillis;
    private final WorthType worth;
    private final double allowed_error;

    // Statistics
    private final boolean statistics;
    private double total_local_error;
    private double total_central_node_error;
    private int timesErrorExceeded = 0;
    private int timesErrorAcceptable = 0;
    private int p = 0;
    private double[] Y; // difference
    private double[] E_DASH;// local model
    private double[] E; // central/obsolete model
    private double[] localPredicted;
    private double[] centralPredicted;
    private double[] actual;
    private final OnlineVarianceMean E_DASH_MeanVariance;
    private final OnlineVarianceMean E_MeanVariance;
    private final OnlineVarianceMean Y_MeanVariance;
    private double[] quantizedError;
    private double generalError = 0;
    private int queries = 0;

    //Sensor Learning
    private final int numberOfFeatures;
    private final SensorManager sensorManager;
    private final OnlineStochasticGradientDescent localModel;
    private final OnlineStochasticGradientDescent centralNodeModel;
    private final OnlineVarianceMean THETA_ERROR_meanVariance;
    private final Clustering clustering;
    private final int maxLearnPoints;
    private int dataCounter = 0;

    private final CentralNodeImpl centralNode;
    private boolean finished;
    private final double degrade_alpha;
    private final double minimum_alpha;

    public LeafNodeImpl(CentralNodeImpl centralNode, int delayMillis,
            String datafile, int sheetNum, XSSFSheet sheet, int startFeature, int numberOfFeatures,
            float alpha, int maxLearnPoints, WorthType worth, double error, Integer k, double row,
            double degrade_alpha, double minimum_alpha, boolean statistics, int max_use_Points, int kfold, int closestK) throws IOException {
        this.maxLearnPoints = maxLearnPoints;
        this.numberOfFeatures = numberOfFeatures;
        this.centralNode = centralNode;
        this.worth = worth;
        this.allowed_error = error;
        this.id = sheetNum;
        this.statistics = statistics;
        if (this.statistics) {
            this.Y = new double[max_use_Points];
            this.E_DASH = new double[max_use_Points];
            this.E = new double[max_use_Points];
            this.localPredicted = new double[max_use_Points];
            this.centralPredicted = new double[max_use_Points];
            this.actual = new double[max_use_Points];
        }

        this.E_DASH_MeanVariance = new OnlineVarianceMean();
        this.E_MeanVariance = new OnlineVarianceMean();
        this.Y_MeanVariance = new OnlineVarianceMean();

        this.localModel = new OnlineStochasticGradientDescent(alpha);
        this.centralNodeModel = new OnlineStochasticGradientDescent(alpha);
        this.THETA_ERROR_meanVariance = new OnlineVarianceMean();

        this.delayMillis = delayMillis;
        this.sensorManager = new SensorManager(sheet, startFeature, numberOfFeatures, datafile, kfold);

        if (k != null) {
            this.clustering = new OnlineKmeans(k, alpha);
        } else {
            this.clustering = new ART(row, alpha);
        }
        this.degrade_alpha = degrade_alpha;
        this.minimum_alpha = minimum_alpha;

        this.quantizedError = new double[closestK];
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
        double localError;
        double centralNodeError;
        try {
            while (sensorManager.isAvailable()) {
                while (sensorManager.isReadyForRead() && (!statistics || dataCounter < maxLearnPoints + 1 + Y.length)) {
                    double[] dataGathered = sensorManager.requestData();
                    learnFromData(dataGathered);
                    if (dataCounter > maxLearnPoints) {
                        tempLocalPredict = localModel.predict(dataGathered[0], dataGathered[1]);
                        tempCentralNodePredict = centralNodeModel.predict(dataGathered[0], dataGathered[1]);
                        localError = tempLocalPredict - dataGathered[2];
                        centralNodeError = tempCentralNodePredict - dataGathered[2];
                        double e_dash = localError * localError;
                        double e = centralNodeError * centralNodeError;
                        if (statistics) {
                            E_DASH[dataCounter - maxLearnPoints - 1] = e_dash;
                            E[dataCounter - maxLearnPoints - 1] = e;
                            if (e > e_dash) { //Local model better than central model (probability approx 90%)
                                Y[dataCounter - maxLearnPoints - 1] = e - e_dash;
                            } else {
                                Y[dataCounter - maxLearnPoints - 1] = 0;
                            }
                            actual[dataCounter - maxLearnPoints - 1] = dataGathered[2];
                            localPredicted[dataCounter - maxLearnPoints - 1] = tempLocalPredict;
                            centralPredicted[dataCounter - maxLearnPoints - 1] = tempCentralNodePredict;
                        }
                        E_DASH_MeanVariance.update(e_dash);
                        E_MeanVariance.update(e);
                        if (e > e_dash) { //Local model better than central model (probability approx 90%)
                            Y_MeanVariance.update(e - e_dash);
                        } else {
                            Y_MeanVariance.update(0);
                        }
                        calculateP(e_dash, e);
                        double differenceInErrorSquared = (localError - centralNodeError) * (localError - centralNodeError);
                        // IS IT WORTH IT TO SEND IT ?
                        switch (worth) {
                            case ALL:
                                sendKnowledge();
                                total_central_node_error += e_dash;
                                total_local_error += e_dash;
                                break;
                            case CHANGE_IN_WEIGHT:
                                //Manhattan distance
                                double manhattanDistance = VectorUtils.summation(VectorUtils.abs(VectorUtils.subtract(centralNodeModel.getWeights(), localModel.getWeights())));
                                if (manhattanDistance > allowed_error) {
                                    LOGGER.debug("Discrepancy in weights between device {} and Central Node is {} which is greater than {}", id, manhattanDistance, allowed_error);
                                    sendKnowledge();
                                    total_central_node_error += e_dash;
                                } else {
                                    //local model not sent
                                    total_central_node_error += e;
                                    timesErrorAcceptable++;
                                }
                                total_local_error += e_dash;
                                break;
                            case THETA:
                                THETA_ERROR_meanVariance.update(differenceInErrorSquared);
                                if (differenceInErrorSquared > allowed_error) {
                                    LOGGER.debug("Discrepancy in error between device {} and Central Node is {} which is greater than {}", id, differenceInErrorSquared, allowed_error);
                                    sendKnowledge();
                                    total_central_node_error += e_dash;
                                } else {
                                    //local model not sent
                                    total_central_node_error += e;
                                    timesErrorAcceptable++;
                                }
                                total_local_error += e_dash;
                                break;
                            case STOPPING_RULE:
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

                // DEGRADE LEARNING RATE
                double alphaUpdate = localModel.getAlpha() - degrade_alpha;
                if (alphaUpdate >= minimum_alpha) {
                    this.localModel.setAlpha(alphaUpdate);
                }

                if (dataCounter > maxLearnPoints) {
                    //Run query tests            
                    for (double[] data : sensorManager.requestValidationData()) {
                        double[] query = {data[0], data[1]};
                        double[] quantizedResult = centralNode.query(query);
                        double generalResult = centralNode.queryAll(query);
                        for (int i = 0; i < quantizedResult.length; i++) {
                            quantizedError[i] += Math.abs(data[2] - quantizedResult[i]);
                        }
                        generalError += Math.abs(data[2] - generalResult);
                        queries++;
                    }
                }
                LOGGER.info("Device {} is at generation {} of {}, Local Error {}, Central Error: {}", id, sensorManager.getCurrentGeneration(), sensorManager.getMaximumGeneration(),
                        this.getAverageLocalError(), this.getAverageCentralNodeError());
                // START SENSOR FROM START increasing generation
                sensorManager.reset();
            }
            finished = true;
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR in sendData.. ", e);
            System.exit(1);
        }
    }

    private void calculateP(double localPredict, double centralNodePredict) {
        if (localPredict < centralNodePredict) {
            p++;
        }
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
    public int getP() {
        return p;
    }

    @Override
    public OnlineVarianceMean getMeanVariance() {
        return THETA_ERROR_meanVariance;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public double[] getY() {
        return Y;
    }

    @Override
    public double[] getE_DASH() {
        return E_DASH;
    }

    @Override
    public double[] getE() {
        return E;
    }

    @Override
    public double[] getLocalPredicted() {
        return localPredicted;
    }

    @Override
    public double[] getCentralPredicted() {
        return centralPredicted;
    }

    @Override
    public double[] getActual() {
        return actual;
    }

    @Override
    public OnlineVarianceMean getE_DASH_MeanVariance() {
        return E_DASH_MeanVariance;
    }

    @Override
    public OnlineVarianceMean getE_MeanVariance() {
        return E_MeanVariance;
    }

    @Override
    public OnlineVarianceMean getY_MeanVariance() {
        return Y_MeanVariance;
    }

    @Override
    public boolean isStatistics() {
        return statistics;
    }

    @Override
    public double[] getQuantizedError() {
        return quantizedError;
    }

    @Override
    public double getGeneralError() {
        return generalError;
    }

    @Override
    public int getQueries() {
        return queries;
    }

}
