// Metaio Cordova Plugin
// (c) Boston Dell-Vandenberg <boston@pomelodesign.com>
// MetaioPlugin.h may be freely distributed under the MIT license.

package com.pomelodesign.cordova.metaio;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;

import android.content.Context;
import android.view.KeyEvent;

public class MetaioCordovaWebView extends CordovaWebView {

	public MetaioCordovaWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
  /*
   * onKeyDown
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    if (keyCode == KeyEvent.KEYCODE_BACK)
    {
      return true;
    }

    return super.onKeyDown(keyCode, event);
  	
  }

}
