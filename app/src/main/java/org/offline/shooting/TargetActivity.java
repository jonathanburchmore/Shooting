/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TargetActivity extends FragmentActivity
{
	private ShootingApplication			mApplication;
	private ContentResolver				mContentResolver;
	private LoaderManager				mLoaderManager;

	private static DetachableAsyncTask	mTaskCreateFromImage;

	private int							mMaxImageWidth;
	private TargetImageLoader			mTargetImageLoader;

	private long						mTargetId;
	private String						mTargetPath;
	private boolean						mCreatedFromImage;

	private ImageView					mImageTarget;
	private TextView					mTextDate;
	private FirearmSpinner				mSpinnerFirearm;
	private EditText					mEditLot;
	private AutoCompleteTextView		mEditAmmo;
	private AutoCompleteTextView		mEditType;
	private EditText					mEditDistance;
	private EditText					mEditShots;
	private EditText					mEditNotes;

	private static final String			STATE_DATE				= "date";
	private static final String			STATE_FIREARM			= "firearm";

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;
		long firearmId;
		String intentAction;
		Resources resources;
		final ActionBar actionBar = getActionBar();

		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_target );
		setTitle( R.string.title_target );

		actionBar.setDisplayHomeAsUpEnabled( true );

		mApplication			= ( ShootingApplication ) getApplicationContext();
		mContentResolver		= getContentResolver();
		mLoaderManager			= getSupportLoaderManager();
		resources				= getResources();

		intent					= getIntent();
		mTargetId				= intent.getLongExtra( ShootingApplication.PARAM_TARGET_ID,		0 );
		firearmId				= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ID,	0 );
		mTargetPath				= intent.getStringExtra( ShootingApplication.PARAM_TARGET_PATH );

		mImageTarget			= ( ImageView ) findViewById( R.id.target_image );
		mTextDate				= ( TextView ) findViewById( R.id.target_date );
		mSpinnerFirearm			= new FirearmSpinner( this, ( Spinner ) findViewById( R.id.target_firearm ) );
		mEditLot				= ( EditText ) findViewById( R.id.target_lot );
		mEditAmmo				= ( AutoCompleteTextView ) findViewById( R.id.target_ammo );
		mEditType				= ( AutoCompleteTextView ) findViewById( R.id.target_type );
		mEditDistance			= ( EditText ) findViewById( R.id.target_distance );
		mEditShots				= ( EditText ) findViewById( R.id.target_shots );
		mEditNotes				= ( EditText ) findViewById( R.id.target_notes );

		switch ( resources.getConfiguration().orientation )
		{
			case Configuration.ORIENTATION_LANDSCAPE :
			{
				mMaxImageWidth	= ( int ) ( mApplication.getScreenWidth( this ) - ( resources.getDimension( R.dimen.activity_horizontal_margin ) * 2 ) ) / 2;
				break;
			}
			default :
			{
				mMaxImageWidth	= ( int ) ( mApplication.getScreenWidth( this ) - ( resources.getDimension( R.dimen.activity_horizontal_margin ) * 2 ) );
				break;
			}
		}

		mTextDate.setOnClickListener( onDateClicked );

		mEditLot.addTextChangedListener( onTextChanged );
		mEditAmmo.addTextChangedListener( onTextChanged );

		mEditAmmo.setAdapter( new TargetAmmoTextViewAdapter( this ) );
		mEditType.setAdapter( new TargetTypeTextViewAdapter( this ) );

		if ( savedInstanceState != null )
		{
			mTextDate.setText( savedInstanceState.getString( STATE_DATE ) );
			mSpinnerFirearm.setSelectedId( savedInstanceState.getLong( STATE_FIREARM ) );
		}
		else if ( mTargetId == 0 )
		{
			mTextDate.setText( mApplication.currentSystemDate() );
			mSpinnerFirearm.setSelectedId( firearmId );
		}
		else
		{
			cursor				= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.Targets.CONTENT_URI, mTargetId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				finish();

				return;
			}

			try
			{
				mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.DATE ) ) ) );
			}
			catch ( ParseException e )
			{
			}

			mSpinnerFirearm.setSelectedId( cursor.getLong( cursor.getColumnIndex( ShootingContract.Targets.FIREARM_ID ) ) );

			mEditLot.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.LOT_ID ) ) );
			mEditAmmo.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.AMMO ) ) );
			mEditType.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.TYPE ) ) );
			mEditDistance.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.DISTANCE ) ) );
			mEditShots.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.SHOTS ) ) );
			mEditNotes.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Targets.NOTES ) ) );

			cursor.close();
		}

		mImageTarget.setVisibility( View.INVISIBLE );
		mImageTarget.setOnClickListener( onTargetImageClicked );

		if ( ( intentAction = intent.getAction() ) != null && intentAction.equals( Intent.ACTION_SEND ) )
		{
			mCreatedFromImage		= true;
			new TargetCreateFromImageTask( ( Uri ) intent.getParcelableExtra( Intent.EXTRA_STREAM ) ).execute();
		}
		else
		{
			mCreatedFromImage		= false;
			mTargetImageLoader		= new TargetImageLoader();
			mTargetImageLoader.execute();
		}

		mLoaderManager.initLoader( ShootingApplication.LOADER_SPINNER_FIREARM_TARGET, null, mSpinnerFirearm );
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if ( mTaskCreateFromImage != null )
		{
			mTaskCreateFromImage.attach( this );
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if ( mTaskCreateFromImage != null )
		{
			mTaskCreateFromImage.detach();
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putString( STATE_DATE, mTextDate.getText().toString() );
		outState.putLong( STATE_FIREARM, mSpinnerFirearm.getSelectedId() );
	}

	private class TargetCreateFromImageTask extends DetachableAsyncTask<Object, Object, String>
	{
		private final Uri	mUri;

		TargetCreateFromImageTask( Uri uri )
		{
			mTaskCreateFromImage	= this;
			mUri					= uri;

			setHasProgressDialog( true );
			attach( TargetActivity.this );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected String doInBackground( Object... params )
		{
			File tempFile;

			try
			{
				tempFile		= File.createTempFile( "target", ".jpg" );
				if ( mApplication.saveUriToFile( mContentResolver, mUri, tempFile ) )
				{
					return tempFile.getAbsolutePath();
				}
			}
			catch ( Exception e )
			{
			}

			return null;
		}

		@Override
		protected void onPostExecute( String targetPath )
		{
			TargetActivity activity;

			dismissProgressDialog();

			if ( ( activity = ( TargetActivity ) getContext() ) != null )
			{
				if ( targetPath == null )
				{
					activity.finish();
					return;
				}

				activity.mTargetPath		= targetPath;
				activity.mTargetImageLoader	= new TargetImageLoader();
				activity.mTargetImageLoader.execute();
			}
		}
	}

	private class TargetImageLoader extends AsyncTask<Object, Object, Bitmap>
	{
		@Override
		protected Bitmap doInBackground( Object... params )
		{
			if ( mTargetId == 0 )
			{
				return mApplication.getBitmap( mTargetPath, mMaxImageWidth, -1 );
			}
			else
			{
				return mApplication.getBitmap( mContentResolver, ContentUris.withAppendedId( ShootingContract.Targets.IMAGE_URI, mTargetId ), mMaxImageWidth, -1 );
			}

		}

		@Override
		protected void onPostExecute( Bitmap bitmap )
		{
			if ( bitmap != null )
			{
				mImageTarget.setImageBitmap( bitmap );
				mImageTarget.setVisibility( View.VISIBLE );
			}

			mTargetImageLoader	= null;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if ( mTargetImageLoader != null )
		{
			mTargetImageLoader.cancel( true );
			mTargetImageLoader	= null;
		}

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_SPINNER_FIREARM_TARGET );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_target, menu );

		menu.findItem( R.id.menu_target_save ).setOnMenuItemClickListener( onSaveTargetClicked );
		menu.findItem( R.id.menu_target_delete ).setOnMenuItemClickListener( onDeleteTargetClicked );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		String lot;

		lot	= mEditLot.getText().toString().trim();
		if ( lot.length() == 0 )
		{
			menu.findItem( R.id.menu_target_save ).setEnabled( mEditAmmo.getText().toString().trim().length() > 0 );
		}
		else
		{
			try
			{
				menu.findItem( R.id.menu_target_save ).setEnabled( Long.valueOf( lot ) > 0 );
			}
			catch ( Exception e )
			{
				menu.findItem( R.id.menu_target_save ).setEnabled( false );
			}
		}

		menu.findItem( R.id.menu_target_delete ).setVisible( mTargetId != 0 );

		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch ( item.getItemId() )
		{
			case android.R.id.home :
			{
				finish();
				return true;
			}
		}

		return super.onOptionsItemSelected( item );
	}

	private final MenuItem.OnMenuItemClickListener onSaveTargetClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			long lotId;
			String lot;
			Cursor lotCursor;
			String dateString;
			ContentValues values;
			String targetDestPath;
			AlertDialog.Builder builder;
			File targetTemp, targetDest;

			lotId			= 0;
			values			= new ContentValues();

			try
			{
				dateString	 = mApplication.systemDateToSQLDate( mTextDate.getText().toString() );
				values.put( ShootingContract.Targets.DATE,		dateString );
			}
			catch ( ParseException e )
			{
			}

			values.put( ShootingContract.Targets.FIREARM_ID,	mSpinnerFirearm.getSelectedId() );

			lot				= mEditLot.getText().toString().trim();
			if ( lot.length() == 0 )
			{
				values.put( ShootingContract.Targets.LOT_ID,	( String ) null );
				values.put( ShootingContract.Targets.AMMO,		mEditAmmo.getText().toString().trim() );
			}
			else
			{
				try
				{
					lotId	= Long.valueOf( mEditLot.getText().toString().trim() );
				}
				catch ( Exception e )
				{
				}

				lotCursor	= mContentResolver.query( ContentUris.withAppendedId( ShootingContract.LoadLots.CONTENT_URI, lotId ), null, null, null, null );
				if ( !lotCursor.moveToFirst() )
				{
					builder	= new AlertDialog.Builder( TargetActivity.this );

					builder.setTitle( R.string.title_target_invalid_lot );
					builder.setMessage( R.string.label_target_invalid_lot );
					builder.setNegativeButton( R.string.button_ok, null );
					builder.show();

					lotCursor.close();

					return true;
				}

				lotCursor.close();

				values.put( ShootingContract.Targets.LOT_ID, lotId );
				values.put( ShootingContract.Targets.AMMO,		( String ) null );
			}

			values.put( ShootingContract.Targets.TYPE,			mEditType.getText().toString().trim() );
			values.put( ShootingContract.Targets.DISTANCE,		mEditDistance.getText().toString().trim() );
			values.put( ShootingContract.Targets.SHOTS,			mEditShots.getText().toString().trim() );
			values.put( ShootingContract.Targets.NOTES,			mEditNotes.getText().toString().trim() );

			if ( mTargetId != 0 )
			{
				mContentResolver.update( ContentUris.withAppendedId( ShootingContract.Targets.CONTENT_URI, mTargetId ), values, null, null );
			}
			else
			{
				targetDestPath	= ShootingContract.Targets.generatePath( TargetActivity.this );
				targetTemp		= new File( mTargetPath );
				targetDest		= new File( targetDestPath );

				if ( !mApplication.copyFile( targetTemp, targetDest ) )
				{
					Toast.makeText( TargetActivity.this, R.string.error_rename_tempfile, Toast.LENGTH_LONG ).show();
					return true;
				}

				targetDest.setReadable( true, false );
				targetDest.setWritable( true, true );

				values.put( ShootingContract.Targets._DATA, targetDestPath );

				mContentResolver.insert( ShootingContract.Targets.CONTENT_URI, values );

				if ( mCreatedFromImage )
				{
					Toast.makeText( TargetActivity.this, R.string.label_target_created_from_image, Toast.LENGTH_LONG ).show();
				}
			}

			finish();
			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteTargetClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( TargetActivity.this );

			builder.setTitle( R.string.title_target_delete );
			builder.setMessage( R.string.confirm_target_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Targets.CONTENT_URI, mTargetId ), null, null );
					finish();
				}
			} );

			builder.show();
			return true;
		}
	};

	private final View.OnClickListener onTargetImageClicked = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Intent intent;

			intent		= new Intent( Intent.ACTION_VIEW );

			if ( mTargetId != 0)
			{
				intent.setData( ContentUris.withAppendedId( ShootingContract.Targets.IMAGE_URI, mTargetId ) );
			}
			else
			{
				intent.setDataAndType( Uri.fromFile( new File( mTargetPath ) ), "image/jpeg" );
			}

			startActivity( intent );
		}
	};

	private final View.OnClickListener onDateClicked = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Calendar existingDate;
			DatePickerDialog dialog;

			existingDate	= Calendar.getInstance();

			try
			{
				existingDate.setTime( mApplication.parseSystemDate( mTextDate.getText().toString() ) );
			}
			catch ( ParseException e )
			{
			}

			dialog			= new DatePickerDialog( TargetActivity.this,
													new DatePickerDialog.OnDateSetListener()
													{
														@Override
														public void onDateSet( DatePicker view, int year, int monthOfYear, int dayOfMonth )
														{
															mTextDate.setText( mApplication.formatSystemDate( new GregorianCalendar( year, monthOfYear, dayOfMonth ).getTime() ) );
														}
													},
													existingDate.get( Calendar.YEAR ),
													existingDate.get( Calendar.MONTH ),
													existingDate.get( Calendar.DAY_OF_MONTH ) );
			dialog.show();
		}
	};

	private final TextWatcher onTextChanged = new TextWatcher()
	{
		@Override
		public void beforeTextChanged( CharSequence charSequence, int i, int i2, int i3 )
		{
		}

		@Override
		public void onTextChanged( CharSequence charSequence, int i, int i2, int i3 )
		{
		}

		@Override
		public void afterTextChanged( Editable editable )
		{
			invalidateOptionsMenu();
		}
	};
}
