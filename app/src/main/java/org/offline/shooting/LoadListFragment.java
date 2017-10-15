package org.offline.shooting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.SearchView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;

public class LoadListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>, FragmentOnSearchRequestedListener
{
	private ShootingApplication	mApplication;
	private FragmentActivity	mActivity;
	private ContentResolver		mContentResolver;
	private LoaderManager		mLoaderManager;
	private Resources			mResources;
	private LoadListAdapter		mAdapter;

	private long				mContextLoadId;
	private boolean				mSearchExpanded;
	private String				mSearchQuery;

	private MenuItem			mMenuItemSearch;
	private SearchView			mViewSearch;

	private String				mSortOrder;

	private static final String	STATE_SEARCH_EXPANDED	= "search_expanded";
	private static final String	STATE_SEARCH_QUERY		= "search_query";

	static class LoadListItemData
	{
		long		loadId;

		TextView 	caliber;
		TextView	charge;
		TextView	powder;
		TextView	bullet;
		TextView	oal;
		TextView	ccount;
	}

	private static final String	SORT_RECENT		= "recent";
	private static final String	SORT_COUNT		= "count";
	private static final String	SORT_CALIBER	= "caliber";
	private static final String	SORT_BULLET		= "bullet";
	private static final String	SORT_POWDER		= "powder";

	private class LoadListAdapter extends CursorAdapter
	{
		private final LayoutInflater	mInflater;
		private final NumberFormat		mNumberFormat;

		public LoadListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );

			mInflater		= LayoutInflater.from( context );
			mNumberFormat	= NumberFormat.getInstance();
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			LoadListItemData data;

			view			= mInflater.inflate( R.layout.fragment_load_list_item, viewGroup, false );

			data			= new LoadListItemData();
			data.caliber	= ( TextView ) view.findViewById( R.id.load_list_item_caliber );
			data.charge		= ( TextView ) view.findViewById( R.id.load_list_item_charge );
			data.powder		= ( TextView ) view.findViewById( R.id.load_list_item_powder );
			data.bullet		= ( TextView ) view.findViewById( R.id.load_list_item_bullet );
			data.oal		= ( TextView ) view.findViewById( R.id.load_list_item_oal );
			data.ccount		= ( TextView ) view.findViewById( R.id.load_list_item_ccount );
			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			LoadListItemData data;

