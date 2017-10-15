/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

public class FirearmSpinner extends CursorAdapter implements LoaderManager.LoaderCallbacks<Cursor>
{
	private final FragmentActivity	mActivity;
	private final LayoutInflater	mInflater;

	private final Spinner			mSpinner;

	private long					mSelectedId;

	static class ViewHolder
	{
		TextView	firearm;
	}

	public FirearmSpinner( FragmentActivity activity, Spinner spinner )
	{
		super( activity, null, 0 );

		mActivity	= activity;
		mInflater	= mActivity.getLayoutInflater();

		mSpinner	= spinner;
		mSpinner.setAdapter( this );
		mSpinner.setOnItemSelectedListener( onItemSelected );
	}

	public void setSelectedId( long id )
	{
		mSelectedId	= id;
		mSpinner.setSelection( getPositionById( id ) );
	}

	public long getSelectedId()
	{
		return mSelectedId;
	}

	private int getPositionById( long id )
	{
		Cursor cursor;
		int pos, indexId;

		if ( ( cursor = getCursor() ) != null )
		{
			indexId	= cursor.getColumnIndex( BaseColumns._ID );
			pos		= 0;

			cursor.moveToFirst();
			do
			{
				if ( cursor.getLong( indexId ) == id )
				{
					return pos;
				}

				pos++;
			} while ( cursor.moveToNext() );
		}

		return 0;
	}

	@Override
	public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
	{
		View view;
		ViewHolder holder;

		view			= mInflater.inflate( android.R.layout.simple_spinner_item, viewGroup, false );

		holder			= new ViewHolder();
		holder.firearm	= ( TextView ) view.findViewById( android.R.id.text1 );
		view.setTag( holder );

		return view;
	}

	@Override
	public View newDropDownView( Context context, Cursor cursor, ViewGroup viewGroup )
	{
		View view;
		ViewHolder holder;

		view			= mInflater.inflate( android.R.layout.simple_spinner_dropdown_item, viewGroup, false );

		holder			= new ViewHolder();
		holder.firearm	= ( TextView ) view.findViewById( android.R.id.text1 );
		view.setTag( holder );

		return view;
	}

	@Override
	public void bindView( View view, Context context, Cursor cursor )
	{
		ViewHolder holder;

		holder	= ( ViewHolder ) view.getTag();
		holder.firearm.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) ) + " " +
								cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) ) );
	}

	@SuppressWarnings( "FieldCanBeLocal" )
	private final Spinner.OnItemSelectedListener onItemSelected = new Spinner.OnItemSelectedListener()
	{
		@Override
		public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
		{
			mSelectedId	= id;
		}

		@Override
		public void onNothingSelected( AdapterView<?> parent )
		{
			mSelectedId	= 0;
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( mActivity,
								 ShootingContract.Firearms.CONTENT_URI,
								 null, null, null,
								 ShootingContract.Firearms.MAKE + ", " + ShootingContract.Firearms.MODEL );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		swapCursor( cursor );

		if ( mSelectedId != 0 )
		{
			mSpinner.setSelection( getPositionById( mSelectedId ) );
		}
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
		swapCursor( null );
	}
}
