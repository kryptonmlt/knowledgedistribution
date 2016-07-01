package org.kryptonmlt.networkdemonstrator.learning;

import java.util.ArrayList;
import java.util.List;

import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;

public class OnlineKmeans implements Clustering {

    private final int k;
    private List<double[]> centroids = new ArrayList<>();
    private List<Double> errors = new ArrayList<>();
    private final double alpha;
    private final double clusteringAlpha;

    public OnlineKmeans(int k, double alpha, double clusteringAlpha) {
        this.k = k;
        this.alpha = alpha;
        this.clusteringAlpha = clusteringAlpha;
    }

    @Override
    public List<double[]> getCentroids() {
        return centroids;
    }

    @Override
    public List<Double> getErrors() {
        return errors;
    }

    @Override
    public Integer update(double[] point) {
        if (centroids.size() < k) {
            centroids.add(point);
            errors.add(0d);
            return centroids.size() - 1;
        } else {
            Integer nearestCentroid = VectorUtils.classify(point, centroids);
            // Move centroid
            this.centroids.set(nearestCentroid,
                    VectorUtils.moveCentroid(point, centroids.get(nearestCentroid), alpha));

            return nearestCentroid;
        }
    }

    @Override
    public void updateError(int i, double e) {
        double oldError = errors.get(i);
        double update = e - oldError;
        errors.set(i, (clusteringAlpha * update) + oldError);
    }

    @Override
    public String getDescription() {
        return k + "_" + alpha;
    }

    @Override
    public void setCentroids(List<double[]> centroids) {
        this.centroids = centroids;
    }

    @Override
    public void setErrors(List<Double> errors) {
        this.errors = errors;
    }
}
