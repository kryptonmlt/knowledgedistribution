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
    private final List<double[]> sampledData;
    private int counter = 0;

    public SensorManager(XSSFSheet sheet, XSSFSheet querySheet, int startCol, int numberOfFeatures, String filename, double samplingRate) {
        DataLoader dL = new DataLoader(sheet, querySheet, startCol, numberOfFeatures, filename, samplingRate);
        this.data = dL.getFeatures();
        this.sampledData = dL.getSampledData();
    }

    public double[] requestData() {
        double[] result = data.get(counter);
        counter++;
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
