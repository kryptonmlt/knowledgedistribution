package org.kryptonmlt.networkdemonstrator.sensors;

import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 *
 * @author Kurt
 */
public class SensorManager {

    private DataLoader dataLoader = null;
    private int counter = 0;

    public SensorManager(XSSFSheet sheet, int startCol, int numberOfFeatures, String filename) {
        dataLoader = new DataLoader(sheet, startCol, numberOfFeatures, filename);
    }

    public double[] requestData() {
        double[] result = dataLoader.getFeaturesInstance(counter).clone();
        counter++;
        return result;
    }

    public boolean isReadyForRead() {
        return counter < dataLoader.getNumberOfSamplesLoaded();
    }
}
