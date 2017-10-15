/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.AlertDialog;
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
import android.widget.EditText;

import java.text.ParseException;
import java.util.Calendar;

public class LoadNoteActivity extends FragmentActivity
{
	private ShootingApplication	mApplication;

	private long				mLoadId;
	private long				mLoadNoteId;

	private EditText			mEditText;

	private static final String	STATE_TITLE		= "title";

	public void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		Cursor cursor;

		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_load_note );
		getActionBar().setDisplayHomeAsUpEnabled( true );

		mApplication	= ( ShootingApplication ) getApplicationContext();

		intent			= getIntent();
		mLoadId			= intent.getLongExtra( ShootingApplication.PARAM_LOAD_ID,		0 );
		mLoadNoteId		= intent.getLongExtra( ShootingApplication.PARAM_LOAD_NOTE_ID,	0 );

		mEditText		= ( EditText ) findViewById( R.id.load_note_text );

		mEditText.addTextChangedListener( onTextChanged );

		if ( savedInstanceState != null )
		{
			setTitle( savedInstanceState.getString( STATE_TITLE ) );
		}
		else if ( mLoadNoteId == 0 )
		{
			setTitle( R.string.title_load_note_add );
		}
		else
		{
			cursor		= getContentResolver().query( ContentUris.withAppendedId( ShootingContract.LoadNotes.CONTENT_URI, mLoadNoteId ), null, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				finish();
				return;
			}

			try
			{
				setTitle( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadNotes.DATE ) ) ) );
			}
			catch ( ParseException e )
			{
				setTitle( R.string.title_load_note_edit );
			}

			mEditText.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadNotes.TEXT ) ) );

			cursor.close();
		}
	}

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putString( STATE_TITLE, getTitle().toString() );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_load_note, menu );

		menu.findItem( R.id.menu_load_note_delete ).setOnMenuItemClickListener( onDeleteLoadNoteClicked );
		menu.findItem( R.id.menu_load_note_save ).setOnMenuItemClickListener( onSaveLoadNoteClicked );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_load_note_delete ).setVisible( mLoadNoteId != 0 );
		menu.findItem( R.id.menu_load_note_save ).setEnabled( mEditText.getText().toString().trim().length() > 0 );

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

	private final MenuItem.OnMenuItemClickListener onSaveLoadNoteClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			ContentValues values;

			values = new ContentValues();
			values.put( ShootingContract.LoadNotes.TEXT,		mEditText.getText().toString().trim() );

			if ( mLoadNoteId != 0 )
			{
				getContentResolver().update( ContentUris.withAppendedId( ShootingContract.LoadNotes.CONTENT_URI, mLoadNoteId ), values, null, null );
			}
			else
			{
				values.put( ShootingContract.LoadNotes.LOAD_ID,	mLoadId );
				values.put( ShootingContract.LoadNotes.DATE,	mApplication.formatSQLDate( Calendar.getInstance().getTime() ) );

				getContentResolver().insert( ShootingContract.LoadNotes.CONTENT_URI, values );
			}

			finish();
			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteLoadNoteClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( LoadNoteActivity.this );

			builder.setTitle( R.string.title_load_note_delete );
			builder.setMessage( R.string.confirm_load_note_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					getContentResolver().delete( ContentUris.withAppendedId( ShootingContract.LoadNotes.CONTENT_URI, mLoadNoteId ), null, null );
					finish();
				}
			} );

			builder.show();
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
