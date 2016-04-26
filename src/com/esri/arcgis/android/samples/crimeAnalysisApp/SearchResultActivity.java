package com.esri.arcgis.android.samples.helloworld;

import java.util.List;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.ags.geocode.Locator;
import com.esri.core.tasks.ags.geocode.LocatorFindParameters;
import com.esri.core.tasks.ags.geocode.LocatorGeocodeResult;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.widget.Toast;

public class SearchResultActivity extends ActionBarActivity{
	
	private TextView txtQuery;
	Locator locator;
	// create UI components
	static ProgressDialog dialog;
	static Handler handler;
	Geometry resultLocGeom;
	// create marker symbol to represent location
	SimpleMarkerSymbol resultSymbol;
	// create graphic object for resulting location
	Graphic resultLocation;
	GraphicsLayer locationLayer;
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_activity);
        // get the action bar
        ActionBar actionBar = getActionBar();
        // Enabling Back navigation on Action Bar icon
        actionBar.setDisplayHomeAsUpEnabled(true);
        handleIntent(getIntent());
    }
 
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }
 
    /**
     * Handling intent data
     * Use this query to display search results like 
     * 1. Getting the data from SQLite and showing in listview 
     * 2. Making webrequest and displaying the data 
     * For now we just display the query only
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //mapView = (MapView)findViewById(R.id.map);
            new GeocoderTask(query);
        }
 
    }
    
    static public class MyRunnable implements Runnable {
		public void run() {
			dialog.dismiss();
		}
	}
    
    /*
     * AsyncTask to geocode an address to a point location Draw resulting point
     * location on the map with matching address
     */
    private class GeocoderTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {

    	String query;

    	GeocoderTask(String aQuery){
    		query= aQuery;
    		locationLayer = new GraphicsLayer();
    		HelloWorldActivity.mapView.addLayer(locationLayer);
    	}

    	// invoke background thread to perform geocode task
    	@Override
    	protected List<LocatorGeocodeResult> doInBackground(
    			LocatorFindParameters... params) {
    		// create results object and set to null
    		List<LocatorGeocodeResult> results = null;
    		// set the geocode service
    		locator = new Locator(query);
    		try {
    			// pass address to find method to return point representing address
    			results = locator.find(params[0]);
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
    			Toast toast = Toast.makeText(SearchResultActivity.this,
    					"No result found.", Toast.LENGTH_LONG);
    			toast.show();
    		} else{
    			// show progress dialog box while geocoding address
    			dialog = ProgressDialog.show(HelloWorldActivity.mapView.getContext(), "Geocoder",
    					"Searching for address ...");
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
    	}
    }

}
