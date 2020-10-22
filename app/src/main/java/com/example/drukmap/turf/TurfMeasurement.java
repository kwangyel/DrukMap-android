package com.example.drukmap.turf;

import static com.example.drukmap.turf.TurfConversion.degreesToRadians;
import static com.example.drukmap.turf.TurfConversion.radiansToDegrees;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.graphhopper.util.PointList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class contains an assortment of methods used to calculate measurements such as bearing,
 * destination, midpoint, etc.
 *
 * @see <a href="http://turfjs.org/docs/">Turf documentation</a>
 * @since 1.2.0
 */
public final class TurfMeasurement {

    private TurfMeasurement() {
        throw new AssertionError("No Instances.");
    }

    /**
     * Earth's radius in meters.
     */

    public static double bearing(@NonNull GeoPoint point1, @NonNull GeoPoint point2) {

        double lon1 = degreesToRadians(point1.getLatitude());
        double lon2 = degreesToRadians(point2.getLongitude());
        double lat1 = degreesToRadians(point1.getLatitude());
        double lat2 = degreesToRadians(point2.getLongitude());
        double value1 = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double value2 = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(lon2 - lon1);

        return radiansToDegrees(Math.atan2(value1, value2));
    }

    @NonNull
    public static GeoPoint destination(@NonNull GeoPoint point, @FloatRange(from = 0) double distance,
                                    @FloatRange(from = -180, to = 180) double bearing,
                                    @NonNull @TurfConstants.TurfUnitCriteria String units) {

        double longitude1 = degreesToRadians(point.getLongitude());
        double latitude1 = degreesToRadians(point.getLatitude());
        double bearingRad = degreesToRadians(bearing);

        double radians = TurfConversion.lengthToRadians(distance, units);

        double latitude2 = Math.asin(Math.sin(latitude1) * Math.cos(radians)
                + Math.cos(latitude1) * Math.sin(radians) * Math.cos(bearingRad));
        double longitude2 = longitude1 + Math.atan2(Math.sin(bearingRad)
                        * Math.sin(radians) * Math.cos(latitude1),
                Math.cos(radians) - Math.sin(latitude1) * Math.sin(latitude2));

        GeoPoint p = new GeoPoint (radiansToDegrees(latitude2), radiansToDegrees(longitude2));
        return p;
    }

    public static double distance(@NonNull GeoPoint point1, @NonNull GeoPoint point2,
                                  @NonNull @TurfConstants.TurfUnitCriteria String units) {
        double difLat = degreesToRadians((point2.getLatitude() - point1.getLatitude()));
        double difLon = degreesToRadians((point2.getLongitude() - point1.getLongitude()));
        double lat1 = degreesToRadians(point1.getLatitude());
        double lat2 = degreesToRadians(point2.getLatitude());

        double value = Math.pow(Math.sin(difLat / 2), 2)
                + Math.pow(Math.sin(difLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);

        return TurfConversion.radiansToLength(
                2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value)), units);
    }



    public static GeoPoint along(@NonNull PointList coords, @FloatRange(from = 0) double distance,
                                 @NonNull @TurfConstants.TurfUnitCriteria String units) {

        double travelled = 0;
        for (int i = 0; i < coords.size(); i++) {
            if (distance >= travelled && i == coords.size() - 1) {
                break;
            } else if (travelled >= distance) {
                double overshot = distance - travelled;
                if (overshot == 0) {
                    return new GeoPoint(coords.getLatitude(i),coords.getLongitude(i));
                } else {
                    GeoPoint pt1 = new GeoPoint(coords.getLatitude(i),coords.getLongitude(i));
                    GeoPoint pt2 = new GeoPoint(coords.getLatitude(i - 1),coords.getLongitude(i - 1));
                    double direction = bearing(pt1, pt2) - 180;
                    return destination(new GeoPoint(coords.getLatitude(i),coords.getLongitude(i)), overshot, direction, units);
                }
            } else {
                GeoPoint pt1 = new GeoPoint(coords.getLatitude(i),coords.getLongitude(i));
                GeoPoint pt2 = new GeoPoint(coords.getLatitude(i + 1),coords.getLongitude(i + 1));
                travelled += distance(pt1, pt2, units);
            }
        }

        return new GeoPoint(coords.getLatitude(coords.size() - 1),coords.getLongitude(coords.size() - 1));
    }







}

