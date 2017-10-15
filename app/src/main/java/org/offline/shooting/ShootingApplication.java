/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class ShootingApplication extends Application
{
	public static final String	PARAM_FIREARM_ID					= "firearm_id";
	public static final String	PARAM_FIREARM_ACQUISITION_ID		= "firearm_acquisition_id";
	public static final String	PARAM_FIREARM_DISPOSITION_ID		= "firearm_disposition_id";
	public static final String	PARAM_FIREARM_NOTE_ID				= "firearm_note_id";

	public static final String	PARAM_LOADER_ID						= "loader_id";
	public static final String	PARAM_TARGET_ID						= "target_id";
	public static final String	PARAM_TARGET_PATH					= "target_path";

	public static final String	PARAM_LOAD_ID						= "load_id";
	public static final String	PARAM_LOAD_LOT_ID					= "load_lot_id";
	public static final String	PARAM_LOAD_NOTE_ID					= "load_note_id";

	public static final String	FRAGMENT_DIALOG_BACKUP				= "dialog_backup";
	public static final String	FRAGMENT_DIALOG_RESTORE				= "dialog_restore";

	public static final int		RESULT_FIREARM_ID					= 1;
	public static final int		RESULT_LOAD_ID						= 2;
	public static final int		RESULT_PHOTO_CAMERA					= 3;
	public static final int		RESULT_PHOTO_GALLERY				= 4;
	public static final int		RESULT_TARGET_CAMERA				= 5;
	public static final int		RESULT_TARGET_GALLERY				= 6;
	public static final int		RESULT_TARGET_ACTIVITY				= 7;
	public static final int		RESULT_RESTORE_BROWSE				= 8;

	public static final int		LOADER_ADD_IMAGES_FIREARMS			= 1;

	public static final int		LOADER_FIREARM_LIST					= 100;
	public static final int		LOADER_FIREARM_ACTIVITY				= 101;
	public static final int		LOADER_FIREARM_PROFILE_FRAGMENT		= 102;
	public static final int		LOADER_FIREARM_ACQUISITION_FRAGMENT	= 103;
	public static final int		LOADER_FIREARM_DISPOSITION_FRAGMENT	= 104;
	public static final int		LOADER_FIREARM_NOTE_LIST			= 105;

	public static final int		LOADER_TARGET_LIST_MAIN				= 200;
	public static final int		LOADER_TARGET_LIST_FIREARM			= 201;
	public static final int		LOADER_TARGET_LIST_LOAD				= 202;

	public static final int		LOADER_LOAD_LIST					= 300;
	public static final int		LOADER_LOAD_ACTIVITY				= 301;
	public static final int		LOADER_LOAD_DATA_FRAGMENT			= 302;
	public static final int		LOADER_LOAD_LOT_LIST				= 303;
	public static final int		LOADER_LOAD_NOTE_LIST				= 304;

	public static final int		LOADER_SPINNER_FIREARM_TARGET		= 400;

	public static final String	PREF_LOAD_LIST_SORT					= "load_list_sort";

	public static final int		THREAD_POOL_SIZE_FIREARM_LIST		= 3;
	public static final int		THREAD_POOL_SIZE_TARGET_LIST		= 2;

	private DateFormat			mSystemDateFormat;
	private SharedPreferences	mSharedPreferences;

	@SuppressWarnings( "FieldCanBeLocal" )
	private final int			mMaxImageWidth						= 1024;
	@SuppressWarnings( "FieldCanBeLocal" )
	private final int			mMaxImageHeight						= 1024;

	public DateFormat getSystemDateFormat()
	{
		if ( mSystemDateFormat == null )
		{
			mSystemDateFormat		= android.text.format.DateFormat.getDateFormat( this );
		}

		return mSystemDateFormat;
	}

	public Date parseSystemDate( String date ) throws ParseException
	{
		return getSystemDateFormat().parse( date );
	}

	public String formatSystemDate( Date date )
	{
		return getSystemDateFormat().format( date );
	}

	@SuppressWarnings( "UnusedDeclaration" )
	public Date parseSQLDate( String date ) throws ParseException
	{
		return ShootingContract.DateFormat.parse( date );
	}
	
	public String formatSQLDate( Date date )
	{
		return ShootingContract.DateFormat.format( date );
	}

	public String currentSystemDate()
	{
		return getSystemDateFormat().format( Calendar.getInstance().getTime() );
	}

	public String systemDateToSQLDate( String date ) throws ParseException
	{
		return ShootingContract.DateFormat.format( getSystemDateFormat().parse( date ) );
	}

	public String sqlDateToSystemDate( String date ) throws ParseException
	{
		return getSystemDateFormat().format( ShootingContract.DateFormat.parse( date ) );
	}

	public SharedPreferences getSharedPreferences()
	{
		if ( mSharedPreferences == null )
		{
			mSharedPreferences = getApplicationContext().getSharedPreferences( "settings", MODE_PRIVATE );
		}

		return mSharedPreferences;
	}

	public String getStringSharedPreference( String key, String defValue )
	{
		return getSharedPreferences().getString( key, defValue );
	}

	public void putStringSharedPreference( String key, String value )
	{
		SharedPreferences.Editor editor;

		editor	= getSharedPreferences().edit();
		editor.putString( key, value );
		editor.commit();
	}

	public boolean copyFile( File source, File dest )
	{
		int numRead;
		byte[] buffer;
		FileOutputStream destStream;
		FileInputStream sourceStream;

		buffer				= new byte[ 8192 ];

		try
		{
			sourceStream	= new FileInputStream( source );
			destStream		= new FileOutputStream( dest );

			while ( ( numRead = sourceStream.read( buffer ) ) > 0 )
			{
				destStream.write( buffer, 0, numRead );
			}
		}
		catch ( Exception e )
		{
			dest.delete();
			return false;
		}

		return true;
	}

	public boolean saveUriToFile( ContentResolver cr, Uri uri, File dest )
	{
		int numRead;
		byte[] buffer;
		InputStream sourceStream;
		FileOutputStream destStream;

		buffer				= new byte[ 8192 ];

		try
		{
			sourceStream	= cr.openInputStream( uri );
			destStream		= new FileOutputStream( dest );

			while ( ( numRead = sourceStream.read( buffer ) ) > 0 )
			{
				destStream.write( buffer, 0, numRead );
			}
		}
		catch ( Exception e )
		{
			dest.delete();
			return false;
		}

		return true;
	}

	public int getScreenWidth( Activity activity )
	{
		Point screenSize;

		screenSize	= new Point();
		activity.getWindowManager().getDefaultDisplay().getSize( screenSize );

		return screenSize.x;
	}

	private int calculateInSampleSize( int imageWidth, int imageHeight, int outputWidth, int outputHeight )
	{
		if ( imageWidth < outputWidth && imageHeight < outputHeight )
		{
			return 1;
		}

		if ( outputWidth <= 0 )					outputWidth		= imageWidth;
		if ( outputHeight <= 0 )				outputHeight	= imageHeight;

		if ( outputWidth > mMaxImageWidth )		outputWidth		= mMaxImageWidth;
		if ( outputHeight > mMaxImageHeight )	outputHeight	= mMaxImageHeight;

		return Math.max( Math.round( ( float ) imageWidth / ( float ) outputWidth ),
						 Math.round( ( float ) imageHeight / ( float ) outputHeight ) );
	}

	public Bitmap getBitmap( String filePath, int width, int height )
	{
		BitmapFactory.Options options;

		options						= new BitmapFactory.Options();
		options.inJustDecodeBounds	= true;

		BitmapFactory.decodeFile( filePath, options );

		options.inJustDecodeBounds	= false;
		options.inPurgeable			= true;
		options.inInputShareable	= true;
		options.inSampleSize		= calculateInSampleSize( options.outWidth, options.outHeight, width, height );

		try
		{
			return BitmapFactory.decodeFile( filePath, options );
		}
		catch ( OutOfMemoryError e )
		{
			return null;
		}
	}

	public Bitmap getBitmap( ContentResolver cr, Uri uri, int width, int height )
	{
		AssetFileDescriptor file;
		BitmapFactory.Options options;

		try
		{
			file	= cr.openAssetFileDescriptor( uri, "r" );
		}
		catch ( Exception e )
		{
			return null;
		}

		options						= new BitmapFactory.Options();
		options.inJustDecodeBounds	= true;

		BitmapFactory.decodeFileDescriptor( file.getFileDescriptor(), null, options );

		options.inJustDecodeBounds	= false;
		options.inPurgeable			= true;
		options.inInputShareable	= true;
		options.inSampleSize		= calculateInSampleSize( options.outWidth, options.outHeight, width, height );

		try
		{
			return BitmapFactory.decodeFileDescriptor( file.getFileDescriptor(), null, options );
		}
		catch ( OutOfMemoryError e )
		{
			return null;
		}
	}
}
