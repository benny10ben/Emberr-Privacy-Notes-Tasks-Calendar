// The colours every home screen widget shares so they all match the app's look.
package com.ben.emberr.presentation.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import com.ben.emberr.ui.theme.CharcoalNoir
import com.ben.emberr.ui.theme.CloudVeil
import com.ben.emberr.ui.theme.IroncladGrey
import com.ben.emberr.ui.theme.UrbanFog

internal val surfaceColor = ColorProvider(day = Color.White, night = Color.Black)
internal val primaryTextColor = ColorProvider(day = Color.Black, night = Color.White)
internal val secondaryTextColor = ColorProvider(day = UrbanFog, night = UrbanFog)
internal val separatorColor = ColorProvider(day = Color(0xFFD4D4D4), night = Color(0xFF3A3A3A))
internal val elevatedSurfaceColor = ColorProvider(day = CloudVeil, night = IroncladGrey)
internal val highlightColor = ColorProvider(day = CharcoalNoir, night = CloudVeil)
internal val onHighlightColor = ColorProvider(day = CloudVeil, night = CharcoalNoir)
