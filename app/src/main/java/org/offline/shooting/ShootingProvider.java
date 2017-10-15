/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;

public class ShootingProvider extends ContentProvider
{
	private static final String		DATABASE_FILENAME				= "shooting.db";
	private static final int		DATABASE_VERSION				= 4;
	private DatabaseHelper			mDatabaseHelper;

	private static final int		URI_FIREARMS_ITEM				= 100;
	private static final int		URI_FIREARMS					= 101;
	private static final int		URI_FIREARM_MAKES				= 102;
	private static final int		URI_FIREARM_TYPES				= 103;
	private static final int		URI_FIREARM_PHOTOS_ITEM			= 104;
	private static final int		URI_FIREARM_PHOTOS				= 105;
	private static final int		URI_FIREARM_ACQUISITION_ITEM	= 106;
	private static final int		URI_FIREARM_ACQUISITION			= 107;
	private static final int		URI_FIREARM_DISPOSITION_ITEM	= 108;
	private static final int		URI_FIREARM_DISPOSITION			= 109;
	private static final int		URI_FIREARM_NOTES_ITEM			= 110;
	private static final int		URI_FIREARM_NOTES				= 111;

	private static final int		URI_TARGETS_ITEM				= 200;
	private static final int		URI_TARGETS_IMAGES_ITEM			= 201;
	private static final int		URI_TARGETS						= 202;
	private static final int		URI_TARGETS_WITHDATA_ITEM		= 203;
	private static final int		URI_TARGETS_WITHDATA			= 204;
	private static final int		URI_TARGET_AMMO					= 205;
	private static final int		URI_TARGET_TYPES				= 206;

	private static final int		URI_LOADS_ITEM					= 300;
	private static final int		URI_LOADS						= 301;
	private static final int		URI_LOAD_LOT_AGGREGATES			= 302;
	private static final int		URI_LOAD_BULLETS				= 303;
	private static final int		URI_LOAD_POWDERS				= 304;
	private static final int		URI_LOAD_LOTS_ITEM				= 305;
	private static final int		URI_LOAD_LOTS					= 306;
	private static final int		URI_LOAD_NOTES_ITEM				= 307;
	private static final int		URI_LOAD_NOTES					= 308;

	private static final int		URI_CALIBERS					= 900;
	private static final int		URI_PRIMERS						= 901;

	private static final UriMatcher	sURIMatcher;

	static
	{
		sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );

