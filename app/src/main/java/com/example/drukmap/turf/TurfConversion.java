package com.example.drukmap.turf;


import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.example.drukmap.turf.TurfConstants.TurfUnitCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is made up of methods that take in an object, convert it, and then return the object
 * in the desired units or object.
 *
 * @see <a href="http://turfjs.org/docs/">Turfjs documentation</a>
 * @since 1.2.0
 */
public final class TurfConversion {

    private static final Map<String, Double> FACTORS;

    static {
        FACTORS = new HashMap<>();
        FACTORS.put(TurfConstants.UNIT_MILES, 3960d);
        FACTORS.put(TurfConstants.UNIT_NAUTICAL_MILES, 3441.145d);
        FACTORS.put(TurfConstants.UNIT_DEGREES, 57.2957795d);
        FACTORS.put(TurfConstants.UNIT_RADIANS, 1d);
        FACTORS.put(TurfConstants.UNIT_INCHES, 250905600d);
        FACTORS.put(TurfConstants.UNIT_YARDS, 6969600d);
        FACTORS.put(TurfConstants.UNIT_METERS, 6373000d);
        FACTORS.put(TurfConstants.UNIT_CENTIMETERS, 6.373e+8d);
        FACTORS.put(TurfConstants.UNIT_KILOMETERS, 6373d);
        FACTORS.put(TurfConstants.UNIT_FEET, 20908792.65d);
        FACTORS.put(TurfConstants.UNIT_CENTIMETRES, 6.373e+8d);
        FACTORS.put(TurfConstants.UNIT_METRES, 6373000d);
        FACTORS.put(TurfConstants.UNIT_KILOMETRES, 6373d);
    }

    private TurfConversion() {
        // Private constructor preventing initialization of this class
    }

    /**
     * Convert a distance measurement (assuming a spherical Earth) from a real-world unit into degrees
     * Valid units: miles, nauticalmiles, inches, yards, meters, metres, centimeters, kilometres,
     * feet.
     *
     * @param distance in real units
     * @param units    can be degrees, radians, miles, or kilometers inches, yards, metres, meters,
     *                 kilometres, kilometers.
     * @return a double value representing the distance in degrees
     * @since 3.0.0
     */
    public static double lengthToDegrees(double distance, @TurfUnitCriteria String units) {
        return radiansToDegrees(lengthToRadians(distance, units));
    }

    /**
     * Converts an angle in degrees to radians.
     *
     * @param degrees angle between 0 and 360 degrees
     * @return angle in radians
     * @since 3.1.0
     */
    public static double degreesToRadians(double degrees) {
        double radians = degrees % 360;
        return radians * Math.PI / 180;
    }

    /**
     * Converts an angle in radians to degrees.
     *
     * @param radians angle in radians
     * @return degrees between 0 and 360 degrees
     * @since 3.0.0
     */
    public static double radiansToDegrees(double radians) {
        double degrees = radians % (2 * Math.PI);
        return degrees * 180 / Math.PI;
    }

    /**
     * Convert a distance measurement (assuming a spherical Earth) from radians to a more friendly
     * unit. The units used here equals the default.
     *
     * @param radians a double using unit radian
     * @return converted radian to distance value
     * @since 1.2.0
     */
    public static double radiansToLength(double radians) {
        return radiansToLength(radians, TurfConstants.UNIT_DEFAULT);
    }

    /**
     * Convert a distance measurement (assuming a spherical Earth) from radians to a more friendly
     * unit.
     *
     * @param radians a double using unit radian
     * @param units   pass in one of the units defined in {@link TurfUnitCriteria}
     * @return converted radian to distance value
     * @since 1.2.0
     */
    public static double radiansToLength(double radians, @NonNull @TurfUnitCriteria String units) {
        return radians * FACTORS.get(units);
    }

    /**
     * Convert a distance measurement (assuming a spherical Earth) from a real-world unit into
     * radians.
     *
     * @param distance double representing a distance value assuming the distance units is in
     *                 kilometers
     * @return converted distance to radians value
     * @since 1.2.0
     */
    public static double lengthToRadians(double distance) {
        return lengthToRadians(distance, TurfConstants.UNIT_DEFAULT);
    }

    /**
     * Convert a distance measurement (assuming a spherical Earth) from a real-world unit into
     * radians.
     *
     * @param distance double representing a distance value
     * @param units    pass in one of the units defined in {@link TurfUnitCriteria}
     * @return converted distance to radians value
     * @since 1.2.0
     */
    public static double lengthToRadians(double distance, @NonNull @TurfUnitCriteria String units) {
        return distance / FACTORS.get(units);
    }

