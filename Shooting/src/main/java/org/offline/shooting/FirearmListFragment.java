package org.offline.shooting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirearmListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>, FragmentOnSearchRequestedListener
{
	private ShootingApplication	mApplication;
	private FragmentActivity	mActivity;
	private Resources			mResources;
	private ContentResolver		mContentResolver;
	private LoaderManager		mLoaderManager;
	private FirearmListAdapter 	mAdapter;
	private ExecutorService		mFirearmPhotoThreadPool;

	private long				mContextFirearmId;
	private boolean				mSearchExpanded;
	private String				mSearchQuery;

	private MenuItem			mMenuItemSearch;
	private SearchView			mViewSearch;

	private static final String	STATE_SEARCH_EXPANDED	= "search_expanded";
	private static final String	STATE_SEARCH_QUERY		= "search_query";

	static class FirearmListItemData
	{
		long		firearmId;
		long		photoId;

		ImageView	image;
		TextView	firearm;
		TextView	serial;
		TextView	caliber;
		TextView	barrel;
	}

	private class FirearmPhotoLoader implements Runnable
	{
		private final FirearmListItemData	mData;
		private final long					mFirearmId;
		private final long					mPhotoId;
		private final int					mMaxImageWidth;
		private final int					mMaxImageHeight;

		FirearmPhotoLoader( FirearmListItemData data )
		{
			mData			= data;
			mFirearmId		= data.firearmId;
			mPhotoId		= data.photoId;

			mMaxImageWidth	= ( int ) mResources.getDimension( R.dimen.firearm_list_item_image_width );
			mMaxImageHeight	= ( int ) mResources.getDimension( R.dimen.firearm_list_item_image_height );
		}

		final Handler handler = new Handler()
		{
			@Override
			public void handleMessage( Message msg )
			{
				float scaleFactor;
				int bitmapWidth, bitmapHeight;
				ViewGroup.LayoutParams imageParams;
				Bitmap bitmap = ( Bitmap ) msg.obj;

				if ( mData.firearmId != mFirearmId )
				{
					return;
				}

				if ( bitmap != null )
				{
					bitmapWidth				= bitmap.getWidth();
					bitmapHeight			= bitmap.getHeight();
					scaleFactor				= Math.min( ( float ) mMaxImageWidth / bitmapWidth, ( float ) mMaxImageHeight / bitmapHeight );

					imageParams				= mData.image.getLayoutParams();
					imageParams.width		= ( int ) ( bitmapWidth * scaleFactor );
					imageParams.height		= ( int ) ( bitmapHeight * scaleFactor );
				}

				mData.image.setImageBitmap( bitmap );
			}
		};

		@Override
		public void run()
		{
			Cursor cursor;
			long photoId;
			Bitmap bitmap;
			Message message;

			if ( ( photoId = mPhotoId ) == 0 )
			{
				cursor		= mContentResolver.query( ShootingContract.FirearmPhotos.CONTENT_URI,
													  null,
												      ShootingContract.FirearmPhotos.FIREARM_ID + " = ?",
													  new String[] { String.valueOf( mFirearmId ) },
													  ShootingContract.FirearmPhotos._ID + " ASC" );
				if ( cursor.moveToFirst() )
				{
					photoId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmPhotos._ID ) );
				}

				cursor.close();
			}

			if ( photoId == 0 )
			{
				bitmap		= null;
			}
			else
			{
				bitmap		= mApplication.getBitmap( mContentResolver,
												      ContentUris.withAppendedId( ShootingContract.FirearmPhotos.CONTENT_URI, photoId ),
													  mMaxImageWidth, mMaxImageHeight );
			}

			message			= Message.obtain();
			message.obj		= bitmap;

			handler.sendMessage( message );
		}
	}

	private class FirearmListAdapter extends CursorAdapter
	{
		private final LayoutInflater mInflater;

		public FirearmListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );
			mInflater = LayoutInflater.from( context );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			FirearmListItemData data;

			view			= mInflater.inflate( R.layout.fragment_firearm_list_item, viewGroup, false );

			data			= new FirearmListItemData();
			data.image		= ( ImageView ) view.findViewById( R.id.firearm_list_item_image );
			data.firearm	= ( TextView ) view.findViewById( R.id.firearm_list_item_firearm );
			data.serial		= ( TextView ) view.findViewById( R.id.firearm_list_item_serial );
			data.caliber	= ( TextView ) view.findViewById( R.id.firearm_list_item_caliber );
			data.barrel		= ( TextView ) view.findViewById( R.id.firearm_list_item_barrel );
			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			long firearmId, photoId;
			FirearmListItemData data;

			data		= ( FirearmListItemData ) view.getTag();
			firearmId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.Firearms._ID ) );
			photoId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.Firearms.LIST_PHOTO_ID ) );

			data.firearm.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) ) + " " +
								  cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) ) );
			data.serial.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.SERIAL ) ) );
			data.caliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.CALIBER ) ) );
			data.barrel.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.BARREL ) ) );

			data.firearm.setHint( "" );
			data.serial.setHint( "" );
			data.caliber.setHint( "" );
			data.barrel.setHint( "" );

			if ( firearmId != data.firearmId || photoId != data.photoId )
			{
				data.firearmId	= firearmId;
				data.photoId	= photoId;

				data.image.setImageDrawable( null );

				mFirearmPhotoThreadPool.submit( new FirearmPhotoLoader( data ) );
			}
		}
	}

	public FirearmListFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mResources			= getResources();
		mContentResolver	= mActivity.getContentResolver();
		mLoaderManager		= mActivity.getSupportLoaderManager();
		mAdapter			= new FirearmListAdapter( mActivity, null, 0 );

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
		ListView listView;

		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_firearm_list_empty ) );

		mFirearmPhotoThreadPool	= Executors.newFixedThreadPool( ShootingApplication.THREAD_POOL_SIZE_FIREARM_LIST );

		listView				= getListView();
		listView.setPadding( ( int ) mResources.getDimension( R.dimen.activity_horizontal_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_vertical_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_horizontal_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_vertical_margin ) );
		listView.setBackgroundColor( mResources.getColor( R.color.firearm_list_background ) );
		listView.setDivider( mResources.getDrawable( R.color.firearm_list_background ) );
		listView.setDividerHeight( ( int ) mResources.getDimension( R.dimen.firearm_list_spacing ) );
		listView.setDrawSelectorOnTop( true );
		listView.setOnCreateContextMenuListener( onCreateFirearmContextMenu );

		mLoaderManager.initLoader( ShootingApplication.LOADER_FIREARM_LIST, null, this );
		mContentResolver.registerContentObserver( ShootingContract.FirearmPhotos.CONTENT_URI, true, onFirearmPhotosChanged );
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

		mFirearmPhotoThreadPool.shutdown();
		mFirearmPhotoThreadPool	= null;

		mContentResolver.unregisterContentObserver( onFirearmPhotosChanged );
		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_LIST );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_firearm_list, menu );

		menu.findItem( R.id.menu_firearm_list_add ).setOnMenuItemClickListener( onFirearmAddClicked );

		mMenuItemSearch	= menu.findItem( R.id.menu_firearm_list_search );
		mViewSearch		= ( SearchView ) mMenuItemSearch.getActionView();

		if ( mSearchExpanded )		mMenuItemSearch.expandActionView();
		if ( mSearchQuery != null )	mViewSearch.setQuery( mSearchQuery, false );

		mMenuItemSearch.setOnActionExpandListener( onFirearmSearchExpanded );
		mViewSearch.setOnQueryTextListener( onFirearmSearch );
		mViewSearch.setQueryHint( getString( R.string.hint_firearm_search ) );
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

	private final MenuItem.OnMenuItemClickListener onFirearmAddClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, FirearmProfileActivity.class );
			startActivityForResult( intent, ShootingApplication.RESULT_FIREARM_ID );

			return true;
		}
	};

	private final MenuItem.OnActionExpandListener onFirearmSearchExpanded = new MenuItem.OnActionExpandListener()
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

	private final SearchView.OnQueryTextListener onFirearmSearch = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit( String query )
		{
			mSearchQuery	= query.isEmpty() ? null : query;
			mLoaderManager.restartLoader( ShootingApplication.LOADER_FIREARM_LIST, null, FirearmListFragment.this );

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

		intent	= new Intent( mActivity, FirearmActivity.class );
		intent.putExtra( ShootingApplication.PARAM_FIREARM_ID, id );

		startActivity( intent );
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		Intent intent;

		if ( ( requestCode == ShootingApplication.RESULT_FIREARM_ID ) && ( resultCode == Activity.RESULT_OK ) )
		{
			intent	= new Intent( mActivity, FirearmActivity.class );
			intent.putExtras( data );

			startActivity( intent );
		}
	}

	private final View.OnCreateContextMenuListener onCreateFirearmContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			FirearmListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info				= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data				= ( FirearmListItemData ) info.targetView.getTag();
			mContextFirearmId	= data.firearmId;

			mActivity.getMenuInflater().inflate( R.menu.context_firearm, menu );

			menu.findItem( R.id.menu_context_firearm_view ).setOnMenuItemClickListener( onContextFirearmView );
			menu.findItem( R.id.menu_context_firearm_edit_profile ).setOnMenuItemClickListener( onContextFirearmEditProfile );
			menu.findItem( R.id.menu_context_firearm_delete ).setOnMenuItemClickListener( onContextFirearmDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmView = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, FirearmActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID, mContextFirearmId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmEditProfile = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, FirearmProfileActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID, mContextFirearmId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_firearm_delete );
			builder.setMessage( R.string.confirm_firearm_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mContextFirearmId ), null, null );
				}
			} );

			builder.show();
			return true;
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle args )
	{
		String selection;
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
										ShootingContract.Firearms.MAKE		+ " LIKE ? OR " +
										ShootingContract.Firearms.MODEL		+ " LIKE ? OR " +
										ShootingContract.Firearms.SERIAL	+ " LIKE ? OR " +
										ShootingContract.Firearms.CALIBER	+ " LIKE ? " +
								    ")";

				selectionArgs.add( term );
				selectionArgs.add( term );
				selectionArgs.add( term );
				selectionArgs.add( term );
			}
		}

		return new CursorLoader( mActivity,
								 ShootingContract.Firearms.CONTENT_URI,
								 null,
								 selection,
								 selectionArgs == null ? null : selectionArgs.toArray( new String[ selectionArgs.size() ] ),
								 ShootingContract.Firearms.MAKE + ", " + ShootingContract.Firearms.MODEL );
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

	private final ContentObserver onFirearmPhotosChanged = new ContentObserver( new Handler() )
	{
		@Override
		public void onChange( boolean selfChange )
		{
			mLoaderManager.restartLoader( ShootingApplication.LOADER_FIREARM_LIST, null, FirearmListFragment.this );
		}
	};
}