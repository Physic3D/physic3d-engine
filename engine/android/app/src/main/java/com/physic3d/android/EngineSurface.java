package com.physic3d.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.*;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

class GameMain implements Runnable
{
	public void run()
	{
		GameActivity.nativeInit( GameActivity.mArgv );
	}
}

class EngineSurface extends SurfaceView implements SurfaceHolder.Callback, View.OnKeyListener
{
	public static final String TAG = "Physic3D-EngineSurface";

	private static Thread mEngThread = null;
	private static Object mPauseLock = new Object();

	private EGLContext  mEGLContext;
	private EGLSurface  mEGLSurface;
	private EGLDisplay  mEGLDisplay;
	private EGL10 mEGL;
	private EGLConfig mEGLConfig;
	private boolean resizing = false;

	public EngineSurface( Context context )
	{
		super( context );
		getHolder().addCallback( this );

		setFocusable( true );
		setFocusableInTouchMode( true );
		requestFocus();
		setOnKeyListener( this );
		setOnTouchListener( new EngineTouchListener() );
	}

	public void surfaceCreated( SurfaceHolder holder )
	{
		Log.v( TAG, "surfaceCreated()" );

		if( mEGL == null )
			return;

		GameActivity.nativeSetPause( 0 );
		GameActivity.mEnginePaused = false;
	}

	public void surfaceDestroyed( SurfaceHolder holder )
	{
		Log.v( TAG, "surfaceDestroyed()" );

		if( mEGL == null )
			return;

		GameActivity.nativeSetPause(1);
	}

	public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
	{
		Log.v( TAG, "surfaceChanged()" );
		if( ( GameActivity.mForceHeight!= 0 && GameActivity.mForceWidth!= 0 || GameActivity.mScale != 0 ) && !resizing )
		{
			int newWidth, newHeight;
			resizing = true;
			if( GameActivity.mForceHeight != 0 && GameActivity.mForceWidth != 0 )
			{
				newWidth = GameActivity.mForceWidth;
				newHeight = GameActivity.mForceHeight;
			}
			else
			{
			newWidth = ( int )( getWidth() / GameActivity.mScale );
			newHeight = ( int )( getHeight() / GameActivity.mScale );
			}
			holder.setFixedSize( newWidth, newHeight );
			GameActivity.mTouchScaleX = ( float )newWidth / getWidth();
			GameActivity.mTouchScaleY = ( float )newHeight / getHeight();

			return;
		}

		if( width > height || mEngThread == null )
			GameActivity.onNativeResize( width, height );

		if( mEngThread == null )
		{
			mEngThread = new Thread( new GameMain(), "EngineThread" );
			mEngThread.start();
		}
		resizing = false;
	}

	public void engineThreadJoin()
	{
		Log.v( TAG, "engineThreadJoin()" );
		try
		{
			if( mEngThread != null )
				mEngThread.join( 5000 );
		}
		catch( InterruptedException e )
		{
		}
	}

	public void engineThreadWait()
	{
		if( GameActivity.fGDBSafe )
			return;

		Log.v( TAG, "engineThreadWait()" );
		synchronized( mPauseLock )
		{
			try
			{
				mPauseLock.wait( 5000 );
			}
			catch( InterruptedException e )
			{
			}
		}
	}

	public void engineThreadNotify()
	{
		if( GameActivity.fGDBSafe )
			return;

		Log.v( TAG, "engineThreadNotify()" );
		synchronized( mPauseLock )
		{
			mPauseLock.notify();
		}
	}

	public void onDraw( Canvas canvas )
	{
	}

	public Surface getNativeSurface()
	{
		return getHolder().getSurface();
	}

	public int getGLAttribute( final int attr )
	{
		try
		{
			EGL10 egl = ( EGL10 )EGLContext.getEGL();
			if( attr == egl.EGL_ALPHA_SIZE ||
				attr == egl.EGL_DEPTH_SIZE ||
				attr == egl.EGL_RED_SIZE ||
				attr == egl.EGL_GREEN_SIZE ||
				attr == egl.EGL_BLUE_SIZE ||
				attr == egl.EGL_STENCIL_SIZE )
			{
				int[] value = new int[1];

				boolean ret = egl.eglGetConfigAttrib(mEGLDisplay, mEGLConfig, attr, value);

				if( !ret )
				{
					Log.e(TAG, "getGLAttribute(): eglGetConfigAttrib error " + egl.eglGetError());
					return 0;
				}

				return value[0];
			}
			else
			{
				Log.e(TAG, "getGLAttribute(): Unknown attribute " + attr);
				return 0;
			}
		}
		catch( Exception e  )
		{
			Log.v( TAG, e + "" );
			for( StackTraceElement s : e.getStackTrace() )
			{
				Log.v( TAG, s.toString() );
			}
		}
		return 0;
	}

