package org.kryptonmlt.networkdemonstrator.node;

import org.kryptonmlt.networkdemonstrator.learning.OnlineVarianceMean;

/**
 *
 * @author Kurt
 */
public interface LeafNode {

    public Long getId();

    public boolean isConnected();

    public int getTotalMessagesToBeSentSoFar();

    public double getAverageLocalError();

    public double getAverageCentralNodeError();

    public boolean isFinished();

    public int getTimesErrorExceeded();

    public int getTimesErrorAcceptable();

    public int getP();

    public OnlineVarianceMean getMeanVariance();

}
