package org.kryptonmlt.networkdemonstrator.pojos;

/**
 *
 * @author Kurt
 */
public class NodeDistance implements Comparable<NodeDistance> {

    private long id;
    private double distance;
    private double error;

    public NodeDistance(long id, double distance, double error) {
        this.id = id;
        this.distance = distance;
        if (error == 0.0) {
            error = Double.MAX_VALUE;
        }
        this.error = error;
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

    @Override
    public int compareTo(NodeDistance o) {
        double e0 = this.error;
        double e1 = o.error;
        double d0 = Math.pow(this.distance, 4);
        double d1 = Math.pow(o.distance, 4);
        if (d0 * e0 < d1 * e1) {
            return -1;
        } else if (d0 * e0 > d1 * e1) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "NodeDistance{" + "id=" + id + ", distance=" + distance + ", error=" + error + '}';
    }

}
