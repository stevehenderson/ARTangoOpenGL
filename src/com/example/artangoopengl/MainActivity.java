package com.example.artangoopengl;

//import com.projecttango.experiments.javamotiontracking.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;


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
import android.widget.Toast;

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
	
	 /**
     * Set up the TangoConfig and the listeners for the Tango service, then begin using the Motion
     * Tracking API. This is called in response to the user clicking the 'Start' Button.
     */
    private void setTangoListeners() {
        // Lock configuration and connect to Tango
        // Select coordinate frame pair
        final ArrayList<TangoCoordinateFramePair> framePairs = 
                new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        // Listen for new Tango data
        mTango.connectListener(framePairs, new OnTangoUpdateListener() {

            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                // Log whenever Motion Tracking enters a n invalid state
                if (!mIsAutoRecovery && (pose.statusCode == TangoPoseData.POSE_INVALID)) {
                    Log.w(TAG, "Invalid State");
                }
                if (mPreviousPoseStatus != pose.statusCode) {
                    count = 0;
                }
                count++;
                mPreviousPoseStatus = pose.statusCode;
                mDeltaTime = (float) (pose.timestamp - mPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPreviousTimeStamp = (float) pose.timestamp;
                // Update the OpenGL renderable objects with the new Tango Pose
                // data
                float[] translation = pose.getTranslationAsFloats();
                
                //TODO:  Update OpenGL camera pose

                // Update the UI with TangoPose information
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DecimalFormat threeDec = new DecimalFormat("0.000");
                        String translationString = "[" + threeDec.format(pose.translation[0])
                                + ", " + threeDec.format(pose.translation[1]) + ", "
                                + threeDec.format(pose.translation[2]) + "] ";
                        String quaternionString = "[" + threeDec.format(pose.rotation[0]) + ", "
                                + threeDec.format(pose.rotation[1]) + ", "
                                + threeDec.format(pose.rotation[2]) + ", "
                                + threeDec.format(pose.rotation[3]) + "] ";

                        // Display pose data on screen in TextViews
                        mPoseTextView.setText(translationString);
                        mQuatTextView.setText(quaternionString);
                        mPoseCountTextView.setText(Integer.toString(count));
                        mDeltaTextView.setText(threeDec.format(mDeltaTime));
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mPoseStatusTextView.setText(R.string.pose_valid);
                        } else if (pose.statusCode == TangoPoseData.POSE_INVALID) {
                            mPoseStatusTextView.setText(R.string.pose_invalid);
                        } else if (pose.statusCode == TangoPoseData.POSE_INITIALIZING) {
                            mPoseStatusTextView.setText(R.string.pose_initializing);
                        } else if (pose.statusCode == TangoPoseData.POSE_UNKNOWN) {
                            mPoseStatusTextView.setText(R.string.pose_unknown);
                        }
                    }
                });
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0) {
                // We are not using TangoXyzIjData for this application
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTangoEventTextView.setText(event.eventKey + ": " + event.eventValue);
                    }
                });
            }
        });
    }

	@Override
	protected void onPause() {
		if ( mWL.isHeld() )
			mWL.release();
		mView.onPause();
		super.onPause();
		try {
            mTango.disconnect();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
		mView.onResume();
		mWL.acquire();
		try {
            setTangoListeners();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), R.string.motiontrackingpermission,
                    Toast.LENGTH_SHORT).show();
        }
        try {
            mTango.connect(mConfig);
        } catch (TangoOutOfDateException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoOutOfDateException,
                    Toast.LENGTH_SHORT).show();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
        }
        try {
            setUpExtrinsics();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), R.string.motiontrackingpermission,
                    Toast.LENGTH_SHORT).show();
        }
	}
	
	private void setUpExtrinsics() {
        // Get device to imu matrix.
        TangoPoseData device2IMUPose = new TangoPoseData();
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
       // mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
       //         device2IMUPose.getTranslationAsFloats(), device2IMUPose.getRotationAsFloats());

        // Get color camera to imu matrix.
        TangoPoseData color2IMUPose = new TangoPoseData();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        color2IMUPose = mTango.getPoseAtTime(0.0, framePair);

       // mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
        //        color2IMUPose.getTranslationAsFloats(), color2IMUPose.getRotationAsFloats());
    }
}
