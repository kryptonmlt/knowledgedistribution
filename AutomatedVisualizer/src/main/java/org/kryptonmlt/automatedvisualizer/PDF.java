package org.kryptonmlt.automatedvisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Kurt
 */
public class PDF {

    private final double mean;
    //private final double variance;
    private final double sd;
    private final Random r;
    private final double INITIAL;

    public PDF(double mean, double variance) {
        this.mean = mean;
        this.sd = Math.sqrt(variance);
        this.r = new Random();
        INITIAL = 1 / (this.sd * Math.sqrt(2 * Math.PI));
    }

    public double calculate(double x) {
        double t = (x - mean) / this.sd;
        return INITIAL * Math.pow(Math.E, -0.5 * t * t);
    }

    public double getNext() {
        return this.calculate(r.nextDouble());
    }

    public List<Double> getNextThousand() {
        List<Double> temp = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            temp.add(this.getNext());
        }
        return temp;
    }

}
