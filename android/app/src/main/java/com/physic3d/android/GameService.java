package com.physic3d.android;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.os.*;
import android.util.*;
import android.graphics.*;
import android.text.method.*;
import android.text.*;
import android.media.*;
import android.hardware.*;
import android.content.*;
import android.widget.*;
import android.content.pm.*;
import android.net.Uri;
import android.provider.*;
import android.database.*;

import android.view.inputmethod.*;

import java.lang.*;
import java.util.List;
import java.security.MessageDigest;

import com.physic3d.android.R;


public class GameService extends Service 
{
	public static final String CHANNEL_ID = "game_service";
	public static Notification notification;
	public static int status_image = R.id.status_image;
	public static int status_text = R.id.status_text;

	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}
	
	public static class exitButtonListener extends BroadcastReceiver 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			GameActivity.mEngineReady = false;
			GameActivity.nativeUnPause();
			GameActivity.nativeOnDestroy();
			if( GameActivity.mSurface != null )
				GameActivity.mSurface.engineThreadJoin();
			System.exit(0);
		}
	}

	private void createNotificationChannel()
	{
		if( Build.VERSION.SDK_INT >= 26 )
		{
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID, "Game Service", NotificationManager.IMPORTANCE_LOW);
			channel.setDescription("Physic3D game engine foreground service");
			NotificationManager nm = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
			nm.createNotificationChannel( channel );
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		int status_exit_button = R.id.status_exit_button;
		int notify = R.layout.notify;
		if( Build.VERSION.SDK_INT >= 21 )
		{
			status_image = R.id.status_image_21;
			status_text = R.id.status_text_21;
			status_exit_button = R.id.status_exit_button_21;
			notify = R.layout.notify_21;
		}
		
		Log.d("GameService", "Service Started");
		
		createNotificationChannel();
		
		Intent engineIntent = new Intent(this, GameActivity.class);
		engineIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		
		Intent exitIntent = new Intent(this, exitButtonListener.class);
		int pendingFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
		PendingIntent pendingExitIntent = PendingIntent.getBroadcast(this, 0, exitIntent, pendingFlags);
		
		RemoteViews contentView = new RemoteViews( getPackageName(), notify );
		contentView.setTextViewText( status_text, "Physic3D Engine" );
		contentView.setOnClickPendingIntent( status_exit_button, pendingExitIntent );

		Notification.Builder builder = new Notification.Builder( this, CHANNEL_ID )
			.setSmallIcon( R.drawable.ic_statusbar )
			.setOngoing( true )
			.setContentIntent( PendingIntent.getActivity( this, 0, engineIntent, pendingFlags ) )
			.setCustomContentView( contentView );
		
		notification = builder.build();
		
		startForeground( 100, notification );
		
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() 
	{
		super.onDestroy();
		Log.d("GameService", "Service Destroyed");
	}

	@Override
	public void onCreate()
	{
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) 
	{
		Log.e("GameService", "OnTaskRemoved");
		//if( GameActivity.mEngineReady )
		{
			GameActivity.mEngineReady = false;
			GameActivity.nativeUnPause();
			GameActivity.nativeOnDestroy();
			if( GameActivity.mSurface != null )
				GameActivity.mSurface.engineThreadJoin();
			System.exit(0);
		}
		stopSelf();
	}
};
