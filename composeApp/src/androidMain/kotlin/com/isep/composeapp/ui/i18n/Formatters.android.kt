package com.isep.composeapp.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.GregorianCalendar
import java.util.Locale
import kotlinx.datetime.LocalDate

@Composable
actual fun currentLanguageTag(): String = LocalConfiguration.current.locales[0].toLanguageTag()

actual fun formatPrice(price: Double, languageTag: String): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)).apply {
        currency = Currency.getInstance("EUR")
    }.format(price)

actual fun formatDate(date: LocalDate, languageTag: String): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag(languageTag))
        .format(GregorianCalendar(date.year, date.month.ordinal, date.day).time)
