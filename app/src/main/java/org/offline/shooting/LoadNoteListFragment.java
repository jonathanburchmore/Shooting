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

public class LoadNoteListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication	mApplication;
	private FragmentActivity	mActivity;
	private ContentResolver		mContentResolver;
	private LoaderManager		mLoaderManager;
	private LoadNoteListAdapter mAdapter;

	private long				mLoadId;
	private long				mContextLoadNoteId;

	static class LoadNoteListItemData
	{
		long		loadNoteId;

		TextView 	date;
		TextView	text;
	}

	private class LoadNoteListAdapter extends CursorAdapter
	{
		private final LayoutInflater mInflater;

		public LoadNoteListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );
			mInflater = LayoutInflater.from( context );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			LoadNoteListItemData data;

			view			= mInflater.inflate( R.layout.fragment_load_note_list_item, viewGroup, false );

			data			= new LoadNoteListItemData();
			data.date		= ( TextView ) view.findViewById( R.id.load_note_list_item_date );
			data.text		= ( TextView ) view.findViewById( R.id.load_note_list_item_text );
			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			LoadNoteListItemData data;

			data			= ( LoadNoteListItemData ) view.getTag();
			data.loadNoteId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.LoadNotes._ID ) );

			try
			{
				data.date.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadNotes.DATE ) ) ) );
				data.date.setHint( "" );
			}
			catch ( java.text.ParseException e )
			{
			}

			data.text.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadNotes.TEXT ) ) );
			data.text.setHint( "" );
		}
	}

	public LoadNoteListFragment()
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
		mAdapter			= new LoadNoteListAdapter( mActivity, null, 0 );

		arguments			= getArguments();
		mLoadId				= arguments.getLong( ShootingApplication.PARAM_LOAD_ID );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_load_note_list_empty ) );

		getListView().setOnCreateContextMenuListener( onCreateLoadNoteContextMenu );

		mLoaderManager.initLoader( ShootingApplication.LOADER_LOAD_NOTE_LIST, null, this );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_LOAD_NOTE_LIST );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_load_note_list, menu );

		menu.findItem( R.id.menu_load_note_add ).setOnMenuItemClickListener( onLoadNoteAddClicked );
	}

	private final MenuItem.OnMenuItemClickListener onLoadNoteAddClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, LoadNoteActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID, mLoadId );
			startActivity( intent );

			return true;
		}
	};

	@Override
	public void onListItemClick( ListView l, View v, int position, long id )
	{
		Intent intent;

		intent	= new Intent( mActivity, LoadNoteActivity.class );
		intent.putExtra( ShootingApplication.PARAM_LOAD_ID,			mLoadId );
		intent.putExtra( ShootingApplication.PARAM_LOAD_NOTE_ID,	id );
		startActivity( intent );
	}

	private final View.OnCreateContextMenuListener onCreateLoadNoteContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			LoadNoteListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info				= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data				= ( LoadNoteListItemData ) info.targetView.getTag();
			mContextLoadNoteId	= data.loadNoteId;

			mActivity.getMenuInflater().inflate( R.menu.context_load_note, menu );

			menu.findItem( R.id.menu_context_load_note_edit ).setOnMenuItemClickListener( onContextLoadNoteEdit );
			menu.findItem( R.id.menu_context_load_note_delete ).setOnMenuItemClickListener( onContextLoadNoteDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadNoteEdit = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, LoadNoteActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID,			mLoadId );
			intent.putExtra( ShootingApplication.PARAM_LOAD_NOTE_ID,	mContextLoadNoteId );
			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadNoteDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_load_note_delete );
			builder.setMessage( R.string.confirm_load_note_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.LoadNotes.CONTENT_URI, mContextLoadNoteId ), null, null );
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
								 ShootingContract.LoadNotes.CONTENT_URI,
								 null,
								 ShootingContract.LoadNotes.LOAD_ID + " = ?",
								 new String[] { String.valueOf( mLoadId ) },
								 ShootingContract.LoadNotes.DATE + " DESC" );
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
