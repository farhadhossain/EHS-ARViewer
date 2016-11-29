package com.wiret.arbrowser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import com.wikitude.architect.StartupConfiguration;
import com.wikitude.architect.StartupConfiguration.CameraPosition;
import com.wikitude.samples.ArchitectViewHolderInterface;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Abstract activity which handles live-cycle events.
 * Feel free to extend from this activity when setting up your own AR-Activity 
 *
 */
public abstract class AbstractArchitectCamActivity extends Activity implements ArchitectViewHolderInterface {

	/**
	 * holds the Wikitude SDK AR-View, this is where camera, markers, compass, 3D models etc. are rendered
	 */
	protected ArchitectView					architectView;
	
	/**
	 * sensor accuracy listener in case you want to display calibration hints
	 */
	protected SensorAccuracyChangeListener	sensorAccuracyListener;
	
	/**
	 * last known location of the user, used internally for content-loading after user location was fetched
	 */
	protected Location 						lastKnownLocaton;

	/**
	 * sample location strategy, you may implement a more sophisticated approach too
	 */
	protected ILocationProvider				locationProvider;
	
	/**
	 * location listener receives location updates and must forward them to the architectView
	 */
	protected LocationListener 				locationListener;
	
	/**
	 * urlListener handling "document.location= 'architectsdk://...' " calls in JavaScript"
	 */
	protected ArchitectUrlListener 			urlListener;
	
	protected JSONArray poiData;

	protected boolean isLoading = false;

	private Uri kmlFile;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate( final Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		this.requestWindowFeature( Window.FEATURE_NO_TITLE );
		this.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );

		/* pressing volume up/down should cause music volume changes */
		this.setVolumeControlStream( AudioManager.STREAM_MUSIC );

		this.setTitle( this.getActivityTitle() );
		
