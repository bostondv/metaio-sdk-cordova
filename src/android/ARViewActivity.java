// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.sdk;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.metaio.sdk.jni.CameraVector;
import com.metaio.sdk.jni.ERENDER_SYSTEM;
import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.MetaioSDK;
import com.metaio.tools.Memory;
import com.metaio.tools.Screen;
import com.metaio.tools.SystemInfo;
import com.metaio.sdk.jni.Vector2di;

import com.pomelodesign.cordova.metaio.MetaioPlugin;

/**
 * This is base activity to use metaio SDK. It creates metaio GLSurfaceView and
 * handle all its callbacks and lifecycle. 
 * 
 */ 
public abstract class ARViewActivity extends Activity implements MetaioSurfaceView.Callback, OnTouchListener
{    
	protected static boolean nativeLibsLoaded;
	
	static 
	{     
		nativeLibsLoaded = IMetaioSDKAndroid.loadNativeLibs();
	}

	/**
	 * Defines whether the activity is currently paused
	 */
	protected boolean mActivityIsPaused;

	/**
	 * Sensor manager
	 */
	protected SensorsComponentAndroid mSensors;

	/**
	 * metaio SurfaceView
	 */
	protected MetaioSurfaceView mSurfaceView;

	/**
	 * GUI overlay, only valid in onStart and if a resource is provided in getGUILayout.
	 */
	protected View mGUIView;

	/**
	 * metaio SDK object
	 */
	protected IMetaioSDKAndroid metaioSDK;

	/** 
	 * flag for the renderer
	 */
	protected boolean mRendererInitialized;

	/**
	 * Camera object
	 */
	protected Camera mCamera;

	/**
	 * Provide resource for GUI overlay if required.
	 * <p> The resource is inflated into mGUIView which is added in onStart
	 * @return Resource ID of the GUI view
	 */
	protected abstract int getGUILayout();
	
	/**
	 * Provide metaio SDK callback handler if desired. 
	 * @see IMetaioSDKCallback
	 * 
	 * @return Return metaio SDK callback handler
	 */
	protected abstract IMetaioSDKCallback getMetaioSDKCallbackHandler();
	
	/**
	 * Load rendering contents to metaio SDK in this method, e.g. 3D models,
	 * environment map etc..
	 */
	protected abstract void loadContents();
	
	/**
	 * Called when a geometry is touched.
	 * 
	 * @param geometry Geometry that is touched
	 */
	protected abstract void onGeometryTouched(IGeometry geometry);
	
	/**
	 * Start camera. Override this to change camera or its parameters such as
	 * resolution, image flip or frame rate
	 */
	protected void startCamera()
	{
		final CameraVector cameras = metaioSDK.getCameraList();
		if (!cameras.isEmpty())
		{
			com.metaio.sdk.jni.Camera camera = cameras.get(0);
			Vector2di cameraResolution = new Vector2di(1280, 1280);
			camera.setResolution(cameraResolution);
			metaioSDK.startCamera(camera);
		}
		else
		{
			MetaioDebug.log(Log.WARN, "No camera found on the device!");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		MetaioDebug.log("ARViewActivity.onCreate");

		metaioSDK = null;
		mSurfaceView = null;
		mRendererInitialized = false;
		
		try
		{
			if (!nativeLibsLoaded)
				throw new Exception("Unsupported platform, failed to load the native libs");
			
			MetaioDebug.log(Log.INFO, "metaioSDK libs loaded for "+SystemInfo.getDeviceABI()+" using "+SystemInfo.getNumCPUCores()+" CPU cores");
			
			// Create sensors component
			mSensors = new SensorsComponentAndroid(getApplicationContext());
			
			// Create metaio SDK by passing the Activity instance and the application signature
			metaioSDK = MetaioSDK.CreateMetaioSDKAndroid(this, getResources().getString(R.string.metaioSDKSignature));
			metaioSDK.registerSensorsComponent(mSensors);
			
			// Inflate GUI view if provided
			final int layout = getGUILayout(); 
			if (layout != 0)
			{
				mGUIView = View.inflate(this, layout, null);
				if (mGUIView == null)
					MetaioDebug.log(Log.ERROR, "ARViewActivity: error inflating the given layout: "+layout);
			}
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onCreate: failed to create or intialize metaio SDK: "+e.getMessage());
			finish();
		}
		
	}
	
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		MetaioDebug.log("ARViewActivity.onStart");
		
		try 
		{
			// Set empty content view
			setContentView(new FrameLayout(this));
			
			// Start camera only when the activity starts the first time
			// (see lifecycle: http://developer.android.com/training/basics/activity-lifecycle/pausing.html)
			if (!mActivityIsPaused)
				startCamera();
		} 
		catch (Exception e) 
		{
			MetaioDebug.log(Log.ERROR, "Error creating views: "+e.getMessage());
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		MetaioDebug.log("ARViewActivity.onPause");
		
		// pause the OpenGL surface
		if (mSurfaceView != null)
			mSurfaceView.onPause();
		
		mActivityIsPaused = true;
		metaioSDK.pause();		
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onResume() 
	{
		super.onResume();
		MetaioDebug.log("ARViewActivity.onResume");
		
		metaioSDK.resume();
		mActivityIsPaused = false;

		// Create GLSurfaceView if not yet created
		if (mSurfaceView == null)
		{
			MetaioDebug.log("ARViewActivity.onResume: creating new MetaioSurfaceView");
			mSurfaceView = new MetaioSurfaceView(this);
			mSurfaceView.registerCallback(this);
			mSurfaceView.setKeepScreenOn(true);
			mSurfaceView.setOnTouchListener(this);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			{
				mSurfaceView.setPreserveEGLContextOnPause(true);
			}
		}

		// Add rendering surface view here to ensure it is placed on top of the camera view. Using
		// bringToFront() will not work because SurfaceViews seem to disobey the View order within
		// their parent, but instead the order of adding them is significant.
		if (mSurfaceView.getParent() == null)
		{
			MetaioDebug.log("ARViewActivity.onResume: addContentView(mSurfaceView)");
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			addContentView(mSurfaceView, params);
			mSurfaceView.setZOrderMediaOverlay(true);
			
			// If GUI view is inflated, add it
	   		if (mGUIView != null)
	   		{
	   			if (mGUIView.getParent() == null)
	   			{
	   				MetaioDebug.log("ARViewActivity.onResume: addContentView(mGUIView)");
	   				addContentView(mGUIView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	   			}
		   		mGUIView.bringToFront();
	   		}
		}

		// make sure to resume the OpenGL surface
		mSurfaceView.onResume();
		
	}

	@Override
	protected void onStop() 
	{
		super.onStop();
		MetaioDebug.log("ARViewActivity.onStop");
	} 

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		MetaioDebug.log("ARViewActivity.onDestroy");

		if (metaioSDK != null) 
		{
			metaioSDK.delete();
			metaioSDK = null;
		}
		
		MetaioDebug.log("ARViewActivity.onDestroy releasing sensors");
		if (mSensors != null)
		{
			mSensors.registerCallback(null);
			mSensors.release();
			mSensors.delete();
			mSensors = null;
		}
		
		ViewGroup v = (ViewGroup) findViewById(android.R.id.content);
		Memory.unbindViews(v);
		v.removeAllViews();
		mSurfaceView = null;
		
		System.runFinalization();
		System.gc();
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);
		
		final ESCREEN_ROTATION rotation = Screen.getRotation(this);
		metaioSDK.setScreenRotation(rotation);
		
		MetaioDebug.log("onConfigurationChanged: "+rotation);
	}
	

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_UP) 
		{
			MetaioDebug.log("ARViewActivity touched at: "+event.toString());
			
			try
			{
				
				final int x = (int) event.getX();
				final int y = (int) event.getY();
				
				// ask the SDK if a geometry has been hit
				IGeometry geometry = metaioSDK.getGeometryFromViewportCoordinates(x, y, true);
				if (geometry != null) 
				{
					MetaioDebug.log("ARViewActivity geometry found: "+geometry);
					onGeometryTouched(geometry);
				}
				
			}
			catch (Exception e)
			{
				MetaioDebug.log(Log.ERROR, "onTouch: "+e.getMessage());
				MetaioDebug.printStackTrace(Log.ERROR, e);
			}
			
		}

