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

public class FirearmAcquisitionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	private ShootingApplication	mApplication;
	private FragmentActivity	mActivity;
	private LoaderManager		mLoaderManager;
	private NumberFormat		mCurrencyFormat;

	private long				mFirearmId;
	private long				mFirearmAcquisitionId;

	private View				mAcquisitionContent;
	private View				mAcquisitionEmpty;
	private TextView			mTextDate;
	private TextView			mTextFrom;
	private TextView			mTextLicense;
	private TextView			mTextAddress;
	private TextView			mTextPrice;

	public FirearmAcquisitionFragment()
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
		mFirearmAcquisitionId	= 0;
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
	{
		return inflater.inflate( R.layout.fragment_firearm_acquisition, null );
	}

	@Override
	public void onViewCreated( View view, Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setHasOptionsMenu( true );

		mAcquisitionContent	= view.findViewById( R.id.firearm_acquisition_content );
		mAcquisitionEmpty	= view.findViewById( R.id.firearm_acquisition_empty );
		mTextDate			= ( TextView ) view.findViewById( R.id.firearm_acquisition_date );
		mTextFrom			= ( TextView ) view.findViewById( R.id.firearm_acquisition_from );
		mTextLicense		= ( TextView ) view.findViewById( R.id.firearm_acquisition_license );
		mTextAddress		= ( TextView ) view.findViewById( R.id.firearm_acquisition_address );
		mTextPrice			= ( TextView ) view.findViewById( R.id.firearm_acquisition_price );

		mLoaderManager.initLoader( ShootingApplication.LOADER_FIREARM_ACQUISITION_FRAGMENT, null, this );
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_ACQUISITION_FRAGMENT );
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate( R.menu.fragment_firearm_acquisition, menu );

		menu.findItem( R.id.menu_firearm_acquisition_add ).setOnMenuItemClickListener( onAddEditAcquisitionClicked );
		menu.findItem( R.id.menu_firearm_acquisition_edit ).setOnMenuItemClickListener( onAddEditAcquisitionClicked );
	}

	@Override
	public void onPrepareOptionsMenu( Menu menu )
	{
		menu.findItem( R.id.menu_firearm_acquisition_add ).setVisible( mFirearmAcquisitionId == 0 );
		menu.findItem( R.id.menu_firearm_acquisition_edit ).setVisible( mFirearmAcquisitionId != 0 );
	}

	private final MenuItem.OnMenuItemClickListener onAddEditAcquisitionClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			Intent intent;

			intent = new Intent( mActivity, FirearmAcquisitionActivity.class );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ID,				mFirearmId );
			intent.putExtra( ShootingApplication.PARAM_FIREARM_ACQUISITION_ID,	mFirearmAcquisitionId );
			startActivity( intent );

			return true;
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( mActivity,
							     ShootingContract.FirearmAcquisition.CONTENT_URI,
								 null,
							     ShootingContract.FirearmAcquisition.FIREARM_ID + " = ?",
							 	 new String[] { String.valueOf( mFirearmId ) },
								 null );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		String license, address, price;

		if ( !cursor.moveToFirst() )
		{
			mFirearmAcquisitionId	= 0;
			mActivity.invalidateOptionsMenu();

			mAcquisitionContent.setVisibility( View.GONE );
			mAcquisitionEmpty.setVisibility( View.VISIBLE );

			return;
		}

		mFirearmAcquisitionId		= cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmAcquisition._ID ) );
		mActivity.invalidateOptionsMenu();

		mAcquisitionContent.setVisibility( View.VISIBLE );
		mAcquisitionEmpty.setVisibility( View.GONE );

		try
		{
			mTextDate.setText( mApplication.sqlDateToSystemDate( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.DATE ) ) ) );
			mTextDate.setHint( "" );
		}
		catch ( java.text.ParseException e )
		{
		}

		mTextFrom.setText( cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.FROM ) ) );
		mTextFrom.setHint( "" );

		if ( ( license = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.LICENSE ) ) ) == null )
		{
			mTextLicense.setText( "" );
			mTextLicense.setHint( R.string.hint_unspecified );
		}
		else
		{
			mTextLicense.setText( license );
			mTextLicense.setHint( "" );
		}

		if ( ( address = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.ADDRESS ) ) ) == null )
		{
			mTextAddress.setText( "" );
			mTextAddress.setHint( R.string.hint_unspecified );
		}
		else
		{
			mTextAddress.setText( address );
			mTextAddress.setHint( "" );
		}

		if ( ( price = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmAcquisition.PRICE ) ) ) == null )
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
