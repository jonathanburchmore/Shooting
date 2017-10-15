package org.offline.shooting;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PositionRetainingListFragment extends ListFragment
{
	private int mTopPosition						= -1;
	private int	mTopY								= 0;

	private static final String	STATE_TOP_POSITION	= "top_position";
	private static final String	STATE_TOP_Y			= "top_y";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if ( savedInstanceState != null )
		{
			mTopPosition	= savedInstanceState.getInt( STATE_TOP_POSITION,	-1 );
			mTopY			= savedInstanceState.getInt( STATE_TOP_Y,			0 );
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		restoreScrollPosition();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		saveScrollPosition();
	}

	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );

		outState.putInt( STATE_TOP_POSITION,	mTopPosition );
		outState.putInt( STATE_TOP_Y,			mTopY );
	}

	public void saveScrollPosition()
	{
		View view;
		ListView list;

		list			= getListView();
		mTopPosition	= list.getFirstVisiblePosition();
		view			= list.getChildAt( 0 );
		mTopY			= view == null ? 0 : view.getTop();
	}

	public void restoreScrollPosition()
	{
		if ( mTopPosition >= 0 )
		{
			getListView().setSelectionFromTop( mTopPosition, mTopY );
		}
	}

	@Override
	public void setListAdapter( ListAdapter adapter )
	{
		super.setListAdapter( adapter );
		restoreScrollPosition();
	}
}
