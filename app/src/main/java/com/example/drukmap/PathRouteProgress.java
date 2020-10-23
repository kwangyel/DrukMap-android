package com.example.drukmap;

import com.graphhopper.ResponsePath;
import com.graphhopper.util.Instruction;

import org.osmdroid.util.GeoPoint;

public class PathRouteProgress {
   private ResponsePath path;
   private double stepDistanceRemaining;
   private int legIndex = 0;
   private GeoPoint currentSnappedLocation;


   public PathRouteProgress(ResponsePath p, double d){
      this.path = p;
      this.stepDistanceRemaining= d;
   }

   public void setStepDistanceRemaining(double distanceRemaining) {
      this.stepDistanceRemaining= distanceRemaining;
   }

   public double getStepDistanceRemaining() {
      return stepDistanceRemaining;
   }

   public void setPath(ResponsePath path) {
      this.path = path;
   }

   public ResponsePath getPath() {
      return path;
   }

   public void setLegIndex(int legIndex) {
      this.legIndex = legIndex;
   }

   public int getLegIndex() {
      return legIndex;
   }

   public Instruction getCurrentLeg (){
      return this.path.getInstructions().get(legIndex);
   }

   public void incrementIndex(){
      int lastIndex = this.path.getInstructions().size() - 1;
      if(legIndex != lastIndex){
         legIndex ++;
      }
   }
   public int getLegs(){
      return this.path.getInstructions().size();
   }

   public void setCurrentSnappedLocation(GeoPoint currentSnappedLocation) {
      this.currentSnappedLocation = currentSnappedLocation;
   }

   public GeoPoint getCurrentSnappedLocation() {
      return currentSnappedLocation;
   }
}
