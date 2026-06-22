#if !defined(__ANDROID__)
#error This file is Android-only
#endif

#include "common.h"
#include "filesystem.h"
#include "fs_int.h"

#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <time.h>

#include "platform/android/android-main.h"

#include <dlfcn.h>
extern byte *fs_mempool;
static inline void *Mem_Calloc( byte *pool, size_t size )
{
	void *ptr = _Mem_Alloc( pool, size, __FILE__, __LINE__ );
	if( ptr ) memset( ptr, 0, size );
	return ptr;
}
static AAssetManager *AAssetManager_fromJava_compat( JNIEnv *env, jobject assetManager )
{
	static AAssetManager *(*real_func)( JNIEnv *, jobject ) = NULL;
	if( !real_func )
	{
		real_func = (AAssetManager *(*)( JNIEnv *, jobject ))dlsym( RTLD_DEFAULT, "AAssetManager_fromJava" );
	}
	if( real_func )
		return real_func( env, assetManager );
	return NULL;
}
#define AAssetManager_fromJava AAssetManager_fromJava_compat

static struct
{
	JNIEnv	*env;
	jobject	activity;
	jclass	activity_class;
	jmethodID getAssetsList;
	jmethodID getAssets;
} jni_assets;

static void Android_GetAssetManager( android_assets_t *assets )
{
	jobject assetManager = jni_assets.env->CallStaticObjectMethod( jni_assets.activity_class, jni_assets.getAssets, assets->engine );

	if( assetManager )
		assets->asset_manager = AAssetManager_fromJava( jni_assets.env, assetManager );
}

static void Android_ListDirectory( stringlist_t *list, const char *path, qboolean engine )
{
	jstring JStr = jni_assets.env->NewStringUTF( path );
	jobjectArray JNIArray = (jobjectArray)jni_assets.env->CallStaticObjectMethod( jni_assets.activity_class, jni_assets.getAssetsList, engine, JStr );
	int JNIArraySize = jni_assets.env->GetArrayLength( JNIArray );

	for( int i = 0; i < JNIArraySize; i++ )
	{
		jstring JNIStr = (jstring)jni_assets.env->GetObjectArrayElement( JNIArray, i );
		const char *CStr = jni_assets.env->GetStringUTFChars( JNIStr, NULL );

		stringlistappend( list, (char *)CStr );
		jni_assets.env->ReleaseStringUTFChars( JNIStr, CStr );
		jni_assets.env->DeleteLocalRef( JNIStr );
	}

	jni_assets.env->DeleteLocalRef( JNIArray );
	jni_assets.env->DeleteLocalRef( JStr );
}

static void FS_CloseAndroidAssets( android_assets_t *assets )
{
	Mem_Free( assets );
}

static android_assets_t *FS_LoadAndroidAssets( qboolean engine )
{
	android_assets_t *assets = (android_assets_t *)Mem_Calloc( fs_mempool, sizeof( *assets ));

	assets->engine = engine;

	Android_GetAssetManager( assets );
	if( !assets->asset_manager )
	{
		MsgDev( D_WARN, "FS_LoadAndroidAssets: Can't get asset manager\n" );
		Mem_Free( assets );
		return NULL;
	}

	return assets;
}

static int FS_FileTime_AndroidAssets( searchpath_t *search, const char *filename )
{
	static time_t time_val;

	if( !time_val )
	{
		Q_memset( &time_val, 0, sizeof( time_val ));
		time_val = time( NULL );
	}

	return (int)time_val;
}

int FS_FindFile_AndroidAssets( searchpath_t *search, const char *path, char *fixedname, size_t len )
{
	AAsset *asset = AAssetManager_open( search->android_assets->asset_manager, path, AASSET_MODE_UNKNOWN );

	if( asset )
	{
		AAsset_close( asset );

		if( fixedname )
			Q_strncpy( fixedname, path, len );
		return 0;
	}

	return -1;
}

