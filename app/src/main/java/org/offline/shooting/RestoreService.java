/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RestoreService extends IntentService
{
	ShootingApplication			mApplication;
	ContentResolver				mContentResolver;
	Handler						mUIThreadHandler;

	public static final String	ACTION_COMPLETE		= "org.offline.shooting.RestoreService.COMPLETE";
	public static final String	PARAM_RESTORE_PATH	= "restore_path";

	public RestoreService()
	{
		super( "RestoreService" );
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mApplication		= ( ShootingApplication ) getApplicationContext();
		mContentResolver	= getContentResolver();
		mUIThreadHandler	= new Handler();
	}

	public static boolean isRunning( Context context )
	{
		ActivityManager manager;

		manager	= ( ActivityManager ) context.getSystemService( ACTIVITY_SERVICE );
		for ( ActivityManager.RunningServiceInfo service : manager.getRunningServices( Integer.MAX_VALUE ) )
		{
			if ( service.service.getClassName().equals( RestoreService.class.getName() ) )
			{
				return true;
			}
		}

		return false;
	}

	private void notifyComplete( final String message )
	{
		sendBroadcast( new Intent().setAction( ACTION_COMPLETE ) );

		mUIThreadHandler.post( new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText( mApplication, message, Toast.LENGTH_LONG ).show();
			}
		} );
	}

	@Override
	protected void onHandleIntent( Intent intent )
	{
		File source;
		ZipEntry entry;
		String filename;
		String restorePath;
		ZipInputStream sourceZip;
		FileInputStream sourceStream;

		if ( ( restorePath = intent.getStringExtra( PARAM_RESTORE_PATH ) ) == null )
		{
			notifyComplete( mApplication.getString( R.string.label_restore_failed ) + mApplication.getString( R.string.label_missing_parameter ) );
			return;
		}

		source				= new File( restorePath );
		sourceStream		= null;
		sourceZip			= null;

		try
		{
			sourceStream	= new FileInputStream( source );
			sourceZip		= new ZipInputStream( sourceStream );

			deleteAllData();

			while ( ( entry = sourceZip.getNextEntry() ) != null )
			{
				filename	= entry.getName();

				if ( filename.equals( "shooting.xml" ) )
				{
					restoreData( sourceZip );
				}
				else if ( filename.startsWith( "firearms/" ) )
				{
					restoreFirearmPhotoImage( sourceZip, filename.substring( 9 ) );
				}
				else if ( filename.startsWith( "targets/" ) )
				{
					restoreTargetImage( sourceZip, filename.substring( 8 ) );
				}
			}

			sourceZip.close();
			sourceStream.close();

			notifyComplete( mApplication.getString( R.string.label_restore_complete ) );
		}
		catch ( Exception e )
		{
			if ( sourceZip != null )
			{
				try
				{
					sourceZip.close();
				}
				catch ( Exception ignored )
				{
				}
			}

			if ( sourceStream != null )
			{
				try
				{
					sourceStream.close();
				}
				catch ( Exception ignored )
				{
				}
			}

			notifyComplete( mApplication.getString( R.string.label_restore_failed ) + e.getMessage() );
		}
	}

	private void deleteAllData()
	{
		mContentResolver.delete( ShootingContract.LoadNotes.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.LoadLots.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.Loads.CONTENT_URI, null, null );

		mContentResolver.delete( ShootingContract.Targets.CONTENT_URI, null, null );

		mContentResolver.delete( ShootingContract.FirearmNotes.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.FirearmDisposition.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.FirearmAcquisition.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.FirearmPhotos.CONTENT_URI, null, null );
		mContentResolver.delete( ShootingContract.Firearms.CONTENT_URI, null, null );
	}

	private void restoreData( ZipInputStream zip ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		XmlPullParser parser;

		parser	= Xml.newPullParser();
  		parser.setFeature( XmlPullParser.FEATURE_PROCESS_NAMESPACES, false );
  		parser.setInput( zip, null );
		parser.nextTag();

		parser.require( XmlPullParser.START_TAG, "", "shooting" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( "firearm" ) )					restoreFirearm( parser );
			else if ( tag.equals( "firearmphoto" ) )		restoreFirearmPhoto( parser );
			else if ( tag.equals( "firearmlistphotoid" ) )	restoreFirearmListPhotoId( parser );
			else if ( tag.equals( "firearmacquisition" ) )	restoreFirearmAcquisition( parser );
			else if ( tag.equals( "firearmdisposition" ) )	restoreFirearmDisposition( parser );
			else if ( tag.equals( "firearmnote" ) )			restoreFirearmNote( parser );
			else if ( tag.equals( "load" ) )				restoreLoad( parser );
			else if ( tag.equals( "loadlot" ) )				restoreLoadLot( parser );
			else if ( tag.equals( "loadnote" ) )			restoreLoadNote( parser );
			else if ( tag.equals( "target" ) )				restoreTarget( parser );
			else											restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "shooting" );
	}

	private void restoreFirearm( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "firearm" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.Firearms._ID ) )			values.put( ShootingContract.Firearms._ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.MAKE ) )	values.put( ShootingContract.Firearms.MAKE,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.MODEL ) )	values.put( ShootingContract.Firearms.MODEL,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.SERIAL ) )	values.put( ShootingContract.Firearms.SERIAL,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.TYPE ) )	values.put( ShootingContract.Firearms.TYPE,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.CALIBER ) )	values.put( ShootingContract.Firearms.CALIBER,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.BARREL ) )	values.put( ShootingContract.Firearms.BARREL,	restoreText( parser ) );
			else														restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearm" );

		mContentResolver.insert( ShootingContract.Firearms.CONTENT_URI, values );
	}

	private void restoreFirearmPhoto( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "firearmphoto" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.FirearmPhotos._ID ) )				values.put( ShootingContract.FirearmPhotos._ID,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmPhotos.FIREARM_ID ) )	values.put( ShootingContract.FirearmPhotos.FIREARM_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmPhotos._DATA ) )		values.put( ShootingContract.FirearmPhotos._DATA,		new File( mApplication.getExternalFilesDir( "firearms" ), restoreText( parser ) ).getAbsolutePath() );
			else																restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearmphoto" );

		mContentResolver.insert( ShootingContract.FirearmPhotos.CONTENT_URI, values );
	}

	private void restoreFirearmListPhotoId( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		long firearmId;
		ContentValues values;

		firearmId	= 0;
		values		= new ContentValues();

		parser.require( XmlPullParser.START_TAG, "", "firearmlistphotoid" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.Firearms._ID ) )					firearmId = Long.valueOf( restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Firearms.LIST_PHOTO_ID ) )	values.put( ShootingContract.Firearms.LIST_PHOTO_ID,	restoreText( parser ) );
			else																restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearmlistphotoid" );

		if ( firearmId != 0 )
		{
			mContentResolver.update( ContentUris.withAppendedId( ShootingContract.Firearms.CONTENT_URI, firearmId ), values, null, null );
		}
	}

	private void restoreFirearmAcquisition( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "firearmacquisition" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.FirearmAcquisition._ID ) )				values.put( ShootingContract.FirearmAcquisition._ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.FIREARM_ID ) )	values.put( ShootingContract.FirearmAcquisition.FIREARM_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.DATE ) )			values.put( ShootingContract.FirearmAcquisition.DATE,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.FROM ) )			values.put( ShootingContract.FirearmAcquisition.FROM,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.LICENSE ) )		values.put( ShootingContract.FirearmAcquisition.LICENSE,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.ADDRESS ) )		values.put( ShootingContract.FirearmAcquisition.ADDRESS,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmAcquisition.PRICE ) )			values.put( ShootingContract.FirearmAcquisition.PRICE,		restoreText( parser ) );
			else																		restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearmacquisition" );

		mContentResolver.insert( ShootingContract.FirearmAcquisition.CONTENT_URI, values );
	}

	private void restoreFirearmDisposition( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "firearmdisposition" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.FirearmDisposition._ID ) )				values.put( ShootingContract.FirearmDisposition._ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.FIREARM_ID ) )	values.put( ShootingContract.FirearmDisposition.FIREARM_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.DATE ) )			values.put( ShootingContract.FirearmDisposition.DATE,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.TO ) )			values.put( ShootingContract.FirearmDisposition.TO,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.LICENSE ) )		values.put( ShootingContract.FirearmDisposition.LICENSE,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.ADDRESS ) )		values.put( ShootingContract.FirearmDisposition.ADDRESS,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmDisposition.PRICE ) )			values.put( ShootingContract.FirearmDisposition.PRICE,		restoreText( parser ) );
			else																		restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearmdisposition" );

		mContentResolver.insert( ShootingContract.FirearmDisposition.CONTENT_URI, values );
	}

	private void restoreFirearmNote( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "firearmnote" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.FirearmNotes._ID ) )				values.put( ShootingContract.FirearmNotes._ID,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmNotes.FIREARM_ID ) )	values.put( ShootingContract.FirearmNotes.FIREARM_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmNotes.DATE ) )		values.put( ShootingContract.FirearmNotes.DATE,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.FirearmNotes.TEXT ) )		values.put( ShootingContract.FirearmNotes.TEXT,			restoreText( parser ) );
			else																restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "firearmnote" );

		mContentResolver.insert( ShootingContract.FirearmNotes.CONTENT_URI, values );
	}

	private void restoreLoad( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "load" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.Loads._ID ) )				values.put( ShootingContract.Loads._ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.CALIBER ) )	values.put( ShootingContract.Loads.CALIBER,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.BULLET ) )		values.put( ShootingContract.Loads.BULLET,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.POWDER ) )		values.put( ShootingContract.Loads.POWDER,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.CHARGE ) )		values.put( ShootingContract.Loads.CHARGE,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.PRIMER ) )		values.put( ShootingContract.Loads.PRIMER,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.OAL ) )		values.put( ShootingContract.Loads.OAL,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Loads.CRIMP) )		values.put( ShootingContract.Loads.CRIMP,	restoreText( parser ) );
			else														restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "load" );

		mContentResolver.insert( ShootingContract.Loads.CONTENT_URI, values );
	}

	private void restoreLoadLot( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "loadlot" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.LoadLots._ID ) )				values.put( ShootingContract.LoadLots._ID,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.LOAD_ID ) )		values.put( ShootingContract.LoadLots.LOAD_ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.DATE ) )		values.put( ShootingContract.LoadLots.DATE,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.CCOUNT ) )		values.put( ShootingContract.LoadLots.CCOUNT,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.POWDER_LOT ) )	values.put( ShootingContract.LoadLots.POWDER_LOT,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.PRIMER ) )		values.put( ShootingContract.LoadLots.PRIMER,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadLots.PRIMER_LOT ) )	values.put( ShootingContract.LoadLots.PRIMER_LOT,	restoreText( parser ) );
			else															restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "loadlot" );

		mContentResolver.insert( ShootingContract.LoadLots.CONTENT_URI, values );
	}

	private void restoreLoadNote( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "loadnote" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.LoadNotes._ID ) )				values.put( ShootingContract.LoadNotes._ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadNotes.LOAD_ID ) )	values.put( ShootingContract.LoadNotes.LOAD_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadNotes.DATE ) )		values.put( ShootingContract.LoadNotes.DATE,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.LoadNotes.TEXT ) )		values.put( ShootingContract.LoadNotes.TEXT,	restoreText( parser ) );
			else															restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "loadnote" );

		mContentResolver.insert( ShootingContract.LoadNotes.CONTENT_URI, values );
	}

	private void restoreTarget( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String tag;
		ContentValues values;

		values	= new ContentValues();
		parser.require( XmlPullParser.START_TAG, "", "target" );

		while ( parser.next() != XmlPullParser.END_TAG )
		{
			if ( parser.getEventType() != XmlPullParser.START_TAG )
			{
				continue;
			}

			tag	= parser.getName();

			if ( tag.equals( ShootingContract.Targets._ID ) )				values.put( ShootingContract.Targets._ID,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.DATE ) )			values.put( ShootingContract.Targets.DATE,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.FIREARM_ID ) )	values.put( ShootingContract.Targets.FIREARM_ID,	restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.AMMO ) )			values.put( ShootingContract.Targets.AMMO,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.LOT_ID ) )		values.put( ShootingContract.Targets.LOT_ID,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.TYPE ) )			values.put( ShootingContract.Targets.TYPE,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.DISTANCE ) )		values.put( ShootingContract.Targets.DISTANCE,		restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.SHOTS ) )		values.put( ShootingContract.Targets.SHOTS,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets.NOTES ) )		values.put( ShootingContract.Targets.NOTES,			restoreText( parser ) );
			else if ( tag.equals( ShootingContract.Targets._DATA ) )		values.put( ShootingContract.Targets._DATA,			new File( mApplication.getExternalFilesDir( "targets" ), restoreText( parser ) ).getAbsolutePath() );
			else															restoreSkip( parser );
		}

		parser.require( XmlPullParser.END_TAG, "", "target" );

		mContentResolver.insert( ShootingContract.Targets.CONTENT_URI, values );
	}

	private String restoreText( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		String text;

		if ( parser.next() != XmlPullParser.TEXT )
		{
			return null;
		}

		text	= parser.getText();
		parser.nextTag();

		return text;
	}

	private void restoreSkip( XmlPullParser parser ) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException
	{
		int depth = 1;

		while ( depth > 0 )
		{
			switch ( parser.next() )
			{
				case XmlPullParser.END_TAG		: depth--;	break;
				case XmlPullParser.START_TAG	: depth++;	break;
			}
		}
	}

	private void restoreFirearmPhotoImage( ZipInputStream zip, String filename )
	{
		File dir;
		File file;
		int numRead;
		byte[] buffer;
		FileOutputStream destStream;

		dir				= mApplication.getExternalFilesDir( "firearms" );
		file			= new File( dir, filename );

		dir.mkdirs();

		buffer			= new byte[ 8192 ];

		try
		{
			destStream	= new FileOutputStream( file );

			while ( ( numRead = zip.read( buffer ) ) > 0 )
			{
				destStream.write( buffer, 0, numRead );
			}
		}
		catch ( Exception e )
		{
			file.delete();
		}
	}

	private void restoreTargetImage( ZipInputStream zip, String filename )
	{
		File dir;
		File file;
		int numRead;
		byte[] buffer;
		FileOutputStream destStream;

		dir				= mApplication.getExternalFilesDir( "targets" );
		file			= new File( dir, filename );

		dir.mkdirs();

		buffer			= new byte[ 8192 ];

		try
		{
			destStream	= new FileOutputStream( file );

			while ( ( numRead = zip.read( buffer ) ) > 0 )
			{
				destStream.write( buffer, 0, numRead );
			}
		}
		catch ( Exception e )
		{
			file.delete();
		}
	}
}
