package com.esri.arcgis.android.samples.helloworld;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import com.esri.android.map.LocationService;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

public class LocateCurrentLocation {
	LocationService locationService;
	Geometry resultLocGeom;
	SimpleMarkerSymbol resultSymbol;
	Graphic resultLocation;
	boolean zoomToMe;
	Point mLocation = null;
	final SpatialReference wm = SpatialReference.create(102100);
	final SpatialReference egs = SpatialReference.create(4326);
	MapView mapView;

	LocateCurrentLocation(MapView map){
		mapView = map;
		locationService();
	}

	private void locationService(){
		locationService = mapView.getLocationService();
		locationService.setLocationListener(new MyLocationListener());
		locationService.start();
		locationService.setAutoPan(false);
	}

	private class MyLocationListener implements LocationListener {
		HelloWorldActivity mainActivity;

		public MyLocationListener() {
			super();
		}

		public void onLocationChanged(Location loc) {
			if (loc == null)
				return;
			zoomToMe = (mLocation == null) ? true : false;
			mLocation = new Point(loc.getLongitude(), loc.getLatitude());
			if (zoomToMe) {
				Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
				mapView.zoomToResolution(p, 20.0);		
			}
		}

		public void onProviderDisabled(String provider) {
			Toast.makeText(mainActivity, "GPS Disabled",
					Toast.LENGTH_SHORT).show();
		}

		public void onProviderEnabled(String provider) {
			Toast.makeText(mainActivity, "GPS Enabled",
					Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

	}
}
