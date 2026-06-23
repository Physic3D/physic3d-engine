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

//=======================================================================
//			GetApi
//=======================================================================
extern "C" EXPORT int GetMenuAPI(UI_FUNCTIONS *pFunctionTable, ui_enginefuncs_t* pEngfuncsFromEngine, ui_globalvars_t *pGlobals)
{
	if( !pFunctionTable || !pEngfuncsFromEngine )
	{
		return false;
	}

	// copy HUD_FUNCTIONS table to engine, copy engfuncs table from engine
	// use manual loops instead of memcpy/memset to avoid any CRT call issues
	{
		const unsigned long *src = (const unsigned long *)&gFunctionTable;
		unsigned long *dst = (unsigned long *)pFunctionTable;
		for( int i = 0; i < (int)(sizeof( UI_FUNCTIONS ) / sizeof( unsigned long )); i++ )
			dst[i] = src[i];
	}
	{
		const unsigned long *src = (const unsigned long *)pEngfuncsFromEngine;
		unsigned long *dst = (unsigned long *)&EngFuncs::engfuncs;
		for( int i = 0; i < (int)(sizeof( ui_enginefuncs_t ) / sizeof( unsigned long )); i++ )
			dst[i] = src[i];
	}
	{
		unsigned long *dst = (unsigned long *)&EngFuncs::textfuncs;
		for( int i = 0; i < (int)(sizeof( ui_extendedfuncs_t ) / sizeof( unsigned long )); i++ )
			dst[i] = 0;
	}

	gpGlobals = pGlobals;

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

	{
		const unsigned long *src = (const unsigned long *)pEngfuncsFromEngine;
		unsigned long *dst = (unsigned long *)&EngFuncs::textfuncs;
		for( int i = 0; i < (int)(sizeof( ui_extendedfuncs_t ) / sizeof( unsigned long )); i++ )
			dst[i] = src[i];
	}
	{
		const unsigned long *src = (const unsigned long *)&gExtendedTable;
		unsigned long *dst = (unsigned long *)pFunctionTable;
		for( int i = 0; i < (int)(sizeof( UI_EXTENDED_FUNCTIONS ) / sizeof( unsigned long )); i++ )
			dst[i] = src[i];
	}

	return true;
}
