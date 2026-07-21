package com.physic3d.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.physic3d.fwgslib.*;

class DummyEdit extends View implements View.OnKeyListener
{
	InputConnection ic;

	public DummyEdit( Context context )
	{
		super( context );
		setFocusableInTouchMode( true );
		setFocusable( true );
		setOnKeyListener( this );
	}

	@Override
	public boolean onCheckIsTextEditor()
	{
		return true;
	}

	@Override
	public boolean onKey( View v, int keyCode, KeyEvent event )
	{
		return GameActivity.handleKey( keyCode, event );
	}

	@Override
	public InputConnection onCreateInputConnection( EditorInfo outAttrs )
	{
		ic = new GameInputConnection( this, true );

		outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
				| 33554432 /* API 11: EditorInfo.IME_FLAG_NO_FULLSCREEN */;

		return ic;
	}
}

class GameInputConnection extends android.view.inputmethod.BaseInputConnection
{
	public GameInputConnection( View targetView, boolean fullEditor )
	{
		super( targetView, fullEditor );
	}

	@Override
	public boolean sendKeyEvent( KeyEvent event )
	{
		if( GameActivity.handleKey( event.getKeyCode(), event ) )
			return true;
		return super.sendKeyEvent( event );
	}

	@Override
	public boolean commitText( CharSequence text, int newCursorPosition )
	{
		if( text.toString().equals( "\n" ) )
		{
			GameActivity.nativeKey( 1, KeyEvent.KEYCODE_ENTER );
			GameActivity.nativeKey( 0, KeyEvent.KEYCODE_ENTER );
		}
		GameActivity.nativeString( text.toString() );

		return super.commitText( text, newCursorPosition );
	}

	@Override
	public boolean setComposingText( CharSequence text, int newCursorPosition )
	{
		return super.setComposingText( text, newCursorPosition );
	}

	public native void nativeSetComposingText( String text, int newCursorPosition );

	@Override
	public boolean deleteSurroundingText( int beforeLength, int afterLength )
	{
		if( beforeLength > 0 && afterLength == 0 )
		{
			boolean ret = true;
			while( beforeLength-- > 0 )
			{
				boolean ret_key = sendKeyEvent( new KeyEvent( KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL ) )
								&& sendKeyEvent( new KeyEvent( KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL ) );
				ret = ret && ret_key;
			}
			return ret;
		}

		return super.deleteSurroundingText(beforeLength, afterLength);
	}
}

class EngineTouchListener implements View.OnTouchListener
{
	float lx = 0, ly = 0;
	boolean secondarypressed = false;

	public boolean onTouch( View v, MotionEvent event )
	{
		final int pointerCount = event.getPointerCount();
		int action = event.getActionMasked();
		int pointerFingerId, mouseButton, i = -1;
		float x, y;

		switch( action )
		{
			case MotionEvent.ACTION_MOVE:
				if( ( !GameActivity.fMouseShown ) && ( ( GameActivity.handler.getSource( event ) & InputDevice.SOURCE_MOUSE ) == InputDevice.SOURCE_MOUSE ) )
				{
					x = event.getX();
					y = event.getY();

					GameActivity.nativeMouseMove( x - lx, y - ly );
					lx = x;
					ly = y;
					return true;
				}

				for( i = 0; i < pointerCount; i++ )
				{
					pointerFingerId = event.getPointerId( i );
					x = event.getX( i ) * GameActivity.mTouchScaleX;
					y = event.getY( i ) * GameActivity.mTouchScaleY;
					GameActivity.nativeTouch( pointerFingerId, 2, x, y );
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_DOWN:
				 if( !GameActivity.fMouseShown && ( ( GameActivity.handler.getSource( event ) & InputDevice.SOURCE_MOUSE ) == InputDevice.SOURCE_MOUSE ) )
				 {
					lx = event.getX();
					ly = event.getY();
					boolean down = ( action == MotionEvent.ACTION_DOWN ) || ( action == MotionEvent.ACTION_POINTER_DOWN );
					int buttonState = GameActivity.handler.getButtonState( event );
					if( down && ( buttonState & MotionEvent.BUTTON_SECONDARY ) != 0 )
					{
						GameActivity.nativeKey( 1, -243 );
						secondarypressed = true;
						return true;
					}
					else if( !down && secondarypressed && ( buttonState & MotionEvent.BUTTON_SECONDARY ) == 0 )
					{
						secondarypressed = false;
						GameActivity.nativeKey( 0, -243 );
						return true;
					}
					GameActivity.nativeKey( down ? 1 : 0, -241 );
					return true;
				}
				i = 0;
				// fallthrough
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_DOWN:
				if( i == -1 )
				{
					i = event.getActionIndex();
				}

				pointerFingerId = event.getPointerId( i );

				x = event.getX( i ) * GameActivity.mTouchScaleX;
				y = event.getY( i ) * GameActivity.mTouchScaleY;
				if( action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP )
					GameActivity.nativeTouch( pointerFingerId,1, x, y );
				if( action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN )
					GameActivity.nativeTouch( pointerFingerId,0, x, y );
				break;
			case MotionEvent.ACTION_CANCEL:
				for( i = 0; i < pointerCount; i++ )
				{
					pointerFingerId = event.getPointerId( i );
					x = event.getX( i ) * GameActivity.mTouchScaleX;
					y = event.getY( i ) * GameActivity.mTouchScaleY;
					GameActivity.nativeTouch( pointerFingerId, 1, x, y );
				}
				break;
			default: break;
		}
		return true;
	}
}

class AndroidBug5497Workaround
{
	public static void assistActivity ( Activity activity )
	{
		new AndroidBug5497Workaround( activity );
	}

