package com.wikitude.samples;

import android.content.res.Configuration;
import android.os.Bundle;

import com.wiret.arbrowser.ArchitectCamActivity;

public class MarkerTrackingPluginActivity extends ArchitectCamActivity {

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        this.architectView.registerNativePlugins("wikitudePlugins", "markertracking");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
