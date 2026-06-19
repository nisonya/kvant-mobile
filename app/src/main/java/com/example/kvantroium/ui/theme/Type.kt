package com.example.kvantroium.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R

val Montserrat = FontFamily(Font(R.font.montserrat))
val MontserratMedium = FontFamily(Font(R.font.montserrat_medium))
val Gothic = FontFamily(Font(R.font.gothic))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Gothic,
        fontWeight = FontWeight.Normal,
        fontSize = 25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MontserratMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )
)
