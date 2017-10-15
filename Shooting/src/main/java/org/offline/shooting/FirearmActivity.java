/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

public class FirearmActivity extends FragmentActivity implements ActionBar.TabListener, LoaderManager.LoaderCallbacks<Cursor>
{
	private LoaderManager			mLoaderManager;

	@SuppressWarnings( "FieldCanBeLocal" )
	private SectionsPagerAdapter	mSectionsPagerAdapter;
	private ViewPager				mViewPager;

	private long					mFirearmId;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		ActionBar.Tab tab;
		ActionBar actionBar;

		super.onCreate( savedInstanceState );

		mLoaderManager			= getSupportLoaderManager();
		intent					= getIntent();
		mFirearmId				= intent.getLongExtra( ShootingApplication.PARAM_FIREARM_ID, 0 );

		setContentView( R.layout.activity_firearm );

		actionBar				=  getActionBar();
		actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
		actionBar.setDisplayHomeAsUpEnabled( true );

		mSectionsPagerAdapter	= new SectionsPagerAdapter( getSupportFragmentManager() );
		mViewPager				= ( ViewPager ) findViewById( R.id.pager );
		mViewPager.setAdapter( mSectionsPagerAdapter );
		mViewPager.setOnPageChangeListener( onPageChange );

		for ( int i = 0; i < mSectionsPagerAdapter.getCount(); i++ )
		{
			tab					= actionBar.newTab();
			tab.setText( mSectionsPagerAdapter.getPageTitle( i ) );
			tab.setTabListener( this );

			actionBar.addTab( tab );
		}

		getSupportLoaderManager().initLoader( ShootingApplication.LOADER_FIREARM_ACTIVITY, null, this );
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_FIREARM_ACTIVITY );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch ( item.getItemId() )
		{
			case android.R.id.home :
			{
				finish();
				return true;
			}
		}

		return super.onOptionsItemSelected( item );
	}

	@Override
	public boolean onSearchRequested()
	{
		Fragment fragment;

		try
		{
			if ( ( fragment = mSectionsPagerAdapter.getActiveFragment( mViewPager, mViewPager.getCurrentItem() ) ) != null )
			{
				return ( ( FragmentOnSearchRequestedListener ) fragment ).onFragmentSearchRequested();
			}
		}
		catch ( ClassCastException e )
		{
		}

		return super.onSearchRequested();
	}

	@Override
	public void onTabSelected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
	{
		mViewPager.setCurrentItem( tab.getPosition() );
	}

	@Override
	public void onTabUnselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
	{
	}

	@Override
	public void onTabReselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction )
	{
	}

	public final ViewPager.SimpleOnPageChangeListener onPageChange = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected( int position )
		{
			getActionBar().setSelectedNavigationItem( position );
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader( int i, Bundle bundle )
	{
		return new CursorLoader( this,
							     ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, mFirearmId ),
								 null, null, null, null );
	}

	@Override
	public void onLoadFinished( Loader<Cursor> cursorLoader, Cursor cursor )
	{
		if ( !cursor.moveToFirst() )
		{
			finish();
			return;
		}

		setTitle( cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MAKE ) ) + " " +
				  cursor.getString( cursor.getColumnIndex( ShootingContract.Firearms.MODEL ) ) );
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{
		public final static int TAB_PROFILE		= 0;
		public final static int TAB_ACQUISITION	= 1;
		public final static int TAB_DISPOSITION	= 2;
		public final static int	TAB_TARGETS		= 3;
		public final static int TAB_NOTES		= 4;
		public final static int TAB_COUNT		= 5;

		public SectionsPagerAdapter( FragmentManager fm )
		{
			super( fm );
		}

		@Override
		public Fragment getItem( int position )
		{
			Bundle args;
			Fragment fragment;

			args	= new Bundle();
			args.putLong( ShootingApplication.PARAM_FIREARM_ID, mFirearmId );

			switch ( position )
			{
				default					: throw new IllegalArgumentException( "Invalid pager position " + position );
				case TAB_PROFILE		: fragment = new FirearmProfileFragment();		break;
				case TAB_ACQUISITION	: fragment = new FirearmAcquisitionFragment();	break;
				case TAB_DISPOSITION	: fragment = new FirearmDispositionFragment();	break;
				case TAB_NOTES			: fragment = new FirearmNoteListFragment();		break;
				case TAB_TARGETS		:
				{
					fragment = new TargetListFragment();
					args.putInt( ShootingApplication.PARAM_LOADER_ID, ShootingApplication.LOADER_TARGET_LIST_FIREARM );

					break;
				}
			}

			fragment.setArguments( args );
			return fragment;
		}

		@Override
		public int getCount()
		{
			return TAB_COUNT;
		}

		@Override
		public CharSequence getPageTitle( int position )
		{
			switch ( position )
			{
				case TAB_PROFILE		: return getString( R.string.tab_firearm_profile );
				case TAB_ACQUISITION	: return getString( R.string.tab_firearm_acquisition );
				case TAB_DISPOSITION	: return getString( R.string.tab_firearm_disposition );
				case TAB_TARGETS		: return getString( R.string.tab_firearm_targets );
				case TAB_NOTES			: return getString( R.string.tab_firearm_notes );
			}

			throw new IllegalArgumentException( "Invalid tab position " + position );
		}

		public Fragment getActiveFragment( ViewPager container, int position )
		{
			return getSupportFragmentManager().findFragmentByTag( makeFragmentName( container.getId(), position ) );
		}

		private String makeFragmentName( int viewId, int index )
		{
		    return "android:switcher:" + viewId + ":" + index;
		}
	}
}
