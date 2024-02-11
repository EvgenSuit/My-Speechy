package com.example.myspeechy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.myspeechy.R

val itimFamily = FontFamily(Font(R.font.itim_regular))
val kalamFamily = FontFamily(Font(R.font.kalam_regular))
val lalezarFamily = FontFamily(Font(R.font.lalezar_regular))
val lemonadaFamily = FontFamily(Font(R.font.lemonada_regular))
val garamondFamily = FontFamily(Font(R.font.garamond))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = lemonadaFamily,
        fontSize = 22.sp,
        color = Color.White,
    ),
    bodySmall = TextStyle(
        fontFamily = garamondFamily,
        fontSize = 25.sp,
        color = Color.Black.copy(0.8f)
    ),
    titleLarge = TextStyle(
        fontFamily = itimFamily,
        fontSize = 48.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 35.sp
    )
)