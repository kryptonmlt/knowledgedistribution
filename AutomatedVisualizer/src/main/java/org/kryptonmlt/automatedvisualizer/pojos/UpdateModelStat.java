package org.kryptonmlt.automatedvisualizer.pojos;

/**
 *
 * @author Kurt
 */
public class UpdateModelStat implements Comparable<UpdateModelStat> {

    private double theta;

    private int messages;

    private double error;

    public UpdateModelStat(double theta, int messages, double error) {
        this.theta = theta;
        this.messages = messages;
        this.error = error;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    @Override
    public int compareTo(UpdateModelStat o) {
        if (this.getError() < o.getError()) {
            return -1;
        } else if (this.getError() > o.getError()) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "UpdateModelStat{" + "theta=" + theta + ", messages=" + messages + ", error=" + error + '}';
    }

}
