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
            int totalUpdates = 0;
            int totalDataToBeSent = 0;
            int peersCount = 0;
            double totalDifferenceError = 0;
            double totalLocalError = 0;
            double totalCentralError = 0;
            int totalTimesErrorExceeded = 0;
            int totalTimesErrorAcceptable = 0;
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
                peersCount++;
            }
            bw.write("System " + peersCount + " devices (Using " + worthType.name() + " at " + theta + " error):\n");
            bw.write(totalUpdates + " of " + totalDataToBeSent + " = " + df.format((totalUpdates / (float) totalDataToBeSent) * 100) + "% messages sent.\n");
            bw.write("Difference Error: " + df.format(totalDifferenceError / (float) peersCount) + ", Local Average Error: "
                    + df.format(totalLocalError / (float) peersCount) + ", Server Average Error: " + df.format(totalCentralError / (float) peersCount) + "\n");
            bw.write("Times Error Exceeded: " + totalTimesErrorExceeded + "\n");
            bw.write("Times Error Acceptable: " + totalTimesErrorAcceptable + "\n");
            bw.write(df.format((totalTimesErrorExceeded / (float) (totalTimesErrorExceeded + totalTimesErrorAcceptable)) * 100) + "% Exceeded\n");
            bw.write("Took " + timeTakenSeconds + " seconds");
            bw.flush();
            bw.close();
            automatedBW.write(peersCount + "\n");
            automatedBW.write(theta + "\n");
            automatedBW.write(worthType.name() + "\n");
            automatedBW.write(df.format(totalDifferenceError / (float) peersCount) + "\n");
            automatedBW.write(df.format((totalUpdates / (float) totalDataToBeSent) * 100) + "\n");
            automatedBW.flush();
            automatedBW.close();
        } catch (IOException ex) {
            LOGGER.error("Error when trying to write stats to results file...", ex);
        }
        if (this.centralNode.getPlot() != null) {
            this.centralNode.getPlot().getChart().dispose();
        }
        System.exit(0);
    }

}
