package com.physic3d.android;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.webkit.*;
import android.widget.*;

import com.physic3d.fwgslib.*;
import android.provider.Settings.Secure;

import org.json.*;
import java.io.*;

public class GameActivity extends Activity {

	protected static GameActivity mSingleton;
	protected static View mTextEdit;
	protected static ViewGroup mLayout;

	public static EngineSurface mSurface;
	public static String mArgv[];
	public static final int sdk = Integer.valueOf( Build.VERSION.SDK );
	public static final String TAG = "Physic3D:GameActivity";
	public static int mPixelFormat;
	public static JoystickHandler handler;
	public static ImmersiveMode mImmersiveMode;
	public static boolean keyboardVisible = false;
	public static boolean mEngineReady = false;
	public static boolean mEnginePaused = false;
	public static Vibrator mVibrator;
	public static boolean fMouseShown = true;
	public static boolean fGDBSafe = false;
	public static float mScale = 0.0f, mTouchScaleX = 1.0f, mTouchScaleY = 1.0f;
	public static int mForceHeight = 0, mForceWidth = 0;
	public static int mMinHeight = 240, mMinWidth = 320;
	public static boolean bIsCstrike = false;

	private static boolean mHasVibrator;
	private int mReturingWithResultCode = 0;
	private boolean mManageStorageRequested = false;

	private static int FPICKER_RESULT = 2;

	public final static byte JOY_HAT_CENTERED = 0;
	public final static byte JOY_HAT_UP       = 1;
	public final static byte JOY_HAT_RIGHT    = 2;
	public final static byte JOY_HAT_DOWN     = 4;
	public final static byte JOY_HAT_LEFT     = 8;

	public final static byte JOY_AXIS_SIDE  = 0;
	public final static byte JOY_AXIS_FWD   = 1;
	public final static byte JOY_AXIS_PITCH = 2;
	public final static byte JOY_AXIS_YAW   = 3;
	public final static byte JOY_AXIS_RT    = 4;
	public final static byte JOY_AXIS_LT    = 5;

	public static SharedPreferences mPref = null;
	private static boolean mUseVolume;
	public static View mDecorView;

	static boolean mNativeLoaded = false;
	static
	{
		try
		{
			System.loadLibrary( "physic3d" );
			mNativeLoaded = true;
		}
		catch( UnsatisfiedLinkError e )
		{
			Log.e( TAG, "Failed to load libphysic3d.so: " + e.getMessage() );
		}
	}

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		Log.v( TAG, "onCreate()" );
		super.onCreate( savedInstanceState );
		mEngineReady = false;

		mSingleton = this;

		requestWindowFeature( Window.FEATURE_NO_TITLE );

		int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN |
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags( flags, flags );

		mPref = this.getSharedPreferences( "engine", 0 );

