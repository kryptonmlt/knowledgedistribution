package org.kryptonmlt.networkdemonstrator.learning;

import java.util.Random;

/**
 *
 * @author Kurt
 */
public class PDF {

    private double mean = 0.0;
    private double variance = 0.0;

    public PDF(double mean, double variance) {
        this.mean = mean;
        this.variance = variance;
    }

    public double calculate(double x) {
        return (1 / (mean * Math.sqrt(2 * Math.PI))) * Math.pow(Math.E, -0.5 * Math.pow((x - mean) / variance, 2));
    }

    public static void main(String[] args) {
        PDF pdf = new PDF(0.1794, 0.2199);
        int max = 1000;
        Random r = new Random();
        for (int i = 0; i < max; i++) {
            System.out.println(pdf.calculate(r.nextDouble()));
        }
    }

}
