package com.esri.arcgis.android.samples.helloworld;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ShowDirectionsActivity extends ListActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Retrieve the list of directions from the intent
		final Intent intent = getIntent();
		final ArrayList<String> directions = intent.getStringArrayListExtra("directions");

		// Sets the list to the list of directions
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.directions_list_item, directions);
		setListAdapter(adapter);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		// Returns the selected item to the calling activity
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				intent.putExtra("returnedDirection", ((TextView) view).getText());
				setResult(RESULT_OK, intent);
				finish();

			}
		});
	}
}
