/*
 * Copyright (c) 2024, Macweese <https://github.com/Macweese>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.dkvl.largelogout;

import net.runelite.api.widgets.InterfaceID;

public class Widgets
{
	public static final int LOGOUT_LAYOUT = PACK(InterfaceID.LOGOUT_PANEL, 0);
	public static final int BUTTON_PANE = PACK(InterfaceID.LOGOUT_PANEL, 1);
	public static final int INFO_TEXT = PACK(InterfaceID.LOGOUT_PANEL, 2);
	public static final int SWITCH_BUTTON = PACK(InterfaceID.LOGOUT_PANEL, 3);
	public static final int LOGOUT_BUTTON = PACK(InterfaceID.LOGOUT_PANEL, 8);
	public static final int LOGOUT_BUTTON_C = PACK(InterfaceID.LOGOUT_PANEL, 9);
	public static final int LOGOUT_BUTTON_L = PACK(InterfaceID.LOGOUT_PANEL, 10);
	public static final int LOGOUT_BUTTON_R = PACK(InterfaceID.LOGOUT_PANEL, 11);
	public static final int LOGOUT_BUTTON_TEXT = PACK(InterfaceID.LOGOUT_PANEL, 12);
	public static final int REVIEW_PANE = PACK(InterfaceID.LOGOUT_PANEL, 13);

	public static final int WORLD_SWITCHER = PACK(InterfaceID.WORLD_SWITCHER, 0);
	public static final int WORLD_SWITCHER_WINDOW = PACK(InterfaceID.WORLD_SWITCHER, 6);
	public static final int WORLD_SWITCHER_WINDOW_SUB = PACK(InterfaceID.WORLD_SWITCHER, 7);
	public static final int WORLD_SWITCHER_SEPARATOR = PACK(InterfaceID.WORLD_SWITCHER, 9);
	public static final int WORLD_SWITCHER_LIST_CONTAINER = PACK(InterfaceID.WORLD_SWITCHER, 10);
	public static final int WORLD_SWITCHER_WORLD_LIST = PACK(InterfaceID.WORLD_SWITCHER, 17);
	public static final int WORLD_SWITCHER_SCROLLBAR = PACK(InterfaceID.WORLD_SWITCHER, 20);
	public static final int WORLD_SWITCHER_BOTTOM_PANEL = PACK(InterfaceID.WORLD_SWITCHER, 21);
	public static final int WORLD_SWITCHER_PLACEHOLDER_TEXT = PACK(InterfaceID.WORLD_SWITCHER, 22);
	public static final int WORLD_SWITCHER_FAVORITE_1 = PACK(InterfaceID.WORLD_SWITCHER, 23);
	public static final int WORLD_SWITCHER_FAVORITE_2 = PACK(InterfaceID.WORLD_SWITCHER, 24);
	public static final int WORLD_SWITCHER_LOGOUT = PACK(InterfaceID.WORLD_SWITCHER, 25);

	private static int PACK(int groupId, int childId)
	{
		return groupId << 16 | childId;
	}
}