void FS_Search_AndroidAssets( searchpath_t *search, stringlist_t *list, const char *pattern, int caseinsensitive )
{
	char temp[MAX_SYSPATH];
	stringlist_t dirlist;
	const char *separator;
	int basepathlength;

	const char *slash = Q_strrchr( pattern, '/' );
	const char *backslash = Q_strrchr( pattern, '\\' );

	separator = slash;
	if( separator < backslash )
		separator = backslash;

	basepathlength = separator ? (separator + 1 - pattern) : 0;

	Q_memset( &dirlist, 0, sizeof( dirlist ));
	Android_ListDirectory( &dirlist, pattern, search->android_assets->engine );

	Q_strncpy( temp, pattern, sizeof( temp ));

	for( int i = 0; i < dirlist.numstrings; i++ )
	{
		Q_strncpy( &temp[basepathlength], dirlist.strings[i], sizeof( temp ) - basepathlength );

		if( matchpattern( temp, (char *)pattern, true ))
		{
			int j;

			for( j = 0; j < list->numstrings; j++ )
			{
				if( !Q_strcmp( list->strings[j], temp ))
					break;
			}

			if( j == list->numstrings )
				stringlistappend( list, temp );
		}
	}

	stringlistfreecontents( &dirlist );
}

int FS_OpenFile_AndroidAssets( searchpath_t *search, const char *filename, off_t *outOffset, off_t *outLength )
{
	AAsset *asset = AAssetManager_open( search->android_assets->asset_manager, filename, AASSET_MODE_RANDOM );

	if( !asset )
		return -1;

	int fd = AAsset_openFileDescriptor( asset, outOffset, outLength );

	AAsset_close( asset );

	return fd;
}

static byte *FS_LoadAndroidAssetsFile( searchpath_t *search, const char *path, fs_offset_t *filesize )
{
	byte *buf;
	off_t size;
	AAsset *asset = AAssetManager_open( search->android_assets->asset_manager, path, AASSET_MODE_BUFFER );

	if( filesize ) *filesize = 0;

	if( !asset )
		return NULL;

	size = AAsset_getLength( asset );

	buf = (byte *)Mem_Alloc( fs_mempool, size + 1 );
	if( !buf )
	{
		MsgDev( D_WARN, "FS_LoadAndroidAssetsFile: can't alloc %d bytes\n", (int)(size + 1) );
		AAsset_close( asset );
		return NULL;
	}

	buf[size] = '\0';

	if( AAsset_read( asset, buf, size ) < 0 )
	{
		Mem_Free( buf );
		AAsset_close( asset );
		return NULL;
	}

	AAsset_close( asset );
	if( filesize ) *filesize = size;

	return buf;
}

searchpath_t *FS_AddAndroidAssets_Fullpath( const char *path, int flags )
{
	searchpath_t *search;
	android_assets_t *assets;
	qboolean engine = true;

	if( !jni_assets.getAssets || !jni_assets.getAssetsList )
		return NULL;

	if( (flags & FS_STATIC_PATH) || (flags & FS_CUSTOM_PATH) )
		return NULL;

	// if gamefolder != basedir, it's a mod, use mod's package name
	if( (flags & FS_GAMEDIR_PATH) && Q_stricmp( GI->basedir, GI->gamefolder ))
		engine = false;

	assets = FS_LoadAndroidAssets( engine );

	if( !assets )
	{
		MsgDev( D_WARN, "FS_AddAndroidAssets_Fullpath: unable to load Android assets\n" );
		return NULL;
	}

	search = (searchpath_t *)Mem_Calloc( fs_mempool, sizeof( searchpath_t ));
	Q_strncpy( search->filename, "android_assets", sizeof( search->filename ));
	search->android_assets = assets;
	search->flags = flags;
	search->flags |= FS_NOWRITE_PATH | FS_CUSTOM_PATH;

	return search;
}

void FS_InitAndroidAssets( void )
{
	jni_assets.env = (JNIEnv *)Android_GetNativeObject( "JNIEnv" );
	jni_assets.activity_class = (jclass)Android_GetNativeObject( "ActivityClass" );

	if( !jni_assets.env || !jni_assets.activity_class )
	{
		MsgDev( D_WARN, "FS_InitAndroidAssets: unable to get JNI env\n" );
		return;
	}

	jni_assets.getAssets = jni_assets.env->GetStaticMethodID( jni_assets.activity_class, "getAssets", "(Z)Landroid/content/res/AssetManager;" );
	jni_assets.getAssetsList = jni_assets.env->GetStaticMethodID( jni_assets.activity_class, "getAssetsList", "(ZLjava/lang/String;)[Ljava/lang/String;" );

	if( !jni_assets.getAssets || !jni_assets.getAssetsList )
		MsgDev( D_WARN, "FS_InitAndroidAssets: unable to find JNI methods\n" );
}

