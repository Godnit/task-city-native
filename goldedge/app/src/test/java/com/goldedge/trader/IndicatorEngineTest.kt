package com.goldedge.trader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorEngineTest {
    @Test
    fun riskCalculatorComputesGoldLotAndRR() {
        val r = IndicatorEngine.risk(
            balance = 1000.0,
            riskPct = 1.0,
            entry = 4500.0,
            sl = 4490.0,
            tp = 4520.0
        )
        assertNotNull(r)
        assertEquals(10.0, r!!.riskAmount, 0.0001)
        assertEquals(2.0, r.rr, 0.0001)
        assertEquals(0.01, r.lotSize, 0.0001)
    }

    @Test
    fun analysisRecognizesStrongRisingSeries() {
        val candles = (0 until 80).map { i ->
            val base = 4300.0 + i * 1.5
            Candle(i.toLong(), base, base + 2.0, base - 1.0, base + 1.0)
        }
        val a = IndicatorEngine.analyze("H1", candles)
        assertNotNull(a)
        assertTrue(a!!.score > 0)
        assertEquals(BiasDirection.BULLISH, a.direction)
    }
}
