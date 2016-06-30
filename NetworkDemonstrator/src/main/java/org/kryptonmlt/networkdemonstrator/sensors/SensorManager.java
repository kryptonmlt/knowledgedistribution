package org.kryptonmlt.networkdemonstrator.sensors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kurt
 */
public class SensorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensorManager.class);

    private final List<double[]> data;
    private final List<double[]> sampledData = new ArrayList<>();
    private int counter = 0;
    private final double samplingRate;
    private final Random r;

    public SensorManager(XSSFSheet sheet, int startCol, int numberOfFeatures, String filename, double samplingRate) {
        this.data = new DataLoader(sheet, startCol, numberOfFeatures, filename).getFeatures();
        this.samplingRate = samplingRate;
        this.r = new Random();
    }

    public double[] requestData() {
        double[] result = data.get(counter);
        counter++;
        double gen = r.nextDouble();
        if (gen < samplingRate) {
            sampledData.add(result);
        }
        return result;
    }

    public List<double[]> requestValidationData() {
        return sampledData;
    }

    public boolean isReadyForRead() {
        return counter < data.size();
    }

    public void reset() {
        counter = 0;
    }

}
