package com.example.drukmap;

import android.os.health.PackageHealthStats;

import com.carrotsearch.hppc.predicates.FloatCharPredicate;
import com.example.drukmap.turf.TurfConstants;
import com.example.drukmap.turf.TurfMeasurement;
import com.example.drukmap.turf.TurfMisc;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

import org.osmdroid.util.GeoPoint;

import java.nio.file.Path;

public class RouteProcessor {
    PathRouteProgress route;
    double stepDistanceRemaining;

    PathRouteProgress buildNewRouteProgress(GeoPoint currentLocation, PathRouteProgress previousroute ){
        this.route = previousroute;
        int currentIndex = previousroute.getLegIndex();
        stepDistanceRemaining  = calculateDistanceRemaining(currentLocation,previousroute);
        double maneuverZone = 40;
        checkManeuverCompletion(maneuverZone);
        route.setStepDistanceRemaining(stepDistanceRemaining);
        return route;
    }

    void checkManeuverCompletion(double maneuverZone){
        boolean withinradius = stepDistanceRemaining < maneuverZone;
        if(withinradius){
            route.incrementIndex();
        }
    }


    static double calculateDistanceRemaining(GeoPoint location , PathRouteProgress path){
        int legIndex = path.getLegIndex();
        GeoPoint snappedPosition = snappedPosition(location , path.getPath().getPoints());
        return stepDistanceRemaining(snappedPosition,legIndex,path);
    }

    static double stepDistanceRemaining(GeoPoint snappedPostition, int legIndex, PathRouteProgress route){
        PointList pts = route.getPath().getInstructions().get(legIndex).getPoints();
        GeoPoint nextturnpoint = nextTurnPoint(legIndex,route.getPath().getInstructions(), pts);

        if(snappedPostition.equals(nextturnpoint) || pts.getSize() < 2){
            return 0;
        }
        return TurfMeasurement.distance(snappedPostition,nextturnpoint, TurfConstants.UNIT_METERS);
    }

    static GeoPoint nextTurnPoint (int legIndex, InstructionList instructions, PointList coords){
        //return last point of the point list although this is not the real last point
        if(instructions.size() > (legIndex + 1)){
            PointList pt = instructions.get(legIndex + 1).getPoints();
            return new GeoPoint(pt.getLatitude(0),pt.getLongitude(0));
        }
        PointList pt = instructions.get(legIndex).getPoints();
        return new GeoPoint(pt.getLatitude(pt.size()-1),pt.getLongitude(pt.size()-1));
    }

    static GeoPoint snappedPosition(GeoPoint location, PointList coordinates){
       if(coordinates.getSize() < 2){
           return location;
       }

       GeoPoint feature = TurfMisc.nearestPointOnLine(location,coordinates);
       return feature;
    }


}