	private View mChildOfContent;
	private int usableHeightPrevious;
	private FrameLayout.LayoutParams frameLayoutParams;

	private AndroidBug5497Workaround( Activity activity )
	{
		FrameLayout content = ( FrameLayout )activity.findViewById( android.R.id.content );
		mChildOfContent = content.getChildAt( 0 );
		mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener( )
		{
			public void onGlobalLayout()
			{
				possiblyResizeChildOfContent();
			}
		});
		frameLayoutParams = ( FrameLayout.LayoutParams )mChildOfContent.getLayoutParams();
	}

	private void possiblyResizeChildOfContent()
	{
		int usableHeightNow = computeUsableHeight();
		if( usableHeightNow != usableHeightPrevious )
		{
			int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
			int heightDifference = usableHeightSansKeyboard - usableHeightNow;
			if( heightDifference > ( usableHeightSansKeyboard / 4 ) )
			{
				frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
				GameActivity.keyboardVisible = true;
			}
			else
			{
				frameLayoutParams.height = usableHeightSansKeyboard;
				GameActivity.keyboardVisible = false;
			}

			if( GameActivity.mImmersiveMode != null )
				GameActivity.mImmersiveMode.apply();

			mChildOfContent.requestLayout();
			usableHeightPrevious = usableHeightNow;
		}
	}

	private int computeUsableHeight()
	{
		Rect r = new Rect();
		mChildOfContent.getWindowVisibleDisplayFrame( r );
		return r.bottom - r.top;
	}
}

class ImmersiveMode
{
	void apply()
	{
	}
}

class ImmersiveMode_v19 extends ImmersiveMode
{
	@Override
	void apply()
	{
		if( !GameActivity.keyboardVisible )
			GameActivity.mDecorView.setSystemUiVisibility(
					0x00000100
					| 0x00000200
					| 0x00000400
					| 0x00000002
					| 0x00000004
					| 0x00001000
					);
		else
			GameActivity.mDecorView.setSystemUiVisibility( 0 );
	}
}

class JoystickHandler
{
	public int getSource( KeyEvent event )
	{
		return InputDevice.SOURCE_UNKNOWN;
	}

	public int getSource( MotionEvent event )
	{
		return InputDevice.SOURCE_UNKNOWN;
	}

	public boolean handleAxis( MotionEvent event )
	{
		return false;
	}

	public boolean isGamepadButton( int keyCode )
	{
		return false;
	}

	public String keyCodeToString( int keyCode )
	{
		return String.valueOf( keyCode );
	}

	public void init()
	{
	}

	public boolean hasVibrator()
	{
		return true;
	}

	public void showMouse( boolean show )
	{
	}

	public int getButtonState( MotionEvent event )
	{
		return 0;
	}
}

class Wrap_NVMouseExtensions
{
	private static Object inputManager;
	private static Method mInputManager_setCursorVisibility;
	private static Method mView_setPointerIcon;
	private static Class mPointerIcon;
	private static Object mEmptyIcon;
	public static int nMotionEvent_AXIS_RELATIVE_X = 0;
	public static int nMotionEvent_AXIS_RELATIVE_Y = 0;

