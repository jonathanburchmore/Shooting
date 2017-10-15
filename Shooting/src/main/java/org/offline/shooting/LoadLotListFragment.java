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

import java.text.NumberFormat;

public class LoadLotListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication		mApplication;
	private FragmentActivity		mActivity;
	private ContentResolver			mContentResolver;
	private LoaderManager			mLoaderManager;
	private LoadLotListAdapter		mAdapter;

	private long					mLoadId;
	private long					mContextLoadLotId;

	static class LoadLotListItemData
	{
		long		loadLotId;

		TextView	lot;
		TextView 	date;
		TextView	primer;
		TextView	ccount;
	}

	private class LoadLotListAdapter extends CursorAdapter
	{
		private final LayoutInflater	mInflater;
		private final NumberFormat		mNumberFormat;

		public LoadLotListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );

			mInflater		= LayoutInflater.from( context );
			mNumberFormat	= NumberFormat.getInstance();
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			LoadLotListItemData data;

			view		= mInflater.inflate( R.layout.fragment_load_lot_list_item, viewGroup, false );

			data		= new LoadLotListItemData();
			data.lot	= ( TextView ) view.findViewById( R.id.load_lot_list_item_lot );
			data.date	= ( TextView ) view.findViewById( R.id.load_lot_list_item_date );
			data.primer	= ( TextView ) view.findViewById( R.id.load_lot_list_item_primer );
			data.ccount	= ( TextView ) view.findViewById( R.id.load_lot_list_item_ccount );
			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			LoadLotListItemData data;

			data			= ( LoadLotListItemData ) view.getTag();
			data.loadLotId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.LoadLots._ID ) );

			try
			{
				data.date.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.DATE ) ) ) );
				data.date.setHint( "" );
			}
			catch ( Exception e )
			{
			}

			data.lot.setText( String.valueOf( data.loadLotId ) );
			data.primer.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLots.PRIMER ) ) );
			data.ccount.setText( mNumberFormat.format( cursor.getLong( cursor.getColumnIndex( ShootingContract.LoadLots.CCOUNT ) ) ) );

			data.lot.setHint( "" );
			data.primer.setHint( "" );
			data.ccount.setHint( "" );
		}
	}

	public LoadLotListFragment()
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
		mAdapter			= new LoadLotListAdapter( mActivity, null, 0 );

		arguments			= getArguments();
		mLoadId				= arguments.getLong( ShootingApplication.PARAM_LOAD_ID );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_load_lot_list_empty ) );

		getListView().setOnCreateContextMenuListener( onCreateLoadLotContextMenu );

		mLoaderManager.initLoader( ShootingApplication.LOADER_LOAD_LOT_LIST, null, this );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_LOAD_LOT_LIST );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_load_lot_list, menu );

		menu.findItem( R.id.menu_load_lot_add ).setOnMenuItemClickListener( onLoadLotAddClicked );
	}

	private final MenuItem.OnMenuItemClickListener onLoadLotAddClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, LoadLotActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID,	mLoadId );
			startActivity( intent );

			return true;
		}
	};

	@Override
	public void onListItemClick( ListView l, View v, int position, long id )
	{
		Intent intent;

		intent	= new Intent( mActivity, LoadLotActivity.class );
		intent.putExtra( ShootingApplication.PARAM_LOAD_ID,		mLoadId );
		intent.putExtra( ShootingApplication.PARAM_LOAD_LOT_ID,	id );
		startActivity( intent );
	}

	private final View.OnCreateContextMenuListener onCreateLoadLotContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			LoadLotListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info				= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data				= ( LoadLotListItemData ) info.targetView.getTag();
			mContextLoadLotId	= data.loadLotId;

			mActivity.getMenuInflater().inflate( R.menu.context_load_lot, menu );

			menu.findItem( R.id.menu_context_load_lot_edit ).setOnMenuItemClickListener( onContextLoadLotEdit );
			menu.findItem( R.id.menu_context_load_lot_delete ).setOnMenuItemClickListener( onContextLoadLotDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadLotEdit = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, LoadLotActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID,		mLoadId );
			intent.putExtra( ShootingApplication.PARAM_LOAD_LOT_ID,	mContextLoadLotId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadLotDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_load_lot_delete );
			builder.setMessage( R.string.confirm_load_lot_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.LoadLots.CONTENT_URI, mContextLoadLotId ), null, null );
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
								 ShootingContract.LoadLots.CONTENT_URI,
								 null,
								 ShootingContract.LoadLots.LOAD_ID + " = ?",
								 new String[] { String.valueOf( mLoadId ) },
								 ShootingContract.LoadLots.DATE + " DESC" );
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