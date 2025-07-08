package xyz.lynxs.terrarium;


public class Util {
    private static final double EARTH_RADIUS = 6378137.0; // Standard Mercator Earth radius (meters)
    private static final double MAX_MERCATOR = EARTH_RADIUS * Math.PI;
    private static final double MAX_LAT = 85.05112878; // Web Mercator maximum latitude

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
            System.arraycopy(source[y1 + i], x1, result[i], 0, width);
        }
        return result;
    }
}
