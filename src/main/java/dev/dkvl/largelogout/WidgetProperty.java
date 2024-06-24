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


import lombok.Getter;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

@Getter
public enum WidgetProperty
{
	// 69:6
	WORLD_SWITCHER_WINDOW(0, 23, 0, 55, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.MINUS, WidgetSizeMode.MINUS),
	// 69:9
	WORLD_SWITCHER_SEPARATOR(WidgetType.RECTANGLE, 0, false, "", 0x73654A, 50, -1, false, 0, 0, 0, 1, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE),
	// 69:21
	WORLD_SWITCHER_BOTTOM_PANEL(0, 0, 0, 32, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE),
	// 69:22
	WORLD_SWITCHER_PLACEHOLDER_TEXT(WidgetType.TEXT, 0, true, "Right-click on worlds to<br>set them as Favourites.", 0xFF981F, 0, -1, false, 0, 0, 21, 0, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.MINUS, WidgetSizeMode.MINUS),
	// 69:23
	WORLD_SWITCHER_FAVORITE_1(0, 0, 21, 8192, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE_16384THS),
	// 69:24
	WORLD_SWITCHER_FAVORITE_2(0, 0, 21, 8192, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE_16384THS),
	// 69:25
	WORLD_SWITCHER_LOGOUT(WidgetType.GRAPHIC, 0, false, "", 0, 0, SpriteID.UNUSED_TAB_LOGOUT_1191, false, 2, 0, 21, 30, WidgetPositionMode.ABSOLUTE_RIGHT, WidgetPositionMode.ABSOLUTE_CENTER, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE),

	// 182:1
	BUTTON_PANE(0, 16, 0, 132, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE),
	// 182:2
	INFO_TEXT(WidgetType.TEXT, 0, false, "Use the buttons below to<br>logout or switch worlds safely.", 0xFF981F, 0, -1, false, 0, 0, 0, 40, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE),
	// 182:3
	SWITCH_BUTTON(0, 0, 144, 36, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_CENTER, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE),
	// 182:8
	LOGOUT_BUTTON(0, 0, 144, 36, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_BOTTOM, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE),
	// 182:9
	LOGOUT_BUTTON_C(WidgetType.GRAPHIC, 0, false, "", 0, 0, SpriteID.UNKNOWN_BUTTON_MIDDLE_SELECTED, true, 26, 0, 94, 36, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE),
	// 182:10
	LOGOUT_BUTTON_L(WidgetType.GRAPHIC, 0, false, "", 0, 0, SpriteID.STATS_TILE_HALF_LEFT_SELECTED, false, 0, 0, 36, 0, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.MINUS),
	// 182:11
	LOGOUT_BUTTON_R(WidgetType.GRAPHIC, 0, false, "", 0, 0, SpriteID.STATS_TILE_HALF_RIGHT_SELECTED, false, 108, 0, 36, 36, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE),
	// 182:12
	LOGOUT_BUTTON_TEXT(WidgetType.TEXT, 205, false, "Click here to logout", 0xF7F0DF, 0, -1, false, 0, 0, 0, 0, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_CENTER, WidgetSizeMode.MINUS, WidgetSizeMode.MINUS),
	;

	private final int type;
	private final int contentType;
	private final boolean hidden;
	private final String text;
	private final int textColor;
	private final int opacity;
	private final int spriteId;
	private final boolean spriteTiling;
	private final int originalX;
	private final int originalY;
	private final int originalWidth;
	private final int originalHeight;
	private final int xPositionMode;
	private final int yPositionMode;
	private final int widthMode;
	private final int heightMode;

	WidgetProperty(int originalX, int originalY, int originalWidth, int originalHeight, int xPositionMode, int yPositionMode, int widthMode, int heightMode)
	{
		this(WidgetType.LAYER, 0, false, "", 0, 0, -1, false, originalX, originalY, originalWidth, originalHeight, xPositionMode, yPositionMode, widthMode, heightMode);
	}

	WidgetProperty(int type, int contentType, boolean hidden, String text, int textColor, int opacity, int spriteId, boolean spriteTiling, int originalX, int originalY, int originalWidth, int originalHeight, int xPositionMode, int yPositionMode, int widthMode, int heightMode)
	{
		this.type = type;
		this.contentType = contentType;
		this.hidden = hidden;
		this.text = text;
		this.textColor = textColor;
		this.opacity = opacity;
		this.spriteId = spriteId;
		this.spriteTiling = spriteTiling;
		this.originalX = originalX;
		this.originalY = originalY;
		this.originalWidth = originalWidth;
		this.originalHeight = originalHeight;
		this.xPositionMode = xPositionMode;
		this.yPositionMode = yPositionMode;
		this.widthMode = widthMode;
		this.heightMode = heightMode;
	}
}
