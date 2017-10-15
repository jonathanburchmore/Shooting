package org.offline.shooting;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener
{
	@SuppressWarnings( "FieldCanBeLocal" )
	private SectionsPagerAdapter	mSectionsPagerAdapter;
	private ViewPager				mViewPager;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		ActionBar.Tab tab;
		final ActionBar actionBar = getActionBar();

		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_main );
		actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );

		mSectionsPagerAdapter	= new SectionsPagerAdapter( getSupportFragmentManager() );
		mViewPager				= ( ViewPager ) findViewById( R.id.pager );
		mViewPager.setAdapter( mSectionsPagerAdapter );

		mViewPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected( int position )
			{
				actionBar.setSelectedNavigationItem( position );
			}
		} );

		for ( int i = 0; i < mSectionsPagerAdapter.getCount(); i++ )
		{
			tab					= actionBar.newTab();
			tab.setText( mSectionsPagerAdapter.getPageTitle( i ) );
			tab.setTabListener( this );

			actionBar.addTab( tab );
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.activity_main, menu );

		menu.findItem( R.id.menu_backup ).setOnMenuItemClickListener( onBackupClicked );
		menu.findItem( R.id.menu_restore ).setOnMenuItemClickListener( onRestoreClicked );

		return true;
	}

	private final MenuItem.OnMenuItemClickListener onBackupClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			BackupDialog dialog;

			dialog	= new BackupDialog();
			dialog.show( getSupportFragmentManager(), ShootingApplication.FRAGMENT_DIALOG_BACKUP );

			return true;
		}
	};

	private final MenuItem.OnMenuItemClickListener onRestoreClicked = new MenuItem.OnMenuItemClickListener()
	{
		@Override
		public boolean onMenuItemClick( MenuItem menuItem )
		{
			RestoreDialog dialog;

			dialog	= new RestoreDialog();
			dialog.show( getSupportFragmentManager(), ShootingApplication.FRAGMENT_DIALOG_RESTORE );

			return true;
		}
	};

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

	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{
		public final static int TAB_FIREARMS	= 0;
		public final static int TAB_TARGETS		= 1;
		public final static int TAB_LOADS		= 2;
		public final static int TAB_COUNT		= 3;

		public SectionsPagerAdapter( FragmentManager fm )
		{
			super( fm );
		}

		@Override
		public Fragment getItem( int position )
		{
			Fragment fragment;

			switch ( position )
			{
				default				: throw new IllegalArgumentException( "Invalid tab position " + position );

				case TAB_FIREARMS	: fragment = new FirearmListFragment();	break;
				case TAB_TARGETS	: fragment = new TargetListFragment();	break;
				case TAB_LOADS		: fragment = new LoadListFragment();	break;
			}

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
				case TAB_FIREARMS	: return getString( R.string.tab_firearms );
				case TAB_TARGETS	: return getString( R.string.tab_targets );
				case TAB_LOADS		: return getString( R.string.tab_loads );
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