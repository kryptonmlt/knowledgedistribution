package org.kryptonmlt.networkdemonstrator.ml_algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jzy3d.maths.Coord3d;

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
    private final double MAX;
    private final double MIN;

    public PDF(double mean, double variance, double min, double max) {
        this.mean = mean;
        this.sd = Math.sqrt(variance);
        this.r = new Random();
        this.INITIAL = 1 / (this.sd * Math.sqrt(2 * Math.PI));
        this.MIN = min;
        this.MAX = max - min;
    }

    public double calculate(double x) {
        double t = (x - mean) / this.sd;
        return INITIAL * Math.pow(Math.E, -0.5 * t * t);
    }

    /**
     *
     * @return {input, output}
     */
    public double[] getNext() {
        double[] result = new double[2];
        result[0] = (r.nextDouble() * MAX) + MIN;
        result[1] = this.calculate(result[0]);
        return result;
    }

    public List<Double> getNextThousand() {
        List<Double> temp = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            temp.add(this.getNext()[0]);
        }
        return temp;
    }

    public List<Coord3d> getNextThousand3D() {
        List<Coord3d> temp = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            double[] d = this.getNext();
            temp.add(new Coord3d(d[0], d[1], 0));
        }
        return temp;
    }

}