		return true;
	}
	

	@Override
	public void onSurfaceCreated() 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceCreated: "+mSurfaceView);
		MetaioDebug.log("ARViewActivity.onSurfaceCreated: GL thread: "+Thread.currentThread().getId());
		try
		{
			// initialize the renderer
			if(!mRendererInitialized)
			{
				MetaioDebug.log("ARViewActivity.onSurfaceCreated: initializing renderer...");
				final ESCREEN_ROTATION rotation = Screen.getRotation(this);
				metaioSDK.setScreenRotation(rotation);
				metaioSDK.initializeRenderer(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
						rotation, ERENDER_SYSTEM.ERENDER_SYSTEM_OPENGL_ES_2_0 );
				mRendererInitialized = true;

				final IMetaioSDKCallback callback = getMetaioSDKCallbackHandler();
				if (callback != null)
					metaioSDK.registerCallback(callback);

				// Add loadContent to the event queue to allow rendering to start 
				mSurfaceView.queueEvent(new Runnable() 
				{
					@Override
					public void run() 
					{
						loadContents();
					}
				});
			}
			else
			{
				MetaioDebug.log("ARViewActivity.onSurfaceCreated: Reloading OpenGL resources...");
				metaioSDK.reloadOpenGLResources();
			}

			// Setup auto-focus

			mCamera = IMetaioSDKAndroid.getCamera(this);
			Camera.Parameters params = mCamera.getParameters();

		    List<String> focusModes = params.getSupportedFocusModes();
		    String CAF_PICTURE = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
		           CAF_VIDEO = Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
		           supportedMode = focusModes
		                  .contains(CAF_PICTURE) ? CAF_PICTURE : focusModes
		                  .contains(CAF_VIDEO) ? CAF_VIDEO : "";

		    if (!supportedMode.equals("")) {
		        params.setFocusMode(supportedMode);
		        mCamera.setParameters(params);
		    } else {
		    	Log.d("HARDWARE", "Auto focus mode not suported.");
		    }

		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onSurfaceCreated: "+e.getMessage());
		}
 	}

 
	@Override
	public void onDrawFrame() 
	{
		try 
		{
			// render the metaio SDK contents
			metaioSDK.render();
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onDrawFrame: Rendering failed with error "+e.getMessage());
		}
	}

	
	@Override
	public void onSurfaceDestroyed() 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceDestroyed: "+mSurfaceView);
		mSurfaceView = null;
	}

	@Override
	public void onSurfaceChanged(int width, int height) 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceChanged: "+width+", "+height);
		
		// resize renderer viewport
		metaioSDK.resizeRenderer(width, height);
	}
	
	@Override
	public void onLowMemory() 
	{	
		MetaioDebug.log(Log.ERROR, "Low memory");
		MetaioDebug.logMemory(getApplicationContext());
	}
	
}
