package org.kryptonmlt.networkdemonstrator.learning;

import java.util.ArrayList;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;

public class ART implements Clustering {

    private List<double[]> centroids = new ArrayList<>();
    private List<Double> errors = new ArrayList<>();
    private final double row;
    private final double alpha;
    private final double clusteringAlpha;

    public ART(double row, double alpha, double clusteringAlpha) {
        this.row = row;
        this.alpha = alpha;
        this.clusteringAlpha = clusteringAlpha;

    }

    @Override
    public List<double[]> getCentroids() {
        return centroids;
    }

    @Override
    public Integer update(double[] point) {
        int nearestCentroid = VectorUtils.classify(point, centroids);
        if (nearestCentroid == -1) {
            centroids.add(point);
            errors.add(0d);
            nearestCentroid = 0;
        } else if (VectorUtils.distance(point, centroids.get(nearestCentroid)) < row) {
            // Move centroid
            this.centroids.set(nearestCentroid,
                    VectorUtils.moveCentroid(point, centroids.get(nearestCentroid), alpha));

        } else {
            centroids.add(point);
            nearestCentroid = centroids.size() - 1;
        }
        return nearestCentroid;
    }

    @Override
    public void updateError(int i, double e) {
        double oldError = errors.get(i);
        double update = e - oldError;
        errors.set(i, (clusteringAlpha * update) + oldError);
    }

    @Override
    public String getDescription() {
        return row + "_" + alpha;
    }

    @Override
    public void setCentroids(List<double[]> centroids) {
        this.centroids = centroids;
    }

    @Override
    public void setErrors(List<Double> errors) {
        this.errors = errors;
    }

    @Override
    public List<Double> getErrors() {
        return errors;
    }
}
