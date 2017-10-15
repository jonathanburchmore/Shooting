package org.offline.shooting;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService extends IntentService
{
	ShootingApplication			mApplication;
	ContentResolver				mContentResolver;
	Handler						mUIThreadHandler;

	public static final String	ACTION_COMPLETE		= "org.offline.shooting.BackupService.COMPLETE";
	public static final String	PARAM_BACKUP_PATH	= "backup_path";

	public BackupService()
	{
		super( "BackupService" );
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
			if ( service.service.getClassName().equals( BackupService.class.getName() ) )
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
		File dest;
		String destPath;
		XmlSerializer xml;
		ZipOutputStream destZip;
		FileOutputStream destStream;

		if ( ( destPath = intent.getStringExtra( PARAM_BACKUP_PATH ) ) == null )
		{
			notifyComplete( mApplication.getString( R.string.label_backup_failed ) + mApplication.getString( R.string.label_missing_parameter ) );
			return;
		}

		dest				= new File( destPath );
		destStream			= null;
		destZip				= null;

		try
		{
			destStream		= new FileOutputStream( dest );
			destZip			= new ZipOutputStream( destStream );

			archiveFirearmPhotoImages( destZip );
			archiveTargetImages( destZip );

			destZip.putNextEntry( new ZipEntry( "shooting.xml" ) );

			xml				= Xml.newSerializer();
			xml.setOutput( destZip, "UTF-8" );
			xml.startDocument( "UTF-8", null );
			xml.setFeature( "http://xmlpull.org/v1/doc/features.html#indent-output", true );

			xml.startTag( "", "shooting" );

			serializeFirearms( xml );
			serializeFirearmPhotos( xml );
			serializeFirearmListPhotoIds( xml );
			serializeFirearmAcquisitions( xml );
			serializeFirearmDispositions( xml );
			serializeFirearmNotes( xml );

			serializeLoads( xml );
			serializeLoadLots( xml );
			serializeLoadNotes( xml );

			serializeTargets( xml );

			xml.endTag( "", "shooting" );
			xml.endDocument();

			destZip.closeEntry();

			destZip.close();
			destStream.close();

			notifyComplete( mApplication.getString( R.string.label_backup_complete ) );
		}
		catch ( Exception e )
		{
			if ( destZip != null )
			{
				try
				{
					destZip.close();
				}
				catch ( Exception ignored )
				{
				}
			}

			if ( destStream != null )
			{
				try
				{
					destStream.close();
				}
				catch ( Exception ignored )
				{
				}
			}

			dest.delete();

			notifyComplete( mApplication.getString( R.string.label_backup_failed ) + e.getMessage() );
		}
	}

	private void serialize( XmlSerializer xml, Cursor cursor, String column ) throws java.io.IOException
	{
		String value;

		if ( ( value = cursor.getString( cursor.getColumnIndex( column ) ) ) != null )
		{
			xml.startTag( "", column );
			xml.text( value );
			xml.endTag( "", column );
		}
	}

	private void serializePath( XmlSerializer xml, Cursor cursor, String column ) throws java.io.IOException
	{
		String value;

		if ( ( value = cursor.getString( cursor.getColumnIndex( column ) ) ) != null )
		{
			xml.startTag( "", column );
			xml.text( new File( value ).getName() );
			xml.endTag( "", column );
		}
	}

	private void serializeFirearms( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.Firearms.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearm" );
			serialize( xml, cursor, ShootingContract.Firearms._ID );
			serialize( xml, cursor, ShootingContract.Firearms.MAKE );
			serialize( xml, cursor, ShootingContract.Firearms.MODEL );
			serialize( xml, cursor, ShootingContract.Firearms.SERIAL );
			serialize( xml, cursor, ShootingContract.Firearms.TYPE );
			serialize( xml, cursor, ShootingContract.Firearms.CALIBER );
			serialize( xml, cursor, ShootingContract.Firearms.BARREL );
			xml.endTag( "", "firearm" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeFirearmPhotos( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.FirearmPhotos.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearmphoto" );
			serialize( xml, cursor, ShootingContract.FirearmPhotos._ID );
			serialize( xml, cursor, ShootingContract.FirearmPhotos.FIREARM_ID );
			serializePath( xml, cursor, ShootingContract.FirearmPhotos._DATA );
			xml.endTag( "", "firearmphoto" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void archiveFirearmPhotoImages( ZipOutputStream zip ) throws java.io.IOException
	{
		int numRead;
		byte[] buffer;
		Cursor cursor;
		String imagePath;
		InputStream image;

		buffer	= new byte[ 8192 ];
		cursor	= mContentResolver.query( ShootingContract.FirearmPhotos.CONTENT_URI,
										  new String[] { ShootingContract.FirearmPhotos._ID, ShootingContract.FirearmPhotos._DATA },
										  null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			if ( ( imagePath = cursor.getString( cursor.getColumnIndex( ShootingContract.FirearmPhotos._DATA ) ) ) == null )
			{
				continue;
			}

			try
			{
				image = mContentResolver.openInputStream( ContentUris.withAppendedId( ShootingContract.FirearmPhotos.CONTENT_URI,
																						cursor.getLong( cursor.getColumnIndex( ShootingContract.FirearmPhotos._ID ) ) ) );
			}
			catch ( java.io.FileNotFoundException e )
			{
				continue;
			}

			zip.putNextEntry( new ZipEntry( "firearms/" + new File( imagePath ).getName() ) );

			while ( ( numRead = image.read( buffer ) ) > 0 )
			{
				zip.write( buffer, 0, numRead );
			}

			zip.closeEntry();
			image.close();
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeFirearmListPhotoIds( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.Firearms.CONTENT_URI,
										  new String[] { ShootingContract.Firearms._ID, ShootingContract.Firearms.LIST_PHOTO_ID },
										  ShootingContract.Firearms.LIST_PHOTO_ID + " IS NOT NULL",
										  null,
										  null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearmlistphotoid" );
			serialize( xml, cursor, ShootingContract.Firearms._ID );
			serialize( xml, cursor, ShootingContract.Firearms.LIST_PHOTO_ID );
			xml.endTag( "", "firearmlistphotoid" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeFirearmAcquisitions( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.FirearmAcquisition.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearmacquisition" );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition._ID );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.FIREARM_ID );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.DATE );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.FROM );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.LICENSE );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.ADDRESS );
			serialize( xml, cursor, ShootingContract.FirearmAcquisition.PRICE );
			xml.endTag( "", "firearmacquisition" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeFirearmDispositions( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.FirearmDisposition.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearmdisposition" );
			serialize( xml, cursor, ShootingContract.FirearmDisposition._ID );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.FIREARM_ID );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.DATE );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.TO );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.LICENSE );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.ADDRESS );
			serialize( xml, cursor, ShootingContract.FirearmDisposition.PRICE );
			xml.endTag( "", "firearmdisposition" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeFirearmNotes( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.FirearmNotes.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "firearmnote" );
			serialize( xml, cursor, ShootingContract.FirearmNotes._ID );
			serialize( xml, cursor, ShootingContract.FirearmNotes.FIREARM_ID );
			serialize( xml, cursor, ShootingContract.FirearmNotes.DATE );
			serialize( xml, cursor, ShootingContract.FirearmNotes.TEXT );
			xml.endTag( "", "firearmnote" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeLoads( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.Loads.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "load" );
			serialize( xml, cursor, ShootingContract.Loads._ID );
			serialize( xml, cursor, ShootingContract.Loads.CALIBER );
			serialize( xml, cursor, ShootingContract.Loads.BULLET );
			serialize( xml, cursor, ShootingContract.Loads.POWDER );
			serialize( xml, cursor, ShootingContract.Loads.CHARGE );
			serialize( xml, cursor, ShootingContract.Loads.PRIMER );
			serialize( xml, cursor, ShootingContract.Loads.OAL );
			serialize( xml, cursor, ShootingContract.Loads.CRIMP );
			xml.endTag( "", "load" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeLoadLots( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.LoadLots.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "loadlot" );
			serialize( xml, cursor, ShootingContract.LoadLots._ID );
			serialize( xml, cursor, ShootingContract.LoadLots.LOAD_ID );
			serialize( xml, cursor, ShootingContract.LoadLots.DATE );
			serialize( xml, cursor, ShootingContract.LoadLots.CCOUNT );
			serialize( xml, cursor, ShootingContract.LoadLots.POWDER_LOT );
			serialize( xml, cursor, ShootingContract.LoadLots.PRIMER );
			serialize( xml, cursor, ShootingContract.LoadLots.PRIMER_LOT );
			xml.endTag( "", "loadlot" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeLoadNotes( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.LoadNotes.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "loadnote" );
			serialize( xml, cursor, ShootingContract.LoadNotes._ID );
			serialize( xml, cursor, ShootingContract.LoadNotes.LOAD_ID );
			serialize( xml, cursor, ShootingContract.LoadNotes.DATE );
			serialize( xml, cursor, ShootingContract.LoadNotes.TEXT );
			xml.endTag( "", "loadnote" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void serializeTargets( XmlSerializer xml ) throws java.io.IOException
	{
		Cursor cursor;

		cursor	= mContentResolver.query( ShootingContract.Targets.CONTENT_URI, null, null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			xml.startTag( "", "target" );
			serialize( xml, cursor, ShootingContract.Targets._ID );
			serialize( xml, cursor, ShootingContract.Targets.DATE );
			serialize( xml, cursor, ShootingContract.Targets.FIREARM_ID );
			serialize( xml, cursor, ShootingContract.Targets.AMMO );
			serialize( xml, cursor, ShootingContract.Targets.LOT_ID );
			serialize( xml, cursor, ShootingContract.Targets.TYPE );
			serialize( xml, cursor, ShootingContract.Targets.DISTANCE );
			serialize( xml, cursor, ShootingContract.Targets.SHOTS );
			serialize( xml, cursor, ShootingContract.Targets.NOTES );
			serializePath( xml, cursor, ShootingContract.Targets._DATA );
			xml.endTag( "", "target" );
		} while ( cursor.moveToNext() );

		cursor.close();
	}

	private void archiveTargetImages( ZipOutputStream zip ) throws java.io.IOException
	{
		int numRead;
		byte[] buffer;
		Cursor cursor;
		String imagePath;
		InputStream image;

		buffer	= new byte[ 8192 ];
		cursor	= mContentResolver.query( ShootingContract.Targets.CONTENT_URI,
										  new String[] { ShootingContract.Targets._ID, ShootingContract.Targets._DATA },
										  null, null, null );
		if ( !cursor.moveToFirst() )
		{
			cursor.close();
			return;
		}

		do
		{
			if ( ( imagePath = cursor.getString( cursor.getColumnIndex( ShootingContract.Targets._DATA ) ) ) == null )
			{
				continue;
			}

			try
			{
				image = mContentResolver.openInputStream( ContentUris.withAppendedId( ShootingContract.Targets.CONTENT_URI,
																					  cursor.getLong( cursor.getColumnIndex( ShootingContract.Targets._ID ) ) ) );
			}
			catch ( java.io.FileNotFoundException e )
			{
				continue;
			}

			zip.putNextEntry( new ZipEntry( "targets/" + new File( imagePath ).getName() ) );

			while ( ( numRead = image.read( buffer ) ) > 0 )
			{
				zip.write( buffer, 0, numRead );
			}

			zip.closeEntry();
			image.close();
		} while ( cursor.moveToNext() );

		cursor.close();
	}
}