		/*  
		 *	this enables remote debugging of a WebView on Android 4.4+ when debugging = true in AndroidManifest.xml
		 *	If you get a compile time error here, ensure to have SDK 19+ used in your ADT/Eclipse.
		 *	You may even delete this block in case you don't need remote debugging or don't have an Android 4.4+ device in place.
		 *	Details: https://developers.google.com/chrome-developer-tools/docs/remote-debugging
		 */
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
		    if ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) ) {
		        WebView.setWebContentsDebuggingEnabled(true);
		    }
		}

		kmlFile = getIntent().getData();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
		} else {
			this.setContentView( this.getContentViewId() );
			initArView();
		}


	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case 101: {
				if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
					this.setContentView( this.getContentViewId() );
					initArView();
					postCreateArView();
					if(kmlFile!=null){
						importData(kmlFile);
					}
				}
				return;
			}

		}
	}

	private void initArView(){
		/* set AR-view for life-cycle notifications etc. */
		this.architectView = (ArchitectView)this.findViewById( this.getArchitectViewId()  );

		/* pass SDK key if you have one, this one is only valid for this package identifier and must not be used somewhere else */
		final StartupConfiguration config = new StartupConfiguration( this.getWikitudeSDKLicenseKey(), this.getFeatures(), this.getCameraPosition() );

		try {
			/* first mandatory life-cycle notification */
			this.architectView.onCreate( config );
		} catch (RuntimeException rex) {
			this.architectView = null;
			Toast.makeText(getApplicationContext(), "can't create Architect View", Toast.LENGTH_SHORT).show();
			Log.e(this.getClass().getName(), "Exception in ArchitectView.onCreate()", rex);
		}

		// set accuracy listener if implemented, you may e.g. show calibration prompt for compass using this listener
		this.sensorAccuracyListener = this.getSensorAccuracyListener();

		// set urlListener, any calls made in JS like "document.location = 'architectsdk://foo?bar=123'" is forwarded to this listener, use this to interact between JS and native Android activity/fragment
		this.urlListener = this.getUrlListener();

		// register valid urlListener in architectView, ensure this is set before content is loaded to not miss any event
		if (this.urlListener != null && this.architectView != null) {
			this.architectView.registerUrlListener( this.getUrlListener() );
		}

		if (hasGeo()) {
			// listener passed over to locationProvider, any location update is handled here
			this.locationListener = new LocationListener() {

				@Override
				public void onStatusChanged( String provider, int status, Bundle extras ) {
				}

				@Override
				public void onProviderEnabled( String provider ) {
				}

				@Override
				public void onProviderDisabled( String provider ) {
				}

				@Override
				public void onLocationChanged( final Location location ) {
					// forward location updates fired by LocationProvider to architectView, you can set lat/lon from any location-strategy
					if (location!=null) {
						// sore last location as member, in case it is needed somewhere (in e.g. your adjusted project)
						AbstractArchitectCamActivity.this.lastKnownLocaton = location;
						if ( AbstractArchitectCamActivity.this.architectView != null ) {
							// check if location has altitude at certain accuracy level & call right architect method (the one with altitude information)
							if ( location.hasAltitude() && location.hasAccuracy() && location.getAccuracy()<7) {
								AbstractArchitectCamActivity.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy() );
							} else {
								AbstractArchitectCamActivity.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.hasAccuracy() ? location.getAccuracy() : 1000 );
							}
						}
					}
				}
			};

			// locationProvider used to fetch user position
			this.locationProvider = getLocationProvider( this.locationListener );
		} else {
			this.locationProvider = null;
			this.locationListener = null;
		}
	}

	private void postCreateArView(){
		if ( this.architectView != null ) {

			// call mandatory live-cycle method of architectView
			this.architectView.onPostCreate();

			try {
				// load content via url in architectView, ensure '<script src="architect://architect.js"></script>' is part of this HTML file, have a look at wikitude.com's developer section for API references
				this.architectView.load( this.getARchitectWorldPath() );

				if (this.getInitialCullingDistanceMeters() != ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS) {
					// set the culling distance - meaning: the maximum distance to render geo-content
					this.architectView.setCullingDistance( this.getInitialCullingDistanceMeters() );
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	protected abstract CameraPosition getCameraPosition();

	private int getFeatures() {
		int features = (hasGeo() ? StartupConfiguration.Features.Geo : 0) | (hasIR() ? StartupConfiguration.Features.Tracking2D : 0);
		return features;
	}

	protected abstract boolean hasGeo();
	protected abstract boolean hasIR();

	@Override
	protected void onPostCreate( final Bundle savedInstanceState ) {
		super.onPostCreate( savedInstanceState );
		postCreateArView();
		if(kmlFile!=null){
			importData(kmlFile);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// call mandatory live-cycle method of architectView
		if ( this.architectView != null ) {
			this.architectView.onResume();
			
			// register accuracy listener in architectView, if set
			if (this.sensorAccuracyListener!=null) {
				this.architectView.registerSensorAccuracyChangeListener( this.sensorAccuracyListener );
			}
		}

		// tell locationProvider to resume, usually location is then (again) fetched, so the GPS indicator appears in status bar
		if ( this.locationProvider != null ) {
			this.locationProvider.onResume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// call mandatory live-cycle method of architectView
		if ( this.architectView != null ) {
			this.architectView.onPause();
			
			// unregister accuracy listener in architectView, if set
			if ( this.sensorAccuracyListener != null ) {
				this.architectView.unregisterSensorAccuracyChangeListener( this.sensorAccuracyListener );
			}
		}
		
		// tell locationProvider to pause, usually location is then no longer fetched, so the GPS indicator disappears in status bar
		if ( this.locationProvider != null ) {
			this.locationProvider.onPause();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// call mandatory live-cycle method of architectView
		if ( this.architectView != null ) {
			this.architectView.onDestroy();
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if ( this.architectView != null ) {
			this.architectView.onLowMemory();
		}
	}

	/**
	 * title shown in activity
	 * @return
	 */
	public abstract String getActivityTitle();
	
	/**
	 * path to the architect-file (AR-Experience HTML) to launch
	 * @return
	 */
	@Override
	public abstract String getARchitectWorldPath();
	
	/**
	 * url listener fired once e.g. 'document.location = "architectsdk://foo?bar=123"' is called in JS
	 * @return
	 */
	@Override
	public abstract ArchitectUrlListener getUrlListener();
	
	/**
	 * @return layout id of your layout.xml that holds an ARchitect View, e.g. R.layout.camview
	 */
	@Override
	public abstract int getContentViewId();
	
	/**
	 * @return Wikitude SDK license key, checkout www.wikitude.com for details
	 */
	@Override
	public abstract String getWikitudeSDKLicenseKey();
	
	/**
	 * @return layout-id of architectView, e.g. R.id.architectView
	 */
	@Override
	public abstract int getArchitectViewId();

	/**
	 * 
	 * @return Implementation of a Location
	 */
	@Override
	public abstract ILocationProvider getLocationProvider(final LocationListener locationListener);
	
	/**
	 * @return Implementation of Sensor-Accuracy-Listener. That way you can e.g. show prompt to calibrate compass
	 */
	@Override
	public abstract ArchitectView.SensorAccuracyChangeListener getSensorAccuracyListener();
	
	/**
	 * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
	 * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
	 */
	public static final boolean isVideoDrawablesSupported() {
		String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
		return extensions != null && extensions.contains( "GL_OES_EGL_image_external" );
	}

	protected void injectData(final String kmlPath) {
		if (!isLoading && this.architectView!=null) {
			final Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					isLoading = true;
					
					final int WAIT_FOR_LOCATION_STEP_MS = 2000;
					
					while (lastKnownLocaton==null && !isFinishing()) {
					
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								Toast.makeText(AbstractArchitectCamActivity.this, R.string.location_fetching, Toast.LENGTH_SHORT).show();	
							}
						});
			
						try {
							Thread.sleep(WAIT_FOR_LOCATION_STEP_MS);
						} catch (InterruptedException e) {
							break;
						}
					}
					
					if (lastKnownLocaton!=null && !isFinishing()) {
						// TODO: you may replace this dummy implementation and instead load POI information e.g. from your database
						poiData = getPoiInformation(lastKnownLocaton, kmlPath);
						callJavaScript("World.loadPoisFromJsonData", new String[] { poiData.toString() });
					}
					
					isLoading = false;
				}
			});
			t.start();
		}
	}

	/**
	 * call JacaScript in architectView
	 * @param methodName
	 * @param arguments
	 */
	private void callJavaScript(final String methodName, final String[] arguments) {
		final StringBuilder argumentsString = new StringBuilder("");
		for (int i= 0; i<arguments.length; i++) {
			argumentsString.append(arguments[i]);
			if (i<arguments.length-1) {
				argumentsString.append(", ");
			}
		}
		
		if (this.architectView!=null) {
			final String js = ( methodName + "( " + argumentsString.toString() + " );" );
			this.architectView.callJavascript(js);
		}
	}
	
	/**
	 * loads poiInformation and returns them as JSONArray. Ensure attributeNames of JSON POIs are well known in JavaScript, so you can parse them easily
	 * @param userLocation the location of the user
	 * @return POI information in JSONArray
	 */
	public JSONArray getPoiInformation(final Location userLocation, String kmlPath) {
		
		if (userLocation==null) {
			return null;
		}

		ArrayList<KmlParser.Place> places = new ArrayList<>();
		try {
			ContentResolver cr = getApplicationContext().getContentResolver();
			InputStream is = cr.openInputStream(Uri.parse(kmlPath));
			places = new KmlParser().parse(is);
		}catch(FileNotFoundException e){
			Toast.makeText(this, "Invalid kml file.",
					Toast.LENGTH_SHORT).show();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			Toast.makeText(this, "Invalid kml file.",
					Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Invalid kml file.",
					Toast.LENGTH_SHORT).show();
		}

		final JSONArray pois = new JSONArray();
		
		// ensure these attributes are also used in JavaScript when extracting POI data
		final String ATTR_ID = "id";
		final String ATTR_NAME = "name";
		final String ATTR_DESCRIPTION = "description";
		final String ATTR_LATITUDE = "latitude";
		final String ATTR_LONGITUDE = "longitude";
		final String ATTR_ALTITUDE = "altitude";
		//ArrayList<KmlParser.Place> places = getIntent().getExtras().getParcelableArrayList("places");
		for (KmlParser.Place place: places) {
			final HashMap<String, String> poiInformation = new HashMap<String, String>();
			poiInformation.put(ATTR_ID, "1");
			poiInformation.put(ATTR_NAME, place.name);
			poiInformation.put(ATTR_DESCRIPTION, place.description!=null?place.description:"");
			//double[] poiLocationLatLon = getRandomLatLonNearby(userLocation.getLatitude(), userLocation.getLongitude());
			poiInformation.put(ATTR_LATITUDE, place.lat);
			poiInformation.put(ATTR_LONGITUDE, place.lon);
			final float UNKNOWN_ALTITUDE = -32768f;  // equals "AR.CONST.UNKNOWN_ALTITUDE" in JavaScript (compare AR.GeoLocation specification)
			// Use "AR.CONST.UNKNOWN_ALTITUDE" to tell ARchitect that altitude of places should be on user level. Be aware to handle altitude properly in locationManager in case you use valid POI altitude value (e.g. pass altitude only if GPS accuracy is <7m).
			poiInformation.put(ATTR_ALTITUDE, String.valueOf(UNKNOWN_ALTITUDE));
			pois.put(new JSONObject(poiInformation));
		}
		
		return pois;
	}
	
	/**
	 * helper for creation of dummy places.
	 * @param lat center latitude
	 * @param lon center longitude
	 * @return lat/lon values in given position's vicinity
	 */
	private static double[] getRandomLatLonNearby(final double lat, final double lon) {
		return new double[] { lat + Math.random()/5-0.1 , lon + Math.random()/5-0.1};
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case KmlFileBrowserActivity.FILE_SELECT_CODE:
				if (resultCode == RESULT_OK) {

					kmlFile =  data.getData();
					if(kmlFile!=null) {
						//getIntent().setData(null);
						try {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
								ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
							} else {
								importData(kmlFile);
							}
						} catch (Exception e) {
							return;
						}
					}

				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void importData(Uri data) {
		final String scheme = data.getScheme();

		if(ContentResolver.SCHEME_FILE.equals(scheme)) {
			try {
				ContentResolver cr = getApplicationContext().getContentResolver();
				InputStream is = cr.openInputStream(data);
				if(is == null) return;
				is.close();

				injectData(data.toString());
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}