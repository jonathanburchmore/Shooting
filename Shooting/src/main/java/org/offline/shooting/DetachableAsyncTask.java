package org.offline.shooting;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

public abstract class DetachableAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result>
{
	private Context			mContext;
	private Fragment		mFragment;

	private boolean			mHasProgressDialog;
	private boolean			mShowProgressDialogOnAttach;
	private ProgressDialog	mDialogProgress;

	public DetachableAsyncTask()
	{
		mContext					= null;
		mFragment					= null;

		mHasProgressDialog			= false;
		mShowProgressDialogOnAttach	= false;
		mDialogProgress				= null;
	}

	public void attach( Context context )
	{
		if ( context != mContext )
		{
			mContext	= context;
			onAttach( mContext );
		}
	}

	public void attach( Fragment fragment )
	{
		mFragment		= fragment;
		attach( fragment.getActivity() );

	}

	public void detach()
	{
		mFragment		= null;

		if ( mContext != null )
		{
			mContext	= null;
			onDetach();
		}
	}

	public Context getContext()
	{
		return mContext;
	}

	public Fragment getFragment()
	{
		return mFragment;
	}

	public void setHasProgressDialog( boolean value )
	{
		mHasProgressDialog	= value;
	}

	@SuppressWarnings( "UnusedDeclaration" )
	public boolean getHasProgressDialog()
	{
		return mHasProgressDialog;
	}

	public void showProgressDialog()
 	{
		if ( mDialogProgress != null )
		{
			mDialogProgress.show();
		}
	}

	public void dismissProgressDialog()
	{
		if ( mDialogProgress != null )
		{
			mDialogProgress.dismiss();
		}
	}

	public boolean isAttached()
	{
		return mContext != null;
	}

	public void onAttach( Context context )
	{
		if ( mHasProgressDialog )
		{
			if ( mDialogProgress == null )
			{
				mDialogProgress	= new ProgressDialog( context );

				mDialogProgress.setProgressStyle( ProgressDialog.STYLE_SPINNER );
				mDialogProgress.setMessage( context.getString( R.string.label_processing ) );
				mDialogProgress.setCancelable( false );
			}

			if ( mShowProgressDialogOnAttach )
			{
				mDialogProgress.show();
			}
		}
	}

	public void onDetach()
	{
		if ( mDialogProgress != null )
		{
			mShowProgressDialogOnAttach	= mDialogProgress.isShowing();
			mDialogProgress				= null;
		}
	}
}