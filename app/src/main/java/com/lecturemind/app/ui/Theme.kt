package com.lecturemind.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

internal fun Context.activityOrNull(): Activity? {
    var current=this
    repeat(10) { current=when(val value=current) { is Activity -> return value; is ContextWrapper -> value.baseContext; else -> return null } }
    return null
}

val Teal=Color(0xFF00796B)
private val Light=lightColorScheme(primary=Teal,onPrimary=Color.White,primaryContainer=Color(0xFFD5F2E9),onPrimaryContainer=Color(0xFF00382F),secondary=Color(0xFF52655F),background=Color(0xFFF7FAF8),surface=Color(0xFFF7FAF8),surfaceContainer=Color(0xFFEDF3F0),surfaceContainerLow=Color.White,onSurface=Color(0xFF192B27),onSurfaceVariant=Color(0xFF52655F),outline=Color(0xFF71847C),outlineVariant=Color(0xFFD5E1DB))
private val Dark=darkColorScheme(primary=Color(0xFF70D8BC),onPrimary=Color(0xFF00382F),primaryContainer=Color(0xFF005044),onPrimaryContainer=Color(0xFFB6F2DF),background=Color(0xFF101B18),surface=Color(0xFF101B18),surfaceContainer=Color(0xFF21312B),surfaceContainerLow=Color(0xFF182721),onSurface=Color(0xFFE1EEE7),onSurfaceVariant=Color(0xFFB4C9BE),outline=Color(0xFF849D90),outlineVariant=Color(0xFF3A5146))
@Composable fun LectureTheme(appearance: String,content: @Composable ()->Unit) {
    val dark=when(appearance){"Dark"->true;"Light"->false;else->isSystemInDarkTheme()}
    val activity=LocalContext.current.activityOrNull()
    SideEffect { activity?.window?.let { window ->
        WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark }
    } }
    MaterialTheme(colorScheme=if(dark) Dark else Light,typography=Typography(
        headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=38.sp),
        headlineMedium=TextStyle(fontWeight=FontWeight.Bold,fontSize=26.sp,lineHeight=32.sp),
        titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=22.sp,lineHeight=28.sp),
        titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=24.sp),
        bodyLarge=TextStyle(fontSize=16.sp,lineHeight=25.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
        labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp)
    ),content=content)
}
