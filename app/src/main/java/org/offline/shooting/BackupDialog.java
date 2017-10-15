package org.offline.shooting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.widget.EditText;

import java.io.File;

public class BackupDialog extends DialogFragment
{
	ShootingApplication			mApplication;
	Activity					mActivity;
	ContentResolver				mContentResolver;

	ProgressDialog				mDialogProgress;
	boolean						mBackupRunning;

	View						mView;
	EditText					mTextFile;
	AlertDialog					mDialog;

	private static final String	STATE_RUNNING	= "backup_running";

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState )
	{
		AlertDialog.Builder builder;

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mContentResolver	= mActivity.getContentResolver();

		mDialogProgress		= null;
		mBackupRunning		= false;

		mView				= mActivity.getLayoutInflater().inflate( R.layout.dialog_backup, null );
		mTextFile			= ( EditText ) mView.findViewById( R.id.backup_file );

		mTextFile.setText( new File( Environment.getExternalStorageDirectory(), "shooting.zip" ).getAbsolutePath() );

		builder				= new AlertDialog.Builder( mActivity );
		builder.setTitle( R.string.title_backup );
		builder.setView( mView );

		builder.setNegativeButton( R.string.button_cancel, null );
		builder.setPositiveButton( R.string.button_backup, null );

		if ( savedInstanceState != null )
		{
			mBackupRunning	= savedInstanceState.getBoolean( STATE_RUNNING, false );
		}

		mDialog	= builder.create();
		return mDialog;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		mDialog.getButton( DialogInterface.BUTTON_POSITIVE ).setOnClickListener( onBackup );
	}

	@Override
	public void onResume()
	{
		super.onResume();

		mActivity.registerReceiver( onBackupComplete, new IntentFilter( BackupService.ACTION_COMPLETE ) );

		if ( BackupService.isRunning( mActivity ) )
		{
			showProgressDialog();
		}
		else if ( mBackupRunning )
		{
			dismiss();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		mActivity.unregisterReceiver( onBackupComplete );

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

		outState.putBoolean( STATE_RUNNING, mBackupRunning );
	}

	private View.OnClickListener onBackup = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Intent intent;

			intent				= new Intent( mActivity, BackupService.class );
			intent.putExtra( BackupService.PARAM_BACKUP_PATH, mTextFile.getText().toString() );

			if ( mActivity.startService( intent ) != null )
			{
				mBackupRunning	= true;
				showProgressDialog();
			}
		}
	};

	private final BroadcastReceiver onBackupComplete = new BroadcastReceiver()
	{
		@Override
		public void onReceive( Context context, Intent intent )
		{
			mBackupRunning	= false;

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
}
