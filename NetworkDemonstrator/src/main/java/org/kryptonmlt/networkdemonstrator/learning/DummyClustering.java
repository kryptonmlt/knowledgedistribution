package org.kryptonmlt.networkdemonstrator.learning;

import java.util.ArrayList;
import java.util.List;
import org.kryptonmlt.networkdemonstrator.utils.ConversionUtils;
import org.kryptonmlt.networkdemonstrator.utils.VectorUtils;

/**
 *
 * @author Kurt
 */
public class DummyClustering implements Clustering {

    private List<double[]> centroids;
    private List<Double> errors;
    private List<Integer> used;

    public DummyClustering(List<double[]> centroids, List<Double> errors, List<Integer> used) {
        this.centroids = centroids;
        this.errors = errors;
        this.used = used;
    }

    @Override
    public List<Double> getUsedNormalized(Double total) {
        return VectorUtils.normalizeList(ConversionUtils.integerListToDoubleList(used), total);
    }

    @Override
    public List<Double> getErrorsNormalized(Double total) {
        return VectorUtils.normalizeList(errors, total);
    }

    @Override
    public List<double[]> getCentroids() {
        return centroids;
    }

    @Override
    public void setCentroids(List<double[]> centroids) {
        this.centroids = centroids;
    }

    @Override
    public List<Double> getErrors() {
        return errors;
    }

    @Override
    public void setErrors(List<Double> errors) {
        this.errors = errors;
    }

    @Override
    public Integer update(double[] point) {
        return VectorUtils.classify(point, centroids);
    }

    @Override
    public String getDescription() {
        return "DummyClustering";
    }

    @Override
    public void updateError(int i, double e) {

    }

    @Override
    public List<Integer> getUsed() {
        return used;
    }

    @Override
    public void setUsed(List<Integer> used) {
        this.used = used;
    }

}