    /**
     * Converts a distance to the default units. Use
     * {@link TurfConversion#convertLength(double, String, String)} to specify a unit to convert to.
     *
     * @param distance     double representing a distance value
     * @param originalUnit of the distance, must be one of the units defined in
     *                     {@link TurfUnitCriteria}
     * @return converted distance in the default unit
     * @since 2.2.0
     */
    public static double convertLength(@FloatRange(from = 0) double distance,
                                       @NonNull @TurfUnitCriteria String originalUnit) {
        return convertLength(distance, originalUnit, TurfConstants.UNIT_DEFAULT);
    }

    /**
     * Converts a distance to a different unit specified.
     *
     * @param distance     the distance to be converted
     * @param originalUnit of the distance, must be one of the units defined in
     *                     {@link TurfUnitCriteria}
     * @param finalUnit    returned unit, {@link TurfConstants#UNIT_DEFAULT} if not specified
     * @return the converted distance
     * @since 2.2.0
     */
    public static double convertLength(@FloatRange(from = 0) double distance,
                                       @NonNull @TurfUnitCriteria String originalUnit,
                                       @Nullable @TurfUnitCriteria String finalUnit) {
        if (finalUnit == null) {
            finalUnit = TurfConstants.UNIT_DEFAULT;
        }
        return radiansToLength(lengthToRadians(distance, originalUnit), finalUnit);
    }






    /**
     * Takes a {@link Polygon} and
     * covert it to a {@link Feature} that contains {@link LineString} or {@link MultiLineString}.
     *
     * @param polygon a {@link Polygon} object
     * @return  a {@link Feature} object that contains {@link LineString} or {@link MultiLineString}
     * @since 4.9.0
     */
    public static Feature polygonToLine(@NonNull Polygon polygon) {
        return polygonToLine(polygon, null);
    }

    /**
     * Takes a {@link MultiPolygon} and
     * covert it to a {@link FeatureCollection} that contains list
     * of {@link Feature} of {@link LineString} or {@link MultiLineString}.
     *
     * @param multiPolygon a {@link MultiPolygon} object
     * @return  a {@link FeatureCollection} object that contains
     *   list of {@link Feature} of {@link LineString} or {@link MultiLineString}
     * @since 4.9.0
     */
    public static FeatureCollection polygonToLine(@NonNull MultiPolygon multiPolygon) {
        return polygonToLine(multiPolygon, null);
    }

    /**
     * Takes a {@link Polygon} and a properties {@link JsonObject} and
     * covert it to a {@link Feature} that contains {@link LineString} or {@link MultiLineString}.
     *
     * @param polygon a {@link Polygon} object
     * @param properties a {@link JsonObject} that represents a feature's properties
     * @return  a {@link Feature} object that contains {@link LineString} or {@link MultiLineString}
     * @since 4.9.0
     */
    public static Feature polygonToLine(@NonNull Polygon polygon, @Nullable JsonObject properties) {
        return coordsToLine(polygon.coordinates(), properties);
    }

    /**
     * Takes a {@link MultiPolygon} and a properties {@link JsonObject} and
     * covert it to a {@link FeatureCollection} that contains list
     * of {@link Feature} of {@link LineString} or {@link MultiLineString}.
     *
     * @param multiPolygon a {@link MultiPolygon} object
     * @param properties a {@link JsonObject} that represents a feature's properties
     * @return  a {@link FeatureCollection} object that contains
     *   list of {@link Feature} of {@link LineString} or {@link MultiLineString}
     * @since 4.9.0
     */
    public static FeatureCollection polygonToLine(@NonNull MultiPolygon multiPolygon,
                                                  @Nullable JsonObject properties) {
        List<List<List<Point>>> coordinates = multiPolygon.coordinates();
        List<Feature> finalFeatureList = new ArrayList<>();
        for (List<List<Point>> polygonCoordinates : coordinates) {
            finalFeatureList.add(coordsToLine(polygonCoordinates, properties));
        }
        return FeatureCollection.fromFeatures(finalFeatureList);
    }



    @Nullable
    private static Feature coordsToLine(@NonNull List<List<Point>> coordinates,
                                        @Nullable JsonObject properties) {
        if (coordinates.size() > 1) {
            return Feature.fromGeometry(MultiLineString.fromLngLats(coordinates), properties);
        } else if (coordinates.size() == 1) {
            LineString lineString = LineString.fromLngLats(coordinates.get(0));
            return Feature.fromGeometry(lineString, properties);
        }
        return null;
    }


}

