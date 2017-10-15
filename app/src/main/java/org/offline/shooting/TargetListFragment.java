/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
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
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TargetListFragment extends PositionRetainingListFragment implements LoaderManager.LoaderCallbacks<Cursor>, FragmentOnSearchRequestedListener
{
	private ShootingApplication			mApplication;
	private FragmentActivity			mActivity;
	private Resources					mResources;
	private ContentResolver				mContentResolver;
	private LoaderManager				mLoaderManager;
	private TargetListAdapter 			mAdapter;

	private static DetachableAsyncTask	mTaskAddTarget;

	private int							mMaxImageWidth;
	private ExecutorService				mTargetImageThreadPool;

	private int							mLoaderId;
	private long						mFirearmId;
	private long						mLoadId;
	private long						mContextTargetId;
	private	String						mTargetPath;
	private boolean						mSearchExpanded;
	private String						mSearchQuery;

	private MenuItem					mMenuItemSearch;
	private SearchView					mViewSearch;

	private static final String			STATE_TARGET_PATH		= "target_path";
	private static final String			STATE_SEARCH_EXPANDED	= "search_expanded";
	private static final String			STATE_SEARCH_QUERY		= "search_query";

	static class TargetListItemData
	{
		long		targetId;

		View		distanceContainer;
		View		shotsContainer;

		ImageView	image;
		TextView	date;
		TextView	firearm;
		TextView	ammo;
		TextView	distance;
		TextView	shots;
		TextView	notes;
	}

	private class TargetImageLoader implements Runnable
	{
		private final TargetListItemData	mData;
		private final long					mTargetId;

		TargetImageLoader( TargetListItemData data )
		{
			mData			= data;
			mTargetId		= data.targetId;
		}

		final Handler handler = new Handler()
		{
			@Override
			public void handleMessage( Message msg )
			{
				Bitmap bitmap = ( Bitmap ) msg.obj;

				if ( mData.targetId != mTargetId )
				{
					return;
				}

				mData.image.setImageBitmap( bitmap );
				mData.image.setVisibility( View.VISIBLE );
			}
		};

		@Override
		public void run()
		{
			Bitmap bitmap;
			Message message;

			if ( ( bitmap = mApplication.getBitmap( mContentResolver,
													ContentUris.withAppendedId( ShootingContract.Targets.IMAGE_URI, mData.targetId ),
													mMaxImageWidth, -1 ) ) == null )
			{
				return;
			}

			message				= Message.obtain();
			message.obj			= bitmap;

			handler.sendMessage( message );
		}
	}

	private class TargetListAdapter extends CursorAdapter
	{
		private final LayoutInflater mInflater;

		public TargetListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );
			mInflater = LayoutInflater.from( context );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			TargetListItemData data;

			view					= mInflater.inflate( R.layout.fragment_target_list_item, viewGroup, false );
			data					= new TargetListItemData();

			data.distanceContainer	= view.findViewById( R.id.target_list_item_distance_container );
			data.shotsContainer		= view.findViewById( R.id.target_list_item_shots_container );

			data.image				= ( ImageView ) view.findViewById( R.id.target_list_item_image );
			data.date				= ( TextView ) view.findViewById( R.id.target_list_item_date );
			data.firearm			= ( TextView ) view.findViewById( R.id.target_list_item_firearm );
			data.ammo				= ( TextView ) view.findViewById( R.id.target_list_item_ammo );
			data.distance			= ( TextView ) view.findViewById( R.id.target_list_item_distance );
			data.shots				= ( TextView ) view.findViewById( R.id.target_list_item_shots );
			data.notes				= ( TextView ) view.findViewById( R.id.target_list_item_notes );

			view.setTag( data );

			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor )
		{
			long targetId;
			String distance, shots;
			TargetListItemData data;

			targetId			= cursor.getLong( cursor.getColumnIndex( ShootingContract.TargetsWithData._ID ) );
			data				= ( TargetListItemData ) view.getTag();

			try
			{
				data.date.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.DATE ) ) ) );
				data.date.setHint( "" );
			}
			catch ( java.text.ParseException e )
			{
			}

			data.firearm.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.FIREARM_MAKE ) ) + " " +
								  cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.FIREARM_MODEL ) ) );

			if ( cursor.getLong( cursor.getColumnIndex( ShootingContract.TargetsWithData.LOT_ID ) ) == 0 )
			{
				data.ammo.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.AMMO ) ) );
			}
			else
			{
				data.ammo.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.LOAD_BULLET ) ) + ", " +
								   cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.LOAD_CHARGE ) ) +
								   getString( R.string.label_target_list_item_grains ) +
								   cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.LOAD_POWDER ) ) );
			}

			distance			= cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.DISTANCE ) );
			shots				= cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.SHOTS ) );

			data.distance.setText( distance );
			data.shots.setText( shots );

			data.distanceContainer.setVisibility( distance.length() == 0 ? View.GONE : View.VISIBLE );
			data.shotsContainer.setVisibility( shots.length() == 0 ? View.GONE : View.VISIBLE );

			data.notes.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.TargetsWithData.NOTES ) ) );

			data.firearm.setHint( "" );
			data.ammo.setHint( "" );
			data.distance.setHint( "" );
			data.shots.setHint( "" );
			data.notes.setHint( "" );

			if ( targetId != data.targetId )
			{
				data.targetId	= targetId;

				data.image.setImageDrawable( null );

				mTargetImageThreadPool.submit( new TargetImageLoader( data ) );
			}
		}
	}

	public TargetListFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		Bundle arguments;

		super.onCreate( savedInstanceState );

		mActivity				= getActivity();
		mApplication			= ( ShootingApplication ) mActivity.getApplicationContext();
		mResources				= getResources();
		mContentResolver		= mActivity.getContentResolver();
		mLoaderManager			= mActivity.getSupportLoaderManager();
		mAdapter				= new TargetListAdapter( mActivity, null, 0 );

		mSearchExpanded			= false;
		mSearchQuery			= null;

		if ( ( arguments = getArguments() ) == null )
		{
			mLoaderId			= ShootingApplication.LOADER_TARGET_LIST_MAIN;
		}
		else
		{
			mLoaderId			= arguments.getInt( ShootingApplication.PARAM_LOADER_ID,	ShootingApplication.LOADER_TARGET_LIST_MAIN );
			mFirearmId			= arguments.getLong( ShootingApplication.PARAM_FIREARM_ID,	0 );
			mLoadId				= arguments.getLong( ShootingApplication.PARAM_LOAD_ID,		0 );
		}

		if ( savedInstanceState != null )
		{
			mTargetPath			= savedInstanceState.getString( STATE_TARGET_PATH );
			mSearchExpanded		= savedInstanceState.getBoolean( STATE_SEARCH_EXPANDED, false );
			mSearchQuery		= savedInstanceState.getString( STATE_SEARCH_QUERY );
		}
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		ListView listView;

		super.onViewCreated( view, savedInstanceState );

		mMaxImageWidth			= ( int ) ( mApplication.getScreenWidth( mActivity ) - ( mResources.getDimension( R.dimen.target_list_item_padding ) * 2 ) ) / 2;
		mTargetImageThreadPool	= Executors.newFixedThreadPool( ShootingApplication.THREAD_POOL_SIZE_TARGET_LIST );

		setHasOptionsMenu( true );
		setEmptyText( getString( R.string.label_target_list_empty ) );

		listView				= getListView();
		listView.setPadding( ( int ) mResources.getDimension( R.dimen.activity_horizontal_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_vertical_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_horizontal_margin ),
							 ( int ) mResources.getDimension( R.dimen.activity_vertical_margin ) );
		listView.setBackgroundColor( mResources.getColor( R.color.target_list_background ) );
		listView.setDivider( mResources.getDrawable( R.color.target_list_background ) );
		listView.setDividerHeight( ( int ) mResources.getDimension( R.dimen.target_list_spacing ) );
		listView.setDrawSelectorOnTop( true );
		listView.setOnCreateContextMenuListener( onCreateTargetContextMenu );

		mLoaderManager.initLoader( mLoaderId, null, this );

		if ( mTaskAddTarget != null )
		{
			mTaskAddTarget.attach( this );
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if ( mTaskAddTarget != null )
		{
			mTaskAddTarget.detach();
		}
	}

	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putString( STATE_TARGET_PATH,		mTargetPath );
		outState.putBoolean( STATE_SEARCH_EXPANDED,	mSearchExpanded );
		outState.putString( STATE_SEARCH_QUERY,		mSearchQuery );
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		mTargetImageThreadPool.shutdown();
		mTargetImageThreadPool = null;

		mLoaderManager.destroyLoader( mLoaderId );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		MenuItem camera;
		PackageManager pm;

		inflater.inflate( R.menu.fragment_target_list, menu );

		pm		= mActivity.getPackageManager();
		camera	= menu.findItem( R.id.menu_target_list_camera );

		camera.setOnMenuItemClickListener( onCameraClicked );
		menu.findItem( R.id.menu_target_list_gallery ).setOnMenuItemClickListener( onGalleryClicked );

		if ( !pm.hasSystemFeature( PackageManager.FEATURE_CAMERA ) &&
			 !pm.hasSystemFeature( PackageManager.FEATURE_CAMERA_ANY ) )
		{
			camera.setVisible( false );
		}

		mMenuItemSearch	= menu.findItem( R.id.menu_target_list_search );
		mViewSearch		= ( SearchView ) mMenuItemSearch.getActionView();

		if ( mSearchExpanded )		mMenuItemSearch.expandActionView();
		if ( mSearchQuery != null )	mViewSearch.setQuery( mSearchQuery, false );

		mMenuItemSearch.setOnActionExpandListener( onTargetSearchExpanded );
		mViewSearch.setOnQueryTextListener( onTargetSearch );
		mViewSearch.setQueryHint( getString( R.string.hint_target_search ) );
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

	private final MenuItem.OnMenuItemClickListener onCameraClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			File target;
			Intent intent;

			try
			{
				target	= File.createTempFile( "target", ".jpg" );
				target.setReadable( true, false );
				target.setWritable( true, false );
			}
			catch ( Exception e )
			{
				return true;
			}

			mTargetPath		= target.getAbsolutePath();

			intent 			= new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
			intent.putExtra( MediaStore.EXTRA_OUTPUT, Uri.fromFile( target ) );

			try
			{
				startActivityForResult( intent, ShootingApplication.RESULT_TARGET_CAMERA );
			}
			catch ( ActivityNotFoundException e )
			{
				Toast.makeText( mActivity, getString( R.string.label_launch_camera_failed ), Toast.LENGTH_SHORT ).show();
			}

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onGalleryClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent 			= new Intent( Intent.ACTION_PICK );
			intent.setType( "image/*" );

			try
			{
				startActivityForResult( intent, ShootingApplication.RESULT_TARGET_GALLERY );
			}
			catch ( ActivityNotFoundException e )
			{
				Toast.makeText( mActivity, getString( R.string.label_launch_gallery_failed ), Toast.LENGTH_SHORT ).show();
			}

			return true;
		}
	};

	private final MenuItem.OnActionExpandListener onTargetSearchExpanded = new MenuItem.OnActionExpandListener()
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

	private final SearchView.OnQueryTextListener onTargetSearch = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit( String query )
		{
			mSearchQuery	= query.isEmpty() ? null : query;
			mLoaderManager.restartLoader( mLoaderId, null, TargetListFragment.this );

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

		intent	= new Intent( mActivity, TargetActivity.class );
		intent.putExtra( ShootingApplication.PARAM_TARGET_ID, id );

		startActivity( intent );
	}

	private final View.OnCreateContextMenuListener onCreateTargetContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			TargetListItemData data;
			AdapterView.AdapterContextMenuInfo info;

			info				= ( AdapterView.AdapterContextMenuInfo ) menuInfo;
			data				= ( TargetListItemData ) info.targetView.getTag();
			mContextTargetId	= data.targetId;

			mActivity.getMenuInflater().inflate( R.menu.context_target, menu );

			menu.findItem( R.id.menu_context_target_edit ).setOnMenuItemClickListener( onContextTargetEdit );
			menu.findItem( R.id.menu_context_target_delete ).setOnMenuItemClickListener( onContextTargetDelete );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextTargetEdit = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, TargetActivity.class );
			intent.putExtra( ShootingApplication.PARAM_TARGET_ID, mContextTargetId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextTargetDelete = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			AlertDialog.Builder builder = new AlertDialog.Builder( mActivity );

			builder.setTitle( R.string.title_target_delete );
			builder.setMessage( R.string.confirm_target_delete );
			builder.setNegativeButton( R.string.button_cancel, null );
			builder.setPositiveButton( R.string.button_delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick( DialogInterface dialogInterface, int i )
				{
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Targets.CONTENT_URI, mContextTargetId ), null, null );
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

		selection			= null;
		selectionArgs		= null;

		if ( mFirearmId != 0 && mLoadId != 0 )
		{
			selection		= ShootingContract.TargetsWithData.FIREARM_ID	+ " = ? AND " +
							  ShootingContract.TargetsWithData.LOAD_ID		+ " = ?";

			selectionArgs	= new ArrayList<String>();
			selectionArgs.add( String.valueOf( mFirearmId ) );
			selectionArgs.add( String.valueOf( mLoadId ) );
		}
		else if ( mFirearmId != 0 )
		{
			selection		= ShootingContract.TargetsWithData.FIREARM_ID	+ " = ?";

			selectionArgs	= new ArrayList<String>();
			selectionArgs.add( String.valueOf( mFirearmId ) );
		}
		else if ( mLoadId != 0 )
		{
			selection		= ShootingContract.TargetsWithData.LOAD_ID 		+ " = ?";

			selectionArgs	= new ArrayList<String>();
			selectionArgs.add( String.valueOf( mLoadId ) );
		}

		if ( mSearchQuery != null )
		{
			if ( selection == null )		selection		= "";
			if ( selectionArgs == null )	selectionArgs	= new ArrayList<String>();

			for ( String term : mSearchQuery.split( " " ) )
			{
				term			= "%" + term + "%";

				if ( !selection.isEmpty() )
				{
					selection	+= " AND ";
				}

				selection		+= "( ";

				if ( mFirearmId == 0 )
				{
					selection	+=	ShootingContract.TargetsWithData.FIREARM_MAKE		+ " LIKE ? OR " +
									ShootingContract.TargetsWithData.FIREARM_MODEL		+ " LIKE ? OR ";

					selectionArgs.add( term );
					selectionArgs.add( term );
				}

				if ( mLoadId == 0 )
				{
					selection	+=	ShootingContract.TargetsWithData.LOAD_CALIBER		+ " LIKE ? OR " +
									ShootingContract.TargetsWithData.LOAD_BULLET		+ " LIKE ? OR " +
									ShootingContract.TargetsWithData.LOAD_POWDER		+ " LIKE ? OR ";

					selectionArgs.add( term );
					selectionArgs.add( term );
					selectionArgs.add( term );
				}

				selection		+=	ShootingContract.TargetsWithData.AMMO				+ " LIKE ? OR " +
									ShootingContract.TargetsWithData.TYPE				+ " LIKE ? OR " +
									ShootingContract.TargetsWithData.NOTES				+ " LIKE ? ";

				selection		+= ")";

				selectionArgs.add( term );
				selectionArgs.add( term );
				selectionArgs.add( term );
			}
		}

		return new CursorLoader( mActivity,
								 ShootingContract.TargetsWithData.CONTENT_URI,
								 null,
								 selection,
								 selectionArgs == null ? null : selectionArgs.toArray( new String[ selectionArgs.size() ] ),
								 ShootingContract.TargetsWithData.DATE + " DESC" );
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

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		File target;
		Intent intent;

		switch ( requestCode )
		{
			case ShootingApplication.RESULT_TARGET_CAMERA :
			{
				target			= new File( mTargetPath );

				if ( resultCode != Activity.RESULT_OK )
				{
					target.delete();
				}
				else
				{
					target.setReadable( true, false );
					target.setWritable( true, true );

					intent		= new Intent( mActivity, TargetActivity.class );
					intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,	mFirearmId );
					intent.putExtra( ShootingApplication.PARAM_TARGET_PATH,	mTargetPath );

					startActivityForResult( intent, ShootingApplication.RESULT_TARGET_ACTIVITY );
				}

				break;
			}
			case ShootingApplication.RESULT_TARGET_GALLERY :
			{
				if ( resultCode == Activity.RESULT_OK )
				{
					new TargetAddFromGalleryTask( data.getData() ).execute();
				}

				break;
			}
			case ShootingApplication.RESULT_TARGET_ACTIVITY :
			{
				if ( mTargetPath != null )
				{
					target		= new File( mTargetPath );
					target.delete();

					mTargetPath	= null;
				}

				break;
			}
		}

		super.onActivityResult( requestCode, resultCode, data );
	}

	private class TargetAddFromGalleryTask extends DetachableAsyncTask<Object, Object, String>
	{
		private final Uri	mUri;

		TargetAddFromGalleryTask( Uri uri )
		{
			mTaskAddTarget	= this;
			mUri			= uri;

			setHasProgressDialog( true );
			attach( TargetListFragment.this );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected String doInBackground( Object... params )
		{
			Cursor cursor;
			File source, dest;

			cursor		= mContentResolver.query( mUri, new String[] { MediaStore.Images.Media.DATA }, null, null, null );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				return null;
			}

			source		= new File( cursor.getString( 0 ) );

			cursor.close();

			try
			{
				Thread.sleep( 15000 );
				dest	= File.createTempFile( "target", ".jpg" );
			}
			catch ( Exception e )
			{
				return null;
			}

			if ( !mApplication.copyFile( source, dest ) )
			{
				return null;
			}

			dest.setReadable( true, false );
			return dest.getAbsolutePath();
		}

		@Override
		protected void onPostExecute( String targetPath )
		{
			Intent intent;
			TargetListFragment fragment;

			mTaskAddTarget	= null;
			dismissProgressDialog();

			if ( targetPath != null )
			{
				if ( ( fragment = ( TargetListFragment ) getFragment() ) == null )
				{
					new File( targetPath ).delete();
				}
				else
				{
					fragment.mTargetPath	= targetPath;
					intent					= new Intent( mActivity, TargetActivity.class );

					intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,	fragment.mFirearmId );
					intent.putExtra( ShootingApplication.PARAM_TARGET_PATH,	fragment.mTargetPath );

					fragment.startActivityForResult( intent, ShootingApplication.RESULT_TARGET_ACTIVITY );
				}
			}
		}
	}
}
