package com.appvin.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WineTest {
    private val year = Calendar.getInstance().get(Calendar.YEAR)

    @Test
    fun drinkStatusFollowsWindow() {
        assertEquals(DrinkStatus.UNKNOWN, Wine(name = "x").drinkStatus)
        assertEquals(DrinkStatus.READY, Wine(name = "x", drinkFrom = year - 1, drinkUntil = year + 1).drinkStatus)
        assertEquals(DrinkStatus.READY, Wine(name = "x", drinkFrom = year, drinkUntil = year).drinkStatus)
        assertEquals(DrinkStatus.TOO_YOUNG, Wine(name = "x", drinkFrom = year + 2, drinkUntil = year + 10).drinkStatus)
        assertEquals(DrinkStatus.PAST_PEAK, Wine(name = "x", drinkFrom = year - 10, drinkUntil = year - 1).drinkStatus)
    }

    @Test
    fun sourcesRoundTripAndDropDuplicates() {
        val raw = formatSources(listOf("a.com" to "https://a.com/1", "b.com" to "https://b.com", "dup" to "https://a.com/1"))
        assertEquals(listOf("a.com" to "https://a.com/1", "b.com" to "https://b.com"), parseSources(raw))
        assertEquals(emptyList<Pair<String, String>>(), parseSources(""))
    }

    @Test
    fun displayNameSkipsBlankAndRepeatedParts() {
        assertEquals("Château Margaux", Wine(name = "Château Margaux", producer = "Château Margaux").displayName)
        assertEquals("Ridge · Monte Bello", Wine(name = "Monte Bello", producer = "Ridge").displayName)
        assertEquals("Monte Bello", Wine(name = "Monte Bello").displayName)
    }
}
