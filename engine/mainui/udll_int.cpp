/*
dll_int.cpp - dll entry point
Copyright (C) 2010 Uncle Mike

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/


#include "extdll_menu.h"
#include "BaseMenu.h"
#include "Utils.h"

ui_enginefuncs_t EngFuncs::engfuncs;
ui_extendedfuncs_t EngFuncs::textfuncs;
ui_globalvars_t	*gpGlobals;
CMenu gMenu;

static UI_FUNCTIONS gFunctionTable = 
{
	UI_VidInit,
	UI_Init,
	UI_Shutdown,
	UI_UpdateMenu,
	UI_KeyEvent,
	UI_MouseMove,
	UI_SetActiveMenu,
	UI_AddServerToList,
	UI_GetCursorPos,
	UI_SetCursorPos,
	UI_ShowCursor,
	UI_CharEvent,
	UI_MouseInRect,
	UI_IsVisible,
	UI_CreditsActive,
	UI_FinalCredits
};

static gameinfo2_t g_fallbackGameInfo;
static gameinfo2_t g_fallbackModInfo[256];

static void DummyInitNetworking( void )
{
}
static void DummyStatus( struct net_status_s *status )
{
}
static void DummySendRequest( int context, int request, int flags, double timeout, struct netadr_s *remote_address, net_api_response_func_t response )
{
}
static void DummyCancelRequest( int context )
{
}
static void DummyCancelAllRequests( void )
{
}
static const char *DummyAdrToString( struct netadr_s *a )
{
	return "";
}
static int DummyCompareAdr( struct netadr_s *a, struct netadr_s *b )
{
	return 0;
}
static int DummyStringToAdr( char *s, struct netadr_s *a )
{
	return 0;
}
static const char *DummyValueForKey( const char *s, const char *key )
{
	return "";
}
static void DummyRemoveKey( char *s, const char *key )
{
}
static void DummySetValueForKey( char *s, const char *key, const char *value, int maxsize )
{
}

static net_api_t g_dummyNetAPI = {
	DummyInitNetworking,
	DummyStatus,
	DummySendRequest,
	DummyCancelRequest,
	DummyCancelAllRequests,
	DummyAdrToString,
	DummyCompareAdr,
	DummyStringToAdr,
	DummyValueForKey,
	DummyRemoveKey,
	DummySetValueForKey
};

static gameinfo2_t *FallbackGetGameInfo( int gi_version )
{
	GAMEINFO old_gi;
	if ( !EngFuncs::engfuncs.pfnGetGameInfo( &old_gi ) )
		return NULL;

	memset( &g_fallbackGameInfo, 0, sizeof( g_fallbackGameInfo ) );
	g_fallbackGameInfo.gi_version = GAMEINFO_VERSION;
	strcpy( g_fallbackGameInfo.gamefolder, old_gi.gamefolder );
	strcpy( g_fallbackGameInfo.startmap, old_gi.startmap );
	strcpy( g_fallbackGameInfo.trainmap, old_gi.trainmap );
	strcpy( g_fallbackGameInfo.title, old_gi.title );
	strcpy( g_fallbackGameInfo.version, old_gi.version );
	g_fallbackGameInfo.flags = old_gi.flags;
	strcpy( g_fallbackGameInfo.game_url, old_gi.game_url );
	strcpy( g_fallbackGameInfo.update_url, old_gi.update_url );
	strcpy( g_fallbackGameInfo.type, old_gi.type );
	strcpy( g_fallbackGameInfo.date, old_gi.date );
	g_fallbackGameInfo.size = atoi( old_gi.size );
	g_fallbackGameInfo.gamemode = (gametype_t)old_gi.gamemode;

	return &g_fallbackGameInfo;
}

static gameinfo2_t *FallbackGetModInfo( int gi_version, int mod_index )
{
	int numGames = 0;
	GAMEINFO **gamesList = EngFuncs::engfuncs.pfnGetGamesList( &numGames );
	if ( !gamesList || mod_index < 0 || mod_index >= numGames )
		return NULL;

	GAMEINFO *old_gi = gamesList[mod_index];
	if ( !old_gi )
		return NULL;

	memset( &g_fallbackModInfo[mod_index % 256], 0, sizeof( gameinfo2_t ) );
	gameinfo2_t *gi = &g_fallbackModInfo[mod_index % 256];
	gi->gi_version = GAMEINFO_VERSION;
	strcpy( gi->gamefolder, old_gi->gamefolder );
	strcpy( gi->startmap, old_gi->startmap );
	strcpy( gi->trainmap, old_gi->trainmap );
	strcpy( gi->title, old_gi->title );
	strcpy( gi->version, old_gi->version );
	gi->flags = old_gi->flags;
	strcpy( gi->game_url, old_gi->game_url );
	strcpy( gi->update_url, old_gi->update_url );
	strcpy( gi->type, old_gi->type );
	strcpy( gi->date, old_gi->date );
	gi->size = atoi( old_gi->size );
	gi->gamemode = (gametype_t)old_gi->gamemode;

	return gi;
}

static char *FallbackParseFile( char *data, char *buf, const int size, unsigned int flags, int *len )
{
	char token[1024];
	char *ret = EngFuncs::engfuncs.COM_ParseFile( data, token );
	if ( ret )
	{
		strncpy( buf, token, size - 1 );
		buf[size - 1] = '\0';
		if ( len ) *len = (int)strlen( token );
	}
	return ret;
}

static double FallbackDoubleTime( void )
{
	return gpGlobals ? gpGlobals->time : 0.0;
}

static int FallbackIsCvarReadOnly( const char *name )
{
	return 0;
}

static int FallbackGetRenderers( unsigned int num, char *short_name, size_t size1, char *long_name, size_t size2 )
{
	return 0;
}

static const char *FallbackAdrToString( const struct netadr_s a )
{
	return "";
}

static int FallbackCompareAdr( const void *a, const void *b )
{
	return 0;
}

//=======================================================================
//			GetApi
//=======================================================================
#pragma optimize("", off)
extern "C" EXPORT int GetMenuAPI(UI_FUNCTIONS *pFunctionTable, ui_enginefuncs_t* pEngfuncsFromEngine, ui_globalvars_t *pGlobals)
{
	if( !pFunctionTable || !pEngfuncsFromEngine )
	{
		return false;
	}

	// Copy UI_FUNCTIONS table to engine
	*pFunctionTable = gFunctionTable;

	// Copy engine funcs to DLL (must do this or UI_Init crashes on null pointers)
	EngFuncs::engfuncs = *pEngfuncsFromEngine;

	// Zero out extended funcs (value-initialization, no CRT call)
	EngFuncs::textfuncs = ui_extendedfuncs_t();

	// Populate fallback extended funcs
	EngFuncs::textfuncs.pfnGetGameInfo = FallbackGetGameInfo;
	EngFuncs::textfuncs.pfnGetModInfo = FallbackGetModInfo;
	EngFuncs::textfuncs.pfnParseFile = FallbackParseFile;
	EngFuncs::textfuncs.pfnDoubleTime = FallbackDoubleTime;
	EngFuncs::textfuncs.pfnIsCvarReadOnly = FallbackIsCvarReadOnly;
	EngFuncs::textfuncs.pfnGetRenderers = FallbackGetRenderers;
	EngFuncs::textfuncs.pfnAdrToString = FallbackAdrToString;
	EngFuncs::textfuncs.pfnCompareAdr = FallbackCompareAdr;
	EngFuncs::textfuncs.pNetAPI = &g_dummyNetAPI;

	gpGlobals = pGlobals;

	return true;
}
#pragma optimize("", on)

struct local_ui_textfuncs_s {
	void (*pfnEnableTextInput)( int enable );
	int (*pfnUtfProcessChar) ( int ch );
	int (*pfnUtfMoveLeft) ( char *str, int pos );
	int (*pfnUtfMoveRight) ( char *str, int pos, int length );
};

extern "C" EXPORT int GiveTextAPI( void *pTextfuncsFromEngine )
{
	if ( !pTextfuncsFromEngine ) return false;
	local_ui_textfuncs_s *tf = (local_ui_textfuncs_s *)pTextfuncsFromEngine;
	EngFuncs::textfuncs.pfnEnableTextInput = tf->pfnEnableTextInput;
	EngFuncs::textfuncs.pfnUtfProcessChar = tf->pfnUtfProcessChar;
	EngFuncs::textfuncs.pfnUtfMoveLeft = tf->pfnUtfMoveLeft;
	EngFuncs::textfuncs.pfnUtfMoveRight = tf->pfnUtfMoveRight;
	return true;
}

static UI_EXTENDED_FUNCTIONS gExtendedTable =
{
	AddTouchButtonToList,
	UI_MenuResetPing_f,
	UI_ConnectionWarning_f,
	UI_UpdateDialog,
	UI_ShowMessageBox,
	UI_ConnectionProgress_Disconnect,
	UI_ConnectionProgress_Download,
	UI_ConnectionProgress_DownloadEnd,
	UI_ConnectionProgress_Precache,
	UI_ConnectionProgress_Connect,
	UI_ConnectionProgress_ChangeLevel,
	UI_ConnectionProgress_ParseServerInfo
};

extern "C" EXPORT int GetExtAPI( int version, UI_EXTENDED_FUNCTIONS *pFunctionTable, ui_extendedfuncs_t *pEngfuncsFromEngine )
{
	if( !pFunctionTable || !pEngfuncsFromEngine )
	{
		return false;
	}

	if( version != MENU_EXTENDED_API_VERSION )
	{
		Con_Printf( "Error: failed to initialize extended menu API. Expected by DLL: %d. Got from engine: %d\n", MENU_EXTENDED_API_VERSION, version );
		return false;
	}

	memcpy( &EngFuncs::textfuncs, pEngfuncsFromEngine, sizeof( ui_extendedfuncs_t ) );
	memcpy( pFunctionTable, &gExtendedTable, sizeof( UI_EXTENDED_FUNCTIONS ));

	return true;
}
