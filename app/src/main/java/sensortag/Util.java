package sensortag;

import java.util.List;

/**
 * Class with helper functions.
 */
public class Util {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Returns a String containing the hexadecimal representation of the given byte array.
     *
     * @param bytes the byte array to convert
     * @return String containing the hexadecimal representation
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts the raw accelerometer data into G units.
     *
     * @param value the raw sensor data
     * @return an array with the G unit values for the x, y and z axes
     */
    public static double[] convertAccel(byte[] value) {
        // ±8 G range
        final float SCALE = 65536f / 500f;

        int x = ((int) (value[1]) << 8) + (int) (value[0] & 0xff);
        int y = ((int) (value[3]) << 8) + (int) (value[2] & 0xff);
        int z = ((int) (value[5]) << 8) + (int) (value[4] & 0xff);
        return new double[]{((x / SCALE) * -1), y / SCALE, ((z / SCALE) * -1)};
    }
}
