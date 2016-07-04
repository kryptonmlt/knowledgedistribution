package org.kryptonmlt.networkdemonstrator.pojos;

/**
 *
 * @author Kurt
 */
public class NodeDistance implements Comparable<NodeDistance> {

    private long id;
    private double distance;

    public NodeDistance(long id, double distance) {
        this.id = id;
        this.distance = distance;
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

    @Override
    public int compareTo(NodeDistance o) {
        if (this.getDistance() < o.getDistance()) {
            return 1;
        } else if (this.getDistance() > o.getDistance()) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "NodeDistance{" + "id=" + id + ", distance=" + distance + '}';
    }

}
