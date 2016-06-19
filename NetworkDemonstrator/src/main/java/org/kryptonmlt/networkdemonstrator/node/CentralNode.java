package org.kryptonmlt.networkdemonstrator.node;

import java.util.Map;
import org.kryptonmlt.networkdemonstrator.pojos.Peer;

/**
 *
 * @author Kurt
 */
public interface CentralNode {

    public Map<Long, Peer> getPeers();

    public double queryAll(double[] query);

    public double query(double[] query);
    
}
