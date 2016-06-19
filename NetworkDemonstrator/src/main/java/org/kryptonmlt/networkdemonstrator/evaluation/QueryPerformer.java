package org.kryptonmlt.networkdemonstrator.evaluation;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kryptonmlt.networkdemonstrator.node.CentralNode;
import org.kryptonmlt.networkdemonstrator.node.LeafNode;
import org.kryptonmlt.networkdemonstrator.pojos.DevicePeer;
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
    private final DecimalFormat df = new DecimalFormat("0.###");

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
        boolean temp = true;
        while (temp) {
            try {
                Thread.sleep(2000l);
            } catch (InterruptedException ex) {
                LOGGER.error("Error when trying to wait to get peers..", ex);
            }
            Map<Long, DevicePeer> peers = centralNode.getPeers();
            for (Long id : peers.keySet()) {
                int updates = peers.get(id).getTimesWeightsUpdated();
                int totalDataToBeSent = leafNodes.get(id).getTotalMessagesToBeSentSoFar();
                LOGGER.info("Peer {} sent {} messages out of {} = {}% messages", id, updates, totalDataToBeSent, df.format((updates / (float) totalDataToBeSent) * 100));
            }
        }
        for (int i = 0; i < 15; i++) {
            try {
                Thread.sleep(10000l);
            } catch (InterruptedException ex) {
                LOGGER.error("Error when trying to wait for querying..", ex);
            }
            double[] query = {0.0, 0.0};
            double result = centralNode.query(query);
            double resultAll = centralNode.queryAll(query);
            LOGGER.info("Query {} = Quantized Result: {} , Average Result: {} ", i, result, resultAll);
        }
    }

}
