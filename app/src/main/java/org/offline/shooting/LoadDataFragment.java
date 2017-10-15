/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LoadDataFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private FragmentActivity	mActivity;
	private ContentResolver		mContentResolver;
	private LoaderManager		mLoaderManager;

	private long				mLoadId;

	private TextView			mTextCaliber;
	private TextView			mTextBullet;
	private TextView			mTextPowder;
	private TextView			mTextCharge;
	private TextView			mTextPrimer;
	private TextView			mTextOAL;
	private TextView			mTextCrimp;

	public LoadDataFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		Bundle arguments;

		super.onCreate( savedInstanceState );

		mActivity				= getActivity();
		mContentResolver		= mActivity.getContentResolver();
		mLoaderManager			= mActivity.getSupportLoaderManager();

		arguments				= getArguments();
		mLoadId					= arguments.getLong( ShootingApplication.PARAM_LOAD_ID );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		return inflater.inflate( R.layout.fragment_load_data, null );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );

		mTextCaliber	= ( TextView ) view.findViewById( R.id.load_data_caliber );
		mTextBullet		= ( TextView ) view.findViewById( R.id.load_data_bullet );
		mTextPowder		= ( TextView ) view.findViewById( R.id.load_data_powder );
		mTextCharge		= ( TextView ) view.findViewById( R.id.load_data_charge );
		mTextPrimer		= ( TextView ) view.findViewById( R.id.load_data_primer );
		mTextOAL		= ( TextView ) view.findViewById( R.id.load_data_oal );
		mTextCrimp		= ( TextView ) view.findViewById( R.id.load_data_crimp );

		mLoaderManager.initLoader( ShootingApplication.LOADER_LOAD_DATA_FRAGMENT, null, this );
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_LOAD_DATA_FRAGMENT );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_load_data, menu );

		menu.findItem( R.id.menu_load_data_edit ).setOnMenuItemClickListener( onEditDataClicked );
		menu.findItem( R.id.menu_load_data_delete ).setOnMenuItemClickListener( onDeleteLoadClicked );
	}

	private final MenuItem.OnMenuItemClickListener onEditDataClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, LoadDataActivity.class );
			intent.putExtra( ShootingApplication.PARAM_LOAD_ID, mLoadId );
			startActivity( intent );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onDeleteLoadClicked = new MenuItem.OnMenuItemClickListener()
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
					mContentResolver.delete( ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ), null, null );
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
							     ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ),
								 null, null, null, null );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		String primer;

		if ( !cursor.moveToFirst() )
		{
			return;
		}

		mTextCaliber.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CALIBER ) ) );
		mTextBullet.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.BULLET ) ) );
		mTextPowder.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.POWDER ) ) );
		mTextCharge.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CHARGE ) ) );
		mTextOAL.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.OAL ) ) );
		mTextCrimp.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CRIMP ) ) );

		mTextCaliber.setHint( "" );
		mTextBullet.setHint( "" );
		mTextPowder.setHint( "" );
		mTextCharge.setHint( "" );
		mTextOAL.setHint( "" );
		mTextCrimp.setHint( "" );

		if ( ( primer = cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.PRIMER ) ) ) == null )
		{
			mTextPrimer.setText( "" );
			mTextPrimer.setHint( getString( R.string.hint_unspecified ) );
		}
		else
		{
			mTextPrimer.setText( primer );
			mTextPrimer.setHint( "" );
		}
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
	}
}