	public boolean InitGL( int stencilBits )
	{
		try
		{
			EGL10 egl = ( EGL10 )EGLContext.getEGL();

			if( egl == null )
			{
				Log.e( TAG, "Cannot get EGL from context" );
				return false;
			}

			EGLDisplay dpy = egl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );

			if( dpy == null )
			{
				Log.e( TAG, "Cannot get display" );
				return false;
			}

			int[] version = new int[2];
			if( !egl.eglInitialize( dpy, version ) )
			{
				Log.e( TAG, "No EGL config available" );
				return false;
			}

			int[][] configSpec =
			{
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   8,
					EGL10.EGL_GREEN_SIZE, 8,
					EGL10.EGL_BLUE_SIZE,  8,
					EGL10.EGL_ALPHA_SIZE, 8,
					EGL10.EGL_NONE
				},
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   8,
					EGL10.EGL_GREEN_SIZE, 8,
					EGL10.EGL_BLUE_SIZE,  8,
					EGL10.EGL_ALPHA_SIZE, 0,
					EGL10.EGL_NONE
				},
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   5,
					EGL10.EGL_GREEN_SIZE, 6,
					EGL10.EGL_BLUE_SIZE,  5,
					EGL10.EGL_ALPHA_SIZE, 0,
					EGL10.EGL_NONE
				},
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   5,
					EGL10.EGL_GREEN_SIZE, 5,
					EGL10.EGL_BLUE_SIZE,  5,
					EGL10.EGL_ALPHA_SIZE, 1,
					EGL10.EGL_NONE
				},
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   4,
					EGL10.EGL_GREEN_SIZE, 4,
					EGL10.EGL_BLUE_SIZE,  4,
					EGL10.EGL_ALPHA_SIZE, 4,
					EGL10.EGL_NONE
				},
				{
					EGL10.EGL_STENCIL_SIZE, stencilBits,
					EGL10.EGL_DEPTH_SIZE, 8,
					EGL10.EGL_RED_SIZE,   3,
					EGL10.EGL_GREEN_SIZE, 3,
					EGL10.EGL_BLUE_SIZE,  2,
					EGL10.EGL_ALPHA_SIZE, 0,
					EGL10.EGL_NONE
				}
			};
			EGLConfig[] configs = new EGLConfig[1];
			int[] num_config = new int[1];
			if( !egl.eglChooseConfig( dpy, configSpec[GameActivity.mPixelFormat], configs, 1, num_config ) || num_config[0] == 0 )
			{
				Log.e( TAG, "Failed to choose config with " + stencilBits + " stencil size. Trying without..." );
				configSpec[GameActivity.mPixelFormat][1] = 0;
				if( !egl.eglChooseConfig( dpy, configSpec[GameActivity.mPixelFormat], configs, 1, num_config ) || num_config[0] == 0 )
				{
					Log.e( TAG, "No EGL config available" );
					return false;
				}
			}
			EGLConfig config = configs[0];

			int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
			int contextAttrs[] = new int[]
			{
				EGL_CONTEXT_CLIENT_VERSION, 1,
				EGL10.EGL_NONE
			};
			EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs );
			if( ctx == EGL10.EGL_NO_CONTEXT )
			{
				Log.e( TAG, "Couldn't create context" );
				return false;
			}

			EGLSurface surface = egl.eglCreateWindowSurface( dpy, config, this, null );
			if( surface == EGL10.EGL_NO_SURFACE )
			{
				Log.e( TAG, "Couldn't create surface" );
				return false;
			}

			if( !egl.eglMakeCurrent( dpy, surface, surface, ctx ) )
			{
				Log.e( TAG, "Couldn't make context current" );
				return false;
			}

			mEGLContext = ctx;
			mEGLDisplay = dpy;
			mEGLSurface = surface;
			mEGL = egl;
			mEGLConfig = config;
		}
		catch( Exception e )
		{
			Log.v( TAG, e + "" );
			for( StackTraceElement s : e.getStackTrace() )
			{
				Log.v( TAG, s.toString() );
			}
		}

		return true;
	}

	public void SwapBuffers()
	{
		if( mEGLSurface == null )
			return;

		mEGL.eglSwapBuffers( mEGLDisplay, mEGLSurface );
	}

	public void toggleEGL( int toggle )
	{
	   if( toggle != 0 )
	   {
			mEGLSurface = mEGL.eglCreateWindowSurface( mEGLDisplay, mEGLConfig, this, null );
			mEGL.eglMakeCurrent( mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext );
	   }
	   else
	   {
			mEGL.eglMakeCurrent( mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT );
			mEGL.eglDestroySurface( mEGLDisplay, mEGLSurface );
			mEGLSurface = null;
	   }
	}

	public void ShutdownGL()
	{
		mEGL.eglDestroyContext( mEGLDisplay, mEGLContext );
		mEGLContext = null;

		mEGL.eglDestroySurface( mEGLDisplay, mEGLSurface );
		mEGLSurface = null;

		mEGL.eglTerminate( mEGLDisplay );
		mEGLDisplay = null;
	}

	@Override
	public boolean onKey( View v, int keyCode, KeyEvent event )
	{
		return GameActivity.handleKey( keyCode, event );
	}

	@Override
	public boolean onKeyPreIme( int keyCode, KeyEvent event )
	{
		Log.v( TAG, "PreIme: " + keyCode );
		return super.dispatchKeyEvent( event );
	}
}
