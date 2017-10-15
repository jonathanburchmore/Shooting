/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class FirearmNoteListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication		mApplication;
	private FragmentActivity		mActivity;
	private ContentResolver			mContentResolver;
	private LoaderManager			mLoaderManager;
	private FirearmNoteListAdapter	mAdapter;

	private long					mFirearmId;
	private long					mContextFirearmNoteId;

	static class FirearmNoteListItemData
	{
		long		firearmNoteId;

		TextView 	date;
		TextView	text;
	}

	private class FirearmNoteListAdapter extends CursorAdapter
	{
		private final LayoutInflater mInflater;

		public FirearmNoteListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );
			mInflater = LayoutInflater.from( context );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			FirearmNoteListItemData data;

			view			= mInflater.inflate( R.layout.fragment_firearm_note_list_item, viewGroup, false );

			data			= new FirearmNoteListItemData();
			data.date		= ( TextView ) view.findViewById( R.id.firearm_note_list_item_date );
			data.text		= ( TextView ) view.findViewById( R.id.firearm_note_list_item_text );
			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			FirearmNoteListItemData data;

			data				= ( FirearmNoteListItemData ) view.getTag();
			data.firearmNoteId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmNotes._ID ) );

			try
			{
				data.date.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmNotes.DATE ) ) ) );
				data.date.setHint( "" );
			}
			catch ( java.text.ParseException e )
			{
			}

			data.text.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmNotes.TEXT ) ) );
			data.text.setHint( "" );
		}
	}

	public FirearmNoteListFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		Bundle arguments;

		super.onCreate( savedInstanceState );

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mContentResolver	= mActivity.getContentResolver();
		mLoaderManager		= mActivity.getSupportLoaderManager();
		mAdapter			= new FirearmNoteListAdapter( mActivity, null, 0 );

		arguments			= getArguments();
		mFirearmId			= arguments.getLong( ShootingApplication.PARAM_FIREARM_ID );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_firearm_note_list_empty ) );

		getListView().setOnCreateContextMenuListener( onCreateFirearmNoteContextMenu );

		mLoaderManager.initLoader( ShootingApplication.LOADER_FIREARM_NOTE_LIST, null, this );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_NOTE_LIST );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_firearm_note_list, menu );

		menu.findItem( R.id.menu_firearm_note_add ).setOnMenuItemClickListener( onFirearmNoteAddClicked );
	}

	private final MenuItem.OnMenuItemClickListener onFirearmNoteAddClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, FirearmNoteActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,	mFirearmId );
			startActivity( intent );

			return true;
		}
	};

	@Override
	public void onListItemClick( ListView l, View v, int position, long id )
	{
		Intent intent;

		intent	= new Intent( mActivity, FirearmNoteActivity.class );
		intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,		mFirearmId );
		intent.putExtra( ShootingApplication.PARAM_FIREARM_NOTE_ID,	id );
		startActivity( intent );
	}

	private final View.OnCreateContextMenuListener onCreateFirearmNoteContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			FirearmNoteListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info					= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data					= ( FirearmNoteListItemData ) info.targetView.getTag();
			mContextFirearmNoteId	= data.firearmNoteId;

			mActivity.getMenuInflater().inflate( R.menu.context_firearm_note, menu );

			menu.findItem( R.id.menu_context_firearm_note_edit ).setOnMenuItemClickListener( onContextFirearmNoteEdit );
			menu.findItem( R.id.menu_context_firearm_note_delete ).setOnMenuItemClickListener( onContextFirearmNoteDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmNoteEdit = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, FirearmNoteActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,		mFirearmId );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_NOTE_ID,	mContextFirearmNoteId );
			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmNoteDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_firearm_note_delete );
			builder.setMessage( R.string.confirm_firearm_note_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.FirearmNotes.CONTENT_URI, mContextFirearmNoteId ), null, null );
				}
			} );

			builder.show();
			return true;
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( mActivity,
								 ShootingContract.FirearmNotes.CONTENT_URI,
								 null,
								 ShootingContract.FirearmNotes.FIREARM_ID + " = ?",
								 new String[] { String.valueOf( mFirearmId ) },
								 ShootingContract.FirearmNotes.DATE + " DESC" );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		mAdapter.swapCursor( cursor );
		setListAdapter( mAdapter );
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
		mAdapter.swapCursor( null );
	}
}
