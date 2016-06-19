package org.kryptonmlt.networkdemonstrator.node;

/**
 *
 * @author Kurt
 */
public interface LeafNode{

    public Long getId();

    public boolean isConnected();

    public int getTotalMessagesToBeSentSoFar();
    
    public double getAverageLocalError();
    
    public double getAverageCentralNodeError();
    
}
