package org.offline.shooting;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PrimerTextViewAdapter extends CursorAdapter
{
	private final ContentResolver	mContentResolver;
	private final LayoutInflater	mInflater;

	static class ViewHolder
	{
		TextView	type;
	}

	public PrimerTextViewAdapter( Activity activity )
	{
		super( activity, null, 0 );

		mContentResolver	= activity.getContentResolver();
		mInflater			= activity.getLayoutInflater();
	}

	@Override
	public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
	{
		View view;
		ViewHolder holder;

		view			= mInflater.inflate( android.R.layout.simple_dropdown_item_1line, viewGroup, false );

		holder			= new ViewHolder();
		holder.type		= ( TextView ) view.findViewById( android.R.id.text1 );
		view.setTag( holder );

		return view;
	}

	@Override
	public void bindView( View view, Context context, Cursor cursor )
	{
		ViewHolder holder;

		holder	= ( ViewHolder ) view.getTag();
		holder.type.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Primers.PRIMER ) ) );
	}

	@Override
	public Cursor runQueryOnBackgroundThread( CharSequence constraint )
	{
		String[] selectionArgs;

		selectionArgs		= new String[ 1 ];
		selectionArgs[ 0 ]	= String.valueOf( constraint ) + "_%";

		return new CursorWrapper( mContentResolver.query( ShootingContract.Primers.CONTENT_URI,
									   					  null,
									   					  ShootingContract.Primers.PRIMER + " LIKE ?",
									   					  selectionArgs,
									   					  ShootingContract.Primers.PRIMER ) )
		{
			@Override
			public int getColumnIndexOrThrow( String columnName ) throws IllegalArgumentException
			{
				if ( columnName.equals( BaseColumns._ID ) )
				{
					return super.getColumnIndexOrThrow( ShootingContract.Primers.PRIMER );
				}

				return super.getColumnIndexOrThrow( columnName );
			}
		};
	}

	@Override
	public CharSequence convertToString( Cursor cursor )
	{
		return cursor.getString( cursor.getColumnIndex( ShootingContract.Primers.PRIMER ) );
	}
}