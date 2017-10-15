package org.offline.shooting;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class LoadLotActivity extends FragmentActivity
{
	private ShootingApplication		mApplication;

	private long					mLoadId;
	private long					mLoadLotId;

	private TextView				mTextDate;
	private EditText				mEditCount;
	private EditText				mEditPowderLot;
	private AutoCompleteTextView	mEditPrimer;
	private EditText				mEditPrimerLot;

	private static final String	STATE_TITLE			= "title";
	private static final String	STATE_DATE			= "date";

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;
		final ActionBar actionBar = getActionBar();

		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_load_lot );

		actionBar.setDisplayHomeAsUpEnabled( true );

		mApplication			= ( ShootingApplication ) getApplicationContext();

		intent					= getIntent();
		mLoadId					= intent.getLongExtra( ShootingApplication.PARAM_LOAD_ID,		0 );
		mLoadLotId				= intent.getLongExtra( ShootingApplication.PARAM_LOAD_LOT_ID,	0 );

		mTextDate				= ( TextView ) findViewById( R.id.load_lot_date );
		mEditCount				= ( EditText ) findViewById( R.id.load_lot_ccount );
		mEditPowderLot			= ( EditText ) findViewById( R.id.load_lot_powder_lot );
		mEditPrimer				= ( AutoCompleteTextView ) findViewById( R.id.load_lot_primer );
		mEditPrimerLot			= ( EditText ) findViewById( R.id.load_lot_primer_lot );

		mTextDate.setText( mApplication.currentSystemDate() );
		mTextDate.setOnClickListener( onDateClicked );

		mEditCount.addTextChangedListener( onTextChanged );
		mEditPrimer.addTextChangedListener( onTextChanged );

		mEditPrimer.setAdapter( new PrimerTextViewAdapter( this ) );

		if ( savedInstanceState != null )
		{
			setTitle( savedInstanceState.getString( STATE_TITLE ) );
			mTextDate.setText( savedInstanceState.getString( STATE_DATE ) );
		}
		else if ( mLoadLotId == 0 )
		{
			setTitle( R.string.title_load_lot_add );

			cursor				= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ),
									 						  new String[] { ShootingContract.Loads.PRIMER },
									 						  null, null, null );
			if ( cursor.moveToFirst() )
			{
				mEditPrimer.setText( cursor.getString( 0 ) );
			}

			cursor.close();
		}
		else
		{
			cursor				= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.LoadLots.CONTENT_URI, mLoadLotId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				finish();

				return;
			}

			setTitle( getString( R.string.title_load_lot_edit ) + cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots._ID ) ) );

			try
			{
				mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.DATE ) ) ) );
			}
			catch ( ParseException e )
			{
			}

			mEditCount.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.CCOUNT ) ) );
			mEditPowderLot.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.POWDER_LOT ) ) );
			mEditPrimer.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.PRIMER ) ) );
			mEditPrimerLot.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.PRIMER_LOT ) ) );

			cursor.close();
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putString( STATE_TITLE,	getTitle().toString() );
		outState.putString( STATE_DATE,		mTextDate.getText().toString() );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_load_lot, menu );

		menu.findItem( R.id.menu_load_lot_save ).setOnMenuItemClickListener( onSaveLoadLotClicked );
		menu.findItem( R.id.menu_load_lot_delete ).setOnMenuItemClickListener( onDeleteLoadLotClicked );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_load_lot_delete ).setVisible( mLoadLotId != 0 );

		try
		{
			menu.findItem( R.id.menu_load_lot_save ).setEnabled( Integer.valueOf( mEditCount.getText().toString().trim() ) > 0 &&
																				  mEditPrimer.getText().toString().trim().length() > 0 );
		}
		catch ( Exception e )
		{
			menu.findItem( R.id.menu_load_lot_save ).setEnabled( false );
		}

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

	private final MenuItem.OnMenuItemClickListener onSaveLoadLotClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			String dateString;
			ContentValues values;

			values = new ContentValues();

			try
			{
				dateString	 = mApplication.systemDateToSQLDate( mTextDate.getText().toString() );
				values.put( ShootingContract.LoadLots.DATE,		dateString );
			}
			catch ( ParseException e )
			{
			}

			values.put( ShootingContract.LoadLots.CCOUNT,		mEditCount.getText().toString().trim() );
			values.put( ShootingContract.LoadLots.POWDER_LOT,	mEditPowderLot.getText().toString().trim() );
			values.put( ShootingContract.LoadLots.PRIMER,		mEditPrimer.getText().toString().trim() );
			values.put( ShootingContract.LoadLots.PRIMER_LOT,	mEditPrimerLot.getText().toString().trim() );

			if ( mLoadLotId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.LoadLots.CONTENT_URI, mLoadLotId ), values, null, null );
			}
			else
			{
				values.put( ShootingContract.LoadLots.LOAD_ID, mLoadId );
				getContentResolver().insert( ShootingContract.LoadLots.CONTENT_URI, values );
			}

			finish();
			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteLoadLotClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( LoadLotActivity.this );

			builder.setTitle( R.string.title_load_lot_delete );
			builder.setMessage( R.string.confirm_load_lot_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					getContentResolver().delete( ContentUris.withAppendedId( ShootingContract.LoadLots.CONTENT_URI, mLoadLotId ), null, null );
					finish();
				}
			} );

			builder.show();
			return true;
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

			dialog			= new DatePickerDialog( LoadLotActivity.this,
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