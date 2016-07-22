package org.kryptonmlt.network.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kryptonmlt.networkdemonstrator.node.Concentrator;
import org.kryptonmlt.networkdemonstrator.node.Sensor;

/**
 *
 * @author Kurt
 */
public class QueryPerformer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryPerformer.class);
    private final Concentrator centralNode;
    private final Map<Long, Sensor> leafNodes;
    private final DecimalFormat df = new DecimalFormat("0.####");
    private final double theta;
    private final WorthType worthType;
    private long timeStarted = 0;
    private final float[] clusterParameter;
    private boolean isKmeans;
    private final int[] closestK;

    public QueryPerformer(Concentrator centralNode, List<Sensor> leafNodes, double theta, WorthType worthType, int[] k, float[] row, int[] closestKCount) {
        timeStarted = System.currentTimeMillis();
        this.centralNode = centralNode;
        this.worthType = worthType;
        this.theta = theta;
        this.closestK = closestKCount;
        if (k == null) {
            this.clusterParameter = row;
            isKmeans = false;
        } else {
            float[] output = new float[k.length];
            for (int i = 0; i < k.length; i++) {
                output[i] = k[i];
            }
            this.clusterParameter = output;
            isKmeans = true;
        }
        this.leafNodes = new HashMap<>();
        for (Sensor lf : leafNodes) {
            while (!lf.isConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.error("Error when trying to wait for peer to connect..", ex);
                }
            }
            this.leafNodes.put(lf.getId(), lf);
        }
    }

    @Override
    public void run() {
        boolean allfinished = false;
        try {
            Thread.sleep(10000l);
            System.gc();
        } catch (InterruptedException ex) {
            LOGGER.error("Error when trying before starting query loop..", ex);
        }
        while (!allfinished) {
            Set<Long> peers = centralNode.getPeers().keySet();
            for (Long id : peers) {
                int updates = centralNode.getPeers().get(id).getTimesWeightsUpdated();
                int totalDataToBeSent = leafNodes.get(id).getTotalMessagesToBeSentSoFar();
                LOGGER.info("Peer {} Finished:{} sent {} of {} = {}% messages sent. Local Average Error: {}, Server Average Error: {}",
                        id, leafNodes.get(id).isFinished(), updates, totalDataToBeSent, df.format((updates / (float) totalDataToBeSent) * 100),
                        df.format(leafNodes.get(id).getAverageLocalError()), df.format(leafNodes.get(id).getAverageCentralNodeError()));
            }
            LOGGER.info("----------------------------------------------------------------------------");
            // Check if all leaf nodes finished sending !
            allfinished = true;
            for (Sensor leaf : leafNodes.values()) {
                if (!leaf.isFinished()) {
                    allfinished = false;
                    break;
                }
            }
            if (!allfinished) {
                try {
                    Thread.sleep(5000l);
                } catch (InterruptedException ex) {
                    LOGGER.error("Error when trying to wait to get peers..", ex);
                }
            }
        }
        //compute query ensemble learning validation
        LOGGER.info("Run finished");
        LOGGER.info("Starting query evaluation");
        for (Sensor leaf : leafNodes.values()) {
            LOGGER.info("Starting query evaluation Device {}", leaf.getId());
            leaf.queryValidation();
        }
        LOGGER.info("Finished query evaluation");

        float timeTakenSeconds = (System.currentTimeMillis() - timeStarted) / 1000.0f;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("ERRORS_STUDY/ND_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter automatedBW = new BufferedWriter(new FileWriter(new File("AUTOMATED_ERRORS_STUDY/AND_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter detailValuesBW = new BufferedWriter(new FileWriter(new File("SENSOR_STUDY/sensor_study_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter concentratorWeightsValuesBW = new BufferedWriter(new FileWriter(new File("SENSOR_STUDY/concentrator_weights_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter localWeightsValuesBW = new BufferedWriter(new FileWriter(new File("SENSOR_STUDY/local_weights_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter minMaxValuesBW = new BufferedWriter(new FileWriter(new File("SENSOR_STUDY/min_max_" + worthType.name() + "_" + theta + ".txt")));

            int totalUpdates = 0;
            int totalDataToBeSent = 0;
            int peersCount = 0;
            double totalDifferenceError = 0;
            double totalLocalError = 0;
            double totalCentralError = 0;
            int totalTimesErrorExceeded = 0;
            int totalTimesErrorAcceptable = 0;
            int totalMean = 0;
            int totalVariance = 0;
            int totalP = 0;
            double[][] quantizedError = new double[closestK.length][clusterParameter.length];
            double[][] quantizedErrorDistanceOnly = new double[closestK.length][clusterParameter.length];
            double generalError = 0;
            double idealError = 0;
            double baseLineError = 0;

            //statistics
            List<Double> E_DASH = new ArrayList<>(); // local model
            List<Double> E = new ArrayList<>();  // central/obsolete model
            List<Double> Y = new ArrayList<>(); // difference
            List<Double> actual = new ArrayList<>(); // local model
            List<Double> localPredicted = new ArrayList<>();  // central/obsolete model
            List<Double> centralPredicted = new ArrayList<>(); // difference
            double E_DASH_Mean = 0;
            double E_DASH_Variance = 0;
            double E_Mean = 0;
            double E_Variance = 0;
            double Y_Mean = 0;
            double Y_Variance = 0;
            List<double[]> weights = new ArrayList<>();
            Map<Long, DevicePeer> peers = centralNode.getPeers();
            for (Long id : peers.keySet()) {
                totalUpdates += peers.get(id).getTimesWeightsUpdated();
                totalDataToBeSent += leafNodes.get(id).getTotalMessagesToBeSentSoFar();
                double localError = leafNodes.get(id).getAverageLocalError();
                double centralError = leafNodes.get(id).getAverageCentralNodeError();
                totalDifferenceError += Math.abs(centralError - localError);
                totalLocalError += localError;
                totalCentralError += centralError;
                totalTimesErrorExceeded += leafNodes.get(id).getTimesErrorExceeded();
                totalTimesErrorAcceptable += leafNodes.get(id).getTimesErrorAcceptable();
                totalP += leafNodes.get(id).getP();
                totalMean += leafNodes.get(id).getMeanVariance().getMean();
                totalVariance += leafNodes.get(id).getMeanVariance().getVariance();

                //statistics
                if (leafNodes.get(id).isStatistics()) {
                    double[] e_dash_temp = leafNodes.get(id).getE_DASH();
                    double[] e_temp = leafNodes.get(id).getE();
                    double[] y_temp = leafNodes.get(id).getY();
                    double[] actual_temp = leafNodes.get(id).getActual();
                    double[] localPredicted_temp = leafNodes.get(id).getLocalPredicted();
                    double[] centralPredicted_temp = leafNodes.get(id).getCentralPredicted();
                    for (int i = 0; i < y_temp.length; i++) {
                        E_DASH.add(e_dash_temp[i]);
                        E.add(e_temp[i]);
                        Y.add(y_temp[i]);
                        actual.add(actual_temp[i]);
                        localPredicted.add(localPredicted_temp[i]);
                        centralPredicted.add(centralPredicted_temp[i]);
                    }
                }

                E_DASH_Mean += leafNodes.get(id).getE_DASH_MeanVariance().getMean();
                E_DASH_Variance += leafNodes.get(id).getE_DASH_MeanVariance().getVariance();
                E_Mean += leafNodes.get(id).getE_MeanVariance().getMean();
                E_Variance += leafNodes.get(id).getE_MeanVariance().getVariance();
                Y_Mean += leafNodes.get(id).getY_MeanVariance().getMean();
                Y_Variance += leafNodes.get(id).getY_MeanVariance().getVariance();

                double[][] tempQError = leafNodes.get(id).getQuantizedError();
                for (int i = 0; i < closestK.length; i++) {
                    for (int j = 0; j < clusterParameter.length; j++) {
                        quantizedError[i][j] += tempQError[i][j];
                    }
                }
                double[][] tempQErrorDistanceOnly = leafNodes.get(id).getQuantizedErrorDistanceOnly();
                for (int i = 0; i < closestK.length; i++) {
                    for (int j = 0; j < clusterParameter.length; j++) {
                        quantizedErrorDistanceOnly[i][j] += tempQErrorDistanceOnly[i][j];
                    }
                }
                generalError += leafNodes.get(id).getGeneralError();
                idealError += leafNodes.get(id).getIdealError();
                baseLineError += leafNodes.get(id).getBaseLineError();
                concentratorWeightsValuesBW.write(ConversionUtils.cleanDoubleArrayToString(peers.get(id).getWeights()) + "\n");
                localWeightsValuesBW.write(ConversionUtils.cleanDoubleArrayToString(leafNodes.get(id).getLocalModel().getWeights()) + "\n");
                minMaxValuesBW.write(leafNodes.get(id).getX1().getMin() + "," + leafNodes.get(id).getX1().getMax() + ","
                        + leafNodes.get(id).getX2().getMin() + "," + leafNodes.get(id).getX2().getMax() + "\n");
                peersCount++;
            }

            concentratorWeightsValuesBW.flush();
            concentratorWeightsValuesBW.close();
            localWeightsValuesBW.flush();
            localWeightsValuesBW.close();
            minMaxValuesBW.flush();
            minMaxValuesBW.close();

            for (int i = 0; i < closestK.length; i++) {
                for (int j = 0; j < clusterParameter.length; j++) {
                    quantizedError[i][j] = quantizedError[i][j] / (float) peersCount;
                    quantizedErrorDistanceOnly[i][j] = quantizedErrorDistanceOnly[i][j] / (float) peersCount;
                }
            }
            generalError = generalError / (float) peersCount;
            idealError = idealError / (float) peersCount;
            baseLineError = baseLineError / (float) peersCount;

            bw.write("System " + peersCount + " devices (Using " + worthType.name() + " at " + theta + " error):\n");
            bw.write(totalUpdates + " of " + totalDataToBeSent + " = " + df.format((totalUpdates / (float) totalDataToBeSent) * 100) + "% messages sent.\n");
            bw.write("Difference Error: " + df.format(totalDifferenceError / (float) peersCount) + ", Local Average Error: "
                    + df.format(totalLocalError / (float) peersCount) + ", Server Average Error: " + df.format(totalCentralError / (float) peersCount) + "\n");
            bw.write("Times Error Exceeded: " + totalTimesErrorExceeded + "\n");
            bw.write("Times Error Acceptable: " + totalTimesErrorAcceptable + "\n");
            bw.write(df.format((totalTimesErrorExceeded / (float) (totalTimesErrorExceeded + totalTimesErrorAcceptable)) * 100) + "% Exceeded\n");
            bw.write("Times Error with update less than without: " + totalP + " of " + totalDataToBeSent + " = " + df.format((totalP / (float) totalDataToBeSent) * 100) + "%\n");
            bw.write("Average Mean: " + df.format(totalMean / (float) peersCount) + "\n");
            bw.write("Average Variance: " + df.format(totalVariance / (float) peersCount) + "\n");

            bw.write("Average E' Mean: " + df.format(E_DASH_Mean / (float) peersCount) + "\n");
            bw.write("Average E' Variance: " + df.format(E_DASH_Variance / (float) peersCount) + "\n");
            bw.write("Average E Mean: " + df.format(E_Mean / (float) peersCount) + "\n");
            bw.write("Average E Variance: " + df.format(E_Variance / (float) peersCount) + "\n");
            bw.write("Average Y Mean: " + df.format(Y_Mean / (float) peersCount) + "\n");
            bw.write("Average Y Variance: " + df.format(Y_Variance / (float) peersCount) + "\n");
            if (isKmeans) {
                bw.write("K: " + Arrays.toString(clusterParameter) + "\n");
            } else {
                bw.write("Row: " + Arrays.toString(clusterParameter) + "\n");
            }
            bw.write("Closest K Used: " + Arrays.toString(closestK) + "\n");
            bw.write("Quantized Error (Distance and Error):\n");
            for (double[] qe : quantizedError) {
                bw.write(Arrays.toString(qe) + "\n");
            }
            bw.write("Quantized Error: (Distance Only):\n");
            for (double[] qed : quantizedErrorDistanceOnly) {
                bw.write(Arrays.toString(qed) + "\n");
            }
            bw.write("General Error: " + df.format(generalError) + "\n");
            bw.write("Ideal Error: " + df.format(idealError) + "\n");
            bw.write("BaseLine Error: " + df.format(baseLineError) + "\n");
            bw.write("Took " + timeTakenSeconds + " seconds");
            bw.flush();
            bw.close();
            automatedBW.write(peersCount + "\n"); //devices
            automatedBW.write(theta + "\n"); //theta
            automatedBW.write(worthType.name() + "\n"); //worth type
            automatedBW.write(df.format(totalDifferenceError / (float) peersCount) + "\n"); //average error
            automatedBW.write(df.format((totalUpdates / (float) totalDataToBeSent) * 100) + "\n"); // messages sent %
            automatedBW.write(df.format(totalMean / (float) peersCount) + "\n"); // average mean
            automatedBW.write(df.format(totalVariance / (float) peersCount) + "\n"); // average variance
            automatedBW.write(df.format(totalP / (float) totalDataToBeSent) + "\n"); // average P            
            automatedBW.write(df.format(E_DASH_Mean / (float) peersCount) + ","
                    + df.format(E_DASH_Variance / (float) peersCount) + ","
                    + df.format(E_Mean / (float) peersCount) + "," + df.format(E_Variance / (float) peersCount) + ","
                    + df.format(Y_Mean / (float) peersCount) + "," + (df.format(Y_Variance / (float) peersCount) + "\n"));
            automatedBW.write(ConversionUtils.cleanFloatArrayToString(clusterParameter) + "\n");
            automatedBW.write(ConversionUtils.cleanIntArrayToString(closestK) + "\n");
            for (double[] qe : quantizedError) {
                automatedBW.write(ConversionUtils.cleanDoubleArrayToString(qe) + "\n");
            }
            for (double[] qed : quantizedErrorDistanceOnly) {
                automatedBW.write(ConversionUtils.cleanDoubleArrayToString(qed) + "\n");
            }
            automatedBW.write(df.format(generalError) + "\n");
            automatedBW.write(df.format(idealError) + "\n");
            automatedBW.write(df.format(baseLineError) + "\n");
            for (int i = 0; i < Y.size(); i++) {
                //automatedBW.write(E_DASH.get(i) + "," + E.get(i) + "," + Y.get(i) + "\n");
                detailValuesBW.write(actual.get(i) + "," + localPredicted.get(i) + "," + centralPredicted.get(i) + "\n");
            }
            automatedBW.flush();
            automatedBW.close();
            detailValuesBW.flush();
            detailValuesBW.close();
        } catch (IOException ex) {
            LOGGER.error("Error when trying to write stats to results file...", ex);
        }

        if (this.centralNode.getPlot() != null) {
            this.centralNode.getPlot().getChart().dispose();
        }
        System.exit(0);
    }

}
