package org.kryptonmlt.networkdemonstrator.sensors;

import java.util.ArrayList;
import java.util.List;
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
    private int counter = 0;
    private int currentGeneration = 0;
    private final int splitAmount;
    private final int maximumGeneration;

    public SensorManager(XSSFSheet sheet, int startCol, int numberOfFeatures, String filename, int kFold) {
        this.data = new DataLoader(sheet, startCol, numberOfFeatures, filename).getFeatures();
        this.splitAmount = (data.size() / kFold) - 1;
        this.maximumGeneration = kFold;
    }

    public double[] requestData() {
        if (counter == (currentGeneration * splitAmount)) {
            counter += splitAmount;
        }
        double[] result = data.get(counter);
        counter++;
        return result;
    }

    public List<double[]> requestValidationData() {
        List<double[]> temp = new ArrayList<>();
        int start = currentGeneration * splitAmount;
        for (int i = 0; i < splitAmount; i++) {
            temp.add(this.data.get(start + i));
        }
        return temp;
    }

    public boolean isReadyForRead() {
        return counter < data.size();
    }

    public boolean isAvailable() {
        return currentGeneration < maximumGeneration;
    }

    public void reset() {
        counter = 0;
        currentGeneration++;
    }

    public int getCurrentGeneration() {
        return currentGeneration;
    }

    public int getMaximumGeneration() {
        return maximumGeneration;
    }

}