	static
	{
		try
		{
			mInputManager_setCursorVisibility =
				Class.forName( "android.hardware.input.InputManager" ).getMethod( "setCursorVisibility", boolean.class );

			inputManager = GameActivity.mSingleton.getSystemService( "input" );
		}
		catch( Exception ex )
		{
			try
			{
				mPointerIcon=Class.forName("android.view.PointerIcon");
				mEmptyIcon = mPointerIcon.getDeclaredMethod("getSystemIcon",android.content.Context.class, int.class).invoke(null,GameActivity.mSingleton.getContext(),0);
				mView_setPointerIcon = View.class.getMethod("setPointerIcon",mPointerIcon);
			}
			catch( Exception ex1 )
			{
				ex1.printStackTrace();
			}
		}
	}

	public static void checkAvailable() throws Exception
	{
		Field fieldMotionEvent_AXIS_RELATIVE_X = MotionEvent.class.getField( "AXIS_RELATIVE_X" );
		nMotionEvent_AXIS_RELATIVE_X = ( Integer )fieldMotionEvent_AXIS_RELATIVE_X.get( null );
		Field fieldMotionEvent_AXIS_RELATIVE_Y = MotionEvent.class.getField( "AXIS_RELATIVE_Y" );
		nMotionEvent_AXIS_RELATIVE_Y = ( Integer )fieldMotionEvent_AXIS_RELATIVE_Y.get( null );
	}

	static void setPointerIcon( View view, boolean fVisibility )
	{
		Log.v("Physic3DInput", "SET CURSOR VISIBILITY " + fVisibility + " obj " + mEmptyIcon.toString() );
		try
		{
			mView_setPointerIcon.invoke(view,fVisibility?null:mEmptyIcon);
		}
		catch( Exception e)
		{
			e.printStackTrace();
		}
	}
	static void setGroupPointerIcon( ViewGroup parent, boolean fVisibility )
	{
		for( int i = parent.getChildCount() - 1; i >= 0; i-- )
		{
			try
			{
				final View child = parent.getChildAt(i);

				if( child == null )
					continue;

				if( child instanceof ViewGroup )
				{
					setGroupPointerIcon((ViewGroup) child, fVisibility);
				}
				setPointerIcon( child, fVisibility);

			}
			catch( Exception ex )
			{
				ex.printStackTrace();
			}
		}
	}

	public static void setCursorVisibility( boolean fVisibility )
	{
		try
		{
			mInputManager_setCursorVisibility.invoke( inputManager, fVisibility );

		}
		catch( Exception e )
		{
			try
			{
				ViewGroup rootViewGroup = (ViewGroup) GameActivity.mSingleton.getWindow().getDecorView();
				setGroupPointerIcon(rootViewGroup, fVisibility);
				setGroupPointerIcon((ViewGroup)GameActivity.mDecorView, fVisibility);
				for (int i = 0; i < rootViewGroup.getChildCount(); i++) {
					View view = rootViewGroup.getChildAt(i);
					setPointerIcon(view, fVisibility);
				}
				}
				catch( Exception ex)
				{
					ex.printStackTrace();
				}
		}
	}

	public static int getAxisRelativeX()
	{
		return nMotionEvent_AXIS_RELATIVE_X;
	}

	public static int getAxisRelativeY()
	{
		return nMotionEvent_AXIS_RELATIVE_Y;
	}
}

class JoystickHandler_v12 extends JoystickHandler
{
	private static float prevSide, prevFwd, prevYaw, prevPtch, prevLT, prevRT, prevHX, prevHY;

	public static boolean mNVMouseExtensions = false;

	static int mouseId;
	static
	{
		try
		{
			Wrap_NVMouseExtensions.checkAvailable();
			mNVMouseExtensions = true;
		}
		catch( Throwable t )
		{
			mNVMouseExtensions = false;
		}
	}

	@Override
	public void init()
	{
		GameActivity.mSurface.setOnGenericMotionListener( new MotionListener() );
		Log.d( GameActivity.TAG, "mNVMouseExtensions = " + mNVMouseExtensions );
	}

	@Override
	public int getSource( KeyEvent event )
	{
		return event.getSource();
	}

	@Override
	public int getSource( MotionEvent event )
	{
		if( event.getDeviceId() == mouseId )
			return InputDevice.SOURCE_MOUSE;
		return event.getSource();
	}