		if( mPref.getBoolean( "folderask", true ) )
		{
			Log.v( TAG, "folderask == true. Opening FPicker..." );

			Intent intent = new Intent( this, FPicker.class );
			startActivityForResult( intent, FPICKER_RESULT );
		}
		else
		{
			Log.v( TAG, "folderask == false. Checking write permission..." );

			String basedir = FWGSLib.getStringExtraFromIntent( getIntent(), "basedir", mPref.getString( "basedir", FWGSLib.getDefaultPhysic3dPath() ) );
			checkWritePermission( basedir );
		}
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent resultData )
	{
		if( resultCode != RESULT_OK )
		{
			Log.v( TAG, "onActivityResult: result is not OK. ReqCode: " + requestCode + ". ResCode: " + resultCode );
		}
		else
		{
			mReturingWithResultCode = requestCode;
			if( requestCode == FPICKER_RESULT )
			{
				String newBaseDir = resultData.getStringExtra( "GetPath" );
				setNewBasedir( newBaseDir );
				setFolderAsk( this, false );
				Log.v( TAG, "Got new basedir from FPicker: " + newBaseDir );
			}
		}
	}

	@Override
	public void onPostResume()
	{
		super.onPostResume();

		if( mReturingWithResultCode != 0 )
		{
			if( mReturingWithResultCode == FPICKER_RESULT )
			{
				String basedir = mPref.getString( "basedir", FWGSLib.getDefaultPhysic3dPath() );
				checkWritePermission( basedir );
			}

			mReturingWithResultCode = 0;
		}
	}

	@Override
	protected void onPause() {
		Log.v( TAG, "onPause()" );

		if( mEngineReady )
		{
			nativeOnPause();
			mSurface.engineThreadWait();
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.v( TAG, "onResume()" );

		if( mManageStorageRequested )
		{
			mManageStorageRequested = false;
			if( Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager() )
			{
				new AlertDialog.Builder( this )
					.setTitle( R.string.write_failed )
					.setMessage( R.string.manage_storage_required )
					.setPositiveButton( R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick( DialogInterface dialog, int whichButton )
						{
							GameActivity.this.finish();
						}
					} )
					.setCancelable( false )
					.show();
				super.onResume();
				return;
			}
			String basedir = mPref.getString( "basedir", FWGSLib.getDefaultPhysic3dPath() );
			checkWritePermission( basedir );
			super.onResume();
			return;
		}

		if( mEngineReady )
		{
			nativeOnResume();
		}

		mEnginePaused = false;

		super.onResume();
	}

	@Override
	protected void onStop() {
		Log.v( TAG, "onStop()" );
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.v( TAG, "onDestroy()" );

		if( mEngineReady )
		{
			nativeUnPause();
			nativeOnDestroy();
			mSurface.engineThreadJoin();
		}

		super.onDestroy();
	}

	@Override
	public void onWindowFocusChanged( boolean hasFocus )
	{
		if( mEngineReady )
		{
			nativeOnFocusChange();
		}

		super.onWindowFocusChanged( hasFocus );

		if( mImmersiveMode != null )
		{
			mImmersiveMode.apply();
		}
	}

	public static void setFolderAsk( Context ctx, Boolean b )
	{
		SharedPreferences pref = ctx.getSharedPreferences( "engine", 0 );

		if( pref.getBoolean( "folderask", true ) == b )
			return;

		SharedPreferences.Editor editor = pref.edit();

		editor.putBoolean( "folderask", b );
		editor.commit();
	}

	private void setNewBasedir( String baseDir )
	{
		SharedPreferences.Editor editor = mPref.edit();

		editor.putString( "basedir", baseDir );
		editor.commit();
	}

	private DialogInterface.OnClickListener folderAskEnable = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick( DialogInterface dialog, int whichButton )
		{
			GameActivity act = GameActivity.this;
			act.setFolderAsk( GameActivity.this, true );
			act.finish();
		}
	};

	private void checkWritePermission( String basedir )
	{
		Log.v( TAG, "Checking write permissions..." );

		if( Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager() )
		{
			Log.v( TAG, "Requesting MANAGE_EXTERNAL_STORAGE permission..." );
			mManageStorageRequested = true;
			Intent intent = new Intent( Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION );
			intent.setData( Uri.parse( "package:" + getPackageName() ) );
			startActivity( intent );
			return;
		}

		launchSurfaceAndEngine();
	}

	private void launchSurfaceAndEngine()
	{
		Log.v( TAG, "Everything is OK. Launching engine..." );

		if( !setupEnvironment() )
		{
			finish();
			return;
		}
		InstallReceiver.extractPAK( this, false );

		mSurface = new EngineSurface( getApplication() );

		mLayout = new FrameLayout( this );
		mLayout.addView( mSurface );
		setContentView( mLayout );

		SurfaceHolder holder = mSurface.getHolder();
		holder.setType( SurfaceHolder.SURFACE_TYPE_GPU );

		if( sdk >= 14 )
			handler = new JoystickHandler_v14();
		else if( sdk >= 12 )
			handler = new JoystickHandler_v12();
		else
			handler = new JoystickHandler();
		handler.init();

		mVibrator = ( Vibrator )getSystemService( Context.VIBRATOR_SERVICE );
		mHasVibrator =  handler.hasVibrator() && (mVibrator != null);

		mPixelFormat = mPref.getInt( "pixelformat", 0 );
		mUseVolume = mPref.getBoolean( "usevolume", false );
		if( mPref.getBoolean( "enableResizeWorkaround", true ) )
			AndroidBug5497Workaround.assistActivity( this );

		Boolean enableImmersive = ( sdk >= 19 ) && ( mPref.getBoolean( "immersive_mode", true ) );
		if( enableImmersive )
			mImmersiveMode = new ImmersiveMode_v19();
		else mImmersiveMode = new ImmersiveMode();

		mDecorView = getWindow().getDecorView();

		if( mPref.getBoolean( "resolution_fixed", false ) )
		{
			if( mPref.getBoolean( "resolution_custom", false ) && !bIsCstrike )
			{
				mForceWidth = mPref.getInt( "resolution_width", 854 );
				mForceHeight = mPref.getInt( "resolution_height", 480 );
				if( mForceWidth < mMinWidth || mForceHeight < mMinHeight )
					mForceWidth = mForceHeight = 0;
			}
			else
			{
				mScale = mPref.getFloat( "resolution_scale", 1 );
				if( mScale < 0.5 )
					mScale = 0;

				DisplayMetrics metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);

				if( (float)metrics.widthPixels / mScale < (float)mMinWidth ||
					(float)metrics.heightPixels / mScale < (float)mMinHeight )
				{
					mScale = 0;
				}
			}
		}
		if( Build.VERSION.SDK_INT >= 33 )
		{
			if( checkSelfPermission( android.Manifest.permission.POST_NOTIFICATIONS ) != PackageManager.PERMISSION_GRANTED )
			{
				requestPermissions( new String[]{ android.Manifest.permission.POST_NOTIFICATIONS }, 1001 );
			}
		}
		startService( new Intent( getBaseContext(), GameService.class ) );
		mEngineReady = true;
	}

	private boolean setupEnvironment()
	{
		Intent intent = getIntent();
		final String enginedir = FWGSLib.getNativeLibDir( this );

		String argv       = FWGSLib.getStringExtraFromIntent( intent, "argv", mPref.getString( "argv", "-dev 3 -log" ) );
		String gamelibdir = FWGSLib.getStringExtraFromIntent( intent, "gamelibdir", enginedir );
		String gamedir    = FWGSLib.getStringExtraFromIntent( intent, "gamedir", "valve" );
		String basedir    = FWGSLib.getStringExtraFromIntent( intent, "basedir", mPref.getString( "basedir", FWGSLib.getDefaultPhysic3dPath() ) );
		String gdbsafe    = intent.getStringExtra( "gdbsafe" );

		bIsCstrike = ( gamedir.equals("cstrike") || gamedir.equals("czero") || gamedir.equals("czeror") );

		if( bIsCstrike )
		{
			mMinWidth = 640;
			mMinHeight = 300;

			final String allowed = "com.physic3d.android.cs16client";
		}

		if( gdbsafe != null || Debug.isDebuggerConnected() )
		{
			fGDBSafe = true;
			Log.e( TAG, "GDBSafe mode enabled!" );
		}

		mArgv = argv.split( " " );

		setenv( "XASH3D_BASEDIR", basedir, true );
		setenv( "XASH3D_ENGLIBDIR",  enginedir,  true );
		setenv( "XASH3D_GAMELIBDIR", gamelibdir, true );
		setenv( "XASH3D_GAMEDIR",    gamedir,    true );
		setenv( "XASH3D_EXTRAS_PAK1", getFilesDir().getPath() + "/extras.pak", true );

		String pakfile = intent.getStringExtra( "pakfile" );
		if( pakfile != null && pakfile != "" )
			setenv( "XASH3D_EXTRAS_PAK2", pakfile, true );

		String[] env = intent.getStringArrayExtra( "env" );
		if( env != null )
		{
			try
			{
				for( int i = 0; i + 1 < env.length; i += 2 )
				{
					setenv( env[i], env[i + 1], true );
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}

		return true;
	}

	// Native methods
	public static native int  nativeInit( Object arguments );
	public static native void nativeQuit();
	public static native void onNativeResize( int x, int y );
	public static native void nativeTouch( int pointerFingerId, int action, float x, float y );
	public static native void nativeKey( int down, int code );
	public static native void nativeString( String text );
	public static native void nativeSetPause( int pause );
	public static native void nativeOnDestroy();
	public static native void nativeOnResume();
	public static native void nativeOnFocusChange();
	public static native void nativeOnPause();
	public static native void nativeUnPause();
	public static native void nativeHat( int id, byte hat, byte keycode, boolean down );
	public static native void nativeAxis( int id, byte axis, short value );
	public static native void nativeJoyButton( int id, byte button, boolean down );
	public static native int  nativeTestWritePermission( String path );
	public static native void nativeMouseMove( float x, float y );
	public static native void nativeBall( int id, byte ball, short xrel, short yrel );
	public static native void nativeJoyAdd( int id );
	public static native void nativeJoyDel( int id );
	public static native int setenv( String key, String value, boolean overwrite );

	// JNI callbacks from C
	public static boolean createGLContext( int stencilBits )
	{
		return mSurface.InitGL(stencilBits);
	}

	public static int getGLAttribute( int attr )
	{
		return mSurface.getGLAttribute( attr );
	}

	public static void swapBuffers()
	{
		mSurface.SwapBuffers();
	}

	public static void engineThreadNotify()
	{
		mSurface.engineThreadNotify();
	}

	public static Surface getNativeSurface()
	{
		return GameActivity.mSurface.getNativeSurface();
	}

	public static void vibrate( int time )
	{
		if( mHasVibrator )
		{
			mVibrator.vibrate( time );
		}
	}

	public static void toggleEGL( int toggle )
	{
		mSurface.toggleEGL( toggle );
	}

	public static boolean deleteGLContext()
	{
		mSurface.ShutdownGL();
		return true;
	}

	public static Context getContext()
	{
		return mSingleton;
	}

	public static android.content.res.AssetManager getAssets( boolean isEngine )
	{
		return mSingleton.getAssets();
	}

	public static String[] getAssetsList( boolean isEngine, String path )
	{
		try
		{
			String[] list = mSingleton.getAssets().list( path );
			return list != null ? list : new String[0];
		}
		catch( Exception e )
		{
			return new String[0];
		}
	}

	protected final String[] messageboxData = new String[2];
	public static void messageBox( String title, String text )
	{
		mSingleton.messageboxData[0] = title;
		mSingleton.messageboxData[1] = text;
		mSingleton.runOnUiThread( new Runnable()
		{
			@Override
			public void run()
			{
				new AlertDialog.Builder( mSingleton )
					.setTitle( mSingleton.messageboxData[0] )
					.setMessage( mSingleton.messageboxData[1] )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener()
						{
							public void onClick( DialogInterface dialog, int whichButton )
							{
								synchronized( mSingleton.messageboxData )
								{
									mSingleton.messageboxData.notify();
								}
							}
						})
					.setCancelable( false )
					.show();
			}
		});
		synchronized( mSingleton.messageboxData )
		{
			try
			{
				mSingleton.messageboxData.wait();
			}
			catch( InterruptedException ex )
			{
				ex.printStackTrace();
			}
		}
	}

	public static boolean handleKey( int keyCode, KeyEvent event )
	{
		if( mUseVolume && ( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
			keyCode == KeyEvent.KEYCODE_VOLUME_UP ) )
			return false;

		final int source = GameActivity.handler.getSource( event );
		final int action = event.getAction();
		final boolean isGamePad  = FWGSLib.FExactBitSet( source, InputDevice.SOURCE_GAMEPAD );
		final boolean isJoystick = FWGSLib.FExactBitSet( source, InputDevice.SOURCE_CLASS_JOYSTICK );
		final boolean isDPad     = FWGSLib.FExactBitSet( source, InputDevice.SOURCE_DPAD );

		if( isDPad )
		{
			byte val;
			final byte hat = 0;
			final int id = 0;
			Log.d( TAG, "DPAD button: " + keyCode );
			switch( keyCode )
			{
			case KeyEvent.KEYCODE_DPAD_CENTER: val = JOY_HAT_CENTERED; break;
			case KeyEvent.KEYCODE_DPAD_UP:     val = JOY_HAT_UP;       break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:  val = JOY_HAT_RIGHT;    break;
			case KeyEvent.KEYCODE_DPAD_DOWN:   val = JOY_HAT_DOWN;     break;
			case KeyEvent.KEYCODE_DPAD_LEFT:   val = JOY_HAT_LEFT;     break;
			default:
				return performEngineKeyEvent( action, keyCode, event );
			}

			if( action == KeyEvent.ACTION_DOWN )
			{
				nativeHat( id, hat, val, true  );
				return true;
			}
			else if( action == KeyEvent.ACTION_UP )
			{
				nativeHat( id, hat, val, false );
				return true;
			}

			return false;
		}

		if( isGamePad || isJoystick || GameActivity.handler.isGamepadButton( keyCode ) )
		{
			final int id = 0;
			byte val = 15;

			switch( keyCode )
			{
			case KeyEvent.KEYCODE_BUTTON_A:      val = 0; break;
			case KeyEvent.KEYCODE_BUTTON_B:	     val = 1; break;
			case KeyEvent.KEYCODE_BUTTON_X:	     val = 2; break;
			case KeyEvent.KEYCODE_BUTTON_Y:	     val = 3; break;
			case KeyEvent.KEYCODE_BUTTON_L1:     val = 4; break;
			case KeyEvent.KEYCODE_BUTTON_R1:     val = 5; break;
			case KeyEvent.KEYCODE_BUTTON_SELECT: val = 6; break;
			case KeyEvent.KEYCODE_BUTTON_MODE:   val = 7; break;
			case KeyEvent.KEYCODE_BUTTON_START:  val = 8; break;
			case KeyEvent.KEYCODE_BUTTON_THUMBL: val = 9; break;
			case KeyEvent.KEYCODE_BUTTON_THUMBR: val = 10; break;

			case KeyEvent.KEYCODE_BUTTON_L2: val = 11; break;
			case KeyEvent.KEYCODE_BUTTON_R2: val = 12; break;
			case KeyEvent.KEYCODE_BUTTON_C:  val = 13; break;
			case KeyEvent.KEYCODE_BUTTON_Z:  val = 14; break;
			default:
				if( keyCode >= KeyEvent.KEYCODE_BUTTON_1 && keyCode <= KeyEvent.KEYCODE_BUTTON_16 )
				{
					val = ( byte )( ( keyCode - KeyEvent.KEYCODE_BUTTON_1 ) + 15);
				}
				else if( GameActivity.handler.isGamepadButton( keyCode ) )
				{
					Log.d( TAG, "Unhandled GamePad button: " + GameActivity.handler.keyCodeToString( keyCode ) );
					return false;
				}
				else
				{
					Log.d( TAG, "Unhandled GamePad button: " + GameActivity.handler.keyCodeToString( keyCode ) + ". Passed as simple key.");
					return performEngineKeyEvent( action, keyCode, event );
				}
			}

			if( action == KeyEvent.ACTION_DOWN )
			{
				nativeJoyButton( id, val, true );
				return true;
			}
			else if( action == KeyEvent.ACTION_UP )
			{
				nativeJoyButton( id, val, false );
				return true;
			}
			return false;
		}

		return performEngineKeyEvent( action, keyCode, event );
	}

	public static boolean performEngineKeyEvent( int action, int keyCode, KeyEvent event )
	{
		if( action == KeyEvent.ACTION_DOWN )
		{
			if( event.isPrintingKey() || keyCode == 62 )
				GameActivity.nativeString( String.valueOf( ( char )event.getUnicodeChar() ) );

			GameActivity.nativeKey( 1, keyCode );

			return true;
		}
		else if( action == KeyEvent.ACTION_UP )
		{
			GameActivity.nativeKey( 0, keyCode );

			return true;
		}
		return false;
	}

	public static float performEngineAxisEvent( float current, byte engineAxis, float prev, float flat )
	{
		if( prev != current )
		{
			final int id = 0;
			final short SHRT_MAX = 32767;
			if( current <= flat && current >= -flat )
				current = 0;

			nativeAxis( id, engineAxis, ( short )( current * SHRT_MAX ) );
		}

		return current;
	}

	public static float performEngineHatEvent( float curr, boolean isXAxis, float prev )
	{
		if( prev != curr )
		{
			final int id = 0;
			final byte hat = 0;
			if( isXAxis )
			{
				     if( curr > 0 ) nativeHat( id, hat, JOY_HAT_RIGHT, true );
				else if( curr < 0 ) nativeHat( id, hat, JOY_HAT_LEFT,  true );
				else if( prev > 0 ) nativeHat( id, hat, JOY_HAT_RIGHT, false );
				else if( prev < 0 ) nativeHat( id, hat, JOY_HAT_LEFT,  false );
			}
			else
			{
				     if( curr > 0 ) nativeHat( id, hat, JOY_HAT_DOWN, true );
				else if( curr < 0 ) nativeHat( id, hat, JOY_HAT_UP,   true );
				else if( prev > 0 ) nativeHat( id, hat, JOY_HAT_DOWN, false );
				else if( prev < 0 ) nativeHat( id, hat, JOY_HAT_UP,   false );
			}
		}
		return curr;
	}

	static class ShowTextInputTask implements Runnable
	{
		private int show;

		public ShowTextInputTask( int show1 )
		{
		   show = show1;
		}

		@Override
		public void run()
		{
			InputMethodManager imm = ( InputMethodManager )getContext().getSystemService( Context.INPUT_METHOD_SERVICE );

			if( mTextEdit == null )
			{
				mTextEdit = new DummyEdit( getContext() );
				mLayout.addView( mTextEdit );
			}
			if( show == 1 )
			{
				mTextEdit.setVisibility( View.VISIBLE );
				mTextEdit.requestFocus();

				imm.showSoftInput( mTextEdit, 0 );
				keyboardVisible = true;
				if( GameActivity.mImmersiveMode != null )
					GameActivity.mImmersiveMode.apply();
			}
			else
			{
				mTextEdit.setVisibility( View.GONE );
				imm.hideSoftInputFromWindow( mTextEdit.getWindowToken(), 0 );
				keyboardVisible = false;
				if( GameActivity.mImmersiveMode != null )
					GameActivity.mImmersiveMode.apply();
			}
		}
	}

	public static void showKeyboard( int show )
	{
		mSingleton.runOnUiThread( new ShowTextInputTask( show ) );
	}

	public static void setIcon( String path )
	{
		if( fGDBSafe )
			return;

		Log.v( TAG, "setIcon(" + path + ")" );

		try
		{
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;

			BitmapFactory.decodeFile( path, o );

			if( o.outWidth < 16 )
				return;

			GameService.notification.contentView.setImageViewUri( GameService.status_image, Uri.parse( "file://" + path ) );

			NotificationManager nm = ( NotificationManager )mSingleton.getApplicationContext().getSystemService( Context.NOTIFICATION_SERVICE );
			nm.notify( 100, GameService.notification );
		}
		catch( Exception e )
		{
		}
	}

	public static void setTitle( String title )
	{
		Log.v( TAG, "setTitle(" + title + ")" );
		SharedPreferences.Editor editor = mPref.edit();
		editor.putBoolean("successfulRun", true);
		editor.commit();
		GameService.notification.contentView.setTextViewText( GameService.status_text, title );
		NotificationManager nm = ( NotificationManager )mSingleton.getApplicationContext().getSystemService( Context.NOTIFICATION_SERVICE );
		nm.notify( 100, GameService.notification );
	}

	public static String getAndroidID()
	{
		String str = Secure.getString( mSingleton.getContentResolver(), Secure.ANDROID_ID );

		if( str == null )
			return "";

		return str;
	}

	public static String loadID()
	{
		return mPref.getString( "xash_id", "" );
	}

	public static void saveID( String id )
	{
		SharedPreferences.Editor editor = mPref.edit();

		editor.putString( "xash_id", id );
		editor.commit();
	}

	public static void showMouse( int show )
	{
		fMouseShown = show != 0;
		handler.showMouse( fMouseShown );
	}

	public static void GenericUpdatePage()
	{
		mSingleton.startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse("https://github.com/tyabus/xash3d/releases/latest" ) ) );
	}

	public static void PlatformUpdatePage()
	{
		GenericUpdatePage();
	}

	public static void shellExecute( String path )
	{
		if( path.equals("PlatformUpdatePage"))
		{
			PlatformUpdatePage();
			return;
		}
		else if( path.equals( "GenericUpdatePage" ))
		{
			GenericUpdatePage();
			return;
		}

		final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(path));
		mSingleton.startActivity(intent);
	}
}
