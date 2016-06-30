package org.kryptonmlt.networkdemonstrator.learning;

import java.util.List;

public interface Clustering {

    List<double[]> getCentroids();

    /**
     * Update the centroid using the point
     *
     * @param point
     * @return cluster id the point is associated with
     */
    Integer update(double[] point);

    /**
     * Description of clustering technique example alpha and K or row
     *
     * @return
     */
    String getDescription();

    /**
     * Sets the centroids of the clustering technique. Can be used in
     * initialization
     *
     * @param centroids
     */
    void setCentroids(List<double[]> centroids);

    public void updateError(int i, double e);

    public List<Double> getErrors();

    public void setErrors(List<Double> errors);
}
