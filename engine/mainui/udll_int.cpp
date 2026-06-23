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

#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>

static void DiagWrite(const char *msg)
{
    HANDLE h = CreateFileA("menu_diag.log", GENERIC_WRITE, FILE_SHARE_READ, NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return;
    SetFilePointer(h, 0, NULL, FILE_END);
    DWORD written;
    WriteFile(h, msg, (DWORD)strlen(msg), &written, NULL);
    WriteFile(h, "\r\n", 2, &written, NULL);
    CloseHandle(h);
}
#endif

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
#pragma optimize("", off)
extern "C" EXPORT int GetMenuAPI(UI_FUNCTIONS *pFunctionTable, ui_enginefuncs_t* pEngfuncsFromEngine, ui_globalvars_t *pGlobals)
{
#ifdef _MSC_VER
	DiagWrite("GetMenuAPI entered");
	DiagWrite("sizeof(UI_FUNCTIONS) = XX"); // avoid sprintf
	if( !pFunctionTable || !pEngfuncsFromEngine )
	{
		DiagWrite("GetMenuAPI: null pointer, returning false");
		return false;
	}
	DiagWrite("pointers OK, about to copy struct");
	__try
	{
		*pFunctionTable = gFunctionTable;
		DiagWrite("struct copy OK");
		gpGlobals = pGlobals;
		DiagWrite("globals assigned OK");
	}
	__except(EXCEPTION_EXECUTE_HANDLER)
	{
		DWORD code = GetExceptionCode();
		char buf[128];
		buf[0] = 0;
		{
			const char *hex = "0123456789ABCDEF";
			buf[0] = 'E';
			buf[1] = 'X';
			buf[2] = 'C';
			buf[3] = ' ';
			buf[4] = hex[(code >> 28) & 0xF];
			buf[5] = hex[(code >> 24) & 0xF];
			buf[6] = hex[(code >> 20) & 0xF];
			buf[7] = hex[(code >> 16) & 0xF];
			buf[8] = hex[(code >> 12) & 0xF];
			buf[9] = hex[(code >> 8) & 0xF];
			buf[10] = hex[(code >> 4) & 0xF];
			buf[11] = hex[code & 0xF];
			buf[12] = 0;
		}
		DiagWrite(buf);
		DiagWrite("SEH caught exception, returning false");
		return false;
	}
	return true;
#else
	if( !pFunctionTable || !pEngfuncsFromEngine )
	{
		return false;
	}
	*pFunctionTable = gFunctionTable;
	gpGlobals = pGlobals;
	return true;
#endif
}
#pragma optimize("", on)

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