	@Override
	public boolean handleAxis( MotionEvent event )
	{
		final InputDevice device = event.getDevice();
		if( device == null )
			return false;

		for( InputDevice.MotionRange range: device.getMotionRanges() )
		{
			final float cur = ( event.getAxisValue( range.getAxis(), event.getActionIndex() ) - range.getMin() ) / range.getRange() * 2.0f - 1.0f;
			final float dead = range.getFlat();
			switch( range.getAxis() )
			{
			case MotionEvent.AXIS_X:
				prevSide = GameActivity.performEngineAxisEvent( cur, GameActivity.JOY_AXIS_SIDE,  prevSide, dead );
				break;
			case MotionEvent.AXIS_Y:
				prevFwd  = GameActivity.performEngineAxisEvent( cur, GameActivity.JOY_AXIS_FWD,   prevFwd,  dead );
				break;

			case MotionEvent.AXIS_Z:
				prevPtch = GameActivity.performEngineAxisEvent( -cur, GameActivity.JOY_AXIS_PITCH, prevPtch, dead );
				break;
			case MotionEvent.AXIS_RZ:
				prevYaw  = GameActivity.performEngineAxisEvent( -cur, GameActivity.JOY_AXIS_YAW,   prevYaw,  dead );
				break;

			case MotionEvent.AXIS_RTRIGGER:
				prevLT = GameActivity.performEngineAxisEvent( cur, GameActivity.JOY_AXIS_RT, prevLT,   dead );
				break;
			case MotionEvent.AXIS_LTRIGGER:
				prevRT = GameActivity.performEngineAxisEvent( cur, GameActivity.JOY_AXIS_LT, prevRT,   dead );
				break;

			case MotionEvent.AXIS_HAT_X:
				prevHX = GameActivity.performEngineHatEvent( cur, true, prevHX );
				break;
			case MotionEvent.AXIS_HAT_Y:
				prevHY = GameActivity.performEngineHatEvent( cur, false, prevHY );
				break;
			}
		}
		return true;
	}

	@Override
	public boolean isGamepadButton( int keyCode )
	{
		return KeyEvent.isGamepadButton( keyCode );
	}

	@Override
	public String keyCodeToString( int keyCode )
	{
		return KeyEvent.keyCodeToString( keyCode );
	}

	class MotionListener implements View.OnGenericMotionListener
	{
		@Override
		public boolean onGenericMotion( View view, MotionEvent event )
		{
			final int source = GameActivity.handler.getSource( event );

			if( FWGSLib.FExactBitSet( source, InputDevice.SOURCE_GAMEPAD ) ||
				FWGSLib.FExactBitSet( source, InputDevice.SOURCE_CLASS_JOYSTICK ) )
				return GameActivity.handler.handleAxis( event );

			if( mNVMouseExtensions )
			{
				float x = event.getAxisValue( Wrap_NVMouseExtensions.getAxisRelativeX(), 0 );
				float y = event.getAxisValue( Wrap_NVMouseExtensions.getAxisRelativeY(), 0 );
				if( !FWGSLib.FExactBitSet( source, InputDevice.SOURCE_MOUSE) && (x != 0 || y != 0 ))
					mouseId = event.getDeviceId();

				switch( event.getAction() )
				{
					case MotionEvent.ACTION_SCROLL:
					if( event.getAxisValue( MotionEvent.AXIS_VSCROLL ) < 0.0f )
					{
						GameActivity.nativeKey( 1, -239 );
						GameActivity.nativeKey( 0, -239 );
						return true;
					}
					else
					{
						GameActivity.nativeKey( 1, -240 );
						GameActivity.nativeKey( 0, -240 );
					}
					return true;
				}

				GameActivity.nativeMouseMove( x, y );
				return true;
			}

			return false;
		}
	}

	@Override
	public boolean hasVibrator()
	{
		if( GameActivity.mVibrator != null )
			return GameActivity.mVibrator.hasVibrator();
		return false;
	}

	@Override
	public void showMouse( boolean show )
	{
		if( mNVMouseExtensions )
			Wrap_NVMouseExtensions.setCursorVisibility( show );
	}
}

class JoystickHandler_v14 extends JoystickHandler_v12
{
	@Override
	public int getButtonState( MotionEvent event )
	{
		return event.getButtonState();
	}
}
