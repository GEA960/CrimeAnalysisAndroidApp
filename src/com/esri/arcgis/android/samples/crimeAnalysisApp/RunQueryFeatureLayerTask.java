package com.esri.arcgis.android.samples.helloworld;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;

/**
 * Run the query task on the feature layer and put the result on the map.
 */
public class RunQueryFeatureLayerTask extends AsyncTask<String, Void, ArrayList<Graphic[]>> {

	ProgressDialog progress;
	HelloWorldActivity queryFeatureServiceActivity;

	// default constructor
	public RunQueryFeatureLayerTask(HelloWorldActivity qActivity) {
		queryFeatureServiceActivity=qActivity;
	}

	@Override
	protected void onPreExecute() {
		progress = ProgressDialog.show(queryFeatureServiceActivity, "", "Please wait....query task is executing");
	}

	@Override
	protected ArrayList<Graphic[]> doInBackground(String... params) {

		ArrayList<Graphic[]> graphicsList = new ArrayList<Graphic[]>();
		
		String[] outFields= {"type", "address","day","month","year","desc_"};
		Log.v("params length", ""+params.length);
		for(int i=1;i<params.length;i++){
			String whereClause = params[0]+"='" + params[i] + "'";
			//String whereClause = "type='Arson'";
			Log.v("params[0]", params[0]);
			//Log.v("params[i+1]", params[i+1]);
			//Log.v("params[i+2]", params[i+2]);
			// Define a new query and set parameters
			Query query = new Query();
			query.setWhere(whereClause);
			query.setOutFields(outFields);
			query.setReturnGeometry(true);
			Log.v("query get text", ""+query.getText());
			// Define the new instance of QueryTask
			try {
				QueryTask qTask = new QueryTask(HelloWorldActivity.featureServiceURL);
				// run the querytask
				FeatureSet fs = qTask.execute(query);
				Log.v("listsize", ""+fs.getFields().size());;
				// Get the graphics from the result feature set
				Graphic[] grs = fs.getGraphics();
				graphicsList.add(grs);
				Log.d("DEBUG- grs",""+grs[0].getAttributeNames().length);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return graphicsList;
		}

		@Override
		protected void onPostExecute(ArrayList<Graphic[]> graphics) {

			// Remove the result from previously run query task
			//GraphicsLayer graphicsLayer = new GraphicsLayer();

			HelloWorldActivity.featureLayer.removeAll();
			queryFeatureServiceActivity.m_callout.hide();
			//homeFeatureLayer.setVisible(false);
			//graphicsLayer.removeAll();
			Graphic[] resultGraphics=new Graphic[100];
			int count=0;
			// Define a new marker symbol for the result graphics
			SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLUE, 6 , SimpleMarkerSymbol.STYLE.CIRCLE);
			//PictureMarkerSymbol pms= new PictureMarkerSymbol(R.drawable.filter_crime_type);

			// Envelope to focus on the map extent on the results
			Envelope extent = new Envelope();

			// iterate through results
			for(int i=0; i < graphics.size();i++){
				for (Graphic gr : graphics.get(i)) {

					//Graphic g = new Graphic(gr.getGeometry(), (Symbol)sms);

					resultGraphics[count] = gr;
					count++;
					Point p = (Point) gr.getGeometry();
					extent.merge(p);
					Log.d("cnt_debug",""+count);
				}
			}
			// Add result graphics on the map
			//graphicsLayer.setOpacity((float)1.0);
			//graphicsLayer.addGraphics(resultGraphics);
			HelloWorldActivity.featureLayer.addGraphics(resultGraphics);
			// Set the map extent to the envelope containing the result graphics
			//map.addLayer(graphicsLayer);

			HelloWorldActivity.mapView.setExtent(extent, 100);
			// Disable the progress dialog
			progress.dismiss();
		}
	}