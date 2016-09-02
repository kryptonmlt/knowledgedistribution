package org.kryptonmlt.automatedvisualizer.pojos;

/**
 *
 * @author Kurt
 */
public class EnsembleModelStat implements Comparable<EnsembleModelStat> {

    private double gamma;

    private int messages3;

    private int messages12;

    private double error;

    private double theta;

    public EnsembleModelStat(double gamma, int messages3, int messages12, double error, double theta) {
        this.gamma = gamma;
        this.messages3 = messages3;
        this.messages12 = messages12;
        this.error = error;
        this.theta = theta;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public int getMessages3() {
        return messages3;
    }

    public void setMessages3(int messages3) {
        this.messages3 = messages3;
    }

    public int getMessages12() {
        return messages12;
    }

    public void setMessages12(int messages12) {
        this.messages12 = messages12;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "EnsembleModelStat{" + "gamma=" + gamma + ", messages3=" + messages3 + ", messages12=" + messages12 + ", error=" + error + ", theta=" + theta + '}';
    }

    @Override
    public int compareTo(EnsembleModelStat o) {
        if (this.getError() < o.getError()) {
            return -1;
        } else if (this.getError() > o.getError()) {
            return 1;
        }
        return 0;
    }
}
