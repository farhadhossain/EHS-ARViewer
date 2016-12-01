package com.wiret.arbrowser;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.wikitude.samples.SamplePoiDetailActivity;

import java.io.InputStream;

public class KmlFileBrowserActivity extends ArchitectCamActivity {

	public static final int FILE_SELECT_CODE = 101;



	@Override
	public void onCreate( final Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		/*
		findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showFileChooser();
			}
		});

		findViewById(R.id.imageButtonRefresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent poiDetailIntent = new Intent(KmlFileBrowserActivity.this, PlaceListActivity.class);
				startActivity(poiDetailIntent);
			}
		});
       */

	}



}