package org.kryptonmlt.network.stats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public QueryPerformer(CentralNode centralNode, List<LeafNode> leafNodes) {
        this.centralNode = centralNode;
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
        boolean temp = true;
        try {
            Thread.sleep(15000l);
            System.gc();
        } catch (InterruptedException ex) {
            LOGGER.error("Error when trying before starting query loop..", ex);
        }
        while (temp) {
            Map<Long, Peer> peers = centralNode.getPeers();
            for (Long id : peers.keySet()) {
                int updates = peers.get(id).getTimesWeightsUpdated();
                int totalDataToBeSent = leafNodes.get(id).getTotalMessagesToBeSentSoFar();
                LOGGER.info("Peer {} sent {} of {} = {}% messages sent. Local Average Error: {}, Server Average Error: {}",
                        id, updates, totalDataToBeSent, df.format((updates / (float) totalDataToBeSent) * 100),
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
        }
    }

}
