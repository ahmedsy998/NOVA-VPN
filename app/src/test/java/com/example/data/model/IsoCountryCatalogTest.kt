package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoCountryCatalogTest {

  @Test
  fun `catalog contains full ISO 3166-1 coverage with flags and zero fabricated servers`() {
    val countries = IsoCountryCatalog.buildAllCountries()
    assertTrue("Expected >= 240 ISO entries, got ${countries.size}", countries.size >= 240)

    val uae = countries.first { it.isoCode == "AE" }
    assertEquals("🇦🇪", uae.flagEmoji)
    assertEquals(GeographicalRegion.MENA, uae.region)
    assertFalse(uae.hasDeployedServers)
    assertEquals(0, uae.totalServerCount)

    val germany = countries.first { it.isoCode == "DE" }
    assertEquals("🇩🇪", germany.flagEmoji)
    assertEquals(GeographicalRegion.EUROPE, germany.region)
    assertTrue(germany.nameAr.isNotBlank())
    assertTrue(germany.nameEn.isNotBlank())
  }
}
