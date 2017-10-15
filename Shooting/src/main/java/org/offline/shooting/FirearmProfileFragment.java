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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;

public class FirearmProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication			mApplication;
	private FragmentActivity			mActivity;
	private ContentResolver 			mContentResolver;
	private LoaderManager				mLoaderManager;
	private LayoutInflater				mInflater;
	private Resources					mResources;
	private FirearmPhotoLoader			mFirearmPhotoLoader;

	private int							mMaxImageWidth;
	private int							mMaxImageHeight;
	private static DetachableAsyncTask	mTaskAddPhoto;

	private long						mFirearmId;
	private long						mFirearmListPhotoId;
	private Uri							mContextFirearmPhotoUri;

	private View						mFirearmPhotoPlaceholder;
	private ViewSwitcher				mFirearmPhotoSwitcher;

	private TextView					mTextMake;
	private TextView					mTextModel;
	private TextView					mTextSerial;
	private TextView					mTextType;
	private TextView					mTextCaliber;
	private TextView					mTextBarrel;

	private static final String			STATE_PHOTO_PATH	= "photo_path";
	private	String						mPhotoPath;

	public FirearmProfileFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		Bundle arguments;

		super.onCreate( savedInstanceState );

		setHasOptionsMenu( true );

		mActivity			= getActivity();
		mApplication		= ( ShootingApplication ) mActivity.getApplicationContext();
		mContentResolver	= mActivity.getContentResolver();
		mLoaderManager		= mActivity.getSupportLoaderManager();
		mInflater			= mActivity.getLayoutInflater();
		mResources			= getResources();

		mMaxImageWidth		= ( int ) ( mApplication.getScreenWidth( mActivity ) -
									    ( ( mResources.getDimension( R.dimen.activity_horizontal_margin ) +
										    mResources.getDimension( R.dimen.firearm_profile_image_padding ) +
											mResources.getDimension( R.dimen.firearm_profile_image_margin ) ) * 2 ) );
		mMaxImageHeight		= -1;
		mTaskAddPhoto		= null;

		arguments			= getArguments();
		mFirearmId			= arguments.getLong( ShootingApplication.PARAM_FIREARM_ID );

		if ( savedInstanceState != null )
		{
			mPhotoPath		= savedInstanceState.getString( STATE_PHOTO_PATH );
		}
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		return inflater.inflate( R.layout.fragment_firearm_profile, null );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		mFirearmPhotoPlaceholder	= view.findViewById( R.id.firearm_profile_photo_placeholder );
		mFirearmPhotoSwitcher		= ( ViewSwitcher ) view.findViewById( R.id.firearm_profile_photos );

		mTextMake					= ( TextView ) view.findViewById( R.id.firearm_profile_make );
		mTextModel					= ( TextView ) view.findViewById( R.id.firearm_profile_model );
		mTextSerial					= ( TextView ) view.findViewById( R.id.firearm_profile_serial );
		mTextType					= ( TextView ) view.findViewById( R.id.firearm_profile_type );
		mTextCaliber				= ( TextView ) view.findViewById( R.id.firearm_profile_caliber );
		mTextBarrel					= ( TextView ) view.findViewById( R.id.firearm_profile_barrel );

		if ( mTaskAddPhoto != null )
		{
			mTaskAddPhoto.attach( mActivity );
		}

		mFirearmPhotoPlaceholder.setVisibility( View.INVISIBLE );
		mFirearmPhotoPlaceholder.post( new Runnable()
		{
			@Override
			public void run()
			{
				mMaxImageHeight		= mFirearmPhotoPlaceholder.getMeasuredHeight();

				mContentResolver.registerContentObserver( ShootingContract.FirearmPhotos.CONTENT_URI, true, onFirearmPhotosChanged );
				onFirearmPhotosChanged.onChange( true );
			}
		} );

		mLoaderManager.initLoader( ShootingApplication.LOADER_FIREARM_PROFILE_FRAGMENT, null, FirearmProfileFragment.this );
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if ( mTaskAddPhoto != null )
		{
			mTaskAddPhoto.detach();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mContentResolver.unregisterContentObserver( onFirearmPhotosChanged );
		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_PROFILE_FRAGMENT );

		if ( mFirearmPhotoLoader != null )
		{
			mFirearmPhotoLoader.cancel( true );
			mFirearmPhotoLoader	= null;
		}
	}

	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );
		outState.putString( STATE_PHOTO_PATH, mPhotoPath );
	}

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( mActivity,
								 ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ),
								 null, null, null, null );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		long firearmListPhotoId;

		if ( !cursor.moveToFirst() )
		{
			return;
		}

		mTextMake.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) ) );
		mTextModel.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) ) );
		mTextSerial.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.SERIAL ) ) );
		mTextType.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.TYPE ) ) );
		mTextCaliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.CALIBER ) ) );
		mTextBarrel.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.BARREL ) ) );

		mTextMake.setHint( "" );
		mTextModel.setHint( "" );
		mTextSerial.setHint( "" );
		mTextType.setHint( "" );
		mTextCaliber.setHint( "" );
		mTextBarrel.setHint( "" );

		firearmListPhotoId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.Firearms.LIST_PHOTO_ID ) );
		if ( mFirearmListPhotoId != firearmListPhotoId )
		{
			mFirearmListPhotoId	= firearmListPhotoId;
			markFirearmListPhoto();
		}
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
	}

	private final ContentObserver onFirearmPhotosChanged = new ContentObserver( new Handler() )
	{
		@Override
		public void onChange( boolean selfChange )
		{
			if ( mFirearmPhotoLoader == null )
			{
				mFirearmPhotoLoader	= new FirearmPhotoLoader();
				mFirearmPhotoLoader.execute();
			}
		}
	};

	private class FirearmPhotoLoader extends AsyncTask<Object, Object, Boolean>
	{
		private LinearLayout	mFirearmPhotos;

		FirearmPhotoLoader()
		{
			super();
		}

		@Override
		protected void onPreExecute()
		{
			mFirearmPhotoPlaceholder.setVisibility( View.INVISIBLE );

			mFirearmPhotos	= ( LinearLayout ) mFirearmPhotoSwitcher.getNextView();
			mFirearmPhotos.removeAllViews();
		}

		@Override
		protected Boolean doInBackground( Object... params )
		{
			Cursor cursor;
			Bitmap bitmap;
			long firearmPhotoId;
			cursor	= mContentResolver.query( ShootingContract.FirearmPhotos.CONTENT_URI,
											  null,
											  ShootingContract.FirearmPhotos.FIREARM_ID + " = ?",
											  new String[] { String.valueOf( mFirearmId ) },
											  ShootingContract.FirearmPhotos._ID + " ASC" );
			if ( !cursor.moveToFirst() )
			{
				cursor.close();
				return false;
			}

			do
			{
				firearmPhotoId	= cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmPhotos._ID ) );
				if ( ( bitmap = mApplication.getBitmap( mContentResolver,
														ContentUris.withAppendedId( ShootingContract.FirearmPhotos.CONTENT_URI, firearmPhotoId ),
														mMaxImageWidth, mMaxImageHeight ) ) != null )
				{
					publishProgress( firearmPhotoId, bitmap );
				}
			} while ( cursor.moveToNext() && !isCancelled() );

			cursor.close();
			return true;
		}

		@Override
		protected void onProgressUpdate( Object... args )
		{
			View layout;
			Bitmap bitmap;
			int photoMargin;
			float scaleFactor;
			long firearmPhotoId;
			ImageView photo, overlay;
			int bitmapWidth, bitmapHeight;
			ViewGroup.LayoutParams photoParams;
			LinearLayout.LayoutParams layoutParams;

			firearmPhotoId			= ( Long ) args[ 0 ];
			bitmap					= ( Bitmap ) args[ 1 ];
			photoMargin				= ( int ) mResources.getDimension( R.dimen.firearm_profile_image_margin );

			layoutParams			= new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT );
			layoutParams.setMargins( photoMargin, 0, photoMargin, 0 );
			layoutParams.gravity	= Gravity.CENTER_VERTICAL;

			layout					= mInflater.inflate( R.layout.fragment_firearm_profile_photo, null );
			photo					= ( ImageView ) layout.findViewById( R.id.firearm_photo_image );
			overlay					= ( ImageView ) layout.findViewById( R.id.firearm_photo_list_overlay );

			bitmapWidth				= bitmap.getWidth();
			bitmapHeight			= bitmap.getHeight();
			scaleFactor				= Math.min( ( float ) mMaxImageWidth / bitmapWidth, ( float ) mMaxImageHeight / bitmapHeight );

			photoParams				= photo.getLayoutParams();
			photoParams.width		= ( int ) ( bitmapWidth * scaleFactor );
			photoParams.height		= ( int ) ( bitmapHeight * scaleFactor );

			photo.setImageBitmap( bitmap );
			photo.setTag( ContentUris.withAppendedId( ShootingContract.FirearmPhotos.CONTENT_URI, firearmPhotoId ) );

			photo.setOnClickListener( onFirearmPhotoClicked );
			photo.setOnCreateContextMenuListener( onCreateFirearmPhotoContextMenu );

			overlay.setVisibility( View.GONE );

			mFirearmPhotos.addView( layout, layoutParams );
		}

		@Override
		protected void onPostExecute( Boolean hasImages )
		{
			mFirearmPhotoSwitcher.showNext();
			( ( LinearLayout ) mFirearmPhotoSwitcher.getNextView() ).removeAllViews();

			if ( !hasImages )
			{
				mFirearmPhotoPlaceholder.setVisibility( View.VISIBLE );
			}
			else
			{
				mFirearmPhotoPlaceholder.setVisibility( View.GONE );
				markFirearmListPhoto();
			}

			mFirearmPhotoLoader	= null;
		}
	}

	private void markFirearmListPhoto()
	{
		int i;
		Uri uri;
		View layout;
		LinearLayout photos;
		ImageView image, overlay;

		if ( mFirearmListPhotoId == 0 )
		{
			return;
		}

		photos	= ( LinearLayout ) mFirearmPhotoSwitcher.getCurrentView();

		for ( i = 0; i < photos.getChildCount(); i++ )
		{
			layout	= photos.getChildAt( i );

			image	= ( ImageView ) layout.findViewById( R.id.firearm_photo_image );
			overlay	= ( ImageView ) layout.findViewById( R.id.firearm_photo_list_overlay );

			if ( ( image == null ) || ( overlay == null ) || ( ( uri = ( Uri ) image.getTag() ) == null ) )
			{
				continue;
			}

			overlay.setVisibility( ContentUris.parseId( uri ) == mFirearmListPhotoId ? View.VISIBLE : View.GONE );
		}
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		MenuItem camera;
		PackageManager pm;

		inflater.inflate( R.menu.fragment_firearm_profile, menu );

		pm		= mActivity.getPackageManager();
		camera	= menu.findItem( R.id.menu_firearm_profile_camera );

		camera.setOnMenuItemClickListener( onCameraClicked );
		menu.findItem( R.id.menu_firearm_profile_gallery ).setOnMenuItemClickListener( onGalleryClicked );
		menu.findItem( R.id.menu_firearm_profile_edit ).setOnMenuItemClickListener( onEditProfileClicked );
		menu.findItem( R.id.menu_firearm_profile_delete ).setOnMenuItemClickListener( onDeleteFirearmClicked );

		if ( !pm.hasSystemFeature( PackageManager.FEATURE_CAMERA ) &&
			 !pm.hasSystemFeature( PackageManager.FEATURE_CAMERA_ANY ) )
		{
			camera.setVisible( false );
		}
	}

	private final MenuItem.OnMenuItemClickListener onCameraClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			File photo;
			Intent intent;

			try
			{
				photo	= new File( ShootingContract.FirearmPhotos.generatePath( mActivity, mFirearmId ) );
				photo.createNewFile();
				photo.setReadable( true, false );
				photo.setWritable( true, false );
			}
			catch ( Exception e )
			{
				return true;
			}

			mPhotoPath		= photo.getAbsolutePath();

			intent 			= new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
			intent.putExtra( MediaStore.EXTRA_OUTPUT, Uri.fromFile( photo ) );

			try
			{
				startActivityForResult( intent, ShootingApplication.RESULT_PHOTO_CAMERA );
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
				startActivityForResult( intent, ShootingApplication.RESULT_PHOTO_GALLERY );
			}
			catch ( ActivityNotFoundException e )
			{
				Toast.makeText( mActivity, getString( R.string.label_launch_gallery_failed ), Toast.LENGTH_SHORT ).show();
			}

			return true;
		}
	};

	private final View.OnClickListener onFirearmPhotoClicked = new View.OnClickListener()
	{
		@Override
		public void onClick( View view )
		{
			Uri photoUri;
			Intent intent;

			photoUri	= ( Uri ) view.getTag();
			intent		= new Intent( Intent.ACTION_VIEW );
			intent.setData( photoUri );

			startActivity( intent );
		}
	};

	private final MenuItem.OnMenuItemClickListener onEditProfileClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent	= new Intent( mActivity, FirearmProfileActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID, mFirearmId );

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteFirearmClicked = new MenuItem.OnMenuItemClickListener()
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
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ), null, null );
				}
			} );

			builder.show();
			return true;
		}
	};

	private final View.OnCreateContextMenuListener onCreateFirearmPhotoContextMenu = new View.OnCreateContextMenuListener()
	{
		@Override
		public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
		{
			mContextFirearmPhotoUri	= ( Uri ) v.getTag();

			mActivity.getMenuInflater().inflate( R.menu.context_firearm_photo, menu );

			menu.findItem( R.id.menu_context_firearm_photo_view ).setOnMenuItemClickListener( onContextFirearmPhotoView );
			menu.findItem( R.id.menu_context_firearm_photo_set_as_list_photo ).setOnMenuItemClickListener( onContextFirearmPhotoSetAsListPhotoClicked );
			menu.findItem( R.id.menu_context_firearm_photo_delete ).setOnMenuItemClickListener( onContextFirearmPhotoDeleteClicked );
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmPhotoView = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent					= new Intent( Intent.ACTION_VIEW );
			intent.setData( mContextFirearmPhotoUri );
			mContextFirearmPhotoUri	= null;

			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmPhotoSetAsListPhotoClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			ContentValues values;

			values					= new ContentValues();
			values.put( ShootingContract.Firearms.LIST_PHOTO_ID,	ContentUris.parseId( mContextFirearmPhotoUri ) );

			mContentResolver.update( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ), values, null, null );
			mContextFirearmPhotoUri	= null;

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onContextFirearmPhotoDeleteClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			mContentResolver.delete( mContextFirearmPhotoUri, null, null );
			mContextFirearmPhotoUri	= null;

			return true;
		}
	};

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		switch ( requestCode )
		{
			case ShootingApplication.RESULT_PHOTO_CAMERA :
			{
				if ( resultCode != Activity.RESULT_OK )
				{
					new File( mPhotoPath ).delete();
				}
				else
				{
					new FirearmPhotoAddFromCameraTask( mPhotoPath ).execute();
					mPhotoPath	= null;
				}

				break;
			}
			case ShootingApplication.RESULT_PHOTO_GALLERY :
			{
				if ( resultCode == Activity.RESULT_OK )
				{
					new FirearmPhotoAddFromGalleryTask( data.getData() ).execute();
				}

				break;
			}
		}

		super.onActivityResult( requestCode, resultCode, data );
	}

	private class FirearmPhotoAddFromCameraTask extends DetachableAsyncTask<Object, Object, Object>
	{
		private final String	mPhotoPath;

		FirearmPhotoAddFromCameraTask( String path )
		{
			mTaskAddPhoto	= this;
			mPhotoPath		= path;

			setHasProgressDialog( true );
			attach( mActivity );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected Object doInBackground( Object... params )
		{
			File photo;
			ContentValues values;

			photo	= new File( mPhotoPath );
			values	= new ContentValues();

			values.put( ShootingContract.FirearmPhotos.FIREARM_ID,	mFirearmId );
			values.put( ShootingContract.FirearmPhotos._DATA,		mPhotoPath );

			if ( mContentResolver.insert( ShootingContract.FirearmPhotos.CONTENT_URI, values ) == null )
			{
				photo.delete();
			}
			else
			{
				photo.setWritable( true, true );
			}

			return null;
		}

		@Override
		protected void onPostExecute( Object param )
		{
			dismissProgressDialog();
			mTaskAddPhoto	= null;
		}
	}

	private class FirearmPhotoAddFromGalleryTask extends DetachableAsyncTask<Object, Object, Object>
	{
		private final Uri	mUri;

		FirearmPhotoAddFromGalleryTask( Uri uri )
		{
			mTaskAddPhoto	= this;
			mUri			= uri;

			setHasProgressDialog( true );
			attach( mActivity );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected Object doInBackground( Object... params )
		{
			Cursor cursor;
			File source, dest;
			ContentValues values;

			cursor			= mContentResolver.query( mUri, new String[] { MediaStore.Images.Media.DATA }, null, null, null );
			if ( cursor.moveToFirst() )
			{
				source		= new File( cursor.getString( 0 ) );
				dest		= new File( ShootingContract.FirearmPhotos.generatePath( mActivity, mFirearmId ) );

				if ( mApplication.copyFile( source, dest ) )
				{
					values	= new ContentValues();
					values.put( ShootingContract.FirearmPhotos.FIREARM_ID,	mFirearmId );
					values.put( ShootingContract.FirearmPhotos._DATA,		dest.getAbsolutePath() );

					mContentResolver.insert( ShootingContract.FirearmPhotos.CONTENT_URI, values );
				}
			}

			cursor.close();
			return null;
		}

		@Override
		protected void onPostExecute( Object param )
		{
			dismissProgressDialog();
			mTaskAddPhoto	= null;
		}
	}
}
