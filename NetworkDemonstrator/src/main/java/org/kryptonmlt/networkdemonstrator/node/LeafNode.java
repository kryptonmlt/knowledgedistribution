package org.kryptonmlt.networkdemonstrator.node;

import java.util.List;
import org.kryptonmlt.networkdemonstrator.learning.Clustering;
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

    public double[] getY();

    public double[] getE_DASH();

    public double[] getE();

    public OnlineVarianceMean getE_DASH_MeanVariance();

    public OnlineVarianceMean getE_MeanVariance();

    public OnlineVarianceMean getY_MeanVariance();

    public boolean isStatistics();

    public double[] getLocalPredicted();

    public double[] getCentralPredicted();

    public double[] getActual();

    public double[] getQuantizedError();

    public double getGeneralError();

    public int getQueries();

    public void queryValidation();

    public Clustering getClustering();
    
    public double getIdealError();
}
