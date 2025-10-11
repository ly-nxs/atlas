package xyz.lynxs.terrarium;


import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class Util {
    private static final double EARTH_RADIUS = 6378137.0; // Standard Mercator Earth radius (meters)
    private static final double MAX_MERCATOR = EARTH_RADIUS * Math.PI;
    private static final double MAX_LAT = 85.05112878; // Web Mercator maximum latitude


    private static final float[] blurKernel = {
            1/9f, 1/9f, 1/9f,
            1/9f, 1/9f, 1/9f,
            1/9f, 1/9f, 1/9f
    };
    private static final Kernel kernel = new Kernel(3, 3, blurKernel); // 3x3 kernel
    private static final ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);


    /**
     * Converts latitude to grid Z coordinate (Web Mercator)
     * @param latitude - Latitude in degrees (-85.051129 to 85.051129)
     * @param maxSize - Grid height (e.g., 256, 512)
     * @return Z coordinate (0 to maxSize-1)
     */
    public static int latToZ(double latitude, int maxSize) {
        // Clamp latitude to valid Web Mercator range
        latitude = Math.max(-MAX_LAT, Math.min(MAX_LAT, latitude));

        // Convert to radians
        double latRad = Math.toRadians(latitude);

        // Web Mercator projection formula
        double mercatorY = Math.log(Math.tan(Math.PI/4 + latRad/2));

        // Normalize to [0,1] range and flip Y to Z (top-to-bottom)
        double normalized = (1.0 - (mercatorY / Math.PI)) / 2.0;

        // Convert to grid coordinate with proper rounding
        int z = (int) Math.floor(normalized * maxSize);

        // Clamp to valid range
        return Math.max(0, Math.min(maxSize - 1, z));
    }
    /**
     * Converts longitude to grid X coordinate (Web Mercator)
     * @param longitude - Longitude in degrees (-180 to 180)
     * @param maxSize - Grid width (e.g., 256, 512)
     * @return X coordinate (0 to maxSize-1)
     */
    public static int lonToX(double longitude, int maxSize) {
        // Normalize longitude to [0,1] range
        double normalized = (longitude + 180.0) / 360.0;

        // Convert to grid coordinate with proper rounding
        int x = (int) Math.floor(normalized * maxSize);

        // Clamp to valid range
        return Math.max(0, Math.min(maxSize - 1, x));
    }

    /**
     * Converts grid X coordinate to longitude (Web Mercator).
     * This function reverses the logic of lonToX.
     *
     * @param x       - X coordinate (0 to maxSize-1)
     * @param maxSize - Grid width (e.g., 256, 512)
     * @return Longitude in degrees (-180 to 180)
     */
    public static float xToLon(int x, int maxSize) {
        // To get the center of the grid cell, we add 0.5 to the coordinate.
        float normalized = (x + 0.5f) / maxSize;

        // Reverse the original normalization formula: (longitude + 180.0) / 360.0

        return (normalized * 360.0f) - 180.0f;
    }

    /**
     * Converts grid Z coordinate to latitude (Web Mercator).
     * This function reverses the logic of latToZ.
     *
     * @param z       - Z coordinate (0 to maxSize-1)
     * @param maxSize - Grid height (e.g., 256, 512)
     * @return Latitude in degrees (-85.051129 to 85.051129)
     */
    public static float zToLat(int z, int maxSize) {
        // To get the center of the grid cell, we add 0.5 to the coordinate.
        double normalized = (z + 0.5) / maxSize;

        // Reverse the normalization: (1.0 - (mercatorY / Math.PI)) / 2.0
        double mercatorY = (1.0 - (normalized * 2.0)) * Math.PI;

        // Reverse the Mercator projection formula: mercatorY = Math.log(Math.tan(Math.PI/4 + latRad/2));
        // This is equivalent to: latRad = 2 * (atan(exp(mercatorY)) - PI/4)
        double latRad = 2 * (Math.atan(Math.exp(mercatorY)) - Math.PI / 4);

        // Convert radians back to degrees

        return (float) Math.toDegrees(latRad);
    }

    public static BufferedImage blur(BufferedImage bi) {
        return op.filter(bi, null);
    }


    public static double truncate(double num, int places){
        return  (int)(num * Math.pow(10, places)) / Math.pow(10, places); // truncatedNumber will be 10.78
    }


    public static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | ((long) z & 0xFFFFFFFFL) << 32;
    }
    public static short[][] getSubArraySystemCopy(short[][] source, int x1, int y1, int x2, int y2) {
        int width = x2 - x1;
        int height = y2 - y1;
        short[][] result = new short[height][width];

        for (int i = 0; i < height; i++) {
            System.arraycopy(source[y1 + i], x1, result[i], 0, width);
        }
        return result;
    }
}
