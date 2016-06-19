package org.kryptonmlt.networkdemonstrator.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 *
 * @author Kurt
 */
public class ConversionUtils {

    private ConversionUtils() {

    }

    public static byte[] toByteArray(double[] doubleArray) {
        int times = Double.SIZE / Byte.SIZE;
        byte[] bytes = new byte[doubleArray.length * times];
        for (int i = 0; i < doubleArray.length; i++) {
            ByteBuffer.wrap(bytes, i * times, times).putDouble(doubleArray[i]);
        }
        return bytes;
    }

    public static double[] toDouble(byte[] byteArray) {
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).getDouble();
        }
        return doubles;
    }

    public static byte[] toByteArray(int[] intArray) {
        int times = Integer.SIZE / Byte.SIZE;
        byte[] bytes = new byte[intArray.length * times];
        for (int i = 0; i < intArray.length; i++) {
            ByteBuffer.wrap(bytes, i * times, times).putInt(intArray[i]);
        }
        return bytes;
    }

    public static int[] toInt(byte[] byteArray) {
        int times = Integer.SIZE / Byte.SIZE;
        int[] ints = new int[byteArray.length / times];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = ByteBuffer.wrap(byteArray, i * times, times).getInt();
        }
        return ints;
    }

    public static int btoi(byte[] byteArray) {
        return ByteBuffer.wrap(byteArray, 0, 4).getInt();
    }

    public static long batol(byte[] buff) {
        return batol(buff, false);
    }

    public static long batol(byte[] buff, boolean littleEndian) {
        assert (buff.length == 8);
        ByteBuffer bb = ByteBuffer.wrap(buff);
        if (littleEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        return bb.getLong();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static short btos(byte b) {
        return (short) b;
    }

    public static byte stob(short s) {
        return (byte) (s & 0xff);
        //return (byte)((x >> 8) & 0xff);
    }

    public static double[] convert2DListToArray(List<double[]> list) {
        double[] arr = new double[list.size() * 2];
        for (int i = 0; i < list.size(); i++) {
            arr[i * 2] = list.get(i)[0];
            arr[(i * 2) + 1] = list.get(i)[1];
        }
        return arr;
    }
}
