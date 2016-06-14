package org.kryptonmlt.networkdemonstrator.learning;

import java.util.ArrayList;
import java.util.List;

import org.kryptonmlt.networkdemonstrator.tools.VectorUtils;

public class OnlineKmeans implements Clustering {

    private final int k;
    private List<double[]> centroids = new ArrayList<>();
    private final double alpha;

    public OnlineKmeans(int k, double alpha) {
        this.k = k;
        this.alpha = alpha;
    }

    @Override
    public List<double[]> getCentroids() {
        return centroids;
    }

    @Override
    public Integer update(double[] point) {
        if (centroids.size() < k) {
            centroids.add(point);
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
    public String getDescription() {
        return k + "_" + alpha;
    }

    @Override
    public void setCentroids(List<double[]> centroids) {
        this.centroids = centroids;

    }
}
