package org.kryptonmlt.network.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kryptonmlt.networkdemonstrator.enums.WorthType;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.pojos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class QueryPerformer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryPerformer.class);
    private final CentralNode centralNode;
    private final Map<Long, LeafNode> leafNodes;
    private final DecimalFormat df = new DecimalFormat("0.####");
    private final List<double[]> validationData = new ArrayList<>();
    private final double theta;
    private final WorthType worthType;
    private long timeStarted = 0;

    public QueryPerformer(CentralNode centralNode, List<LeafNode> leafNodes, double theta, WorthType worthType) {
        timeStarted = System.currentTimeMillis();
        this.centralNode = centralNode;
        this.worthType = worthType;
        this.theta = theta;
        this.leafNodes = new HashMap<>();
        for (LeafNode lf : leafNodes) {
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
        double[] query = {0.0, 0.0};
        boolean allfinished = false;
        try {
            Thread.sleep(15000l);
            System.gc();
        } catch (InterruptedException ex) {
            LOGGER.error("Error when trying before starting query loop..", ex);
        }
        while (!allfinished) {
            Map<Long, Peer> peers = centralNode.getPeers();
            for (Long id : peers.keySet()) {
                int updates = peers.get(id).getTimesWeightsUpdated();
                int totalDataToBeSent = leafNodes.get(id).getTotalMessagesToBeSentSoFar();
                LOGGER.info("Peer {} Finished:{} sent {} of {} = {}% messages sent. Local Average Error: {}, Server Average Error: {}",
                        id, leafNodes.get(id).isFinished(), updates, totalDataToBeSent, df.format((updates / (float) totalDataToBeSent) * 100),
                        df.format(leafNodes.get(id).getAverageLocalError()), df.format(leafNodes.get(id).getAverageCentralNodeError()));
            }
            // run query at origin
            double result = centralNode.query(query);
            double resultAll = centralNode.queryAll(query);
            LOGGER.info("Query - Quantized Result: {} , Average Result: {}"
                    + "\n-------------------------------------------------------------------------------------------------------------", result, resultAll);
            try {
                Thread.sleep(5000l);
            } catch (InterruptedException ex) {
                LOGGER.error("Error when trying to wait to get peers..", ex);
            }

            // Check if all leaf nodes finished sending !
            allfinished = true;
            for (LeafNode leaf : leafNodes.values()) {
                if (!leaf.isFinished()) {
                    allfinished = false;
                    break;
                }
            }
        }
        LOGGER.info("Run finished");
        float timeTakenSeconds = (System.currentTimeMillis() - timeStarted) / 1000.0f;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("ERRORS_STUDY/ND_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter automatedBW = new BufferedWriter(new FileWriter(new File("AUTOMATED_ERRORS_STUDY/AND_" + worthType.name() + "_" + theta + ".txt")));
            BufferedWriter detailValuesBW = new BufferedWriter(new FileWriter(new File("sensor_study_" + worthType.name() + "_" + theta + ".txt")));
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

            Map<Long, Peer> peers = centralNode.getPeers();
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
                    E_DASH_Mean += leafNodes.get(id).getE_DASH_MeanVariance().getMean();
                    E_DASH_Variance += leafNodes.get(id).getE_DASH_MeanVariance().getVariance();
                    E_Mean += leafNodes.get(id).getE_MeanVariance().getMean();
                    E_Variance += leafNodes.get(id).getE_MeanVariance().getVariance();
                    Y_Mean += leafNodes.get(id).getY_MeanVariance().getMean();
                    Y_Variance += leafNodes.get(id).getY_MeanVariance().getVariance();
                }

                peersCount++;
            }
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

            bw.write("Took " + timeTakenSeconds + " seconds");
            bw.flush();
            bw.close();
            automatedBW.write(peersCount + "\n");
            automatedBW.write(theta + "\n");
            automatedBW.write(worthType.name() + "\n");
            automatedBW.write(df.format(totalDifferenceError / (float) peersCount) + "\n");
            automatedBW.write(df.format((totalUpdates / (float) totalDataToBeSent) * 100) + "\n");
            automatedBW.write(df.format(totalMean / (float) peersCount) + "\n");
            automatedBW.write(df.format(totalVariance / (float) peersCount) + "\n");
            automatedBW.write(df.format(totalP / (float) totalDataToBeSent) + "\n");
            for (int i = 0; i < Y.size(); i++) {
                automatedBW.write(E_DASH.get(i) + "," + E.get(i) + "," + Y.get(i) + "\n");
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
