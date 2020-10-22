package com.example.drukmap.turf;

import android.icu.util.ULocale;
import android.util.Log;

import com.graphhopper.util.PointList;

import org.osmdroid.util.GeoPoint;

public class TurfMisc {
    public static GeoPoint nearestPointOnLine(GeoPoint pt, PointList pl){
        if(pl.getSize() < 2){
            Log.e("kinley","nearestPointOnLine requires Poinlist with more than one GeoPoints");
        }

        double closestDistance = Double.POSITIVE_INFINITY;
        int closestIndexKey;
        GeoPoint closestPt = pt;
        double startDistance;
        double stopDistance;

        for (int i = 0; i < pl.getSize() - 1; i++) {
            GeoPoint start = new GeoPoint(pl.getLatitude(i),pl.getLongitude(i));
            GeoPoint stop = new GeoPoint(pl.getLatitude(i + 1),pl.getLongitude(i + 1));


            startDistance = TurfMeasurement.distance(pt,start,TurfConstants.UNIT_KILOMETERS);
            stopDistance = TurfMeasurement.distance(pt,stop,TurfConstants.UNIT_KILOMETERS);

            //perpendicular
            double heightDistance = Math.max(startDistance,stopDistance);

            double direction = TurfMeasurement.bearing(start, stop);


            GeoPoint startPerpend = TurfMeasurement.destination(pt,heightDistance,direction + 90,TurfConstants.UNIT_KILOMETERS);
            GeoPoint stopPerpend = TurfMeasurement.destination(pt,heightDistance,direction - 90,TurfConstants.UNIT_KILOMETERS);


            LineIntersectsResult intersect = lineIntersects(
                    startPerpend.getLongitude(),
                    startPerpend.getLatitude(),
                    stopPerpend.getLongitude(),
                    stopPerpend.getLatitude(),
                    start.getLongitude(),
                    start.getLatitude(),
                    stop.getLongitude(),
                    stop.getLatitude()
            );

            GeoPoint intersectPt = null;
            double intersectDistance = 0.0;

            if (intersect != null) {
                intersectPt = new GeoPoint(intersect.horizontalIntersection(),intersect.verticalIntersection());
                intersectDistance = TurfMeasurement.distance(pt,intersectPt,TurfConstants.UNIT_KILOMETERS);
            }

            if(startDistance < closestDistance){
                closestPt = start;
                closestDistance = startDistance;
                closestIndexKey = i;
            }

            if(stopDistance < closestDistance){
                closestPt = stop;
                closestDistance = stopDistance;
                closestIndexKey = i;
            }

            if(intersectPt != null && intersectDistance < closestDistance){
                closestPt = intersectPt;
                closestDistance = intersectDistance;
                closestIndexKey = i;
            }
        }

        return closestPt;

    }

    private static LineIntersectsResult lineIntersects(double line1StartX, double line1StartY,
                                                       double line1EndX, double line1EndY,
                                                       double line2StartX, double line2StartY,
                                                       double line2EndX, double line2EndY) {
        // If the lines intersect, the result contains the x and y of the intersection
        // (treating the lines as infinite) and booleans for whether line segment 1 or line
        // segment 2 contain the point
        LineIntersectsResult result = LineIntersectsResult.builder()
                .onLine1(false)
                .onLine2(false)
                .build();

        double denominator = ((line2EndY - line2StartY) * (line1EndX - line1StartX))
                - ((line2EndX - line2StartX) * (line1EndY - line1StartY));
        if (denominator == 0) {
            if (result.horizontalIntersection() != null && result.verticalIntersection() != null) {
                return result;
            } else {
                return null;
            }
        }
        double varA = line1StartY - line2StartY;
        double varB = line1StartX - line2StartX;
        double numerator1 = ((line2EndX - line2StartX) * varA) - ((line2EndY - line2StartY) * varB);
        double numerator2 = ((line1EndX - line1StartX) * varA) - ((line1EndY - line1StartY) * varB);
        varA = numerator1 / denominator;
        varB = numerator2 / denominator;

        // if we cast these lines infinitely in both directions, they intersect here:
        result = result.toBuilder().horizontalIntersection(line1StartX
                + (varA * (line1EndX - line1StartX))).build();
        result = result.toBuilder().verticalIntersection(line1StartY
                + (varA * (line1EndY - line1StartY))).build();

        // if line1 is a segment and line2 is infinite, they intersect if:
        if (varA > 0 && varA < 1) {
            result = result.toBuilder().onLine1(true).build();
        }
        // if line2 is a segment and line1 is infinite, they intersect if:
        if (varB > 0 && varB < 1) {
            result = result.toBuilder().onLine2(true).build();
        }
        // if line1 and line2 are segments, they intersect if both of the above are true
        if (result.onLine1() && result.onLine2()) {
            return result;
        } else {
            return null;
        }
    }
}
