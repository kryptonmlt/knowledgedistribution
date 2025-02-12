package org.kryptonmlt.networkdemonstrator.node.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.ART;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.Clustering;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.OnlineKmeans;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.impl.OnlineStochasticGradientDescent;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.OnlineVarianceMean;
import org.kryptonmlt.networkdemonstrator.ml_algorithms.PDF;
import org.kryptonmlt.networkdemonstrator.pojos.MinMax;
import org.kryptonmlt.networkdemonstrator.sensors.SensorManager;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kryptonmlt.networkdemonstrator.node.Sensor;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;

/**
 * Uses the Sensor Manager to read data. Extracts knowledge from the data and 
 * keeps running until the data finishes. It keeps counters of the different statistics
 * which are used by the QueryPerformer
 * @author Kurt
 */
public class SensorImpl implements Sensor, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensorImpl.class);

    private final long id;
    private final int delayMillis;
    private final WorthType worth;
    private final double theta_error;
    private final int errorMultiplier;
    private final float gamma;

    // Statistics
    private final boolean statistics;
    private double total_local_error;
    private double total_central_node_error;
    private double currentAccumulatedError;
    private int timesErrorExceeded = 0;
    private int timesErrorAcceptable = 0;
    private final int[] clustersTimesUpdated;
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
    private final double[][] quantizedError;
    private final double[][] quantizedErrorDistanceOnly;
    private double generalError = 0;
    private double idealError = 0;
    private double baseLineError = 0;
    private int queries = 0;

    //Sensor Learning
    private final int numberOfFeatures;
    private final SensorManager sensorManager;
    private final OnlineStochasticGradientDescent localModel;
    private final OnlineStochasticGradientDescent centralNodeModel;
    private final OnlineVarianceMean THETA_ERROR_meanVariance;
    private final Clustering[] clustering;
    private final Clustering[] concentratorClustering;
    private final int maxLearnPoints;
    private int dataCounter = 0;
    private final MinMax X1 = new MinMax();
    private final MinMax X2 = new MinMax();

    private final ConcentratorImpl centralNode;
    private boolean finished;
    private final boolean sendClustersAfterFirst = true;

    public SensorImpl(ConcentratorImpl centralNode, int delayMillis,
            String datafile, int sheetNum, XSSFSheet sheet, XSSFSheet querySheet, int startFeature, int numberOfFeatures,
            float learningRate, float clusteringAlpha, int maxLearnPoints, WorthType worth, double theta_error, int[] k, float[] row,
            boolean statistics, int max_use_Points, double samplingRate, int closestK, int errorMultiplier, float gamma) throws IOException {
        this.maxLearnPoints = maxLearnPoints;
        this.numberOfFeatures = numberOfFeatures;
        this.centralNode = centralNode;
        this.worth = worth;
        this.theta_error = theta_error;
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

        this.localModel = new OnlineStochasticGradientDescent(learningRate);
        this.centralNodeModel = new OnlineStochasticGradientDescent(learningRate);
        this.THETA_ERROR_meanVariance = new OnlineVarianceMean();

        this.delayMillis = delayMillis;
        this.sensorManager = new SensorManager(sheet, querySheet, startFeature, numberOfFeatures, datafile, samplingRate);
        if (k != null) {
            clustering = new Clustering[k.length];
            concentratorClustering = new Clustering[k.length];
            this.clustersTimesUpdated = new int[k.length];
            for (int i = 0; i < k.length; i++) {
                this.clustering[i] = new OnlineKmeans(k[i], learningRate, clusteringAlpha);
                this.concentratorClustering[i] = new OnlineKmeans(k[i], learningRate, clusteringAlpha);
            }
        } else {
            clustering = new Clustering[row.length];
            concentratorClustering = new Clustering[row.length];
            this.clustersTimesUpdated = new int[row.length];
            for (int i = 0; i < row.length; i++) {
                this.clustering[i] = new ART(row[i], learningRate, clusteringAlpha);
                this.concentratorClustering[i] = new ART(row[i], learningRate, clusteringAlpha);
            }
        }

        this.quantizedError = new double[closestK][clustering.length];
        this.quantizedErrorDistanceOnly = new double[closestK][clustering.length];
        this.errorMultiplier = errorMultiplier;
        this.gamma = gamma;
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
        double maxErrorStoppingRule = errorMultiplier * theta_error;
        try {
            while (sensorManager.isReadyForRead() && (!statistics || dataCounter < maxLearnPoints + 1 + Y.length)) {
                double[] dataGathered = sensorManager.requestData();
                //sendFeatures(dataGathered);
                //learn local model
                int[] clustersChosen = learnFromData(dataGathered);
                //if we reached 1000 points 
                if (dataCounter > maxLearnPoints) {
                    tempLocalPredict = localModel.predict(dataGathered[0], dataGathered[1]);
                    tempCentralNodePredict = centralNodeModel.predict(dataGathered[0], dataGathered[1]);
                    localError = tempLocalPredict - dataGathered[2];
                    centralNodeError = tempCentralNodePredict - dataGathered[2];
                    double e_dash = localError * localError;
                    double e = centralNodeError * centralNodeError;
                    for (int i = 0; i < clustersChosen.length; i++) {
                        clustering[i].updateError(clustersChosen[i], e_dash);
                    }

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

                    double y = 0;
                    if (e > e_dash) { //Local model better than central model (probability approx 90%)
                        y = e - e_dash;
                    }
                    calculateP(e_dash, e);

                    boolean send = false;
                    // IS IT WORTH IT TO SEND IT ?
                    switch (worth) {
                        case ALL:
                            send = true;
                            break;
                        case CHANGE_IN_WEIGHT:
                            //Manhattan distance
                            double manhattanDistance = VectorUtils.summation(VectorUtils.abs(VectorUtils.subtract(centralNodeModel.getWeights(), localModel.getWeights())));
                            if (manhattanDistance > theta_error) {
                                LOGGER.debug("Discrepancy in weights between device {} and Central Node is {} which is greater than {}", id, manhattanDistance, theta_error);
                                send = true;
                            }
                            break;
                        case THETA:
                            THETA_ERROR_meanVariance.update(y);
                            if (y > theta_error) {
                                LOGGER.debug("Discrepancy in error between device {} and Central Node is {} which is greater than {}", id, y, theta_error);
                                send = true;
                            }
                            break;
                        case STOPPING_RULE:
                            currentAccumulatedError += e; //add the central node error
                            double errorRemaining = maxErrorStoppingRule - currentAccumulatedError;
                            if (isProbableToReachError(errorRemaining)) {
                                LOGGER.debug("Device {} states that it is probable that it will reach {} error remaining therefore sending knowledge", id, errorRemaining);
                                send = true;
                            }
                            break;
                        default:
                            throw new AssertionError(worth.name());
                    }
                    if (send) {
                        currentAccumulatedError = 0;
                        total_central_node_error += e_dash;
                        if (sendClustersAfterFirst) {
                            List<Integer> needsUpdate = new ArrayList<>();
                            for (int i = 0; i < clustering.length; i++) {
                                double tempDistance = 0;
                                for (int j = 0; j < clustering[i].getCentroids().size(); j++) {
                                    tempDistance += VectorUtils.distance(clustering[i].getCentroids().get(j), concentratorClustering[i].getCentroids().get(j));
                                }
                                if (tempDistance > gamma) {
                                    needsUpdate.add(i);
                                    clustersTimesUpdated[i]++;
                                }
                            }
                            sendKnowledge(ConversionUtils.toIntArray(needsUpdate));
                        } else {
                            sendKnowledge(new int[0]);
                        }
                        E_MeanVariance.update(e_dash);
                        Y_MeanVariance.update(e_dash);
                    } else {
                        total_central_node_error += e;
                        timesErrorAcceptable++;
                        E_MeanVariance.update(e);
                        Y_MeanVariance.update(y);
                    }
                    total_local_error += e_dash;
                    E_DASH_MeanVariance.update(e_dash);

                    if (delayMillis != 0) {
                        try {
                            Thread.sleep(delayMillis);
                        } catch (InterruptedException ex) {
                            LOGGER.error("Error when waiting to read from sensor", ex);
                        }
                    }
                } else if (dataCounter == maxLearnPoints) {
                    LOGGER.info("Device {} finished learning stage now sending knowledge.. ", id);
                    int[] needsUpdate = new int[clustering.length];
                    for (int i = 0; i < clustering.length; i++) {
                        needsUpdate[i] = i;
                        clustersTimesUpdated[i]++;
                    }
                    sendKnowledge(needsUpdate);
                }
                dataCounter++;
            }
            LOGGER.info("Device {} finished, Local Error {}, Central Error: {}", id, this.getAverageLocalError(), this.getAverageCentralNodeError());
            finished = true;
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR in sendData.. ", e);
            System.exit(1);
        }
    }

    @Override
    public void queryValidation() {
        //Run query tests            
        for (double[] data : sensorManager.requestValidationData()) {
            double[] query = {data[0], data[1]};
            double[][] quantizedResultError = centralNode.query(query, true);
            for (int i = 0; i < quantizedResultError.length; i++) {
                for (int j = 0; j < quantizedResultError[i].length; j++) {
                    quantizedError[i][j] += Math.pow(data[2] - quantizedResultError[i][j], 2);
                }
            }
            double[][] quantizedResultSimple = centralNode.query(query, false);
            for (int i = 0; i < quantizedResultSimple.length; i++) {
                for (int j = 0; j < quantizedResultSimple[i].length; j++) {
                    quantizedErrorDistanceOnly[i][j] += Math.pow(data[2] - quantizedResultSimple[i][j], 2);
                }
            }
            double generalResult = centralNode.queryAll(query);
            generalError += Math.pow(data[2] - generalResult, 2);
            double idealQuery = centralNode.queryLeafNode(id, query);
            idealError += Math.pow(data[2] - idealQuery, 2);
            double baseLineQuery = centralNode.queryBaseLineSolution(query);
            baseLineError += Math.pow(data[2] - baseLineQuery, 2);
            queries++;
        }
        //calulate average query error
        for (int i = 0; i < quantizedError.length; i++) {
            for (int j = 0; j < quantizedError[i].length; j++) {
                quantizedError[i][j] = quantizedError[i][j] / (float) queries;
            }
        }
        for (int i = 0; i < quantizedErrorDistanceOnly.length; i++) {
            for (int j = 0; j < quantizedErrorDistanceOnly[i].length; j++) {
                quantizedErrorDistanceOnly[i][j] = quantizedErrorDistanceOnly[i][j] / (float) queries;
            }
        }
        generalError = generalError / (float) queries;
        idealError = idealError / (float) queries;
        baseLineError = baseLineError / (float) queries;
    }

    /**
     * Calculate the probability using the integral of the PDF
     *
     * @param errorRemaining
     * @return true if it is probable to reach the errorRemaining
     */
    private boolean isProbableToReachError(double errorRemaining) {
        PDF pdf = new PDF(Y_MeanVariance.getMean(), Y_MeanVariance.getVariance(), -10, 10);
        // Use integral of pdf with limits errorRemaining and infinity ?    
        double probability = 0.0;
        return probability > 0.05;
    }

    private void calculateP(double localPredict, double centralNodePredict) {
        if (localPredict < centralNodePredict) {
            p++;
        }
    }

    private void sendKnowledge(int[] clustersUpdated) throws IOException {
        LOGGER.debug("Device {} sending: {}", id, Arrays.toString(localModel.getWeights()));
        Clustering[] toSend = null;
        if (clustersUpdated.length != 0) {
            toSend = new Clustering[clustersUpdated.length];
            for (int i = 0; i < clustersUpdated.length; i++) {
                toSend[i] = clustering[clustersUpdated[i]];

                List<double[]> centroidsCopy = new ArrayList<>();
                for (double[] centroid : clustering[clustersUpdated[i]].getCentroids()) {
                    centroidsCopy.add(centroid.clone());
                }
                concentratorClustering[clustersUpdated[i]].setCentroids(centroidsCopy);
            }
        }
        centralNode.addKnowledge(id, localModel.getWeights(), toSend, clustersUpdated);
        centralNodeModel.setWeights(localModel.getWeights());
        LOGGER.debug("Device {} sent data successfully", id);
        timesErrorExceeded++;
    }

    private void sendFeatures(double[] dataGathered) throws IOException {
        centralNode.addFeatures(id, dataGathered);
    }

    private int[] learnFromData(double[] dataGathered) {
        X1.update(dataGathered[0]);
        X2.update(dataGathered[1]);
        localModel.learn(dataGathered[0], dataGathered[1], dataGathered[2]);
        double[] inputSpace = {dataGathered[0], dataGathered[1]};
        int[] clustersChosen = new int[clustering.length];
        for (int i = 0; i < clustersChosen.length; i++) {
            clustersChosen[i] = clustering[i].update(inputSpace);
        }
        return clustersChosen;
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
    public double[][] getQuantizedError() {
        return quantizedError;
    }

    @Override
    public double[][] getQuantizedErrorDistanceOnly() {
        return quantizedErrorDistanceOnly;
    }

    @Override
    public double getGeneralError() {
        return generalError;
    }

    @Override
    public double getIdealError() {
        return idealError;
    }

    @Override
    public double getBaseLineError() {
        return baseLineError;
    }

    @Override
    public int getQueries() {
        return queries;
    }

    @Override
    public Clustering[] getClustering() {
        return clustering;
    }

    @Override
    public OnlineStochasticGradientDescent getLocalModel() {
        return localModel;
    }

    @Override
    public MinMax getX1() {
        return X1;
    }

    @Override
    public MinMax getX2() {
        return X2;
    }

    @Override
    public int[] getClustersTimesUpdated() {
        return clustersTimesUpdated;
    }

}
