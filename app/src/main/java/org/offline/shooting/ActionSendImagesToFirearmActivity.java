/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.support.v4.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class ActionSendImagesToFirearmActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication			mApplication;
	private ContentResolver				mContentResolver;
	private LoaderManager				mLoaderManager;
	private LayoutInflater				mInflater;
	private Intent						mIntent;
	private FirearmListAdapter			mAdapter;

	private static DetachableAsyncTask	mTaskAddPhotos;

	public void onCreate( Bundle savedInstanceState )
	{
		ListView firearms;
		String intentAction;

		super.onCreate( savedInstanceState );

		mApplication		= ( ShootingApplication ) getApplicationContext();
		mContentResolver	= getContentResolver();
		mLoaderManager		= getSupportLoaderManager();
		mInflater			= getLayoutInflater();
		mIntent				= getIntent();
		mAdapter			= new FirearmListAdapter( this, null, 0 );

		setContentView( R.layout.activity_action_send_images );

		firearms			= ( ListView ) findViewById( R.id.action_send_images_firearms );
		firearms.setAdapter( mAdapter );

		if ( ( intentAction = mIntent.getAction() ) != null )
		{
			if ( intentAction.equals( Intent.ACTION_SEND ) )
			{
				setTitle( R.string.title_add_photo_to );
				firearms.setOnItemClickListener( onFirearmSelectedSingleImage );
			}
			else if ( intentAction.equals( Intent.ACTION_SEND_MULTIPLE ) )
			{
				setTitle( R.string.title_add_photos_to );
				firearms.setOnItemClickListener( onFirearmSelectedMultipleImages );
			}
		}

		mLoaderManager.initLoader( ShootingApplication.LOADER_ADD_IMAGES_FIREARMS, null, this );
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if ( mTaskAddPhotos != null )
		{
			mTaskAddPhotos.attach( this );
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if ( mTaskAddPhotos != null )
		{
			mTaskAddPhotos.detach();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_ADD_IMAGES_FIREARMS );
	}

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( this,
								 ShootingContract.Firearms.CONTENT_URI,
								 null, null, null,
								 ShootingContract.Firearms.MAKE + ", " + ShootingContract.Firearms.MODEL );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		mAdapter.swapCursor( cursor );
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
		mAdapter.swapCursor( null );
	}

	static class ViewHolder
	{
		TextView	firearm;
	}

	private class FirearmListAdapter extends CursorAdapter
	{
		public FirearmListAdapter( Context context, Cursor c, int flags )
		{
			super( context, c, flags );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup viewGroup )
		{
			View view;
			ViewHolder holder;

			view			= mInflater.inflate( android.R.layout.simple_list_item_1, viewGroup, false );

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

	}

	private final ListView.OnItemClickListener onFirearmSelectedSingleImage = new ListView.OnItemClickListener()
	{
		@Override
		public void onItemClick( AdapterView<?> adapterView, View view, int position, long id )
		{
			new FirearmSinglePhotoAddTask( ( Uri ) mIntent.getParcelableExtra( Intent.EXTRA_STREAM ), ( Cursor ) mAdapter.getItem( position ) ).execute();
		}
	};

	private class FirearmSinglePhotoAddTask extends DetachableAsyncTask<Object, Object, Object>
	{
		private final Uri				mUri;
		private final long				mFirearmId;
		private final String			mFirearmMake;
		private final String			mFirearmModel;

		FirearmSinglePhotoAddTask( Uri uri, Cursor cursor )
		{
			super();

			mTaskAddPhotos	= this;
			mUri			= uri;
			mFirearmId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.Firearms._ID ) );
			mFirearmMake	= cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) );
			mFirearmModel	= cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) );

			setHasProgressDialog( true );
			attach( ActionSendImagesToFirearmActivity.this );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected Object doInBackground( Object... params )
		{
			File dest;
			ContentValues values;

			dest		= new File( ShootingContract.FirearmPhotos.generatePath( ActionSendImagesToFirearmActivity.this, mFirearmId ) );

			if ( mApplication.saveUriToFile( mContentResolver, mUri, dest ) )
			{
				values	= new ContentValues();
				values.put( ShootingContract.FirearmPhotos.FIREARM_ID,	mFirearmId );
				values.put( ShootingContract.FirearmPhotos._DATA,		dest.getAbsolutePath() );

				mContentResolver.insert( ShootingContract.FirearmPhotos.CONTENT_URI, values );
			}

			return null;
		}

		@Override
		protected void onPostExecute( Object param )
		{
			ActionSendImagesToFirearmActivity activity;

			mTaskAddPhotos	= null;
			dismissProgressDialog();

			Toast.makeText( mApplication,
							mApplication.getString( R.string.label_photo_added_to ) + " " +
							mFirearmMake + " " + mFirearmModel,
							Toast.LENGTH_LONG ).show();

			if ( ( activity = ( ActionSendImagesToFirearmActivity ) getContext() ) != null )
			{
				activity.finish();
			}
		}
	}

	private final ListView.OnItemClickListener onFirearmSelectedMultipleImages = new ListView.OnItemClickListener()
	{
		@Override
		public void onItemClick( AdapterView<?> adapterView, View view, int position, long id )
		{
			new FirearmMultiplePhotoAddTask( mIntent.getParcelableArrayListExtra( Intent.EXTRA_STREAM ), ( Cursor ) mAdapter.getItem( position ) ).execute();
		}
	};

	private class FirearmMultiplePhotoAddTask extends DetachableAsyncTask<Object, Object, Object>
	{
		private final ArrayList<Parcelable>	mParcels;
		private final long					mFirearmId;
		private final String				mFirearmMake;
		private final String				mFirearmModel;

		FirearmMultiplePhotoAddTask( ArrayList<Parcelable> parcels, Cursor cursor )
		{
			mTaskAddPhotos	= this;
			mParcels		= parcels;
			mFirearmId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.Firearms._ID ) );
			mFirearmMake	= cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) );
			mFirearmModel	= cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) );

			setHasProgressDialog( true );
			attach( ActionSendImagesToFirearmActivity.this );
		}

		@Override
		protected void onPreExecute()
		{
			showProgressDialog();
		}

		@Override
		protected Object doInBackground( Object... params )
		{
			File dest;
			ContentValues values;

			for ( Parcelable parcel : mParcels )
			{
				dest		= new File( ShootingContract.FirearmPhotos.generatePath( ActionSendImagesToFirearmActivity.this, mFirearmId ) );

				if ( mApplication.saveUriToFile( mContentResolver, ( Uri ) parcel, dest ) )
				{
					values	= new ContentValues();
					values.put( ShootingContract.FirearmPhotos.FIREARM_ID,	mFirearmId );
					values.put( ShootingContract.FirearmPhotos._DATA,		dest.getAbsolutePath() );

					mContentResolver.insert( ShootingContract.FirearmPhotos.CONTENT_URI, values );
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute( Object param )
		{
			ActionSendImagesToFirearmActivity activity;

			mTaskAddPhotos	= null;
			dismissProgressDialog();

			Toast.makeText( mApplication,
							mApplication.getString( R.string.label_photos_added_to ) + " " +
							mFirearmMake + " " + mFirearmModel,
							Toast.LENGTH_LONG ).show();

			if ( ( activity = ( ActionSendImagesToFirearmActivity ) getContext() ) != null )
			{
				activity.finish();
			}
		}
	}
}
