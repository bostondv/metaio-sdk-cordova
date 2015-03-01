package  com.pomelodesign.cordova.metaio;

import java.io.File;
import java.io.IOException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import com.pomelodesign.cordova.metaio.ARELViewActivity;
import com.bullsitoy.scanit.BuildConfig;
import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

public class MetaioPlugin extends CordovaPlugin {

	private static CordovaInterface context;
	private static MetaioPlugin mPlugin = null;
	private AssetsExtracter mTask;
  private String Url;
  private Boolean loaded;
  private Intent cordovaIntent;
  private Intent metaioIntent;
	
	private static String arelConfigPath = null;

  public static MetaioPlugin getPlugin()
  {
  	return mPlugin;
  }
  
  public static int getSignature()
  {
  	return mPlugin.cordova.getActivity().getResources().getIdentifier("metaioSDKSignature", "string", mPlugin.cordova.getActivity().getClass().getPackage().getName());
  }

  public static void setPlugin(MetaioPlugin plugin)
  {
  	mPlugin = plugin;
  }

  public static Context getContext()
  {
      return mPlugin.cordova.getActivity();
  }

  public static void setContext(CordovaInterface context)
  {
  	MetaioPlugin.context = context;
  }
	    
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView)
  {
		super.initialize(cordova, webView);
		
		// Enable metaio SDK debug log messages based on build configuration
    MetaioDebug.enableLogging(BuildConfig.DEBUG);
		
		if (mPlugin == null)
			MetaioPlugin.setPlugin(this);
		
		MetaioPlugin.arelConfigPath = GetConfigFilePath(this.cordova.getActivity());
		
		if (MetaioPlugin.arelConfigPath == null)
			MetaioPlugin.arelConfigPath = "www/metaio/arelConfig.xml";
			
		
	}
	
	
	public void onReceive(Context context, Intent intent)
  {
	}
	

  public String GetConfigFilePath(Activity action)
  {
    if (action == null) {
        return null;
    }

    
    int id = action.getResources().getIdentifier("config", "xml", action.getClass().getPackage().getName());
    if (id == 0) {
        id = action.getResources().getIdentifier("cordova", "xml", action.getPackageName());
        return null;
    }
    if (id == 0) {
        return null;
    }

    XmlResourceParser xml = action.getResources().getXml(id);
    int eventType = -1;
    while (eventType != XmlResourceParser.END_DOCUMENT) {
        if (eventType == XmlResourceParser.START_TAG) {
            String strNode = xml.getName();

            if (strNode.equals("preference")) {
                String name = xml.getAttributeValue(null, "name").toLowerCase(Locale.getDefault());
                if (name.equalsIgnoreCase("arelConfigPath")) {
                    String arelConfigPath = xml.getAttributeValue(null, "value");
                    return arelConfigPath;
                }
            }
        }
        try {
            eventType = xml.next();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    return null;
  }


	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		if (action.equals("open")) {
			
      MetaioDebug.log("Metaio open...");

      try {

  		cordova.getActivity().runOnUiThread(
  		  new Runnable() {
	          public void run() {
	            if (mTask == null) {
	              mTask = new AssetsExtracter();
	              mTask.execute(0);
	            } else {
	              mTask.onPostExecute(true);
	            }
	            callbackContext.success();
	          }
          }
  		);
  		
  		loaded = true;
  		return true;

      } catch (Exception e) {

        MetaioDebug.log(Log.ERROR, "Error opening metaio: " + e.getMessage());
        MetaioDebug.printStackTrace(Log.ERROR, e);
        callbackContext.error("Error opening metaio");
        return false;

      }

		} else if (action.compareTo("destroy") == 0) {

    	MetaioDebug.log("Metaio destroy...");

      try {
    	  
    	  if (loaded == true) {
    		  cordova.getActivity().runOnUiThread(
	            new Runnable() {
	                public void run() {
	                	ARELViewActivity.removeARELView();
	                	loaded = false;
	                	callbackContext.success();
	                }
	            }
	        );
    		  
    		  return true;
    		  
    	  }
    	  
    	  callbackContext.error("Error detroying metaio, metaio not loaded");
          return true;

      } catch (Exception e) {

        MetaioDebug.log(Log.ERROR, "Error destroying metaio: " + e.getMessage());
        MetaioDebug.printStackTrace(Log.ERROR, e);
        callbackContext.error("Error detroying metaio");
        return false;

      }

    } else if (action.compareTo("close") == 0) {

      MetaioDebug.log("Metaio close...");

      try {

    	if (cordovaIntent == null) {
    		cordovaIntent = new Intent(MetaioPlugin.getContext().getApplicationContext(), this.cordova.getClass());
    	}
    	cordovaIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        MetaioPlugin.getContext().startActivity(cordovaIntent);

        Url =  args.getString(0);

         cordova.getActivity().runOnUiThread(new Runnable() {
          public void run() {
            String url = Url;
            if (url != null && url.length() > 0 && url.compareTo("null") != 0) {
              mPlugin.webView.loadUrl("javascript:cordova.fireDocumentEvent('metaioclose', { 'detail': '" + url + "' })");
            } else {
              mPlugin.webView.loadUrl("javascript:cordova.fireDocumentEvent('metaioclose')");
            }
          }
        });
         
        callbackContext.success();
   		return true;

      } catch (Exception e) {

        MetaioDebug.log(Log.ERROR, "Error hiding metaio: " + e.getMessage());
        MetaioDebug.printStackTrace(Log.ERROR, e);
        callbackContext.error("Error closing metaio");
        return false;

      }

    }

		return false;

	}	
	
	/**
	 * This task extracts all the assets to an external or internal location
	 * to make them accessible to metaio SDK
	 */
	private class AssetsExtracter extends AsyncTask<Integer, Integer, Boolean>
	{

		@Override
		protected void onPreExecute() 
		{
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) 
		{
			
			try 
			{
				// Extract all assets and overwrite existing files if debug build
        String[] ignoreList = {""};
        AssetsManager.extractAllAssets(MetaioPlugin.getContext().getApplicationContext(), "www/metaio", ignoreList, BuildConfig.DEBUG);
			} 
			catch (IOException e) 
			{
				MetaioDebug.log(Log.ERROR, "Error extracting assets: "+e.getMessage());
				MetaioDebug.printStackTrace(Log.ERROR, e);
				return false;
			}
			
			return true;
		}
		
	
		@Override
		protected void onPostExecute(Boolean result) 
		{

			// create AREL template and present it
				final String arelConfigFilePath = AssetsManager.getAssetPath(MetaioPlugin.arelConfigPath);//"AssetsManager.getAssetPath("arelConfig.xml");
				MetaioDebug.log("arelConfig to be passed to intent: "+arelConfigFilePath);
				
				if (metaioIntent == null) {
					metaioIntent = new Intent(MetaioPlugin.getContext().getApplicationContext(), ARELViewActivity.class);
					metaioIntent.putExtra(MetaioPlugin.getContext().getPackageName()+".AREL_SCENE", arelConfigFilePath);
				}
				
				metaioIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				MetaioPlugin.getContext().startActivity(metaioIntent);
				
	    }
		
	}

}
