package org.kryptonmlt.networkdemonstrator.tools;

/**
 *
 * @author Kurt
 */
public class MessageUtils {

    private MessageUtils() {

    }

    public static byte[] constructMessage(short request, long id, double[] data) {
        byte requestToSend = ConversionUtils.stob(request);
        byte[] idToSend = ConversionUtils.longToBytes(id);
        byte[] dataToSend = ConversionUtils.toByteArray(data);
        byte[] buffer = new byte[1 + idToSend.length + dataToSend.length];
        buffer[0] = requestToSend;
        System.arraycopy(idToSend, 0, buffer, 1, Long.BYTES);
        System.arraycopy(dataToSend, 0, buffer, Long.BYTES + 1, dataToSend.length);
        return buffer;
    }

    public static byte[] constructMessage(short request, int data) {
        byte requestToSend = ConversionUtils.stob(request);
        int[] toConvert = {data};
        byte[] dataToSend = ConversionUtils.toByteArray(toConvert);
        byte[] buffer = new byte[1 + dataToSend.length];
        buffer[0] = requestToSend;
        System.arraycopy(dataToSend, 0, buffer, 1, dataToSend.length);
        return buffer;
    }
}
