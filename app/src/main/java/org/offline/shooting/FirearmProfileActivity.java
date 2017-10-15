/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

public class FirearmProfileActivity extends FragmentActivity
{
	private long					mFirearmId;

	private AutoCompleteTextView	mEditMake;
	private EditText				mEditModel;
	private EditText				mEditSerial;
	private AutoCompleteTextView	mEditType;
	private AutoCompleteTextView	mEditCaliber;
	private EditText				mEditBarrel;

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;

		super.onCreate( savedInstanceState );

		intent			= getIntent();
		mFirearmId		= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ID, 0 );

		setContentView( R.layout.activity_firearm_profile );
		getActionBar().setDisplayHomeAsUpEnabled( true );

		mEditMake		= ( AutoCompleteTextView ) findViewById( R.id.firearm_profile_make );
		mEditModel		= ( EditText ) findViewById( R.id.firearm_profile_model );
		mEditSerial		= ( EditText ) findViewById( R.id.firearm_profile_serial );
		mEditType		= ( AutoCompleteTextView ) findViewById( R.id.firearm_profile_type );
		mEditCaliber	= ( AutoCompleteTextView ) findViewById( R.id.firearm_profile_caliber );
		mEditBarrel		= ( EditText ) findViewById( R.id.firearm_profile_barrel );

		mEditMake.addTextChangedListener( onTextChanged );
		mEditModel.addTextChangedListener( onTextChanged );
		mEditSerial.addTextChangedListener( onTextChanged );
		mEditType.addTextChangedListener( onTextChanged );
		mEditCaliber.addTextChangedListener( onTextChanged );
		mEditBarrel.addTextChangedListener( onTextChanged );

		mEditMake.setAdapter( new FirearmMakeTextViewAdapter( this ) );
		mEditType.setAdapter( new FirearmTypeTextViewAdapter( this ) );
		mEditCaliber.setAdapter( new CaliberTextViewAdapter( this ) );

		if ( mFirearmId == 0 )
		{
			setTitle( R.string.title_firearm_add );
		}
		else
		{
			setTitle( R.string.title_firearm_profile );

			if ( savedInstanceState == null )
			{
				cursor	= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ), null, null, null, null );
				if ( !cursor.moveToFirst() )
				{
					cursor.close();
					finish();

					return;
				}

				mEditMake.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) ) );
				mEditModel.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) ) );
				mEditSerial.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.SERIAL ) ) );
				mEditType.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.TYPE ) ) );
				mEditCaliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.CALIBER ) ) );
				mEditBarrel.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.BARREL ) ) );

				cursor.close();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_firearm_profile, menu );
		menu.findItem( R.id.menu_firearm_profile_save ).setOnMenuItemClickListener( onSave );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_firearm_profile_save ).setEnabled( mEditMake.getText().toString().trim().length() > 0 &&
							  										mEditModel.getText().toString().trim().length() > 0 &&
							  										mEditSerial.getText().toString().trim().length() > 0 &&
							  										mEditType.getText().toString().trim().length() > 0 &&
							  										mEditCaliber.getText().toString().trim().length() > 0 &&
							  										mEditBarrel.getText().toString().trim().length() > 0 );
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

	private final MenuItem.OnMenuItemClickListener onSave = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent data;
			long addedFirearmId;
			ContentValues values;

			values = new ContentValues();
			values.put( ShootingContract.Firearms.MAKE,		mEditMake.getText().toString().trim() );
			values.put( ShootingContract.Firearms.MODEL,	mEditModel.getText().toString().trim() );
			values.put( ShootingContract.Firearms.SERIAL,	mEditSerial.getText().toString().trim() );
			values.put( ShootingContract.Firearms.TYPE,		mEditType.getText().toString().trim() );
			values.put( ShootingContract.Firearms.CALIBER,	mEditCaliber.getText().toString().trim() );
			values.put( ShootingContract.Firearms.BARREL,	mEditBarrel.getText().toString().trim() );

			if ( mFirearmId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ), values, null, null );
			}
			else
			{
				addedFirearmId	= ContentUris.parseId( getContentResolver().insert( ShootingContract.Firearms.CONTENT_URI, values ) );
				data			= new Intent();
				data.putExtra( ShootingApplication.PARAM_FIREARM_ID, addedFirearmId );

				setResult( Activity.RESULT_OK, data );
			}

			finish();
			return true;
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
