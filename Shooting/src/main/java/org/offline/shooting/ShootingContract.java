/*
 * :vi ts=4 sts=4 sw=4
 *
 * Copyright (c) Jonathan Burchmore
 */

package org.offline.shooting;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.File;
import java.text.SimpleDateFormat;

public class ShootingContract
{
	public static final String				AUTHORITY	= "org.offline.shooting.provider";
	public static final SimpleDateFormat 	DateFormat	= new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class Firearms implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearms" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".firearm";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm";

		public static final String	MAKE				= "make";
		public static final String	MODEL				= "model";
		public static final String	SERIAL				= "serial";
		public static final String	TYPE				= "type";
		public static final String	CALIBER				= "caliber";
		public static final String	BARREL				= "barrel";
		public static final String	LIST_PHOTO_ID		= "list_photo_id";
	}

	public static class FirearmMakes
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearms/makes" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.make";

		public static final String	MAKE				= "make";
	}

	public static class FirearmTypes
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearms/types" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.type";

		public static final String	TYPE				= "type";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class FirearmPhotos implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearmphotos" );
		public static final String	CONTENT_ITEM_TYPE	= "image/jpeg";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.photo";

		public static final String	FIREARM_ID			= "firearm_id";
		public static final String	_DATA				= "_data";

		public static String generatePath( Context c, long firearmId )
		{
			File dir;
			File file;

			dir		= c.getExternalFilesDir( "firearms" );
			file	= new File( dir, String.valueOf( firearmId ) + "-" + String.valueOf( System.currentTimeMillis() ) + ".jpg" );
			dir.mkdirs();

			return file.getAbsolutePath();
		}
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class FirearmAcquisition implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearmacquisition" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".firearm.acquisition";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.acquisition";

		public static final String	FIREARM_ID			= "firearm_id";
		public static final String	DATE				= "acq_date";
		public static final String	FROM				= "acq_from";
		public static final String	LICENSE				= "license";
		public static final String	ADDRESS				= "address";
		public static final String	PRICE				= "price";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class FirearmDisposition implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearmdisposition" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".firearm.disposition";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.disposition";

		public static final String	FIREARM_ID			= "firearm_id";
		public static final String	DATE				= "disp_date";
		public static final String	TO					= "disp_to";
		public static final String	LICENSE				= "license";
		public static final String	ADDRESS				= "address";
		public static final String	PRICE				= "price";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class FirearmNotes implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/firearmnotes" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".firearm.note";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".firearm.note";

		public static final String	FIREARM_ID			= "firearm_id";
		public static final String	DATE				= "date";
		public static final String	TEXT				= "note_text";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class Targets implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/targets" );
		public static final Uri		IMAGE_URI			= Uri.parse( "content://" + AUTHORITY + "/targets/images" );
		public static final String	CONTENT_IMAGE_TYPE	= "image/jpeg";
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".target";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".target";

		public static final String	DATE				= "date";
		public static final String	FIREARM_ID			= "firearm_id";
		public static final String	AMMO				= "ammo";
		public static final String	LOT_ID				= "lot_id";
		public static final String	TYPE				= "type";
		public static final String	DISTANCE			= "distance";
		public static final String	SHOTS				= "shots";
		public static final String	NOTES				= "notes";
		public static final String	_DATA				= "_data";

		public static String generatePath( Context c )
		{
			File dir;
			File file;

			dir		= c.getExternalFilesDir( "targets" );
			file	= new File( dir, String.valueOf( System.currentTimeMillis() ) + ".jpg" );
			dir.mkdirs();

			return file.getAbsolutePath();
		}
	}

	public static class TargetsWithData extends Targets
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/targets/withdata" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".target.withdata";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".target.withdata";

		public static final String	FIREARM_MAKE		= "firearm_make";
		public static final String	FIREARM_MODEL		= "firearm_model";
		public static final String	FIREARM_SERIAL		= "firearm_serial";
		public static final String	FIREARM_TYPE		= "firearm_type";
		public static final String	FIREARM_CALIBER		= "firearm_caliber";
		public static final String	FIREARM_BARREL		= "firearm_barrel";

		public static final String	LOAD_ID				= "load_id";
		public static final String	LOAD_CALIBER		= "load_caliber";
		public static final String	LOAD_BULLET			= "load_bullet";
		public static final String	LOAD_POWDER			= "load_powder";
		public static final String	LOAD_CHARGE			= "load_charge";
		public static final String	LOAD_PRIMER			= "load_primer";
		public static final String	LOAD_OAL			= "load_oal";
		public static final String	LOAD_CRIMP			= "load_crimp";

		public static final String	LOT_DATE			= "lot_date";
		public static final String	LOT_CCOUNT			= "lot_ccount";
		public static final String	LOT_POWDER_LOT		= "lot_powder_lot";
		public static final String	LOT_PRIMER			= "lot_primer";
		public static final String	LOT_PRIMER_LOT		= "lot_primer_lot";
	}

	public static class TargetAmmo
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/targets/ammo" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".target.ammo";

		public static final String	AMMO				= "ammo";
	}

	public static class TargetTypes
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/targets/types" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".target.type";

		public static final String	TYPE				= "type";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class Loads implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loads" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".load";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load";

		public static final String	CALIBER				= "caliber";
		public static final String	BULLET				= "bullet";
		public static final String	POWDER				= "powder";
		public static final String	CHARGE				= "charge";
		public static final String	PRIMER				= "primer";
		public static final String	OAL					= "oal";
		public static final String	CRIMP				= "crimp";
	}

	public static class LoadLotAggregates extends Loads
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loads/lotaggregates" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load.logaggregate";

		public static final String	CCOUNT				= "ccount";
		public static final String	DATE				= "date";
	}

	public static class LoadBullets
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loads/bullets" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load.bullet";

		public static final String	BULLET				= "bullet";
	}

	public static class LoadPowders
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loads/powders" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load.powder";

		public static final String	POWDER				= "powder";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class LoadLots implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loadlots" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".load.lot";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load.lot";

		public static final String	LOAD_ID				= "load_id";
		public static final String	DATE				= "date";
		public static final String	CCOUNT				= "ccount";
		public static final String	POWDER_LOT			= "powder_lot";
		public static final String	PRIMER				= "primer";
		public static final String	PRIMER_LOT			= "primer_lot";
	}

	@SuppressWarnings( "SuperClassHasFrequentlyUsedInheritors" )
	public static class LoadNotes implements BaseColumns
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/loadnotes" );
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + ".load.note";
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".load.note";

		public static final String	LOAD_ID				= "load_id";
		public static final String	DATE				= "date";
		public static final String	TEXT				= "note_text";
	}

	public static class Calibers
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/calibers" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".caliber";

		public static final String	CALIBER				= "caliber";
	}

	public static class Primers
	{
		public static final Uri		CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/primers" );
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + ".primer";

		public static final String	PRIMER				= "primer";
	}
}