		sURIMatcher.addURI( ShootingContract.AUTHORITY, "firearms/#",				URI_FIREARMS_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "firearms",					URI_FIREARMS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "firearms/makes",			URI_FIREARM_MAKES );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "firearms/types",			URI_FIREARM_TYPES );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "firearmphotos/#",			URI_FIREARM_PHOTOS_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmphotos",			URI_FIREARM_PHOTOS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmacquisition/#",		URI_FIREARM_ACQUISITION_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmacquisition",		URI_FIREARM_ACQUISITION );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmdisposition/#",		URI_FIREARM_DISPOSITION_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmdisposition",		URI_FIREARM_DISPOSITION );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmnotes/#",			URI_FIREARM_NOTES_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"firearmnotes",				URI_FIREARM_NOTES );

		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/#",				URI_TARGETS_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/images/#",			URI_TARGETS_IMAGES_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets",					URI_TARGETS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/withdata/#",		URI_TARGETS_WITHDATA_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/withdata",			URI_TARGETS_WITHDATA );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/ammo",				URI_TARGET_AMMO );
		sURIMatcher.addURI( ShootingContract.AUTHORITY,	"targets/types",			URI_TARGET_TYPES );

		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loads/#",					URI_LOADS_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loads",					URI_LOADS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loads/lotaggregates",		URI_LOAD_LOT_AGGREGATES );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loads/bullets",			URI_LOAD_BULLETS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loads/powders",			URI_LOAD_POWDERS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loadlots/#",				URI_LOAD_LOTS_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loadlots",					URI_LOAD_LOTS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loadnotes/#",				URI_LOAD_NOTES_ITEM );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "loadnotes",				URI_LOAD_NOTES );

		sURIMatcher.addURI( ShootingContract.AUTHORITY, "calibers",					URI_CALIBERS );
		sURIMatcher.addURI( ShootingContract.AUTHORITY, "primers",					URI_PRIMERS );
	}

	@Override
	public boolean onCreate()
	{
		mDatabaseHelper	= new DatabaseHelper( getContext(), DATABASE_FILENAME, null, DATABASE_VERSION );
		return true;
	}

	@Override
	public String getType( Uri uri )
	{
		switch ( sURIMatcher.match( uri ) )
		{
			default								: throw new IllegalArgumentException( "Invalid URI '" + uri + "'" );

			case URI_FIREARMS_ITEM				: return ShootingContract.Firearms.CONTENT_ITEM_TYPE;
			case URI_FIREARMS					: return ShootingContract.Firearms.CONTENT_TYPE;
			case URI_FIREARM_MAKES				: return ShootingContract.FirearmMakes.CONTENT_TYPE;
			case URI_FIREARM_TYPES				: return ShootingContract.FirearmTypes.CONTENT_TYPE;
			case URI_FIREARM_PHOTOS_ITEM		: return ShootingContract.FirearmPhotos.CONTENT_ITEM_TYPE;
			case URI_FIREARM_PHOTOS				: return ShootingContract.FirearmPhotos.CONTENT_TYPE;
			case URI_FIREARM_ACQUISITION_ITEM	: return ShootingContract.FirearmAcquisition.CONTENT_ITEM_TYPE;
			case URI_FIREARM_ACQUISITION		: return ShootingContract.FirearmAcquisition.CONTENT_TYPE;
			case URI_FIREARM_DISPOSITION_ITEM	: return ShootingContract.FirearmDisposition.CONTENT_ITEM_TYPE;
			case URI_FIREARM_DISPOSITION		: return ShootingContract.FirearmDisposition.CONTENT_TYPE;
			case URI_FIREARM_NOTES_ITEM			: return ShootingContract.FirearmNotes.CONTENT_ITEM_TYPE;
			case URI_FIREARM_NOTES				: return ShootingContract.FirearmNotes.CONTENT_TYPE;

			case URI_TARGETS_ITEM				: return ShootingContract.Targets.CONTENT_ITEM_TYPE;
			case URI_TARGETS_IMAGES_ITEM		: return ShootingContract.Targets.CONTENT_IMAGE_TYPE;
			case URI_TARGETS					: return ShootingContract.Targets.CONTENT_TYPE;
			case URI_TARGETS_WITHDATA_ITEM		: return ShootingContract.TargetsWithData.CONTENT_ITEM_TYPE;
			case URI_TARGETS_WITHDATA			: return ShootingContract.TargetsWithData.CONTENT_TYPE;
			case URI_TARGET_AMMO				: return ShootingContract.TargetAmmo.CONTENT_TYPE;
			case URI_TARGET_TYPES				: return ShootingContract.TargetTypes.CONTENT_TYPE;

			case URI_LOADS_ITEM					: return ShootingContract.Loads.CONTENT_ITEM_TYPE;
			case URI_LOADS						: return ShootingContract.Loads.CONTENT_TYPE;
			case URI_LOAD_LOT_AGGREGATES		: return ShootingContract.LoadLotAggregates.CONTENT_TYPE;
			case URI_LOAD_BULLETS				: return ShootingContract.LoadBullets.CONTENT_TYPE;
			case URI_LOAD_POWDERS				: return ShootingContract.LoadPowders.CONTENT_TYPE;
			case URI_LOAD_LOTS_ITEM				: return ShootingContract.LoadLots.CONTENT_ITEM_TYPE;
			case URI_LOAD_LOTS					: return ShootingContract.LoadLots.CONTENT_TYPE;
			case URI_LOAD_NOTES_ITEM			: return ShootingContract.LoadNotes.CONTENT_ITEM_TYPE;
			case URI_LOAD_NOTES					: return ShootingContract.LoadNotes.CONTENT_TYPE;

			case URI_CALIBERS					: return ShootingContract.Calibers.CONTENT_TYPE;
			case URI_PRIMERS					: return ShootingContract.Primers.CONTENT_TYPE;
		}
	}

	@Override
	public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder )
	{
		Cursor cursor;
		SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();

		switch ( sURIMatcher.match( uri ) )
		{
			default								: throw new IllegalArgumentException( "Invalid URI '" + uri + "'" );

			case URI_FIREARMS_ITEM				: cursor = queryItem( database, TABLE_FIREARMS, uri, projection, selection, selectionArgs, sortOrder );					break;
			case URI_FIREARMS					: cursor = database.query( TABLE_FIREARMS, projection, selection, selectionArgs, null, null, sortOrder );				break;
			case URI_FIREARM_MAKES				: cursor = database.query( VIEW_FIREARM_MAKES, projection, selection, selectionArgs, null, null, sortOrder );			break;
			case URI_FIREARM_TYPES				: cursor = database.query( VIEW_FIREARM_TYPES, projection, selection, selectionArgs, null, null, sortOrder );			break;
			case URI_FIREARM_PHOTOS_ITEM		: cursor = queryItem( database, TABLE_FIREARM_PHOTOS, uri, projection, selection, selectionArgs, sortOrder );			break;
			case URI_FIREARM_PHOTOS				: cursor = database.query( TABLE_FIREARM_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder );			break;
			case URI_FIREARM_ACQUISITION_ITEM	: cursor = queryItem( database, TABLE_FIREARM_ACQUISITION, uri, projection, selection, selectionArgs, sortOrder );		break;
			case URI_FIREARM_ACQUISITION		: cursor = database.query( TABLE_FIREARM_ACQUISITION, projection, selection, selectionArgs, null, null, sortOrder );	break;
			case URI_FIREARM_DISPOSITION_ITEM	: cursor = queryItem( database, TABLE_FIREARM_DISPOSITION, uri, projection, selection, selectionArgs, sortOrder );		break;
			case URI_FIREARM_DISPOSITION		: cursor = database.query( TABLE_FIREARM_DISPOSITION, projection, selection, selectionArgs, null, null, sortOrder );	break;
			case URI_FIREARM_NOTES_ITEM			: cursor = queryItem( database, TABLE_FIREARM_NOTES, uri, projection, selection, selectionArgs, sortOrder );			break;
			case URI_FIREARM_NOTES				: cursor = database.query( TABLE_FIREARM_NOTES, projection, selection, selectionArgs, null, null, sortOrder );			break;

			case URI_TARGETS_ITEM				:
			case URI_TARGETS_IMAGES_ITEM		: cursor = queryItem( database, TABLE_TARGETS, uri, projection, selection, selectionArgs, sortOrder );					break;
			case URI_TARGETS					: cursor = database.query( TABLE_TARGETS, projection, selection, selectionArgs, null, null, sortOrder );				break;
			case URI_TARGETS_WITHDATA_ITEM		: cursor = queryItem( database, VIEW_TARGETS_WITHDATA, uri, projection, selection, selectionArgs, sortOrder );			break;
			case URI_TARGETS_WITHDATA			: cursor = database.query( VIEW_TARGETS_WITHDATA, projection, selection, selectionArgs, null, null, sortOrder );		break;
			case URI_TARGET_AMMO				: cursor = database.query( VIEW_TARGET_AMMO, projection, selection, selectionArgs, null, null, sortOrder );				break;
			case URI_TARGET_TYPES				: cursor = database.query( VIEW_TARGET_TYPES, projection, selection, selectionArgs, null, null, sortOrder );			break;

			case URI_LOADS_ITEM					: cursor = queryItem( database, TABLE_LOADS, uri, projection, selection, selectionArgs, sortOrder );					break;
			case URI_LOADS						: cursor = database.query( TABLE_LOADS, projection, selection, selectionArgs, null, null, sortOrder );					break;
			case URI_LOAD_LOT_AGGREGATES		: cursor = database.query( VIEW_LOAD_LOT_AGGREGATES, projection, selection, selectionArgs, null, null, sortOrder );		break;
			case URI_LOAD_BULLETS				: cursor = database.query( VIEW_LOAD_BULLETS, projection, selection, selectionArgs, null, null, sortOrder );			break;
			case URI_LOAD_POWDERS				: cursor = database.query( VIEW_LOAD_POWDERS, projection, selection, selectionArgs, null, null, sortOrder );			break;
			case URI_LOAD_LOTS_ITEM				: cursor = queryItem( database, TABLE_LOAD_LOTS, uri, projection, selection, selectionArgs, sortOrder );				break;
			case URI_LOAD_LOTS					: cursor = database.query( TABLE_LOAD_LOTS, projection, selection, selectionArgs, null, null, sortOrder );				break;
			case URI_LOAD_NOTES_ITEM			: cursor = queryItem( database, TABLE_LOAD_NOTES, uri, projection, selection, selectionArgs, sortOrder );				break;
			case URI_LOAD_NOTES					: cursor = database.query( TABLE_LOAD_NOTES, projection, selection, selectionArgs, null, null, sortOrder );				break;

			case URI_CALIBERS					: cursor = database.query( VIEW_CALIBERS, projection, selection, selectionArgs, null, null, sortOrder );				break;
			case URI_PRIMERS					: cursor = database.query( VIEW_PRIMERS, projection, selection, selectionArgs, null, null, sortOrder );			break;
		}

		cursor.setNotificationUri( getContext().getContentResolver(), uri );
		return cursor;
	}

	@Override
	public Uri insert( Uri uri, ContentValues values )
	{
		long inserted_id;
		LinkedList<Uri> notifyUris;
		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		notifyUris = new LinkedList<Uri>();
		notifyUris.add( uri );

		switch ( sURIMatcher.match( uri ) )
		{
			default							: throw new IllegalArgumentException( "Invalid URI '" + uri + "'" );

			case URI_FIREARMS				: inserted_id = database.insert( TABLE_FIREARMS, null, values );			break;
			case URI_FIREARM_ACQUISITION	: inserted_id = database.insert( TABLE_FIREARM_ACQUISITION, null, values );	break;
			case URI_FIREARM_DISPOSITION	: inserted_id = database.insert( TABLE_FIREARM_DISPOSITION, null, values );	break;
			case URI_FIREARM_NOTES			: inserted_id = database.insert( TABLE_FIREARM_NOTES, null, values );		break;
			case URI_FIREARM_PHOTOS			: inserted_id = database.insert( TABLE_FIREARM_PHOTOS, null, values );		break;

			case URI_TARGETS				: notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
											  inserted_id = database.insert( TABLE_TARGETS, null, values );
											  break;

			case URI_LOADS					: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
											  inserted_id = database.insert( TABLE_LOADS, null, values );
											  break;
			case URI_LOAD_LOTS				: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
											  inserted_id = database.insert( TABLE_LOAD_LOTS, null, values );
											  break;
			case URI_LOAD_NOTES				: inserted_id = database.insert( TABLE_LOAD_NOTES, null, values );			break;
		}

		notify( notifyUris );
		return ContentUris.withAppendedId( uri, inserted_id );
	}

	@Override
	public int update( Uri uri, ContentValues values, String selection, String[] selectionArgs )
	{
		int nRows;
		LinkedList<Uri> notifyUris;
		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		notifyUris = new LinkedList<Uri>();
		notifyUris.add( uri );

		switch ( sURIMatcher.match( uri ) )
		{
			default								: throw new IllegalArgumentException( "Invalid URI '" + uri + "'" );

			case URI_FIREARMS_ITEM				: notifyUris.add( ShootingContract.Firearms.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_FIREARMS, uri, values, selection, selectionArgs );
												  break;
			case URI_FIREARM_PHOTOS_ITEM		: notifyUris.add( ShootingContract.FirearmPhotos.CONTENT_URI );
								 				  nRows	= updateItem( database, TABLE_FIREARM_PHOTOS, uri, values, selection, selectionArgs );
												  break;
			case URI_FIREARM_ACQUISITION_ITEM	: notifyUris.add( ShootingContract.FirearmAcquisition.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_FIREARM_ACQUISITION, uri, values, selection, selectionArgs );
												  break;
			case URI_FIREARM_DISPOSITION_ITEM	: notifyUris.add( ShootingContract.FirearmDisposition.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_FIREARM_DISPOSITION, uri, values, selection, selectionArgs );
												  break;
			case URI_FIREARM_NOTES_ITEM			: notifyUris.add( ShootingContract.FirearmNotes.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_FIREARM_NOTES, uri, values, selection, selectionArgs );
												  break;
			case URI_FIREARMS					: nRows	= database.update( TABLE_FIREARMS, values, selection, selectionArgs );				break;
			case URI_FIREARM_PHOTOS				: nRows	= database.update( TABLE_FIREARM_PHOTOS, values, selection, selectionArgs );		break;
			case URI_FIREARM_ACQUISITION		: nRows	= database.update( TABLE_FIREARM_ACQUISITION, values, selection, selectionArgs );	break;
			case URI_FIREARM_DISPOSITION		: nRows	= database.update( TABLE_FIREARM_DISPOSITION, values, selection, selectionArgs );	break;
			case URI_FIREARM_NOTES				: nRows	= database.update( TABLE_FIREARM_NOTES, values, selection, selectionArgs );			break;

			case URI_TARGETS_ITEM				: notifyUris.add( ShootingContract.Targets.CONTENT_URI );
												  notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  notifyUris.add( ContentUris.withAppendedId( ShootingContract.TargetsWithData.CONTENT_URI, ContentUris.parseId( uri ) ) );
												  nRows	= updateItem( database, TABLE_TARGETS, uri, values, selection, selectionArgs );
												  break;
			case URI_TARGETS					: notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  nRows	= database.update( TABLE_TARGETS, values, selection, selectionArgs );
												  break;

			case URI_LOADS_ITEM					: notifyUris.add( ShootingContract.Loads.CONTENT_URI );
												  notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows = updateItem( database, TABLE_LOADS, uri, values, selection, selectionArgs );
												  break;
			case URI_LOAD_LOTS_ITEM				: notifyUris.add( ShootingContract.LoadLots.CONTENT_URI );
												  notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_LOAD_LOTS, uri, values, selection, selectionArgs );
												  break;
			case URI_LOAD_LOTS					: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows	= database.update( TABLE_LOAD_LOTS, values, selection, selectionArgs );
												  break;
			case URI_LOAD_NOTES_ITEM			: notifyUris.add( ShootingContract.LoadNotes.CONTENT_URI );
												  nRows	= updateItem( database, TABLE_LOAD_NOTES, uri, values, selection, selectionArgs );
												  break;
			case URI_LOADS						: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows = database.update( TABLE_LOADS, values, selection, selectionArgs );
												  break;
			case URI_LOAD_NOTES					: nRows	= database.update( TABLE_LOAD_NOTES, values, selection, selectionArgs );			break;
		}

		if ( nRows > 0 )
		{
			notify( notifyUris );
		}

		return nRows;
	}

	@Override
	public int delete( Uri uri, String selection, String[] selectionArgs )
	{
		int nRows;
		LinkedList<Uri> notifyUris;
		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		notifyUris = new LinkedList<Uri>();
		notifyUris.add( uri );

		switch ( sURIMatcher.match( uri ) )
		{
			default								: throw new IllegalArgumentException( "Invalid URI '" + uri + "'" );

			case URI_FIREARMS_ITEM				: notifyUris.add( ShootingContract.Firearms.CONTENT_URI );
												  notifyUris.add( ShootingContract.FirearmPhotos.CONTENT_URI );
												  notifyUris.add( ShootingContract.Targets.CONTENT_URI );
												  notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  nRows	= deleteFirearms( database, uri, selection, selectionArgs );
												  break;
			case URI_FIREARM_PHOTOS_ITEM		: notifyUris.add( ShootingContract.FirearmPhotos.CONTENT_URI );
								 				  nRows	= deleteFirearmPhotos( database, uri, selection, selectionArgs );
												  break;
			case URI_FIREARM_ACQUISITION_ITEM	: notifyUris.add( ShootingContract.FirearmAcquisition.CONTENT_URI );
												  nRows	= deleteItem( database, TABLE_FIREARM_ACQUISITION, uri, selection, selectionArgs );
												  break;
			case URI_FIREARM_DISPOSITION_ITEM	: notifyUris.add( ShootingContract.FirearmDisposition.CONTENT_URI );
												  nRows	= deleteItem( database, TABLE_FIREARM_DISPOSITION, uri, selection, selectionArgs );
												  break;
			case URI_FIREARM_NOTES_ITEM			: notifyUris.add( ShootingContract.FirearmNotes.CONTENT_URI );
												  nRows	= deleteItem( database, TABLE_FIREARM_NOTES, uri, selection, selectionArgs );
												  break;
			case URI_FIREARMS					: notifyUris.add( ShootingContract.FirearmPhotos.CONTENT_URI );
												  notifyUris.add( ShootingContract.Targets.CONTENT_URI );
												  notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  nRows	= deleteFirearms( database, uri, selection, selectionArgs );
												  break;
			case URI_FIREARM_PHOTOS				: nRows	= deleteFirearmPhotos( database, uri, selection, selectionArgs );					break;
			case URI_FIREARM_ACQUISITION		: nRows	= database.delete( TABLE_FIREARM_ACQUISITION, selection, selectionArgs );			break;
			case URI_FIREARM_DISPOSITION		: nRows	= database.delete( TABLE_FIREARM_DISPOSITION, selection, selectionArgs );			break;
			case URI_FIREARM_NOTES				: nRows	= database.delete( TABLE_FIREARM_NOTES, selection, selectionArgs );					break;

			case URI_TARGETS_ITEM				: notifyUris.add( ShootingContract.Targets.CONTENT_URI );
												  notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  notifyUris.add( ContentUris.withAppendedId( ShootingContract.TargetsWithData.CONTENT_URI, ContentUris.parseId( uri ) ) );
												  nRows	= deleteTargets( database, uri, selection, selectionArgs );
												  break;
			case URI_TARGETS					: notifyUris.add( ShootingContract.TargetsWithData.CONTENT_URI );
												  nRows	= deleteTargets( database, uri, selection, selectionArgs );
												  break;

			case URI_LOADS_ITEM					: notifyUris.add( ShootingContract.Loads.CONTENT_URI );
												  notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows = deleteItem( database, TABLE_LOADS, uri, selection, selectionArgs );
												  break;
			case URI_LOAD_LOTS_ITEM				: notifyUris.add( ShootingContract.LoadLots.CONTENT_URI );
												  notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows	= deleteItem( database, TABLE_LOAD_LOTS, uri, selection, selectionArgs );
												  break;
			case URI_LOAD_LOTS					: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows	= database.delete( TABLE_LOAD_LOTS, selection, selectionArgs );
												  break;
			case URI_LOAD_NOTES_ITEM			: notifyUris.add( ShootingContract.LoadNotes.CONTENT_URI );
												  nRows	= deleteItem( database, TABLE_LOAD_NOTES, uri, selection, selectionArgs );
												  break;
			case URI_LOADS						: notifyUris.add( ShootingContract.LoadLotAggregates.CONTENT_URI );
												  nRows = database.delete( TABLE_LOADS, selection, selectionArgs );
												  break;
			case URI_LOAD_NOTES					: nRows	= database.delete( TABLE_LOAD_NOTES, selection, selectionArgs );					break;
		}

		if ( nRows > 0 )
		{
			notify( notifyUris );
		}

		return nRows;
	}

	@Override
	public ParcelFileDescriptor openFile( Uri uri, String mode ) throws FileNotFoundException
	{
		return openFileHelper( uri, mode );
	}

	private Cursor queryItem( SQLiteDatabase database, String table, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder )
	{
		String querySelection;
		String[] querySelectionArgs;

		querySelection				= BaseColumns._ID + " = ?";
		if ( selection != null )
		{
			querySelection			+= " AND ( " + selection + " )";
		}

		querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
		querySelectionArgs[ 0 ]		= String.valueOf( ContentUris.parseId( uri ) );

		if ( selectionArgs != null )
		{
			System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
		}

		return database.query( table, projection, querySelection, querySelectionArgs, null, null, sortOrder );
	}

	private int updateItem( SQLiteDatabase database, String table, Uri uri, ContentValues values, String selection, String[] selectionArgs )
	{
		String querySelection;
		String[] querySelectionArgs;

		querySelection				= BaseColumns._ID + " = ?";
		if ( selection != null )
		{
			querySelection			+= " AND ( " + selection + " )";
		}

		querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
		querySelectionArgs[ 0 ]		= String.valueOf( ContentUris.parseId( uri ) );

		if ( selectionArgs != null )
		{
			System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
		}

		return database.update( table, values, querySelection, querySelectionArgs );
	}

	private int deleteItem( SQLiteDatabase database, String table, Uri uri, String selection, String[] selectionArgs )
	{
		String querySelection;
		String[] querySelectionArgs;

		querySelection				= BaseColumns._ID + " = ?";
		if ( selection != null )
		{
			querySelection			+= " AND ( " + selection + " )";
		}

		querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
		querySelectionArgs[ 0 ]		= String.valueOf( ContentUris.parseId( uri ) );

		if ( selectionArgs != null )
		{
			System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
		}

		return database.delete( table, querySelection, querySelectionArgs );
	}

	private int deleteFirearms( SQLiteDatabase database, Uri uri, String selection, String[] selectionArgs )
	{
		long firearmId;
		Cursor firearmCursor;
		String querySelection;
		String[] querySelectionArgs;

		try
		{
			firearmId					= ContentUris.parseId( uri );
		}
		catch ( Exception e )
		{
			firearmId					= -1;
		}

		if ( firearmId == -1 )
		{
			querySelection				= selection;
			querySelectionArgs			= selectionArgs;
		}
		else
		{
			querySelection				= ShootingContract.Firearms._ID + " = ?";
			if ( selection != null )
			{
				querySelection			+= " AND ( " + selection + " )";
			}

			querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
			querySelectionArgs[ 0 ]		= String.valueOf( firearmId );

			if ( selectionArgs != null )
			{
				System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
			}
		}

		firearmCursor	= database.query( TABLE_FIREARMS,
										  new String[] { ShootingContract.Firearms._ID },
										  querySelection,
										  querySelectionArgs,
										  null, null, null );
		if ( firearmCursor.moveToFirst() )
		{
			do
			{
				deleteFirearmPhotos( database,
									 ShootingContract.FirearmPhotos.CONTENT_URI,
									 ShootingContract.FirearmPhotos.FIREARM_ID + " = ?",
									 new String[] { String.valueOf( firearmCursor.getLong( 0 ) ) } );
				deleteTargets( database,
							   ShootingContract.Targets.CONTENT_URI,
							   ShootingContract.Targets.FIREARM_ID + " = ?",
							   new String[] { String.valueOf( firearmCursor.getLong( 0 ) ) } );
			} while ( firearmCursor.moveToNext() );
		}

		return database.delete( TABLE_FIREARMS, querySelection, querySelectionArgs );
	}

	private int deleteFirearmPhotos( SQLiteDatabase database, Uri uri, String selection, String[] selectionArgs )
	{
		File photo;
		long firearmPhotoId;
		String querySelection;
		Cursor firearmPhotoCursor;
		String[] querySelectionArgs;

		try
		{
			firearmPhotoId				= ContentUris.parseId( uri );
		}
		catch ( Exception e )
		{
			firearmPhotoId				= -1;
		}

		if ( firearmPhotoId == -1 )
		{
			querySelection				= selection;
			querySelectionArgs			= selectionArgs;
		}
		else
		{
			querySelection				= ShootingContract.FirearmPhotos._ID + " = ?";
			if ( selection != null )
			{
				querySelection			+= " AND ( " + selection + " )";
			}

			querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
			querySelectionArgs[ 0 ]		= String.valueOf( firearmPhotoId );

			if ( selectionArgs != null )
			{
				System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
			}
		}

		firearmPhotoCursor	= database.query( TABLE_FIREARM_PHOTOS,
										  	  new String[] { ShootingContract.FirearmPhotos._DATA },
										  	  querySelection,
										  	  querySelectionArgs,
										  	  null, null, null );
		if ( firearmPhotoCursor.moveToFirst() )
		{
			do
			{
				photo	= new File( firearmPhotoCursor.getString( 0 ) );
				photo.delete();
			} while ( firearmPhotoCursor.moveToNext() );
		}

		return database.delete( TABLE_FIREARM_PHOTOS, querySelection, querySelectionArgs );
	}

	private int deleteTargets( SQLiteDatabase database, Uri uri, String selection, String[] selectionArgs )
	{
		File target;
		long targetId;
		Cursor targetCursor;
		String querySelection;
		String[] querySelectionArgs;

		try
		{
			targetId					= ContentUris.parseId( uri );
		}
		catch ( Exception e )
		{
			targetId					= -1;
		}

		if ( targetId == -1 )
		{
			querySelection				= selection;
			querySelectionArgs			= selectionArgs;
		}
		else
		{
			querySelection				= ShootingContract.Targets._ID + " = ?";
			if ( selection != null )
			{
				querySelection			+= " AND ( " + selection + " )";
			}

			querySelectionArgs			= new String[ selectionArgs == null ? 1 : selectionArgs.length + 1 ];
			querySelectionArgs[ 0 ]		= String.valueOf( targetId );

			if ( selectionArgs != null )
			{
				System.arraycopy( selectionArgs, 0, querySelectionArgs, 1, selectionArgs.length );
			}
		}

		targetCursor	= database.query( TABLE_TARGETS,
										  new String[] { ShootingContract.Targets._DATA },
										  querySelection,
										  querySelectionArgs,
										  null, null, null );
		if ( targetCursor.moveToFirst() )
		{
			do
			{
				target	= new File( targetCursor.getString( 0 ) );
				target.delete();
			} while ( targetCursor.moveToNext() );
		}

		return database.delete( TABLE_TARGETS, querySelection, querySelectionArgs );
	}

	private void notify( LinkedList<Uri> notifyUris )
	{
		final ContentResolver contentResolver = getContext().getContentResolver();

		for ( Uri notifyUri : notifyUris )
		{
			contentResolver.notifyChange( notifyUri, null );
		}
	}

	private static final String TABLE_FIREARMS				= "Firearms";
	private static final String VIEW_FIREARM_MAKES			= "FirearmMakes";
	private static final String VIEW_FIREARM_TYPES			= "FirearmTypes";
	private static final String	TABLE_FIREARM_PHOTOS		= "FirearmPhotos";
	private static final String	TABLE_FIREARM_ACQUISITION	= "FirearmAcquisition";
	private static final String	TABLE_FIREARM_DISPOSITION	= "FirearmDisposition";
	private static final String	TABLE_FIREARM_NOTES			= "FirearmNotes";

	private static final String TABLE_TARGETS				= "Targets";
	private static final String VIEW_TARGETS_WITHDATA		= "TargetsWithData";
	private static final String VIEW_TARGET_AMMO			= "TargetAmmo";
	private static final String VIEW_TARGET_TYPES			= "TargetTypes";

	private static final String TABLE_LOADS					= "Loads";
	private static final String VIEW_LOAD_LOT_AGGREGATES	= "LoadLotAggregates";
	private static final String VIEW_LOAD_BULLETS			= "LoadBullets";
	private static final String VIEW_LOAD_POWDERS			= "LoadPowders";
	private static final String TABLE_LOAD_LOTS				= "LoadLots";
	private static final String TABLE_LOAD_NOTES			= "LoadNotes";

	private static final String VIEW_CALIBERS				= "Calibers";
	private static final String VIEW_PRIMERS				= "Primers";

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		public DatabaseHelper( Context context, String name, SQLiteDatabase.CursorFactory factory, int version )
		{
			super( context, name, factory, version );
		}

		@Override
		public void onOpen( SQLiteDatabase db )
		{
		    super.onOpen( db );

		    if ( !db.isReadOnly() )
			{
		        db.execSQL( "PRAGMA foreign_keys = ON" );
		    }
		}

		@Override
		public void onCreate( SQLiteDatabase db )
		{
			db.execSQL( "CREATE TABLE " + TABLE_FIREARMS + " " +
						"( " +
							ShootingContract.Firearms._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.Firearms.MAKE			+ " TEXT, " +
							ShootingContract.Firearms.MODEL			+ " TEXT, " +
							ShootingContract.Firearms.SERIAL		+ " TEXT, " +
							ShootingContract.Firearms.TYPE			+ " TEXT, " +
							ShootingContract.Firearms.CALIBER		+ " TEXT, " +
							ShootingContract.Firearms.BARREL		+ " NUMERIC " +
						")" );

			db.execSQL( "CREATE VIEW " + VIEW_FIREARM_MAKES + " AS SELECT DISTINCT " + ShootingContract.Firearms.MAKE + " FROM " + TABLE_FIREARMS );
			db.execSQL( "CREATE VIEW " + VIEW_FIREARM_TYPES + " AS SELECT DISTINCT " + ShootingContract.Firearms.TYPE + " FROM " + TABLE_FIREARMS );

			db.execSQL( "CREATE TABLE " + TABLE_FIREARM_PHOTOS + " " +
						"( " +
							ShootingContract.FirearmPhotos._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.FirearmPhotos.FIREARM_ID	+ " INTEGER REFERENCES " + TABLE_FIREARMS + " ( " + ShootingContract.Firearms._ID + " ) ON DELETE RESTRICT, " +
							ShootingContract.FirearmPhotos._DATA		+ " TEXT " +
						")" );

			db.execSQL( "ALTER TABLE " + TABLE_FIREARMS + " " +
						"ADD " + ShootingContract.Firearms.LIST_PHOTO_ID + " INTEGER REFERENCES " + TABLE_FIREARM_PHOTOS + " ( " + ShootingContract.FirearmPhotos._ID + " ) ON DELETE SET NULL" );

			db.execSQL( "CREATE TABLE " + TABLE_FIREARM_ACQUISITION + " " +
						"( " +
							ShootingContract.FirearmAcquisition._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.FirearmAcquisition.FIREARM_ID	+ " INTEGER REFERENCES " + TABLE_FIREARMS + " ( " + ShootingContract.Firearms._ID + " ) ON DELETE CASCADE, " +
							ShootingContract.FirearmAcquisition.DATE		+ " TEXT, " +
							ShootingContract.FirearmAcquisition.FROM		+ " TEXT, " +
							ShootingContract.FirearmAcquisition.LICENSE		+ " TEXT, " +
							ShootingContract.FirearmAcquisition.ADDRESS		+ " TEXT, " +
							ShootingContract.FirearmAcquisition.PRICE		+ " NUMERIC " +
						")" );

			db.execSQL( "CREATE TABLE " + TABLE_FIREARM_DISPOSITION + " " +
						"( " +
							ShootingContract.FirearmDisposition._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.FirearmDisposition.FIREARM_ID	+ " INTEGER REFERENCES " + TABLE_FIREARMS + " ( " + ShootingContract.Firearms._ID + " ) ON DELETE CASCADE, " +
							ShootingContract.FirearmDisposition.DATE		+ " TEXT, " +
							ShootingContract.FirearmDisposition.TO			+ " TEXT, " +
							ShootingContract.FirearmDisposition.LICENSE		+ " TEXT, " +
							ShootingContract.FirearmDisposition.ADDRESS		+ " TEXT, " +
							ShootingContract.FirearmDisposition.PRICE		+ " NUMERIC " +
						")" );

			db.execSQL( "CREATE TABLE " + TABLE_FIREARM_NOTES + " " +
						"( " +
							ShootingContract.FirearmNotes._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.FirearmNotes.FIREARM_ID	+ " INTEGER REFERENCES " + TABLE_FIREARMS + " ( " + ShootingContract.Firearms._ID + " ) ON DELETE CASCADE, " +
							ShootingContract.FirearmNotes.DATE			+ " TEXT, " +
							ShootingContract.FirearmNotes.TEXT 			+ " TEXT " +
						")" );

			db.execSQL( "CREATE TABLE " + TABLE_TARGETS + " " +
						"( " +
							ShootingContract.Targets._ID		+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.Targets.DATE		+ " TEXT, " +
							ShootingContract.Targets.FIREARM_ID	+ " INTEGER REFERENCES " + TABLE_FIREARMS + " ( " + ShootingContract.Firearms._ID + " ) ON DELETE RESTRICT, " +
							ShootingContract.Targets.AMMO		+ " TEXT, " +
							ShootingContract.Targets.LOT_ID		+ " INTEGER REFERENCES " + TABLE_LOAD_LOTS + " ( " + ShootingContract.LoadLots._ID + " ) ON DELETE SET NULL, " +
							ShootingContract.Targets.TYPE		+ " TEXT, " +
							ShootingContract.Targets.DISTANCE	+ " INTEGER, " +
							ShootingContract.Targets.SHOTS		+ " INTEGER, " +
							ShootingContract.Targets.NOTES		+ " TEXT, " +
							ShootingContract.Targets._DATA		+ " TEXT " +
						")" );

			db.execSQL( "CREATE VIEW " + VIEW_TARGET_AMMO + " AS SELECT DISTINCT " + ShootingContract.Targets.AMMO + " FROM " + TABLE_TARGETS );
			db.execSQL( "CREATE VIEW " + VIEW_TARGET_TYPES + " AS SELECT DISTINCT " + ShootingContract.Targets.TYPE + " FROM " + TABLE_TARGETS );

			db.execSQL( "CREATE TABLE " + TABLE_LOADS + " " +
						"( " +
							ShootingContract.Loads._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.Loads.CALIBER		+ " TEXT, " +
							ShootingContract.Loads.BULLET		+ " TEXT, " +
							ShootingContract.Loads.POWDER		+ " TEXT, " +
							ShootingContract.Loads.CHARGE		+ " NUMERIC, " +
							ShootingContract.Loads.PRIMER		+ " TEXT, " +
							ShootingContract.Loads.OAL			+ " NUMERIC, " +
							ShootingContract.Loads.CRIMP		+ " TEXT " +
						")" );

			db.execSQL( "CREATE VIEW " + VIEW_LOAD_BULLETS + " AS SELECT DISTINCT " + ShootingContract.Loads.BULLET + " FROM " + TABLE_LOADS );
			db.execSQL( "CREATE VIEW " + VIEW_LOAD_POWDERS + " AS SELECT DISTINCT " + ShootingContract.Loads.POWDER + " FROM " + TABLE_LOADS );

			db.execSQL( "CREATE TABLE " + TABLE_LOAD_LOTS + " " +
						"( " +
							ShootingContract.LoadLots._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.LoadLots.LOAD_ID		+ " INTEGER REFERENCES " + TABLE_LOADS + " ( " + ShootingContract.Loads._ID + " ) ON DELETE CASCADE, " +
							ShootingContract.LoadLots.DATE			+ " TEXT, " +
							ShootingContract.LoadLots.CCOUNT		+ " INTEGER, " +
							ShootingContract.LoadLots.POWDER_LOT	+ " TEXT, " +
							ShootingContract.LoadLots.PRIMER		+ " TEXT, " +
							ShootingContract.LoadLots.PRIMER_LOT	+ " TEXT " +
						")" );

			db.execSQL( "CREATE TABLE " + TABLE_LOAD_NOTES + " " +
						"( " +
							ShootingContract.LoadNotes._ID			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
							ShootingContract.LoadNotes.LOAD_ID		+ " INTEGER REFERENCES " + TABLE_LOADS + " ( " + ShootingContract.Loads._ID + " ) ON DELETE CASCADE, " +
							ShootingContract.LoadNotes.DATE			+ " TEXT, " +
							ShootingContract.LoadNotes.TEXT 		+ " TEXT " +
						")" );

			db.execSQL( "CREATE VIEW " + VIEW_TARGETS_WITHDATA + " " +
						"AS " +
						"SELECT " +
							"t.*, " +
							"f." + ShootingContract.Firearms.MAKE			+ " AS " + ShootingContract.TargetsWithData.FIREARM_MAKE	+ ", " +
						    "f." + ShootingContract.Firearms.MODEL			+ " AS " + ShootingContract.TargetsWithData.FIREARM_MODEL	+ ", " +
						    "f." + ShootingContract.Firearms.SERIAL			+ " AS " + ShootingContract.TargetsWithData.FIREARM_SERIAL	+ ", " +
						    "f." + ShootingContract.Firearms.TYPE			+ " AS " + ShootingContract.TargetsWithData.FIREARM_TYPE	+ ", " +
						    "f." + ShootingContract.Firearms.CALIBER		+ " AS " + ShootingContract.TargetsWithData.FIREARM_CALIBER	+ ", " +
						    "f." + ShootingContract.Firearms.BARREL			+ " AS " + ShootingContract.TargetsWithData.FIREARM_BARREL	+ ", " +

							"l." + ShootingContract.Loads._ID				+ " AS " + ShootingContract.TargetsWithData.LOAD_ID			+ ", " +
							"l." + ShootingContract.Loads.CALIBER			+ " AS " + ShootingContract.TargetsWithData.LOAD_CALIBER	+ ", " +
							"l." + ShootingContract.Loads.BULLET			+ " AS " + ShootingContract.TargetsWithData.LOAD_BULLET		+ ", " +
							"l." + ShootingContract.Loads.POWDER			+ " AS " + ShootingContract.TargetsWithData.LOAD_POWDER		+ ", " +
							"l." + ShootingContract.Loads.CHARGE			+ " AS " + ShootingContract.TargetsWithData.LOAD_CHARGE		+ ", " +
							"l." + ShootingContract.Loads.PRIMER			+ " AS " + ShootingContract.TargetsWithData.LOAD_PRIMER		+ ", " +
							"l." + ShootingContract.Loads.OAL				+ " AS " + ShootingContract.TargetsWithData.LOAD_OAL		+ ", " +
							"l." + ShootingContract.Loads.CRIMP				+ " AS " + ShootingContract.TargetsWithData.LOAD_CRIMP		+ ", " +

							"ll." + ShootingContract.LoadLots.DATE			+ " AS " + ShootingContract.TargetsWithData.LOT_DATE		+ ", " +
							"ll." + ShootingContract.LoadLots.CCOUNT		+ " AS " + ShootingContract.TargetsWithData.LOT_CCOUNT		+ ", " +
							"ll." + ShootingContract.LoadLots.POWDER_LOT	+ " AS " + ShootingContract.TargetsWithData.LOT_POWDER_LOT	+ ", " +
							"ll." + ShootingContract.LoadLots.PRIMER		+ " AS " + ShootingContract.TargetsWithData.LOT_PRIMER		+ ", " +
							"ll." + ShootingContract.LoadLots.PRIMER_LOT	+ " AS " + ShootingContract.TargetsWithData.LOT_PRIMER_LOT	+ " " +
						"FROM " +
							TABLE_TARGETS + " t " +
							"LEFT OUTER JOIN " + TABLE_LOAD_LOTS + " ll ON ll." + ShootingContract.LoadLots._ID + " = t." + ShootingContract.Targets.LOT_ID + " " +
							"LEFT OUTER JOIN " + TABLE_LOADS + " l ON l." + ShootingContract.Loads._ID + " = ll." + ShootingContract.LoadLots.LOAD_ID + ", " +
							TABLE_FIREARMS + " f " +
						"WHERE " +
							"f." + ShootingContract.Firearms._ID + " = t." + ShootingContract.Targets.FIREARM_ID );

			db.execSQL( "CREATE VIEW " + VIEW_LOAD_LOT_AGGREGATES  + " " +
						"AS " +
						"SELECT " +
							"l.*, " +
							"IFNULL( SUM( ll." + ShootingContract.LoadLots.CCOUNT + " ), 0 ) AS " + ShootingContract.LoadLotAggregates.CCOUNT + ", " +
							"MAX( ll." + ShootingContract.LoadLots.DATE + " ) AS " + ShootingContract.LoadLotAggregates.DATE + " " +
						"FROM " +
							TABLE_LOADS + " l " +
							"LEFT OUTER JOIN " + TABLE_LOAD_LOTS + " ll ON ll." + ShootingContract.LoadLots.LOAD_ID + " = l." + ShootingContract.Loads._ID + " " +
						"GROUP BY " +
							"l." + ShootingContract.Loads._ID );

			db.execSQL( "CREATE VIEW " + VIEW_CALIBERS + " " +
					    "AS " +
						"SELECT DISTINCT " + ShootingContract.Calibers.CALIBER + " FROM " +
						"( " +
							"SELECT DISTINCT " + ShootingContract.Firearms.CALIBER + " AS " + ShootingContract.Calibers.CALIBER + " FROM " + TABLE_FIREARMS + " " +
							"UNION ALL " +
							"SELECT DISTINCT " + ShootingContract.Loads.CALIBER + " AS " + ShootingContract.Calibers.CALIBER + " FROM " + TABLE_LOADS + " " +
						")" );

			db.execSQL( "CREATE VIEW " + VIEW_PRIMERS + " " +
					    "AS " +
						"SELECT DISTINCT " + ShootingContract.Primers.PRIMER + " FROM " +
						"( " +
							"SELECT DISTINCT " + ShootingContract.Loads.PRIMER + " AS " + ShootingContract.Primers.PRIMER + " FROM " + TABLE_LOADS + " " +
							"UNION ALL " +
							"SELECT DISTINCT " + ShootingContract.LoadLots.PRIMER + " AS " + ShootingContract.Primers.PRIMER + " FROM " + TABLE_LOAD_LOTS + " " +
						")" );
		}

		@Override
		public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
		{
			if ( oldVersion < 2 )
			{
				/*
				 * ChangeLog:
				 *
				 * 		Added FirearmMakes view
				 * 		Added FirearmTypes view
				 * 		Replaced LoadCalibers view with a generic Calibers view that pulls values from both Firearms and Loads
				 * 		Added Primers view
				 *
				 */

				db.execSQL( "CREATE VIEW " + VIEW_FIREARM_MAKES + " AS SELECT DISTINCT " + ShootingContract.Firearms.MAKE + " FROM " + TABLE_FIREARMS );
				db.execSQL( "CREATE VIEW " + VIEW_FIREARM_TYPES + " AS SELECT DISTINCT " + ShootingContract.Firearms.TYPE + " FROM " + TABLE_FIREARMS );
				db.execSQL( "DROP VIEW LoadCalibers" );
				db.execSQL( "CREATE VIEW " + VIEW_CALIBERS + " " +
						    "AS " +
							"SELECT DISTINCT " + ShootingContract.Calibers.CALIBER + " FROM " +
							"( " +
								"SELECT DISTINCT " + ShootingContract.Firearms.CALIBER + " AS " + ShootingContract.Calibers.CALIBER + " FROM " + TABLE_FIREARMS + " " +
								"UNION ALL " +
								"SELECT DISTINCT " + ShootingContract.Loads.CALIBER + " AS " + ShootingContract.Calibers.CALIBER + " FROM " + TABLE_LOADS + " " +
							")" );
				db.execSQL( "CREATE VIEW " + VIEW_PRIMERS + " " +
						    "AS " +
							"SELECT DISTINCT " + ShootingContract.Primers.PRIMER + " FROM " +
							"( " +
								"SELECT DISTINCT " + ShootingContract.Loads.PRIMER + " AS " + ShootingContract.Primers.PRIMER + " FROM " + TABLE_LOADS + " " +
								"UNION ALL " +
								"SELECT DISTINCT " + ShootingContract.LoadLots.PRIMER + " AS " + ShootingContract.Primers.PRIMER + " FROM " + TABLE_LOAD_LOTS + " " +
							")" );
			}

			if ( oldVersion < 3 )
			{
				/*
				 * ChangeLog:
				 *
				 * 		Added list_photo_id to Firearms
				 *
				 */

				db.execSQL( "ALTER TABLE " + TABLE_FIREARMS + " " +
							"ADD " + ShootingContract.Firearms.LIST_PHOTO_ID + " INTEGER REFERENCES " + TABLE_FIREARM_PHOTOS + " ( " + ShootingContract.FirearmPhotos._ID + " ) ON DELETE SET NULL" );
			}

			if ( oldVersion < 4 )
			{
				/*
				 * ChangeLog:
				 *
				 * 		Replaced LoadsWithCount view with LoadLotAggregates, which includes the most recently loaded date
				 *
				 */

				db.execSQL( "DROP VIEW LoadsWithCount" );
				db.execSQL( "CREATE VIEW " + VIEW_LOAD_LOT_AGGREGATES  + " " +
							"AS " +
							"SELECT " +
								"l.*, " +
								"IFNULL( SUM( ll." + ShootingContract.LoadLots.CCOUNT + " ), 0 ) AS " + ShootingContract.LoadLotAggregates.CCOUNT + ", " +
								"MAX( ll." + ShootingContract.LoadLots.DATE + " ) AS " + ShootingContract.LoadLotAggregates.DATE + " " +
							"FROM " +
								TABLE_LOADS + " l " +
								"LEFT OUTER JOIN " + TABLE_LOAD_LOTS + " ll ON ll." + ShootingContract.LoadLots.LOAD_ID + " = l." + ShootingContract.Loads._ID + " " +
							"GROUP BY " +
								"l." + ShootingContract.Loads._ID );
			}
		}
	}
}
