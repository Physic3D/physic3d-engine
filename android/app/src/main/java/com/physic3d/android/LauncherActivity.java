package com.physic3d.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.method.*;
import android.graphics.Typeface;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.physic3d.fwgslib.*;
import java.io.*;
import java.net.*;
import org.json.*;
import android.preference.*;

public class LauncherActivity extends AppCompatActivity
{
	public final static int sdk = FWGSLib.sdk;
	static SharedPreferences mPref;

	static EditText cmdArgs, resPath, resWidth, resHeight;
	static CompoundButton resizeWorkaround, immersiveMode, resolution;
	static Spinner pixelSpinner;
	static LinearLayout resolutionSettings;
	
	static int mEngineWidth, mEngineHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setTheme(R.style.AppTheme);
		setContentView(R.layout.activity_launcher);

		TabHost tabHost = (TabHost) findViewById(R.id.tabhost);

		tabHost.setup();
		
		TabHost.TabSpec tabSpec;
		tabSpec = tabHost.newTabSpec("tabtag1");
		tabSpec.setIndicator(getString(R.string.text_tab1));
		tabSpec.setContent(R.id.tab1);
		tabHost.addTab(tabSpec);

		tabSpec = tabHost.newTabSpec("tabtag2");
		tabSpec.setIndicator(getString(R.string.text_tab2));
		tabSpec.setContent(R.id.tab2);
		tabHost.addTab(tabSpec);

