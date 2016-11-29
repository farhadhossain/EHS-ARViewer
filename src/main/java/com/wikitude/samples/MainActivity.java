package com.wikitude.samples;


import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wiret.arbrowser.KmlFileBrowserActivity;
import com.wiret.arbrowser.R;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;


/**
 * Activity launched when pressing app-icon.
 * It uses very basic ListAdapter for UI representation
 */
public class MainActivity extends FragmentActivity{

	private Uri kmlFile;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );


		this.setContentView( R.layout.list_startscreen );

		findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showFileChooser();
			}
		});
		// ensure to clean cache when it is no longer required
		MainActivity.deleteDirectoryContent ( ArchitectView.getCacheDirectoryAbsoluteFilePath(this) );



		kmlFile = getIntent().getData();
		if(kmlFile!=null) {
			getIntent().setData(null);
			try {
				if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
					ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
				} else {
					importData(kmlFile);
				}
			} catch (Exception e) {
				// warn user about bad data here
				//finish();
				return;
			}
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case 101: {
				if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
					importData(kmlFile);
				}
				return;
			}

		}
	}

	private void importData(Uri data) {
		final String scheme = data.getScheme();

		if(ContentResolver.SCHEME_FILE.equals(scheme)) {
			try {
				ContentResolver cr = getApplicationContext().getContentResolver();
				InputStream is = cr.openInputStream(data);
				if(is == null) return;
				is.close();

				/*final FragmentTransaction fragmentTransaction =this.getSupportFragmentManager().beginTransaction();
				fragmentTransaction.replace( R.id.mainFragement, new SampleCamFragment() );
				fragmentTransaction.commit();*/


				final Intent intent = new Intent(this, KmlFileBrowserActivity.class);
				intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING, "EHSData-ARBrowser");
				intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL, "samples"
						+ File.separator + "6_Browsing$Pois_2_Adding$Radar"
						+ File.separator + "index.html");
				intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_IR, true);
				intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_GEO, true);
				intent.putExtra("filePath", data.toString());
				this.startActivity(intent);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	/**
	 * deletes content of given directory
	 * @param path
	 */
	private static void deleteDirectoryContent(final String path) {
		try {
			final File dir = new File (path);
			if (dir.exists() && dir.isDirectory()) {
				final String[] children = dir.list();
		        for (int i = 0; i < children.length; i++) {
		            new File(dir, children[i]).delete();
		        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	/**
	 * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
	 * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
	 */
	public static final boolean isVideoDrawablesSupported() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Lollipop: assume it's ok
			// because creating a new GL context only to check this extension is overkill
			return true;
		} else {
			String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
			return extensions != null && extensions.contains( "GL_OES_EGL_image_external" );
		}
	}


	private static final int FILE_SELECT_CODE = 0;

	private void showFileChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			startActivityForResult(
					Intent.createChooser(intent, "Select a File to Upload"),
					FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case FILE_SELECT_CODE:
				if (resultCode == RESULT_OK) {

					kmlFile =  data.getData();
					if(kmlFile!=null) {
						getIntent().setData(null);
						try {
							if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
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

	public  String getPath(Context context, Uri uri) throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		}
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}


}
