package org.kryptonmlt.networkdemonstrator.utils;

/**
 *
 * @author Kurt
 */
public class MessageUtils {

    private MessageUtils() {

    }

    public static byte[] constructKnowledgeMessage(long id, double[] data, double[] centroids) {
        byte requestToSend = ConversionUtils.stob((short) 2);
        byte[] idToSend = ConversionUtils.longToBytes(id);
        byte[] dataToSend = ConversionUtils.toByteArray(data);
        byte[] centroidsToSend = ConversionUtils.toByteArray(centroids);
        byte[] buffer = new byte[1 + idToSend.length + dataToSend.length + centroidsToSend.length];
        buffer[0] = requestToSend;
        System.arraycopy(idToSend, 0, buffer, 1, Long.BYTES);
        System.arraycopy(dataToSend, 0, buffer, 1 + Long.BYTES, dataToSend.length);
        System.arraycopy(centroidsToSend, 0, buffer, 1 + Long.BYTES + dataToSend.length, centroidsToSend.length);
        return buffer;
    }

    public static byte[] constructFeaturesMessage(long id, double[] data) {
        byte requestToSend = ConversionUtils.stob((short) 1);
        byte[] idToSend = ConversionUtils.longToBytes(id);
        byte[] dataToSend = ConversionUtils.toByteArray(data);
        byte[] buffer = new byte[1 + idToSend.length + dataToSend.length];
        buffer[0] = requestToSend;
        System.arraycopy(idToSend, 0, buffer, 1, Long.BYTES);
        System.arraycopy(dataToSend, 0, buffer, Long.BYTES + 1, dataToSend.length);
        return buffer;
    }

    public static byte[] constructRegistrationMessage(int data) {
        byte requestToSend = ConversionUtils.stob((short) 0);
        int[] toConvert = {data};
        byte[] dataToSend = ConversionUtils.toByteArray(toConvert);
        byte[] buffer = new byte[1 + dataToSend.length];
        buffer[0] = requestToSend;
        System.arraycopy(dataToSend, 0, buffer, 1, dataToSend.length);
        return buffer;
    }
}
