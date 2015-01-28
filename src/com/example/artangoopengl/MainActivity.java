package com.example.artangoopengl;

//import com.projecttango.experiments.javamotiontracking.R;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int SECS_TO_MILLISECS = 1000;
	private Tango mTango;
    private TangoConfig mConfig;
	private MainView mView;
	private WakeLock mWL;
	private TextView mDeltaTextView;
    private TextView mPoseCountTextView;
    private TextView mPoseTextView;
    private TextView mQuatTextView;
    private TextView mPoseStatusTextView;
    private TextView mTangoServiceVersionTextView;
    private TextView mApplicationVersionTextView;
    private TextView mTangoEventTextView;
    private Button mMotionResetButton;
    private float mPreviousTimeStamp;
    private int mPreviousPoseStatus;
    private int count;
    private float mDeltaTime;
    private boolean mIsAutoRecovery;
    

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
                Tango.TANGO_INTENT_ACTIVITYCODE);
		// full screen & full brightness
		requestWindowFeature ( Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mWL = ((PowerManager)getSystemService ( Context.POWER_SERVICE )).newWakeLock(PowerManager.FULL_WAKE_LOCK, "WakeLock");
		mWL.acquire();
		mView = new MainView(this);
		setContentView(R.layout.activity_main);
		//setContentView ( mView );
		
		//YO!!  PLAY WITH THIS LATER
		mIsAutoRecovery = false;
		
		
		// Text views for displaying translation and rotation data
        mPoseTextView = (TextView) findViewById(R.id.pose);
        mQuatTextView = (TextView) findViewById(R.id.quat);
        mPoseCountTextView = (TextView) findViewById(R.id.posecount);
        mDeltaTextView = (TextView) findViewById(R.id.deltatime);
        mTangoEventTextView = (TextView) findViewById(R.id.tangoevent);
        
        
        // Buttons for selecting camera view and Set up button click listeners
        //findViewById(R.id.first_person_button).setOnClickListener(this);
        //findViewById(R.id.third_person_button).setOnClickListener(this);
        //findViewById(R.id.top_down_button).setOnClickListener(this);

        // Button to reset motion tracking
        mMotionResetButton = (Button) findViewById(R.id.resetmotion);

        // Text views for the status of the pose data and Tango library versions
        mPoseStatusTextView = (TextView) findViewById(R.id.status);
        mTangoServiceVersionTextView = (TextView) findViewById(R.id.version);
        mApplicationVersionTextView = (TextView) findViewById(R.id.appversion);
        
        // Instantiate the Tango service
        mTango = new Tango(this);
        // Create a new Tango Configuration and enable the MotionTrackingActivity API
        mConfig = new TangoConfig();
        mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        
        // The Auto-Recovery ToggleButton sets a boolean variable to determine
        // if the
        // Tango service should automatically attempt to recover when
        // / MotionTrackingActivity enters an invalid state.
        if (mIsAutoRecovery) {
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
            Log.i(TAG, "Auto Reset On!!!");
        } else {
            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, false);
            Log.i(TAG, "Auto Reset Off!!!");
        }

        PackageInfo packageInfo;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            mApplicationVersionTextView.setText(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Display the library version for debug purposes
        mTangoServiceVersionTextView.setText(mConfig.getString("tango_service_library_version"));
	}

	@Override
	protected void onPause() {
		if ( mWL.isHeld() )
			mWL.release();
		mView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mView.onResume();
		mWL.acquire();
	}
}
