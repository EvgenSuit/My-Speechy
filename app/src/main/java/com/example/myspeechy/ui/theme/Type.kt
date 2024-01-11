package com.example.myspeechy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myspeechy.R

val itimFamily = FontFamily(Font(R.font.itim_regular))
val kalamFamily = FontFamily(Font(R.font.kalam_regular))
val lalezarFamily = FontFamily(Font(R.font.lalezar_regular))
val lemonadaFamily = FontFamily(Font(R.font.lemonada_regular))

// Set of Material typography styles to start with
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
    titleLarge = TextStyle(
        fontFamily = itimFamily,
        fontSize = 48.sp,
        color = Color.White,
    )
)