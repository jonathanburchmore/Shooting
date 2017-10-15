/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

public class TabbableAutoCompleteTextView extends AutoCompleteTextView
{
	public TabbableAutoCompleteTextView( Context context, AttributeSet attrs )
	{
		super( context, attrs );

		setOnItemClickListener( onItemClicked );
	}

	@Override
	protected void onDetachedFromWindow()
	{
		CursorAdapter adapter;

		super.onDetachedFromWindow();

		try
		{
			if ( ( adapter = ( CursorAdapter ) getAdapter() ) != null )
			{
				adapter.changeCursor( null );
			}
		}
		catch ( ClassCastException e )
		{
		}
	}

	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event )
	{
		switch ( keyCode )
		{
			case KeyEvent.KEYCODE_TAB :
			{
				if ( isPopupShowing() && event.hasNoModifiers() && focusNext() )
				{
					return true;
				}

				break;
			}
			case KeyEvent.KEYCODE_ENTER :
			{
				if ( event.hasNoModifiers() )
				{
					if ( isPopupShowing() )
					{
						performCompletion();
					}

					if ( focusNext() )
					{
						return true;
					}
				}

				break;
			}
		}

		return super.onKeyUp( keyCode, event );
	}

	@SuppressWarnings( "FieldCanBeLocal" )
	private final ListView.OnItemClickListener onItemClicked = new ListView.OnItemClickListener()
	{
		@Override
		public void onItemClick( AdapterView<?> adapterView, View view, int i, long l )
		{
			focusNext();
		}
	};

	private boolean focusNext()
	{
		View next;

		if ( ( next = focusSearch( View.FOCUS_DOWN ) ) != null )
		{
			next.requestFocus();
			return true;
		}

		return false;
	}
}
