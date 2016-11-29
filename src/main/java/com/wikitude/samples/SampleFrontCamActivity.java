package com.wikitude.samples;

import com.wikitude.architect.StartupConfiguration.CameraPosition;
import com.wiret.arbrowser.ArchitectCamActivity;

/**
 * This sample will use CameraPosition.FRONT on startup.
 */
public class SampleFrontCamActivity extends ArchitectCamActivity {

	@Override
	protected CameraPosition getCameraPosition() {
		return CameraPosition.FRONT;
	}
}
