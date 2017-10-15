/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class FirearmAcquisitionActivity extends FragmentActivity
{
	private ShootingApplication	mApplication;

	private long				mFirearmId;
	private long				mFirearmAcquisitionId;

	private TextView			mTextDate;
	private EditText			mEditFrom;
	private EditText			mEditLicense;
	private EditText			mEditAddress;
	private EditText			mEditPrice;

	private static final String	STATE_DATE				= "date";

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;
		final ActionBar actionBar = getActionBar();

		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_firearm_acquisition );
		setTitle( R.string.title_firearm_acquisition );

		actionBar.setDisplayHomeAsUpEnabled( true );

		mApplication			= ( ShootingApplication ) getApplicationContext();

		intent					= getIntent();
		mFirearmId				= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ID,				0 );
		mFirearmAcquisitionId	= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ACQUISITION_ID,	0 );

		mTextDate				= ( TextView ) findViewById( R.id.firearm_acquisition_date );
		mEditFrom				= ( EditText ) findViewById( R.id.firearm_acquisition_from );
		mEditLicense			= ( EditText ) findViewById( R.id.firearm_acquisition_license );
		mEditAddress			= ( EditText ) findViewById( R.id.firearm_acquisition_address );
		mEditPrice				= ( EditText ) findViewById( R.id.firearm_acquisition_price );

		mTextDate.setText( mApplication.currentSystemDate() );
		mTextDate.setOnClickListener( onDateClicked );

		mEditFrom.addTextChangedListener( onTextChanged );

		if ( savedInstanceState != null )
		{
			mTextDate.setText( savedInstanceState.getString( STATE_DATE ) );
		}
		else if ( mFirearmAcquisitionId != 0 )
		{
			cursor				= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.FirearmAcquisition.CONTENT_URI, mFirearmAcquisitionId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				finish();

				return;
			}

			try
			{
				mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.DATE ) ) ) );
			}
			catch ( ParseException e )
			{
			}

			mEditFrom.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.FROM ) ) );
			mEditLicense.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.LICENSE ) ) );
			mEditAddress.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.ADDRESS ) ) );
			mEditPrice.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.PRICE ) ) );

			cursor.close();
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putString( STATE_DATE, mTextDate.getText().toString() );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_firearm_acquisition, menu );

		menu.findItem( R.id.menu_firearm_acquisition_save ).setOnMenuItemClickListener( onSaveFirearmAcquisitionClicked );
		menu.findItem( R.id.menu_firearm_acquisition_delete ).setOnMenuItemClickListener( onDeleteFirearmAcquisitionClicked );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_firearm_acquisition_save ).setEnabled( mEditFrom.getText().toString().trim().length() > 0 );
		menu.findItem( R.id.menu_firearm_acquisition_delete ).setVisible( mFirearmAcquisitionId != 0 );

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

	private final MenuItem.OnMenuItemClickListener onSaveFirearmAcquisitionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			String dateString;
			ContentValues values;
			String license, address, price;

			values = new ContentValues();

			try
			{
				dateString	 = mApplication.systemDateToSQLDate( mTextDate.getText().toString() );
				values.put( ShootingContract.FirearmAcquisition.DATE,		dateString );
			}
			catch ( ParseException e )
			{
			}

			values.put( ShootingContract.FirearmAcquisition.FROM,			mEditFrom.getText().toString().trim() );

			license	= mEditLicense.getText().toString().trim();
			address	= mEditAddress.getText().toString().trim();
			price	= mEditPrice.getText().toString().trim();

			values.put( ShootingContract.FirearmAcquisition.LICENSE,		license.length() == 0 ? null : license );
			values.put( ShootingContract.FirearmAcquisition.ADDRESS,		address.length() == 0 ? null : address );
			values.put( ShootingContract.FirearmAcquisition.PRICE,			price.length() == 0 ? null : price );

			if ( mFirearmAcquisitionId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.FirearmAcquisition.CONTENT_URI, mFirearmAcquisitionId ), values, null, null );
			}
			else
			{
				values.put( ShootingContract.FirearmAcquisition.FIREARM_ID,	mFirearmId );
				getContentResolver().insert( ShootingContract.FirearmAcquisition.CONTENT_URI, values );
			}

			finish();
			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteFirearmAcquisitionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( FirearmAcquisitionActivity.this );

			builder.setTitle( R.string.title_firearm_acquisition_delete );
			builder.setMessage( R.string.confirm_firearm_acquisition_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					getContentResolver().delete( ContentUris.withAppendedId( ShootingContract.FirearmAcquisition.CONTENT_URI, mFirearmAcquisitionId ), null, null );
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

			dialog			= new DatePickerDialog( FirearmAcquisitionActivity.this,
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