			data		= ( LoadListItemData ) view.getTag();
			data.loadId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.LoadLotAggregates._ID ) );

			data.caliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.CALIBER ) ) );
			data.charge.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.CHARGE ) ) );
			data.powder.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.POWDER ) ) );
			data.bullet.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.BULLET ) ) );
			data.oal.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.OAL ) ) );
			data.ccount.setText( mNumberFormat.format( cursor.getLong( cursor.getColumnIndex( ShootingContract.LoadLotAggregates.CCOUNT ) ) ) );

			data.caliber.setHint( "" );
			data.charge.setHint( "" );
			data.powder.setHint( "" );
			data.bullet.setHint( "" );
			data.oal.setHint( "" );
			data.ccount.setHint( "" );
		}
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mContentResolver	= mActivity.getContentResolver();
		mLoaderManager		= mActivity.getSupportLoaderManager();
		mResources			= mActivity.getResources();
		mAdapter			= new LoadListAdapter( mActivity, null, 0 );

		mSortOrder			= mApplication.getStringSharedPreference( ShootingApplication.PREF_LOAD_LIST_SORT, SORT_RECENT );

		mSearchExpanded		= false;
		mSearchQuery		= null;

		if ( savedInstanceState != null )
		{
			mSearchExpanded	= savedInstanceState.getBoolean( STATE_SEARCH_EXPANDED, false );
			mSearchQuery	= savedInstanceState.getString( STATE_SEARCH_QUERY );
		}
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_load_list_empty ) );

		getListView().setOnCreateContextMenuListener( onCreateLoadContextMenu );

		mLoaderManager.initLoader( ShootingApplication.LOADER_LOAD_LIST, null, this );
	}

	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putBoolean( STATE_SEARCH_EXPANDED,	mSearchExpanded );
		outState.putString( STATE_SEARCH_QUERY,		mSearchQuery );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_LOAD_LIST );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_load_list, menu );

		menu.findItem( R.id.menu_load_list_add ).setOnMenuItemClickListener( onLoadAddClicked );
		menu.findItem( R.id.menu_load_list_sort ).setOnMenuItemClickListener( onLoadListSortClicked );

		mMenuItemSearch	= menu.findItem( R.id.menu_load_list_search );
		mViewSearch		= ( SearchView ) mMenuItemSearch.getActionView();

		if ( mSearchExpanded )		mMenuItemSearch.expandActionView();
		if ( mSearchQuery != null )	mViewSearch.setQuery( mSearchQuery, false );

		mMenuItemSearch.setOnActionExpandListener( onLoadSearchExpanded );
		mViewSearch.setOnQueryTextListener( onLoadSearch );
		mViewSearch.setQueryHint( getString( R.string.hint_load_search ) );
	}

	@Override
	public boolean onFragmentSearchRequested()
	{
		if ( mMenuItemSearch == null )
		{
			return false;
		}

		mMenuItemSearch.expandActionView();
		mViewSearch.requestFocus();

		return true;
	}

	private final MenuItem.OnMenuItemClickListener onLoadAddClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, LoadDataActivity.class );
			startActivityForResult( intent, ShootingApplication.RESULT_LOAD_ID );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onLoadListSortClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			int selected;
			String[] options;
			AlertDialog.Builder builder;

			options	= mResources.getStringArray( R.array.options_load_list_sort );
			builder	= new AlertDialog.Builder( mActivity );

			if ( mSortOrder.equals( SORT_RECENT ) )				selected = 0;
			else if ( mSortOrder.equals( SORT_COUNT ) )			selected = 1;
			else if ( mSortOrder.equals( SORT_CALIBER ) )		selected = 2;
			else if ( mSortOrder.equals( SORT_BULLET ) )		selected = 3;
			else if ( mSortOrder.equals( SORT_POWDER ) )		selected = 4;
			else												selected = 0;

			builder.setTitle( R.string.title_load_list_sort );
			builder.setSingleChoiceItems( options, selected, onLoadListSortItemClicked );
			builder.show();

			return true;
		}
	};

	private final DialogInterface.OnClickListener onLoadListSortItemClicked = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick( DialogInterface dialog, int which )
		{
			switch ( which )
			{
				default	:
				case 0	: mSortOrder = SORT_RECENT;		break;
				case 1	: mSortOrder = SORT_COUNT;		break;
				case 2	: mSortOrder = SORT_CALIBER;	break;
				case 3	: mSortOrder = SORT_BULLET;		break;
				case 4	: mSortOrder = SORT_POWDER;		break;
			}

			mApplication.putStringSharedPreference( ShootingApplication.PREF_LOAD_LIST_SORT, mSortOrder );
			mLoaderManager.restartLoader( ShootingApplication.LOADER_LOAD_LIST, null, LoadListFragment.this );

			dialog.dismiss();
		}
	};

	private final MenuItem.OnActionExpandListener onLoadSearchExpanded = new MenuItem.OnActionExpandListener()
	{
		@Override
		public boolean onMenuItemActionExpand( MenuItem menuItem )
		{
			mSearchExpanded	= true;
			return true;
		}

		@Override
		public boolean onMenuItemActionCollapse( MenuItem menuItem )
		{
			mSearchExpanded	= false;
			mViewSearch.setQuery( "", true );

			return true;
		}
	};

	private final SearchView.OnQueryTextListener onLoadSearch = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit( String query )
		{
			mSearchQuery	= query.isEmpty() ? null : query;
			mLoaderManager.restartLoader( ShootingApplication.LOADER_LOAD_LIST, null, LoadListFragment.this );

			return true;
		}

		@Override
		public boolean onQueryTextChange( String newText )
		{
			return onQueryTextSubmit( newText );
		}
	};

	@Override
	public void onListItemClick( ListView listView, View view, int position, long id )
	{
		Intent intent;

		super.onListItemClick( listView, view, position, id );

		intent	= new Intent( mActivity, LoadActivity.class );
		intent.putExtra( ShootingApplication.PARAM_LOAD_ID, id );

		startActivity( intent );
	}

	private final View.OnCreateContextMenuListener onCreateLoadContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			LoadListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info			= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data			= ( LoadListItemData ) info.targetView.getTag();
			mContextLoadId	= data.loadId;

			mActivity.getMenuInflater().inflate( R.menu.context_load, menu );

			menu.findItem( R.id.menu_context_load_view ).setOnMenuItemClickListener( onContextLoadView );
			menu.findItem( R.id.menu_context_load_edit_data ).setOnMenuItemClickListener( onContextLoadEditData );
			menu.findItem( R.id.menu_context_load_delete ).setOnMenuItemClickListener( onContextLoadDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadView = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, LoadActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID, mContextLoadId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadEditData = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, LoadDataActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID, mContextLoadId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextLoadDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_load_delete );
			builder.setMessage( R.string.confirm_load_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mContextLoadId ), null, null );
				}
			} );

			builder.show();
			return true;
		}
	};

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		Intent intent;

		if ( ( requestCode == ShootingApplication.RESULT_LOAD_ID ) && ( resultCode == Activity.RESULT_OK ) )
		{
			intent	= new Intent( mActivity, LoadActivity.class );
			intent.putExtras( data );

			startActivity( intent );
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle args )
	{
		String selection, sortOrder;
		ArrayList<String> selectionArgs;

		selection				= null;
		selectionArgs			= null;

		if ( mSearchQuery != null )
		{
			selection			= "";
			selectionArgs		= new ArrayList<String>();

			for ( String term : mSearchQuery.split( " " ) )
			{
				term			= "%" + term + "%";

				if ( !selection.isEmpty() )
				{
					selection	+= " AND ";
				}

				selection		+= "( " +
										ShootingContract.LoadLotAggregates.CALIBER	+ " LIKE ? OR " +
										ShootingContract.LoadLotAggregates.BULLET	+ " LIKE ? OR " +
										ShootingContract.LoadLotAggregates.POWDER	+ " LIKE ? " +
								    ")";

				selectionArgs.add( term );
				selectionArgs.add( term );
				selectionArgs.add( term );
			}
		}

		if ( mSortOrder.equals( SORT_COUNT ) )
		{
			sortOrder			= ShootingContract.LoadLotAggregates.CCOUNT		+ " DESC, " +
								  ShootingContract.LoadLotAggregates.CALIBER	+ " ASC, " +
								  ShootingContract.LoadLotAggregates.BULLET		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.POWDER		+ " ASC, " +
							   	  ShootingContract.LoadLotAggregates.CHARGE		+ " ASC";
		}
		else if ( mSortOrder.equals( SORT_CALIBER ) )
		{
			sortOrder			= ShootingContract.LoadLotAggregates.CALIBER	+ " ASC, " +
								  ShootingContract.LoadLotAggregates.BULLET		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.POWDER		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.CHARGE		+ " ASC";
		}
		else if ( mSortOrder.equals( SORT_BULLET ) )
		{
			sortOrder			= ShootingContract.LoadLotAggregates.BULLET		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.CALIBER	+ " ASC, " +
								  ShootingContract.LoadLotAggregates.POWDER		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.CHARGE		+ " ASC";
		}
		else if ( mSortOrder.equals( SORT_POWDER ) )
		{
			sortOrder			= ShootingContract.LoadLotAggregates.POWDER		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.CALIBER	+ " ASC, " +
								  ShootingContract.LoadLotAggregates.BULLET		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.CHARGE		+ " ASC";
		}
		else
		{
			sortOrder			= ShootingContract.LoadLotAggregates.DATE		+ " DESC, " +
								  ShootingContract.LoadLotAggregates.CALIBER	+ " ASC, " +
								  ShootingContract.LoadLotAggregates.BULLET		+ " ASC, " +
								  ShootingContract.LoadLotAggregates.POWDER		+ " ASC, " +
							   	  ShootingContract.LoadLotAggregates.CHARGE		+ " ASC";
		}

		return new CursorLoader( mActivity,
								 ShootingContract.LoadLotAggregates.CONTENT_URI,
								 null,
								 selection,
								 selectionArgs == null ? null : selectionArgs.toArray( new String[ selectionArgs.size() ] ),
								 sortOrder );
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