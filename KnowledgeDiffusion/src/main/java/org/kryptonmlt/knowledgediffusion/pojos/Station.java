package org.kryptonmlt.knowledgediffusion.pojos;

/**
 *
 * @author Kurt
 */
public class Station {

    private String id;
    private String name;
    private double latitude;
    private double longtitude;

    public Station(String id, String name, double latitude, double longtitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    @Override
    public String toString() {
        return "Station{" + "id=" + id + ", name=" + name + ", latitude=" + latitude + ", longtitude=" + longtitude + '}';
    }

}
