package com.esri.arcgis.android.samples.helloworld;

import android.app.Application;

public class CrimeMap extends Application {
	private String mSearchText;

    public String getSearchText() {
        return mSearchText;
    }

    public void setSearchText(String searchText) {
        this.mSearchText = searchText;
    }
}
