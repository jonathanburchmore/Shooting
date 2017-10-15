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

public class FirearmDispositionActivity extends FragmentActivity
{
	private ShootingApplication	mApplication;

	private long				mFirearmId;
	private long				mFirearmDispositionId;

	private TextView			mTextDate;
	private EditText			mEditTo;
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

		setContentView( R.layout.activity_firearm_disposition );
		setTitle( R.string.title_firearm_disposition );

		actionBar.setDisplayHomeAsUpEnabled( true );

		mApplication			= ( ShootingApplication ) getApplicationContext();

		intent					= getIntent();
		mFirearmId				= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ID,				0 );
		mFirearmDispositionId	= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_DISPOSITION_ID,	0 );

		mTextDate				= ( TextView ) findViewById( R.id.firearm_disposition_date );
		mEditTo					= ( EditText ) findViewById( R.id.firearm_disposition_to );
		mEditLicense			= ( EditText ) findViewById( R.id.firearm_disposition_license );
		mEditAddress			= ( EditText ) findViewById( R.id.firearm_disposition_address );
		mEditPrice				= ( EditText ) findViewById( R.id.firearm_disposition_price );

		mTextDate.setText( mApplication.currentSystemDate() );
		mTextDate.setOnClickListener( onDateClicked );

		mEditTo.addTextChangedListener( onTextChanged );

		if ( savedInstanceState != null )
		{
			mTextDate.setText( savedInstanceState.getString( STATE_DATE ) );
		}
		else if ( mFirearmDispositionId != 0 )
		{
			cursor				= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.FirearmDisposition.CONTENT_URI, mFirearmDispositionId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				finish();

				return;
			}

			try
			{
				mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.DATE ) ) ) );
			}
			catch ( ParseException e )
			{
			}

			mEditTo.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.TO ) ) );
			mEditLicense.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.LICENSE ) ) );
			mEditAddress.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.ADDRESS ) ) );
			mEditPrice.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.PRICE ) ) );

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
		getMenuInflater().inflate( R.menu.activity_firearm_disposition, menu );

		menu.findItem( R.id.menu_firearm_disposition_save ).setOnMenuItemClickListener( onSaveFirearmDispositionClicked );
		menu.findItem( R.id.menu_firearm_disposition_delete ).setOnMenuItemClickListener( onDeleteFirearmDispositionClicked );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_firearm_disposition_save ).setEnabled( mEditTo.getText().toString().trim().length() > 0 );
		menu.findItem( R.id.menu_firearm_disposition_delete ).setVisible( mFirearmDispositionId != 0 );

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

	private final MenuItem.OnMenuItemClickListener onSaveFirearmDispositionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			String dateString;
			ContentValues values;
			String license, address, price;

			values	= new ContentValues();

			try
			{
				dateString	 = mApplication.systemDateToSQLDate( mTextDate.getText().toString() );
				values.put( ShootingContract.FirearmDisposition.DATE,		dateString );
			}
			catch ( ParseException e )
			{
			}

			values.put( ShootingContract.FirearmDisposition.TO,				mEditTo.getText().toString().trim() );

			license	= mEditLicense.getText().toString().trim();
			address	= mEditAddress.getText().toString().trim();
			price	= mEditPrice.getText().toString().trim();

			values.put( ShootingContract.FirearmDisposition.LICENSE,		license.length() == 0 ? null : license );
			values.put( ShootingContract.FirearmDisposition.ADDRESS,		address.length() == 0 ? null : address );
			values.put( ShootingContract.FirearmDisposition.PRICE,			price.length() == 0 ? null : price );

			if ( mFirearmDispositionId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.FirearmDisposition.CONTENT_URI, mFirearmDispositionId ), values, null, null );
			}
			else
			{
				values.put( ShootingContract.FirearmDisposition.FIREARM_ID,	mFirearmId );
				getContentResolver().insert( ShootingContract.FirearmDisposition.CONTENT_URI, values );
			}

			finish();
			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteFirearmDispositionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( FirearmDispositionActivity.this );

			builder.setTitle( R.string.title_firearm_disposition_delete );
			builder.setMessage( R.string.confirm_firearm_disposition_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					getContentResolver().delete( ContentUris.withAppendedId( ShootingContract.FirearmDisposition.CONTENT_URI, mFirearmDispositionId ), null, null );
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

			dialog			= new DatePickerDialog( FirearmDispositionActivity.this,
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
