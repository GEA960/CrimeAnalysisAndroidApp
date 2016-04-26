package com.esri.arcgis.android.samples.helloworld;

import java.util.List;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.ags.geocode.Locator;
import com.esri.core.tasks.ags.geocode.LocatorFindParameters;
import com.esri.core.tasks.ags.geocode.LocatorGeocodeResult;


class MyRunnable implements Runnable {
	public void run() {
		GeocoderTask.dialog.dismiss();
	}
}

/*
 * AsyncTask to geocode an address to a point location Draw resulting point
 * location on the map with matching address
 */
public class GeocoderTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {

	String query;
	Locator locator;
	// create UI components
	static ProgressDialog dialog;
	Geometry resultLocGeom;
	// create marker symbol to represent location
	SimpleMarkerSymbol resultSymbol;
	// create graphic object for resulting location
	Graphic resultLocation;
	HelloWorldActivity helloWorldActivity;
	GraphicsLayer locationLayer;
	// create handler to update the UI
	static Handler handler = new Handler();
	
	GeocoderTask(HelloWorldActivity mainActivity, GraphicsLayer layer){
		locationLayer = layer;
		// remove any previous graphics
		locationLayer.removeAll();
		helloWorldActivity = mainActivity;
		// create handler to update the UI
		Log.v("debug geocoder","got here");
	}

	// invoke background thread to perform geocode task
	@Override
	protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... params) {
		// create results object and set to null
		List<LocatorGeocodeResult> results = null;	
		// set the geocode service
		locator = new Locator(helloWorldActivity.getResources().getString(R.string.geocode_url));
		try {
			// pass address to find method to return point representing address
			results = locator.find(params[0]);
			Log.v("debug results length",results.size()+"");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// return the resulting point(s)
		return results;			
	}

	// The result of geocode task is passed as a parameter to map the results
	protected void onPostExecute(List<LocatorGeocodeResult> result) {
		if (result == null || result.size() == 0) {
			// update UI with notice that no results were found
			Toast toast = Toast.makeText(helloWorldActivity,
					"No result found.", Toast.LENGTH_LONG);
			toast.show();
		} else{
			// show progress dialog box while geocoding address
			dialog = ProgressDialog.show(HelloWorldActivity.mapView.getContext(), "Geocoder",
					"Searching for address ...");
			Log.v("debug post execute",result.get(0).getLocation().getX()+"");
			showLocationPointer(result.get(0).getLocation());				
			showLocationText(result.get(0).getAddress());
			// zoom to geocode result
			HelloWorldActivity.mapView.zoomToResolution(result.get(0).getLocation(), 2);
			// create a runnable to be added to message queue
			handler.post(new MyRunnable());				
		}
	}
	

	private void showLocationPointer(Point p){
		// get return geometry from geocode result
		resultLocGeom = p;
		// create marker symbol to represent location
		resultSymbol = new SimpleMarkerSymbol(Color.BLUE, 20, SimpleMarkerSymbol.STYLE.CIRCLE);
		// create graphic object for resulting location
		resultLocation = new Graphic(resultLocGeom, resultSymbol);
		// add graphic to location layer
		locationLayer.addGraphic(resultLocation);	
		Log.v("debug geocoder","got here add pointer");
	}

	private void showLocationText(String address){
		// create text symbol for return address
		TextSymbol resultAddress = new TextSymbol(12, address, Color.BLACK);
		// create offset for text
		resultAddress.setOffsetX(10);
		resultAddress.setOffsetY(30);
		// create a graphic object for address text
		Graphic resultText = new Graphic(resultLocGeom, resultAddress);
		// add address text graphic to location graphics layer
		locationLayer.addGraphic(resultText);
		Log.v("debug geocoder","got here add text");
	}
}

