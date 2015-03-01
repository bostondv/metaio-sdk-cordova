// Copyright 2007-2013 metaio GmbH. All rights reserved.
package  com.pomelodesign.cordova.metaio;

import android.view.KeyEvent;
import com.metaio.sdk.ARELActivity;

public class ARELViewActivity extends ARELActivity {

	private static ARELViewActivity _instance;
	
	@Override
	protected int getGUILayout() {
		return 0;
	}

	protected void onStart() 
	{
		super.onStart();
		_instance = this;
	}
	
	public static void removeARELView()
	{
		_instance.finish();
	}
	
	@Override
	public void onDestroy()
	{
	    if (_instance.mWebView != null)
	    {
	    	_instance.mWebView.removeAllViews();
	    	_instance.mWebView.destroy();
	    }
	    super.onDestroy();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{

	     if (keyCode == KeyEvent.KEYCODE_BACK)
	     {
	         return true;
	     }

	    return false;
	}

}
