/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class RestoreDialog extends DialogFragment
{
	ShootingApplication			mApplication;
	Activity					mActivity;
	ContentResolver				mContentResolver;

	ProgressDialog				mDialogProgress;
	boolean						mRestoreRunning;

	View						mView;
	EditText					mTextFile;
	Button						mButtonBrowse;
	AlertDialog					mDialog;

	private static final String	STATE_RUNNING	= "restore_running";

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState )
	{
		AlertDialog.Builder builder;

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mContentResolver	= mActivity.getContentResolver();

		mDialogProgress		= null;
		mRestoreRunning		= false;

		mView				= mActivity.getLayoutInflater().inflate( R.layout.dialog_restore, null );
		mTextFile			= ( EditText ) mView.findViewById( R.id.restore_file );
		mButtonBrowse		= ( Button ) mView.findViewById( R.id.restore_browse );

		mButtonBrowse.setOnClickListener( onBrowse );
		mTextFile.setText( new File( Environment.getExternalStorageDirectory(), "shooting.zip" ).getAbsolutePath() );

		builder				= new AlertDialog.Builder( mActivity );

		builder.setTitle( R.string.title_restore );
		builder.setView( mView );

		builder.setNegativeButton( R.string.button_cancel, null );
		builder.setPositiveButton( R.string.button_restore, null );

		if ( savedInstanceState != null )
		{
			mRestoreRunning	= savedInstanceState.getBoolean( STATE_RUNNING, false );
		}

		mDialog				= builder.create();
		return mDialog;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		mDialog.getButton( DialogInterface.BUTTON_POSITIVE ).setOnClickListener( onRestore );
	}

	@Override
	public void onResume()
	{
		super.onResume();

		mActivity.registerReceiver( onRestoreComplete, new IntentFilter( RestoreService.ACTION_COMPLETE ) );

		if ( RestoreService.isRunning( mActivity ) )
		{
			showProgressDialog();
		}
		else if ( mRestoreRunning )
		{
			dismiss();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		mActivity.unregisterReceiver( onRestoreComplete );

		if ( mDialogProgress != null )
		{
			mDialogProgress.dismiss();
			mDialogProgress	= null;
		}
	}

	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putBoolean( STATE_RUNNING, mRestoreRunning );
	}

	private final View.OnClickListener onBrowse = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Intent intent;

			intent	= new Intent( Intent.ACTION_GET_CONTENT );
			intent.setType( "application/zip" );

			try
			{
				startActivityForResult( intent, ShootingApplication.RESULT_RESTORE_BROWSE );
			}
			catch ( ActivityNotFoundException e )
			{
				Toast.makeText( mActivity, getString( R.string.label_restore_browse_failed ), Toast.LENGTH_SHORT ).show();
			}
		}
	};

	private final View.OnClickListener onRestore = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Intent intent;

			intent				= new Intent( mActivity, RestoreService.class );
			intent.putExtra( RestoreService.PARAM_RESTORE_PATH, mTextFile.getText().toString() );

			if ( mActivity.startService( intent ) != null )
			{
				mRestoreRunning	= true;
				showProgressDialog();
			}
		}
	};

	private final BroadcastReceiver onRestoreComplete = new BroadcastReceiver()
	{
		@Override
		public void onReceive( Context context, Intent intent )
		{
			mRestoreRunning	= false;

			if ( mDialogProgress != null )
			{
				mDialogProgress.dismiss();
			}

			dismiss();
		}
	};

	private void showProgressDialog()
	{
		if ( mDialogProgress == null )
		{
			mDialogProgress	= new ProgressDialog( mActivity );

			mDialogProgress.setProgressStyle( ProgressDialog.STYLE_SPINNER );
			mDialogProgress.setMessage( mActivity.getString( R.string.label_processing ) );
			mDialogProgress.setCancelable( false );
		}

		mDialogProgress.show();
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );

		if ( ( requestCode == ShootingApplication.RESULT_RESTORE_BROWSE ) && ( resultCode == Activity.RESULT_OK ) )
		{
			mTextFile.setText( data.getData().getPath() );
		}
	}
}
