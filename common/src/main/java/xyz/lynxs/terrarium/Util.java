package xyz.lynxs.terrarium;


import xyz.lynxs.terrarium.gen.EcoregionResult;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.sql.*;

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

    private static final double WEB_MERCATOR_HALF_EXTENT = 20037508.34;

    /**
     * Converts pixel coordinates (x, z) from a 2D grid at a given zoom level
     * into meters in the EPSG:3857 coordinate system (Web Mercator).
     * * The grid (0, 0) is assumed to be the top-left corner.
     * * @param xPixel The horizontal pixel coordinate (0 to MapSize - 1).
     * @param zPixel The vertical pixel coordinate (0 to MapSize - 1), where Z increases downward.
     * @param zoomLevel The zoom level (Z).
     * @return A double array [X_3857, Y_3857] in meters.
     */
    public static double[] toWebMercator(int xPixel, int zPixel, int zoomLevel) {

        // 1. Calculate the total map size in pixels for the given zoom level.
        // The base tile size is 256. MapSize = 256 * 2^Z
        long mapSizePixels = 256L << zoomLevel;

        // 2. Calculate the resolution (meters per pixel).
        // Total Web Mercator width is 2 * L.
        double totalMapWidthMeters = 2 * WEB_MERCATOR_HALF_EXTENT;
        double resolution = totalMapWidthMeters / mapSizePixels;

        // 3. Calculate X_3857 coordinate (Horizontal)
        // X starts at -L and increases as xPixel increases.
        double x3857 = (xPixel * resolution) - WEB_MERCATOR_HALF_EXTENT;

        // 4. Calculate Y_3857 coordinate (Vertical)
        // Y starts at +L and decreases as zPixel increases (inverting the axis).
        double y3857 = WEB_MERCATOR_HALF_EXTENT - (zPixel * resolution);

        return new double[]{x3857, y3857};
    }

    /**
     * Executes a spatial query against a GeoPackage/SpatiaLite database to find
     * the ecoregion containing the specified point (X, Y in EPSG:3857).
     *
     * @param conn The active JDBC connection to the SpatiaLite database.
     * @param x The X coordinate (longitude equivalent) in EPSG:3857 meters.
     * @param z The Y coordinate (latitude equivalent) in EPSG:3857 meters.
     * @return An EcoregionResult object with BIOME_NUM and BIOME_NAME, or null if no region is found.
     */
    public static EcoregionResult findEcoregion(Connection conn, int x, int z, int zoom) {
        EcoregionResult result = null;
        double[] webPoints = toWebMercator(x, z, zoom);
        // 1. Create a WKT point from your coordinates
        String wktPoint = "POINT(" + webPoints[0] + " " + webPoints[1] + ")";

        // 2. The Spatial SQL Query: Selects the two required attributes
        // The 'geom' column is not selected since it is only used in the WHERE clause.
        String sql = "SELECT BIOME_NUM, BIOME_NAME " +
                "FROM reprojected " +
                // ST_Within uses the spatial index for quick candidate filtering
                "WHERE ST_Within(ST_GeomFromText(?, 3857), geom) " +
                "LIMIT 1"; // Stop after the first match

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind the WKT point to the prepared statement
            pstmt.setString(1, wktPoint);

            // Execute the query
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Extract the required attributes and create the result object
                    String biomeNum = rs.getString("BIOME_NUM");
                    String biomeName = rs.getString("BIOME_NAME");
                    result = new EcoregionResult(biomeNum, biomeName);
                }
            }

        } catch (SQLException e) {
            // Use java.sql.SQLException for standard JDBC error handling
            System.err.println("Database query failed: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public static Connection connect(String gpkgPath){
        try {
            // 1. Load the SQLite JDBC driver (if not done automatically)
            Class.forName("org.sqlite.JDBC");

            // 2. Establish the connection to the .gpkg file
            String url = "jdbc:sqlite:" + gpkgPath;
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

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
