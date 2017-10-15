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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

public class LoadDataActivity extends FragmentActivity
{
	private long					mLoadId;

	private AutoCompleteTextView	mEditCaliber;
	private AutoCompleteTextView	mEditBullet;
	private AutoCompleteTextView	mEditPowder;
	private EditText				mEditCharge;
	private AutoCompleteTextView	mEditPrimer;
	private EditText				mEditOAL;
	private EditText				mEditCrimp;

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;

		super.onCreate( savedInstanceState );

		intent			= getIntent();
		mLoadId			= intent.getLongExtra( ShootingApplication.PARAM_LOAD_ID, 0 );

		setContentView( R.layout.activity_load_data );
		getActionBar().setDisplayHomeAsUpEnabled( true );

		mEditCaliber	= ( AutoCompleteTextView ) findViewById( R.id.load_data_caliber );
		mEditBullet		= ( AutoCompleteTextView ) findViewById( R.id.load_data_bullet );
		mEditPowder		= ( AutoCompleteTextView ) findViewById( R.id.load_data_powder );
		mEditCharge		= ( EditText ) findViewById( R.id.load_data_charge );
		mEditPrimer		= ( AutoCompleteTextView ) findViewById( R.id.load_data_primer );
		mEditOAL		= ( EditText ) findViewById( R.id.load_data_oal );
		mEditCrimp		= ( EditText ) findViewById( R.id.load_data_crimp );

		mEditCaliber.addTextChangedListener( onTextChanged );
		mEditBullet.addTextChangedListener( onTextChanged );
		mEditPowder.addTextChangedListener( onTextChanged );
		mEditCharge.addTextChangedListener( onTextChanged );
		mEditOAL.addTextChangedListener( onTextChanged );

		mEditCaliber.setAdapter( new CaliberTextViewAdapter( this ) );
		mEditBullet.setAdapter( new LoadBulletTextViewAdapter( this ) );
		mEditPowder.setAdapter( new LoadPowderTextViewAdapter( this ) );
		mEditPrimer.setAdapter( new PrimerTextViewAdapter( this ) );

		if ( mLoadId == 0 )
		{
			setTitle( R.string.title_load_add );
		}
		else
		{
			setTitle( R.string.title_load_data );

			cursor		= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				finish();

				return;
			}

			mEditCaliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CALIBER ) ) );
			mEditBullet.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.BULLET ) ) );
			mEditPowder.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.POWDER ) ) );
			mEditCharge.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CHARGE ) ) );
			mEditPrimer.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.PRIMER ) ) );
			mEditOAL.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.OAL ) ) );
			mEditCrimp.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CRIMP ) ) );

			cursor.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_load_data, menu );
		menu.findItem( R.id.menu_load_data_save ).setOnMenuItemClickListener( onSave );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_load_data_save ).setEnabled( mEditCaliber.getText().toString().trim().length() > 0 &&
							  								  mEditBullet.getText().toString().trim().length() > 0 &&
							  								  mEditPowder.getText().toString().trim().length() > 0 &&
							  								  mEditCharge.getText().toString().trim().length() > 0 &&
							  								  mEditOAL.getText().toString().trim().length() > 0  );
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
			String primer;
			long addedLoadId;
			ContentValues values;

			values = new ContentValues();
			values.put( ShootingContract.Loads.CALIBER,	mEditCaliber.getText().toString().trim() );
			values.put( ShootingContract.Loads.BULLET,	mEditBullet.getText().toString().trim() );
			values.put( ShootingContract.Loads.POWDER,	mEditPowder.getText().toString().trim() );
			values.put( ShootingContract.Loads.CHARGE,	mEditCharge.getText().toString().trim() );
			values.put( ShootingContract.Loads.OAL,		mEditOAL.getText().toString().trim() );
			values.put( ShootingContract.Loads.CRIMP,	mEditCrimp.getText().toString().trim() );

			primer	= mEditPrimer.getText().toString().trim();
			values.put( ShootingContract.Loads.PRIMER,	primer.length() == 0 ? null : primer );

			if ( mLoadId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ), values, null, null );
			}
			else
			{
				addedLoadId	= ContentUris.parseId( getContentResolver().insert( ShootingContract.Loads.CONTENT_URI, values ) );
				data		= new Intent();
				data.putExtra( ShootingApplication.PARAM_LOAD_ID, addedLoadId );

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