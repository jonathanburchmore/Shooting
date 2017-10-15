package org.offline.shooting;

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

import java.text.NumberFormat;

public class FirearmDispositionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication	mApplication;
	private FragmentActivity	mActivity;
	private LoaderManager		mLoaderManager;
	private NumberFormat		mCurrencyFormat;

	private long				mFirearmId;
	private long				mFirearmDispositionId;

	private View				mDispositionContent;
	private View				mDispositionEmpty;
	private TextView			mTextDate;
	private TextView			mTextTo;
	private TextView			mTextLicense;
	private TextView			mTextAddress;
	private TextView			mTextPrice;

	public FirearmDispositionFragment()
	{
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		Bundle arguments;

		super.onCreate( savedInstanceState );

		mActivity				= getActivity();
		mApplication			= ( ShootingApplication ) mActivity.getApplicationContext();
		mLoaderManager			= mActivity.getSupportLoaderManager();
		mCurrencyFormat			= NumberFormat.getCurrencyInstance();

		arguments				= getArguments();
		mFirearmId				= arguments.getLong( ShootingApplication.PARAM_FIREARM_ID );
		mFirearmDispositionId	= 0;
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		return inflater.inflate( R.layout.fragment_firearm_disposition, null );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );

		mDispositionContent	= view.findViewById( R.id.firearm_disposition_content );
		mDispositionEmpty	= view.findViewById( R.id.firearm_disposition_empty );
		mTextDate			= ( TextView ) view.findViewById( R.id.firearm_disposition_date );
		mTextTo				= ( TextView ) view.findViewById( R.id.firearm_disposition_to );
		mTextLicense		= ( TextView ) view.findViewById( R.id.firearm_disposition_license );
		mTextAddress		= ( TextView ) view.findViewById( R.id.firearm_disposition_address );
		mTextPrice			= ( TextView ) view.findViewById( R.id.firearm_disposition_price );

		mLoaderManager.initLoader( ShootingApplication.LOADER_FIREARM_DISPOSITION_FRAGMENT, null, this );
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_DISPOSITION_FRAGMENT );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_firearm_disposition, menu );

		menu.findItem( R.id.menu_firearm_disposition_add ).setOnMenuItemClickListener( onAddEditDispositionClicked );
		menu.findItem( R.id.menu_firearm_disposition_edit ).setOnMenuItemClickListener( onAddEditDispositionClicked );
	}

	@Override
	public void onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_firearm_disposition_add ).setVisible( mFirearmDispositionId == 0 );
		menu.findItem( R.id.menu_firearm_disposition_edit ).setVisible( mFirearmDispositionId != 0 );
	}

	private final MenuItem.OnMenuItemClickListener onAddEditDispositionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, FirearmDispositionActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,				mFirearmId );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_DISPOSITION_ID,	mFirearmDispositionId );
			startActivity( intent );

			return true;
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( mActivity,
							     ShootingContract.FirearmDisposition.CONTENT_URI,
								 null,
							     ShootingContract.FirearmDisposition.FIREARM_ID + " = ?",
							 	 new String[] { String.valueOf( mFirearmId ) },
								 null );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		String license, address, price;

		if ( !cursor.moveToFirst() )
		{
			mFirearmDispositionId	= 0;
			mActivity.invalidateOptionsMenu();

			mDispositionContent.setVisibility( View.GONE );
			mDispositionEmpty.setVisibility( View.VISIBLE );

			return;
		}

		mFirearmDispositionId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmDisposition._ID ) );
		mActivity.invalidateOptionsMenu();

		mDispositionContent.setVisibility( View.VISIBLE );
		mDispositionEmpty.setVisibility( View.GONE );

		try
		{
			mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.DATE ) ) ) );
			mTextDate.setHint( "" );
		}
		catch ( java.text.ParseException e )
		{
		}

		mTextTo.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.TO ) ) );
		mTextTo.setHint( "" );

		if ( ( license = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.LICENSE ) ) ) == null )
		{
			mTextLicense.setText( "" );
			mTextLicense.setHint( R.string.hint_unspecified );
		}
		else
		{
			mTextLicense.setText( license );
			mTextLicense.setHint( "" );
		}

		if ( ( address = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.ADDRESS ) ) ) == null )
		{
			mTextAddress.setText( "" );
			mTextAddress.setHint( R.string.hint_unspecified );
		}
		else
		{
			mTextAddress.setText( address );
			mTextAddress.setHint( "" );
		}

		if ( ( price = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmDisposition.PRICE ) ) ) == null )
		{
			mTextPrice.setText( "" );
			mTextPrice.setHint( R.string.hint_unspecified );
		}
		else
		{
			mTextPrice.setText( mCurrencyFormat.format( Float.valueOf( price ) ) );
			mTextPrice.setHint( "" );
		}
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
	}
}
