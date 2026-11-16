package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitModeIconTest {
    @Test fun usesMvvProductInsteadOfGuessingFromLine() {
        assertEquals("🚌", TransitModeIcon.symbol("Bus", "56"))
        assertEquals("🚇", TransitModeIcon.symbol("U-Bahn", "U6"))
        assertEquals("🚋", TransitModeIcon.symbol("Tram", "18"))
        assertEquals("Ⓢ", TransitModeIcon.symbol("S-Bahn", "S7"))
        assertEquals("🚋", TransitModeIcon.symbol("Straßenbahn", "U"))
    }

    @Test fun fallsBackToRecognizableLinePrefixesOnly() {
        assertEquals("🚇", TransitModeIcon.symbol("", "U6"))
        assertEquals("Ⓢ", TransitModeIcon.symbol("", "S8"))
        assertEquals("🚍", TransitModeIcon.symbol("", "56"))
        assertEquals("🚍", TransitModeIcon.symbol("", "U"))
    }

    @Test fun walkingOverridesProductAndUsesOwnIcon() {
        assertEquals("🚶", TransitModeIcon.symbol("Bus", "56", walking = true))
        assertEquals("Fußweg", TransitModeIcon.label("Bus", "56", walking = true))
    }
}
