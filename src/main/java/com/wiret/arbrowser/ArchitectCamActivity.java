package com.wiret.arbrowser;

import java.io.File;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.net.Uri;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import com.wikitude.architect.StartupConfiguration.CameraPosition;
import com.wikitude.samples.ArchitectViewHolderInterface;
import com.wikitude.samples.LocationProvider;
import com.wikitude.samples.SamplePoiDetailActivity;
import com.wikitude.samples.WikitudeSDKConstants;

public class ArchitectCamActivity extends AbstractArchitectCamActivity {

	/**
	 * last time the calibration toast was shown, this avoids too many toast shown when compass needs calibration
	 */
	private long lastCalibrationToastShownTimeMillis = System.currentTimeMillis();

    protected Bitmap screenCapture = null;

    private static final int WIKITUDE_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 3;

	@Override
	public String getARchitectWorldPath() {
		return "samples" + File.separator + "6_Browsing$Pois_2_Adding$Radar" + File.separator + "index.html";
	}

	@Override
	public String getActivityTitle() {
		return "EHSData-ARBrowser";
	}

	@Override
	public int getContentViewId() {
		return R.layout.sample_cam;
	}

	@Override
	public int getArchitectViewId() {
		return R.id.architectView;
	}
	
	@Override
	public String getWikitudeSDKLicenseKey() {
		return WikitudeSDKConstants.WIKITUDE_SDK_KEY;
	}
	
	@Override
	public SensorAccuracyChangeListener getSensorAccuracyListener() {
		return new SensorAccuracyChangeListener() {
			@Override
			public void onCompassAccuracyChanged( int accuracy ) {
				/* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
				if ( accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && ArchitectCamActivity.this != null && !ArchitectCamActivity.this.isFinishing() && System.currentTimeMillis() - ArchitectCamActivity.this.lastCalibrationToastShownTimeMillis > 5 * 1000) {
					Toast.makeText( ArchitectCamActivity.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG ).show();
					ArchitectCamActivity.this.lastCalibrationToastShownTimeMillis = System.currentTimeMillis();
				}
			}
		};
	}

	@Override
	public ArchitectUrlListener getUrlListener() {
		return new ArchitectUrlListener() {

			@Override
			public boolean urlWasInvoked(String uriString) {
				Uri invokedUri = Uri.parse(uriString);
				
				// pressed "More" button on POI-detail panel
				if ("markerselected".equalsIgnoreCase(invokedUri.getHost())) {
					final Intent poiDetailIntent = new Intent(ArchitectCamActivity.this, SamplePoiDetailActivity.class);
					poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_ID, String.valueOf(invokedUri.getQueryParameter("id")) );
					poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_TITILE, String.valueOf(invokedUri.getQueryParameter("title")) );
					poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_DESCR, String.valueOf(invokedUri.getQueryParameter("description")) );
					ArchitectCamActivity.this.startActivity(poiDetailIntent);
					return true;
				}else if ("radarselected".equalsIgnoreCase(invokedUri.getHost())) {
					final Intent poiDetailIntent = new Intent(ArchitectCamActivity.this, PlaceListActivity.class);
					startActivity(poiDetailIntent);
					return true;
				}else if ("openNew".equalsIgnoreCase(invokedUri.getHost())) {
				    showFileChooser();
					return true;
			    }

				return true;
			}
		};
	}


	@Override
	public ILocationProvider getLocationProvider(final LocationListener locationListener) {
		return new LocationProvider(this, locationListener);
	}
	
	@Override
	public float getInitialCullingDistanceMeters() {
		// you need to adjust this in case your POIs are more than 50km away from user here while loading or in JS code (compare 'AR.context.scene.cullingDistance')
		return ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS;
	}

	@Override
	protected boolean hasGeo() {
		return true;
	}

	@Override
	protected boolean hasIR() {
		return true;
	}

	@Override
	protected CameraPosition getCameraPosition() {
		return CameraPosition.DEFAULT;
	}

	private void showFileChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(
					Intent.createChooser(intent, "Select a File to Open"),
					KmlFileBrowserActivity.FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
	}

}
