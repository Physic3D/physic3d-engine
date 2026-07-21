package com.physic3d.android;

import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import com.physic3d.fwgslib.*;
import org.json.*;
import java.io.*;

public class LauncherActivity extends AppCompatActivity
{
	static SharedPreferences mPref;
	WebView mWebView;
	int mEngineWidth, mEngineHeight;
	int mSdk = FWGSLib.sdk;

	public static final int ID_SELECT_FOLDER = 42;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mPref = getSharedPreferences("engine", 0);

		WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
		mEngineWidth = metrics.getBounds().width();
		mEngineHeight = metrics.getBounds().height();

		if (FWGSLib.isLandscapeOrientation(this))
		{
			int tmp = mEngineWidth;
			mEngineWidth = mEngineHeight;
			mEngineHeight = tmp;
		}

		mWebView = new WebView(this);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setBackgroundColor(0xFF0F1215);
		mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mWebView.addJavascriptInterface(new JSInterface(), "Android");
		mWebView.loadUrl("file:///android_asset/launcher/index.html");

		setContentView(mWebView);
	}

	public class JSInterface
	{
		@JavascriptInterface
		public String getSettings()
		{
			try
			{
				JSONObject obj = new JSONObject();
				obj.put("basedir", mPref.getString("basedir", FWGSLib.getDefaultPhysic3dPath()));
				obj.put("argv", mPref.getString("argv", "-dev 3 -log"));
				obj.put("pixelformat", mPref.getInt("pixelformat", 0));
				obj.put("resizeWorkaround", mPref.getBoolean("enableResizeWorkaround", true));
				obj.put("immersiveMode", mPref.getBoolean("immersive_mode", true));
				obj.put("resolutionFixed", mPref.getBoolean("resolution_fixed", false));
				obj.put("resWidth", mPref.getInt("resolution_width", mEngineWidth));
				obj.put("resHeight", mPref.getInt("resolution_height", mEngineHeight));
				obj.put("deviceWidth", mEngineWidth);
				obj.put("deviceHeight", mEngineHeight);
				obj.put("sdk", mSdk);
				return obj.toString();
			}
			catch (JSONException e)
			{
				return "{}";
			}
		}

		@JavascriptInterface
		public boolean validatePath(String path)
		{
			if (path == null || path.isEmpty())
				return false;

			File dir = new File(path);
			if (!dir.exists() || !dir.isDirectory())
				return false;

			File valveDir = new File(dir, "valve");
			return valveDir.exists() && valveDir.isDirectory();
		}

		@JavascriptInterface
		public void launchGame(String settingsJson)
		{
			try
			{
				JSONObject s = new JSONObject(settingsJson);
				String basedir = s.optString("basedir", "");

				if (basedir.isEmpty())
					return;

				SharedPreferences.Editor editor = mPref.edit();
				editor.putString("basedir", basedir);
				editor.putBoolean("folderask", false);
				editor.putString("argv", s.optString("argv", "-dev 3 -log"));
				editor.putInt("pixelformat", s.optInt("pixelformat", 0));
				editor.putBoolean("enableResizeWorkaround", s.optBoolean("resizeWorkaround", true));
				editor.putBoolean("resolution_fixed", s.optBoolean("resolutionFixed", false));
				editor.putInt("resolution_width", s.optInt("resWidth", mEngineWidth));
				editor.putInt("resolution_height", s.optInt("resHeight", mEngineHeight));

				if (mSdk >= 19)
					editor.putBoolean("immersive_mode", s.optBoolean("immersiveMode", true));
				else
					editor.putBoolean("immersive_mode", false);
				editor.apply();

				Intent intent = new Intent(LauncherActivity.this, GameActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			catch (JSONException e)
			{
				Log.e("Physic3D", "launchGame failed", e);
			}
		}

		@JavascriptInterface
		public void pickFolder()
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Intent intent = new Intent(LauncherActivity.this, FPicker.class);
					startActivityForResult(intent, ID_SELECT_FOLDER);
				}
			});
		}

		@JavascriptInterface
		public void openUrl(String url)
		{
			try
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
				startActivity(intent);
			}
			catch (Exception e)
			{
				Log.e("Physic3D", "openUrl failed", e);
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		super.onActivityResult(requestCode, resultCode, resultData);
		if (requestCode == ID_SELECT_FOLDER && resultCode == RESULT_OK && resultData != null)
		{
			String path = resultData.getStringExtra("GetPath");
			if (path != null)
			{
				String escaped = path.replace("\\", "\\\\").replace("'", "\\'");
				mWebView.evaluateJavascript("javascript:onFolderPicked('" + escaped + "')", null);
			}
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}
}