		TabWidget tw = tabHost.getTabWidget();
		tw.setDividerDrawable(null);
		tw.setBackgroundColor(0);
		for (int i = 0; i < tw.getChildCount(); i++)
		{
			View v = tw.getChildAt(i);
			v.setBackgroundResource(R.drawable.tab_tui_background);
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.setMargins(dp(4), 0, dp(4), 0);
			v.setLayoutParams(lp);
			if (v instanceof TextView)
			{
				((TextView) v).setAllCaps(false);
				((TextView) v).setTypeface(Typeface.DEFAULT, Typeface.BOLD);
				((TextView) v).setTextSize(14);
			}
		}
		updateTabColors(tw);
		tabHost.setCurrentTab(0);
		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				updateTabColors(tabHost.getTabWidget());
				new android.os.Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						View focused = getCurrentFocus();
						if( focused != null && focused instanceof EditText )
							focused.clearFocus();
					}
				}, 50);
			}
		});

		mPref        = getSharedPreferences("engine", 0);
		cmdArgs      = (EditText) findViewById(R.id.cmdArgs);
		resPath      = (EditText) findViewById( R.id.cmd_path );
		pixelSpinner = (Spinner) findViewById( R.id.pixelSpinner );
		resizeWorkaround = (CompoundButton) findViewById( R.id.enableResizeWorkaround );
		immersiveMode = (CompoundButton) findViewById( R.id.immersive_mode );
		resolution = (CompoundButton) findViewById(R.id.resolution);
		resWidth = (EditText) findViewById(R.id.resolution_width);
		resHeight = (EditText) findViewById(R.id.resolution_height);
		resolutionSettings = (LinearLayout) findViewById( R.id.resolution_settings );
		
		final String[] list = {
			"32 bit (RGBA8888)",
			"24 bit (RGB888)",
			"16 bit (RGB565)",
			"16 bit (RGBA5551)",
			"16 bit (RGBA4444)",
			"8 bit (RGB332)"
		};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pixelSpinner.setAdapter(adapter);

		Button selectFolderButton = (Button) findViewById(R.id.button_select);
		if (selectFolderButton != null) {
			selectFolderButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectFolder(v);
				}
			});
		}

		updatePath(mPref.getString("basedir", FWGSLib.getDefaultXashPath() ) );
		cmdArgs.setText(mPref.getString("argv","-dev 3 -log"));
		pixelSpinner.setSelection(mPref.getInt("pixelformat", 0));
		resizeWorkaround.setChecked(mPref.getBoolean("enableResizeWorkaround", true));
		resolution.setChecked( mPref.getBoolean("resolution_fixed", false ) );
		
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		// Swap resolution here, because engine is always(should be always) run in landscape mode
		if( FWGSLib.isLandscapeOrientation( this ) )
		{
			mEngineWidth = metrics.widthPixels;
			mEngineHeight = metrics.heightPixels;
		}
		else
		{
			mEngineWidth = metrics.heightPixels;
			mEngineHeight = metrics.widthPixels;
		}		
		
		resWidth.setText(String.valueOf(mPref.getInt("resolution_width", mEngineWidth )));
		resHeight.setText(String.valueOf(mPref.getInt("resolution_height", mEngineHeight )));
		
		resWidth.addTextChangedListener( resWidthTextChangeWatcher );
		resHeight.addTextChangedListener( resTextChangeWatcher );
		
		resolution.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged( CompoundButton v, boolean isChecked )
			{
				resolutionSettings.setVisibility( isChecked ? View.VISIBLE : View.GONE );
			}
		});
		
		if( sdk >= 19 )
		{
			immersiveMode.setChecked(mPref.getBoolean("immersive_mode", true));
		}
		else
		{
			immersiveMode.setVisibility(View.GONE); // not available
		}
		
		resPath.setOnFocusChangeListener( new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				updatePath( resPath.getText().toString() );
			}
		} );

		resolutionSettings.setVisibility( resolution.isChecked() ? View.VISIBLE : View.GONE );
		
		new android.os.Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				View focused = getCurrentFocus();
				if( focused != null && focused instanceof EditText )
					focused.clearFocus();
			}
		}, 50);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if( hasFocus )
		{
			new android.os.Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					View focused = getCurrentFocus();
					if( focused != null && focused instanceof EditText )
						focused.clearFocus();
				}
			}, 50);
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
	}

	void updatePath( String text )
	{
		resPath.setText(text);
	}

	void updateTabColors(TabWidget tw)
	{
		android.util.TypedValue typedValue = new android.util.TypedValue();
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
		int selectedColor = getResources().getColor(typedValue.resourceId);
		int unselectedColor = getResources().getColor(android.R.color.darker_gray);
		for (int i = 0; i < tw.getChildCount(); i++)
		{
			View v = tw.getChildAt(i);
			if (v instanceof TextView)
			{
				((TextView) v).setTextColor(v.isSelected() ? selectedColor : unselectedColor);
			}
		}
	}
	
	TextWatcher resWidthTextChangeWatcher = new TextWatcher()
	{
		@Override
		public void afterTextChanged(Editable s){}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after){}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			int h = (int)((float)mEngineHeight / mEngineWidth * getCustomEngineWidth());
			resHeight.setText(String.valueOf(h));
		}
	};

	TextWatcher resTextChangeWatcher = new TextWatcher()
	{
		@Override
		public void afterTextChanged(Editable s){}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after){}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}
	};
	
	
	int getCustomEngineHeight()
	{
		return FWGSLib.atoi( resHeight.getText().toString(), mEngineHeight );
	}
	
	int getCustomEngineWidth()
	{
		return FWGSLib.atoi( resWidth.getText().toString(), mEngineWidth );
	}
	
    public void startGame(View view)
    {
		Intent intent = new Intent(this, GameActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		SharedPreferences.Editor editor = mPref.edit();
		editor.putString("argv", cmdArgs.getText().toString());
		editor.putString("basedir", resPath.getText().toString());
		editor.putInt("pixelformat", pixelSpinner.getSelectedItemPosition());
		editor.putBoolean("enableResizeWorkaround",resizeWorkaround.isChecked());
		editor.putBoolean("resolution_fixed", resolution.isChecked());
		editor.putInt("resolution_width", getCustomEngineWidth() );
		editor.putInt("resolution_height", getCustomEngineHeight() );
		
		if( sdk >= 19 )
			editor.putBoolean("immersive_mode", immersiveMode.isChecked());
		else
			editor.putBoolean("immersive_mode", false); // just in case...
		editor.commit();
		startActivity(intent);
    }

	public void showAbout(View view)
	{
		final LauncherActivity a = this;
		this.runOnUiThread(new Runnable() 
		{
			public void run()
			{
				View dialogView = getLayoutInflater().inflate(R.layout.about, null);
				final AlertDialog dialog = new MaterialAlertDialogBuilder(a, R.style.TuiDialog)
					.setView(dialogView)
					.create();
				dialog.show();
				
				dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
				dialog.getWindow().setDimAmount(0.6f);

				((Button)dialogView.findViewById( R.id.button_about_ok )).setOnClickListener(new View.OnClickListener(){
	       			@Override
	    			public void onClick(View v) {
	    				dialog.dismiss();
					}
				});
			}
		});
	}

	public static final int ID_SELECT_FOLDER = 42;

	public void selectFolder(View view)
	{
		Intent intent = new Intent(this, FPicker.class);
		startActivityForResult(intent, ID_SELECT_FOLDER);
		resPath.setEnabled(false);
		try
		{
			GameActivity.setFolderAsk( this, false );
		}
		catch( LinkageError e ) {}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent resultData) 
	{
		super.onActivityResult(requestCode, resultCode, resultData);
		switch(requestCode)
		{
		case ID_SELECT_FOLDER:
		{
			if (resultCode == RESULT_OK) 
			{
				try	
				{
					if( resPath == null )
						return;
					updatePath(resultData.getStringExtra("GetPath"));
					resPath.setEnabled( true );
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			resPath.setEnabled(true);
			break;
		}
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    private int dp(int dp)
    {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
