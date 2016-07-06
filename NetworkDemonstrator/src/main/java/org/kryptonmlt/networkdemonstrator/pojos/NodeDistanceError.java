package org.kryptonmlt.networkdemonstrator.pojos;

/**
 *
 * @author Kurt
 */
public class NodeDistanceError implements Comparable<NodeDistanceError> {

    private long id;
    private double distance;
    private double error;
    private double weight;

    public NodeDistanceError(long id, double distance, Double error) {
        this.id = id;
        this.distance = distance;

        if (error == null) {
            this.error = 0;
            this.weight = 1;
        } else {
            if (error == 0.0) {
                error = Double.MAX_VALUE;
            }
            this.error = error;
            double sigmoidDistance = 1 / (1 + Math.exp(-this.distance));
            double sigmoidError = 1 / (1 + Math.exp(-this.error));
            this.weight = (0.5 * Math.exp(-sigmoidDistance)) + (0.5 * Math.exp(-sigmoidError));
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /*@Override
    public int compareTo(NodeDistanceError o) {
        if (this.getWeight() < o.getWeight()) {
            return 1;
        } else if (this.getWeight() > o.getWeight()) {
            return -1;
        }
        return 0;
    }*/
    @Override
    public int compareTo(NodeDistanceError o) {
        if (this.getDistance() < o.getDistance()) {
            return -1;
        } else if (this.getDistance() > o.getDistance()) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "NodeDistance{" + "id=" + id + ", distance=" + distance + ", error=" + error + '}';
    }

}
