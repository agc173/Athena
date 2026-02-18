package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.model.ApiResult

private const val DEFAULT_DATE = "2026-02-18"

class HoroscopeRepositoryImpl : HoroscopeRepository {
    override suspend fun getDaily(sign: ZodiacSign, dateIso: String?): ApiResult<DailyHoroscope> {
        val resolvedDate = dateIso ?: DEFAULT_DATE
        val entry = horoscopeBySign.getValue(sign)
        val horoscope = DailyHoroscope(
            sign = sign,
            dateIso = resolvedDate,
            text = entry.text,
            mood = entry.mood,
            luckyNumber = entry.luckyNumber,
            luckyColor = entry.luckyColor,
            shareText = "${entry.symbol} ${sign.label} — ${entry.text}",
        )
        return ApiResult.Success(horoscope)
    }

    private data class MockEntry(
        val symbol: String,
        val text: String,
        val mood: String,
        val luckyNumber: Int,
        val luckyColor: String,
    )

    private val horoscopeBySign: Map<ZodiacSign, MockEntry> = mapOf(
        ZodiacSign.aries to MockEntry("♈", "Te conviene actuar con decisión, pero sin prisa.", "Motivado", 7, "Rojo"),
        ZodiacSign.taurus to MockEntry("♉", "La constancia te abre una oportunidad inesperada.", "Estable", 4, "Verde"),
        ZodiacSign.gemini to MockEntry("♊", "Una conversación clave te dará claridad.", "Curioso", 9, "Amarillo"),
        ZodiacSign.cancer to MockEntry("♋", "Escucha tu intuición antes de tomar decisiones.", "Sensitivo", 2, "Plateado"),
        ZodiacSign.leo to MockEntry("♌", "Tu energía inspira a quienes te rodean.", "Brillante", 1, "Dorado"),
        ZodiacSign.virgo to MockEntry("♍", "Organizar tu día te dará más calma.", "Práctico", 5, "Azul marino"),
        ZodiacSign.libra to MockEntry("♎", "Buscar equilibrio será tu mejor estrategia.", "Armónico", 6, "Rosa"),
        ZodiacSign.scorpio to MockEntry("♏", "Cierra ciclos pendientes para avanzar liviano.", "Intenso", 8, "Borgoña"),
        ZodiacSign.sagittarius to MockEntry("♐", "Una idea nueva merece que la explores hoy.", "Aventurero", 3, "Morado"),
        ZodiacSign.capricorn to MockEntry("♑", "Tu disciplina tendrá recompensa concreta.", "Enfocado", 10, "Gris"),
        ZodiacSign.aquarius to MockEntry("♒", "Compartir tu visión abrirá nuevas alianzas.", "Innovador", 11, "Turquesa"),
        ZodiacSign.pisces to MockEntry("♓", "La creatividad será tu mejor guía hoy.", "Soñador", 12, "Lavanda"),
    )
}
