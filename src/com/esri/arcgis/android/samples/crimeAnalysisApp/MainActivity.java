package com.esri.arcgis.android.samples.helloworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.STYLE;
import com.esri.core.tasks.ags.geocode.LocatorFindParameters;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;


@SuppressWarnings("deprecation")
public class MainActivity extends ActionBarActivity {

	static ArcGISFeatureLayer featureLayer;
	static MapView mapView ;
	static String featureServiceURL;
	protected Callout m_callout;
	private int m_calloutStyle;
	private ViewGroup calloutContent;
	private Graphic m_identifiedGraphic;
	final Envelope initExtent = new Envelope(-13061847.0123223, 3835572.35035401, -13016380.0023292,
			3923422.33833413);
	static final int ARSON_DIALOG_ID= 1;
	Spinner queryParameters;
	boolean m_isMapLoaded;
	ArcGISTiledMapServiceLayer basemapStreet;
	ArcGISTiledMapServiceLayer basemapTopo;
	ArcGISTiledMapServiceLayer basemapNatGeo;
	ArcGISTiledMapServiceLayer basemapOcean;
	GraphicsLayer locationLayer;
	String searchText;
	ArrayList<Integer> mSelectedItems;
	SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(Color.BLUE, 10, STYLE.DIAMOND);
    SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.YELLOW, 3);
    SimpleFillSymbol fillSymbol = new SimpleFillSymbol(Color.argb(100, 0, 225, 255));
    GraphicsLayer routeLayer, hiddenSegmentsLayer;
    // Symbol used to make route segments "invisible"
    SimpleLineSymbol segmentHider = new SimpleLineSymbol(Color.WHITE, 5);
    // Symbol used to highlight route segments
    SimpleLineSymbol segmentShower = new SimpleLineSymbol(Color.RED, 5);
    // Label showing the current direction, time, and length
    Button directionsLabel;
    // List of the directions for the current route (used for the ListActivity)
    ArrayList<String> curDirections = null;
    // Current route, route summary, and gps location
    Route curRoute = null;
    String routeSummary = null;
    Point mLocation = null;
    // Global results variable for calculating route on separate thread
    RouteTask mRouteTask = null;
    RouteResult mResults = null;
    // Progress dialog to show when route is being calculated
    ProgressDialog dialog;
    // Variable to hold server exception to show to user
    Exception mException = null;
    // Handler for processing the results
    final Handler mHandler = new Handler();
    final Runnable mUpdateResults = new Runnable() {
    	public void run() {
    		updateUI();
    	}
    };
    // Spatial references used for projecting points
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);
    // Index of the currently selected route segment (-1 = no selection)
    int selectedSegmentID = -1;
    boolean startRouting = false;
	
	/** Called by the system, as part of destroying an activity due to a configuration change. */
	/*
	@Override
	//public Object onRetainNonConfigurationInstance() {
		return mapView.retainState();
	}*/

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		Log.v("debug","Here");
		handleIntent(getIntent());
		// Retrieve the map and initial extent from XML layout
		mapView = (MapView)findViewById(R.id.map);
		// Add dynamic layer to MapView
		basemapStreet = new ArcGISTiledMapServiceLayer(this.getResources()
				.getString(R.string.WORLD_STREET_MAP));
		basemapTopo = new ArcGISTiledMapServiceLayer(this.getResources()
				.getString(R.string.WORLD_TOPO_MAP));
		basemapNatGeo = new ArcGISTiledMapServiceLayer(this.getResources()
				.getString(R.string.WORLD_NATGEO_MAP));
		basemapOcean = new ArcGISTiledMapServiceLayer(this.getResources()
				.getString(R.string.OCEAN_BASEMAP));
		locationLayer = new GraphicsLayer();
		// Add basemap to MapView
		mapView.addLayer(basemapStreet);
		mapView.addLayer(basemapTopo);
		mapView.addLayer(basemapNatGeo);
		mapView.addLayer(basemapOcean);
		// set visibility
		basemapTopo.setVisible(false);
		basemapNatGeo.setVisible(false);
		basemapOcean.setVisible(false);
		// Get the feature service URL from values->strings.xml
        featureServiceURL = this.getResources().getString(R.string.featureServiceURL);
        // Add Feature layer to the MapView
        featureLayer = new ArcGISFeatureLayer(featureServiceURL, ArcGISFeatureLayer.MODE.SNAPSHOT);
        mapView.addLayer(featureLayer);
        mapView.addLayer(locationLayer);
        
        // Add the route graphic layer (shows the full route)
        routeLayer = new GraphicsLayer();
        mapView.addLayer(routeLayer);
        // Initialize the RouteTask
        try {
          mRouteTask = RouteTask.createOnlineRouteTask(
              "http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World", null);
        } catch (Exception e1) {
          e1.printStackTrace();
        }
        // Add the hidden segments layer (for highlighting route segments)
        hiddenSegmentsLayer = new GraphicsLayer();
        mapView.addLayer(hiddenSegmentsLayer);
        // Make the segmentHider symbol "invisible"
        segmentHider.setAlpha(1);
        // Get the location service and start reading location. Don't auto-pan to center our position
        new LocateCurrentLocation(mapView);
        // Set the directionsLabel with initial instructions.
        directionsLabel = (Button) findViewById(R.id.directionsLabel);
        directionsLabel.setText(getString(R.string.route_label));

		mapView.setExtent(initExtent);
		
		initializeCallout();
		/*initializeSpinner();
		initializeSpinnerListener();*/
		initializeMapStatusListener();
		initializeSingleTapListener();
		initializeDirectionsLabelClickListener();
		initializeDirectionsLabelLongClickListener();
		initializeMapLongPressListener();
		
		// attribute ESRI logo to map
		mapView.setEsriLogoVisible(true);
		// enable map to wrap around date line
		mapView.enableWrapAround(true);	
		
		//Retrieve the non-configuration instance data that was previously returned. 
		/*Object init = getLastNonConfigurationInstance();
		if (init != null) {
			mapView.restoreState((String) init);
		}*/
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.basemap_menu, menu);
		// Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.location_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            public boolean onSuggestionClick(int position) {
                CursorAdapter selectedView = searchView.getSuggestionsAdapter();
                Cursor cursor = (Cursor) selectedView.getItem(position);
                int index = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1);
                searchView.setQuery(cursor.getString(index), true);
                return true;
            }
        });
		return true;
	}
	
	public Dialog onCreateDialog(int id) {
		switch(id){
		case ARSON_DIALOG_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			mSelectedItems = new ArrayList<Integer>();
		    // Set the dialog title
		    builder.setTitle(R.string.subCrimeType)
		           .setMultiChoiceItems(R.array.arson_subtype, null,
		                      new DialogInterface.OnMultiChoiceClickListener() {
		               public void onClick(DialogInterface dialog, int which, boolean isChecked) {
		                   if (isChecked) {
		                       // If the user checked the item, add it to the selected items
		                       mSelectedItems.add(which);
		                   } else if (mSelectedItems.contains(which)) {
		                       // Else, if the item is already in the array, remove it 
		                       mSelectedItems.remove(Integer.valueOf(which));
		                   }
		               }
		           })
		    // Set the action buttons
		           .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int id) {
		                   // User clicked OK, so save the mSelectedItems results somewhere
		                   // or return them to the component that opened the dialog
		            	   String[] mSelectedStrings = new String[3];
		            	   mSelectedStrings[0]="desc_";
		            	   Iterator<Integer> iterator = mSelectedItems.iterator();
		            	   while(iterator.hasNext()){
		            	       Integer index = iterator.next();
		            	       if(index==0)
		            	    	   mSelectedStrings[1]="Arson of property";
		            	       else if(index==1)
		            	    	   mSelectedStrings[2]="Arson inhabited structure or property";
		            	   }
		            	   new RunQueryFeatureLayerTask(HelloWorldActivity.this).execute(mSelectedStrings);
		               }
		           })
		           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int id) {
		                  
		               }
		           });

		    return 	builder.create();
		}
		return null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mapView.pause();
	}
	
	@Override 	protected void onResume() {
		super.onResume(); 
		mapView.unpause();
		searchText = ((CrimeMap)this.getApplicationContext()).getSearchText();
		//startSearch();
	}
	
	@Override 
	protected void onDestroy() { 
		super.onDestroy();
	}
	
	@Override
	protected void onStop() {
	    super.onStop();
	    finish();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.v("debug","Here new intent");
	    //setIntent(intent);
	    handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		Log.v("debug","Here handle intent");
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      searchText = intent.getStringExtra(SearchManager.QUERY);
	      ((CrimeMap)this.getApplicationContext()).setSearchText(searchText);
	      Log.v("Debug  text",searchText);
	      startSearch();
	      //this.finish();
	    }
	}
	public void findMyLocation(View view){
		new LocateCurrentLocation(mapView);
	}
	
	private void showHome()
	{
		Toast.makeText(getApplicationContext(), "HOME Button", Toast.LENGTH_SHORT).show();
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
		
		case R.id.location_search:
            startSearch();
            return true;
		case R.id.Home_Location:
			//showHome();
			new LocateCurrentLocation(mapView);
			return true;
		case R.id.World_Street_Map:
			basemapStreet.setVisible(true);
			basemapTopo.setVisible(false);
			basemapNatGeo.setVisible(false);
			basemapOcean.setVisible(false);
			Log.v("STREET", "MAP");
			return true;	
		case R.id.World_Topo:
			basemapStreet.setVisible(false);
			basemapNatGeo.setVisible(false);
			basemapOcean.setVisible(false);
			basemapTopo.setVisible(true);
			return true;
		case R.id.NatGeo:
			basemapStreet.setVisible(false);
			basemapTopo.setVisible(false);
			basemapOcean.setVisible(false);
			basemapNatGeo.setVisible(true);
			return true;
		case R.id.Ocean_Basemap:
			basemapStreet.setVisible(false);
			basemapTopo.setVisible(false);
			basemapNatGeo.setVisible(false);
			basemapOcean.setVisible(true);
			return true;
		case R.id.Arson:
			showDialog(ARSON_DIALOG_ID);
			return true;
		case R.id.Burglary:
			new RunQueryFeatureLayerTask(HelloWorldActivity.this).execute("type","BURGLARY");
			return true;
		case R.id.action_measure:
			Unit[] linearUnits = new Unit[] {
					Unit.create(LinearUnit.Code.CENTIMETER),
					Unit.create(LinearUnit.Code.METER),
					Unit.create(LinearUnit.Code.KILOMETER),
					Unit.create(LinearUnit.Code.INCH),
					Unit.create(LinearUnit.Code.FOOT),
					Unit.create(LinearUnit.Code.YARD),
					Unit.create(LinearUnit.Code.MILE_STATUTE)
			};
			fillSymbol.setOutline(new SimpleLineSymbol(Color.TRANSPARENT, 0));

			// create the tool, required.
			MeasuringTool measuringTool = new MeasuringTool(mapView);

			// customize the tool, optional.
			measuringTool.setLinearUnits(linearUnits);
			measuringTool.setMarkerSymbol(markerSymbol);
			measuringTool.setLineSymbol(lineSymbol);
			measuringTool.setFillSymbol(fillSymbol);

			// fire up the tool, required.
			startActionMode(measuringTool);
			return true;
		case R.id.action_route:
			startRouting=true;
			return true;        
		case R.id.Exit_App:
			onStop();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void initializeCallout(){
		// Get the MapView's callout from xml->identify_calloutstyle.xml
		m_calloutStyle = R.xml.identify_calloutstyle;
		LayoutInflater inflater = getLayoutInflater();
		m_callout = mapView.getCallout();
		// Get the layout for the Callout from layout->identify_callout_content.xml
		calloutContent = (ViewGroup) inflater.inflate(R.layout.identify_callout_content, null);
		m_callout.setContent(calloutContent);
	}
	
	/*private void initializeSpinner(){
		// Create a spinner with the drop down values specified in values->queryparameters.xml
		queryParameters = (Spinner) this.findViewById(R.id.spinner1);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.QueryParameters,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		queryParameters.setAdapter(adapter);
	}*/
	
	/*private void initializeSpinnerListener(){
		// Perform action when an item is selected from the Spinner
		queryParameters.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				switch (pos) {
				// Query Parameters
				case 0:
					mapView.setExtent(initExtent);
					//graphicsLayer.removeAll();
					break;
				case 2:
					new RunQueryFeatureLayerTask(HelloWorldActivity.this).execute("ARSON");
					break;
				case 3:
					new RunQueryFeatureLayerTask(HelloWorldActivity.this).execute("BURGLARY");
					break;
				default:
					break;
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});
	}*/
	
	private void initializeMapStatusListener(){
		mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
			private static final long serialVersionUID = 1L;
			public void onStatusChanged(Object source, STATUS status) {
				// Check to see if map has successfully loaded
				if ((source == mapView) && (status == STATUS.INITIALIZED)) {
					// Set the flag to true
					m_isMapLoaded = true;
				}
			}
		});
	}
	
	/**On single clicking the directions label, start a ListActivity to show the list of all directions for this route.
	 * Selecting one of those items will return to the map and highlight that segment.
	 */
	private void initializeDirectionsLabelClickListener(){
		directionsLabel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (curDirections == null)
					return;
				Intent intent = new Intent(getApplicationContext(), ShowDirectionsActivity.class);
				intent.putStringArrayListExtra("directions", curDirections);
				startActivityForResult(intent, 1);
			}
		});
	}
	
	/**
     * On long clicking the directions label, removes the current route and resets all affiliated variables.
     */
	private void initializeDirectionsLabelLongClickListener(){
		directionsLabel.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				routeLayer.removeAll();
				hiddenSegmentsLayer.removeAll();
				curRoute = null;
				curDirections = null;
				directionsLabel.setText(getString(R.string.route_label));
				return true;
			}
		});
	}
	
	/**
     * On long pressing the map view, route from our current location to the pressed location.
     */
	private void initializeMapLongPressListener(){
		mapView.setOnLongPressListener(new OnLongPressListener() {
			private static final long serialVersionUID = 1L;
			public boolean onLongPress(final float x, final float y) {
				// Clear the graphics and empty the directions list
				routeLayer.removeAll();
				hiddenSegmentsLayer.removeAll();
				curDirections = new ArrayList<String>();
				mResults = null;
				// retrieve the user clicked location
				final Point loc = mapView.toMapPoint(x, y);
				// Show that the route is calculating
				dialog = ProgressDialog.show(HelloWorldActivity.this, "", "Calculating route...", true);
				// Spawn the request off in a new thread to keep UI responsive
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							// Start building up routing parameters
							RouteParameters rp = mRouteTask.retrieveDefaultRouteTaskParameters();
							NAFeaturesAsFeature rfaf = new NAFeaturesAsFeature();
							// Convert point to EGS (decimal degrees)
							Point p = (Point) GeometryEngine.project(loc, wm, egs);
							// Create the stop points (start at our location, go to pressed location)
							StopGraphic startPoint = new StopGraphic(mLocation);
							StopGraphic endPoint = new StopGraphic(p);
							rfaf.setFeatures(new Graphic[] { startPoint, endPoint });
							rfaf.setCompressedRequest(true);
							rp.setStops(rfaf);
							// Set the routing service output SR to our map service's SR
							rp.setOutSpatialReference(wm);
							// Solve the route and use the results to update UI when received
							mResults = mRouteTask.solve(rp);
							
							Log.v("mResults length:",mResults.getMessages().size()+"");
							mHandler.post(mUpdateResults);
						} catch (Exception e) {
							mException = e;
							mHandler.post(mUpdateResults);
						}
					}
				};
				// Start the operation
				thread.start();
				return true;
			}
		});
	}
	
	private void initializeSingleTapListener(){
		mapView.setOnSingleTapListener(new OnSingleTapListener() {
			private static final long serialVersionUID = 1L;
			public void onSingleTap(float x, float y) {
				if (m_isMapLoaded) {
					// If map is initialized and Single tap is registered on screen,
					// identify the location selected
					identifyLocation(x, y);
				}
				if(startRouting == true){
					// Get all the graphics within 20 pixels the click
					int[] indexes = hiddenSegmentsLayer.getGraphicIDs(x, y, 20);
					// Hide the currently selected segment
					hiddenSegmentsLayer.updateGraphic(selectedSegmentID, segmentHider);
					if (indexes.length < 1) {
						// If no segments were found but there is currently a route,
						// zoom to the extent of the full route
						if (curRoute != null) {
							mapView.setExtent(curRoute.getEnvelope(), 250);
							directionsLabel.setText(routeSummary);
						}
						return;
					}
					// Otherwise update our currently selected segment
					selectedSegmentID = indexes[0];
					Graphic selected = hiddenSegmentsLayer.getGraphic(selectedSegmentID);
					// Highlight it on the map
					hiddenSegmentsLayer.updateGraphic(selectedSegmentID, segmentShower);
					String direction = ((String) selected.getAttributeValue("text"));
					double time = ((Double) selected.getAttributeValue("time")).doubleValue();
					double length = ((Double) selected.getAttributeValue("length")).doubleValue();
					// Update the label with this direction's information
					String label = String.format("%s%nTime: %.1f minutes, Length: %.1f miles", direction, time, length);
					directionsLabel.setText(label);
					// Zoom to the extent of that segment
					mapView.setExtent(selected.getGeometry(), 50);
				}

			}
		});
	}
	
	private void startSearch(){
		setSearchParamsAndExecute(searchText);
		Toast.makeText(getApplicationContext(), "Searching location", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Updates the UI after a successful rest response has been received.
	 */
	void updateUI() {
		dialog.dismiss();
		if (mResults == null) {
			Toast.makeText(HelloWorldActivity.this, mException.toString(), Toast.LENGTH_LONG).show();
			return;
		}
		curRoute = mResults.getRoutes().get(0);
		// Symbols for the route and the destination (blue line, checker flag)
		SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE, 3);
		PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(mapView.getContext(), getResources().getDrawable(
				R.drawable.flag_finish));
		// Add all the route segments with their relevant information to the hiddenSegmentsLayer, and 
		//add the direction information to the list of directions
		for (RouteDirection rd : curRoute.getRoutingDirections()) {
			HashMap<String, Object> attributes = new HashMap<String, Object>();
			attributes.put("text", rd.getText());
			attributes.put("time", Double.valueOf(rd.getMinutes()));
			attributes.put("length", Double.valueOf(rd.getLength()));
			curDirections.add(String.format("%s%nTime: %.1f minutes, Length: %.1f miles", rd.getText(), rd.getMinutes(),
					rd.getLength()));
			Graphic routeGraphic = new Graphic(rd.getGeometry(), segmentHider, attributes);
			hiddenSegmentsLayer.addGraphic(routeGraphic);
		}
		// Reset the selected segment
		selectedSegmentID = -1;

		// Add the full route graphic and destination graphic to the routeLayer
		Graphic routeGraphic = new Graphic(curRoute.getRouteGraphic().getGeometry(), routeSymbol);
		Graphic endGraphic = new Graphic(((Polyline) routeGraphic.getGeometry()).getPoint(((Polyline) routeGraphic.getGeometry()).getPointCount() - 1),
				destinationSymbol);
		routeLayer.addGraphics(new Graphic[] { routeGraphic, endGraphic });
		// Get the full route summary and set it as our current label
		routeSummary = String.format("%s%nTotal time: %.1f minutes, length: %.1f miles", curRoute.getRouteName(),
				curRoute.getTotalMinutes(), curRoute.getTotalMiles());
		directionsLabel.setText(routeSummary);
		// Zoom to the extent of the entire route with a padding
		mapView.setExtent(curRoute.getEnvelope(), 250);
	}
	
	/**
	 * On returning from the list of directions, highlight and zoom to the segment that was selected from the list.
	 * (Activity simply resumes if the back button was hit instead).
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// Response from directions list view
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				String direction = data.getStringExtra("returnedDirection");
				if (direction == null)
					return;
				// Look for the graphic that corresponds to this direction
				for (int index : hiddenSegmentsLayer.getGraphicIDs()) {
					Graphic g = hiddenSegmentsLayer.getGraphic(index);
					if (direction.contains((String) g.getAttributeValue("text"))) {
						// When found, hide the currently selected, show the new selection
						hiddenSegmentsLayer.updateGraphic(selectedSegmentID, segmentHider);
						hiddenSegmentsLayer.updateGraphic(index, segmentShower);
						selectedSegmentID = index;
						// Update label with information for that direction
						directionsLabel.setText(direction);
						// Zoom to the extent of that segment
						mapView.setExtent(hiddenSegmentsLayer.getGraphic(selectedSegmentID).getGeometry(), 50);
						break;
					}
				}
			}
		}
	}

	  
	private void setSearchParamsAndExecute(String address) {
		try {
			// create Locator parameters from single line address string
			LocatorFindParameters findParams = new LocatorFindParameters(
					address);
			// set the search country to USA
			findParams.setSourceCountry("USA");
			// limit the results to 2
			findParams.setMaxLocations(2);
			// set address spatial reference to match map
			findParams.setOutSR(mapView.getSpatialReference());
			// execute async task to geocode address
			new GeocoderTask(this, locationLayer).execute(findParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Takes in the screen location of the point to identify the feature on map.
	 * 
	 * @param x
	 *          x co-ordinate of point
	 * @param y
	 *          y co-ordinate of point
	 */
	void identifyLocation(float x, float y) {
		// Hide the callout, if the callout from previous tap is still showing on map
		if (m_callout.isShowing()) {
			m_callout.hide();
		}
		// Find out if the user tapped on a feature
		SearchForFeature(x, y);
		// If the user tapped on a feature, then display information regarding the feature in the callout
		if (m_identifiedGraphic != null) {
			Point mapPoint = mapView.toMapPoint(x, y);
			// Show Callout
			ShowCallout(m_callout, m_identifiedGraphic, mapPoint);
		}
	}

	/**
	 * Sets the value of m_identifiedGraphic to the Graphic present on the
	 * location of screen tap
	 * 
	 * @param x
	 *          x co-ordinate of point
	 * @param y
	 *          y co-ordinate of point
	 */
	private void SearchForFeature(float x, float y) {
		Point mapPoint = mapView.toMapPoint(x, y);
		if (mapPoint != null) {
			for (Layer layer : mapView.getLayers()) {
				if (layer == null)
					continue;
				Log.v("Name",""+layer.getName());	
				Log.v("Title",""+layer.getTitle());
				Log.v("Title",""+layer.getClass().toString());
				Log.v("URL",""+layer.getUrl());	
				Log.v("ID",""+layer.getID());	
				if (layer instanceof ArcGISFeatureLayer) {
					ArcGISFeatureLayer fLayer = (ArcGISFeatureLayer) layer;  
					// Get the Graphic at location x,y
					m_identifiedGraphic = GetFeature(fLayer, x, y);
				} else
					continue;
			}
		}
	}

	/**
	 * Returns the Graphic present the location of screen tap
	 * 
	 * @param fLayer
	 * @param x
	 *          x co-ordinate of point
	 * @param y
	 *          y co-ordinate of point
	 * @return Graphic at location x,y
	 */
	private Graphic GetFeature(ArcGISFeatureLayer fLayer, float x, float y) {
		// Get the graphics near the Point.
		int[] ids = fLayer.getGraphicIDs(x, y, 10, 1);
		if (ids == null || ids.length == 0) {
			return null;
		}
		Graphic g = fLayer.getGraphic(ids[0]);
		return g;
	}

	/**
	 * Shows the Attribute values for the Graphic in the Callout
	 * 
	 * @param calloutView
	 * @param graphic
	 * @param mapPoint
	 */
	private void ShowCallout(Callout calloutView, Graphic graphic, Point mapPoint) {
		// Get the values of attributes for the Graphic
		String crimeType = (String) graphic.getAttributeValue("type");
		String location = (String) graphic.getAttributeValue("address");
		String day = (graphic.getAttributeValue("day")).toString();
		String month = (graphic.getAttributeValue("month")).toString();
		String year = (graphic.getAttributeValue("year")).toString();
		String desc = (graphic.getAttributeValue("desc_")).toString();

		// Set callout properties
		calloutView.setCoordinates(mapPoint);
		calloutView.setStyle(m_calloutStyle);
		calloutView.setMaxWidth(1200);
		calloutView.setMaxHeight(1200);

		// Compose the string to display the results
		/*StringBuilder cityCountryName = new StringBuilder();
      cityCountryName.append(cityName);
      cityCountryName.append(", ");
      cityCountryName.append(countryName);*/

		TextView calloutTextLine1 = (TextView) findViewById(R.id.crimetype);
		calloutTextLine1.setText(crimeType);

		// Compose the string to display the results
		StringBuilder crimeLocation = new StringBuilder();
		crimeLocation.append("Location: ");
		crimeLocation.append(location);
		TextView calloutTextLine2 = (TextView) findViewById(R.id.location);
		calloutTextLine2.setText(crimeLocation);

		StringBuilder crimeDate = new StringBuilder();
		crimeDate.append("Date Time: ");
		crimeDate.append(month+'/');
		//crimeDate.append(day+'/');
		crimeDate.append(year);
		TextView calloutTextLine3 = (TextView) findViewById(R.id.datetime);
		calloutTextLine3.setText(crimeDate);
		
		TextView calloutTextLine4 = (TextView) findViewById(R.id.desc);
		calloutTextLine4.setText(desc);
		
		calloutView.setContent(calloutContent);
		calloutView.show();
	}

}