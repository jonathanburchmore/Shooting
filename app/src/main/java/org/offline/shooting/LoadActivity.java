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

public class LoadActivity extends FragmentActivity implements ActionBar.TabListener, LoaderManager.LoaderCallbacks<Cursor>
{
	private LoaderManager			mLoaderManager;

	@SuppressWarnings( "FieldCanBeLocal" )
	private SectionsPagerAdapter	mSectionsPagerAdapter;
	private ViewPager				mViewPager;

	private long					mLoadId;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		Intent intent;
		ActionBar.Tab tab;
		ActionBar actionBar;

		super.onCreate( savedInstanceState );

		mLoaderManager			= getSupportLoaderManager();

		intent					= getIntent();
		mLoadId					= intent.getLongExtra( ShootingApplication.PARAM_LOAD_ID, 0 );

		setContentView( R.layout.activity_load );

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

		mLoaderManager.initLoader( ShootingApplication.LOADER_LOAD_ACTIVITY, null, this );
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mLoaderManager.destroyLoader( ShootingApplication.LOADER_LOAD_ACTIVITY );
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
							     ContentUris.withAppendedId( ShootingContract.Loads.CONTENT_URI, mLoadId ),
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

		setTitle( cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.CALIBER ) ) + ": " +
				  cursor.getString( cursor.getColumnIndex( ShootingContract.Loads.BULLET ) ) );
	}

	@Override
	public void onLoaderReset( Loader<Cursor> cursorLoader )
	{
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{
		public final static int TAB_DATA		= 0;
		public final static int TAB_LOTS		= 1;
		public final static int	TAB_TARGETS		= 2;
		public final static int TAB_NOTES		= 3;
		public final static int TAB_COUNT		= 4;

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
			args.putLong( ShootingApplication.PARAM_LOAD_ID, mLoadId );

			switch ( position )
			{
				default				: throw new IllegalArgumentException( "Invalid pager position " + position );
				case TAB_DATA		: fragment = new LoadDataFragment();		break;
				case TAB_LOTS		: fragment = new LoadLotListFragment();		break;
				case TAB_NOTES		: fragment = new LoadNoteListFragment();	break;
				case TAB_TARGETS	:
				{
					fragment = new TargetListFragment();
					args.putInt( ShootingApplication.PARAM_LOADER_ID, ShootingApplication.LOADER_TARGET_LIST_LOAD );

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
				case TAB_DATA		: return getString( R.string.tab_load_data );
				case TAB_LOTS		: return getString( R.string.tab_load_lots );
				case TAB_TARGETS	: return getString( R.string.tab_load_targets );
				case TAB_NOTES		: return getString( R.string.tab_load_notes );
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
