package xyz.lynxs.terrarium;


public class Util {
    private static final double EARTH_RADIUS = 6378137.0; // Standard Mercator Earth radius (meters)
    private static final double MAX_MERCATOR = EARTH_RADIUS * Math.PI; // ~20,037,508 meters
    /**
     * Convert integer X/Z in a variable-sized grid to latitude/longitude.
     * @param x X coordinate (integer, 0 to maxSize-1)
     * @param z Z coordinate (integer, 0 to maxSize-1)
     * @param maxSize The maximum grid size (e.g., 256, 512, etc.)
     * @return double[] where [0] = longitude, [1] = latitude
     */
    public static double[] gridToLatLon(int x, int z, int maxSize) {
        // Normalize X (longitude is linear)
        double normalizedX = (x / (double) (maxSize - 1)) * 2 - 1; // [-1, 1]
        double lon = normalizedX * 180.0; // Longitude ranges -180 to 180

        // Normalize Z (latitude is nonlinear due to Mercator)
        double normalizedZ = 1.0 - (z / (double) (maxSize - 1)); // Flip Z (0=top, maxSize-1=bottom)
        double mercatorZ = normalizedZ * 2 * MAX_MERCATOR - MAX_MERCATOR; // [-MAX_MERCATOR, MAX_MERCATOR]
        double lat = Math.toDegrees(Math.atan(Math.sinh(mercatorZ / EARTH_RADIUS))); // Inverse Mercator

        return new double[]{lon, lat};
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
            System.arraycopy(source[Math.min(y1 + i, source.length - 1)], x1, result[i], 0, width);
        }
        return result;
    }
}
