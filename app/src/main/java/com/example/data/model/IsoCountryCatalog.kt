package com.example.data.model

import java.util.Locale

/**
 * Complete ISO 3166-1 country & territory catalog generator.
 * Combines JVM's ISO 3166-1 alpha-2 registry with explicit territory codes,
 * official Unicode flag emojis, localized Arabic/English names, and geographical regions.
 * Never fabricates VPN servers, IPs, or ping metrics.
 */
object IsoCountryCatalog {

  private val menaCountries = setOf(
    "AE", "BH", "DJ", "DZ", "EG", "EH", "IQ", "JO", "KM", "KW", "LB", "LY",
    "MA", "MR", "OM", "PS", "QA", "SA", "SD", "SO", "SY", "TN", "YE",
  )

  private val europeCountries = setOf(
    "AD", "AL", "AT", "AX", "BA", "BE", "BG", "BY", "CH", "CY", "CZ", "DE",
    "DK", "EE", "ES", "FI", "FO", "FR", "GB", "GG", "GI", "GR", "HR", "HU",
    "IE", "IM", "IS", "IT", "JE", "LI", "LT", "LU", "LV", "MC", "MD", "ME",
    "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ",
    "SK", "SM", "UA", "VA", "XK",
  )

  private val northAmericaCountries = setOf(
    "BM", "CA", "GL", "MX", "PM", "US",
  )

  private val southAmericaCountries = setOf(
    "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GS", "GY", "PE", "PY",
    "SR", "UY", "VE",
  )

  private val centralAsiaCaucasusCountries = setOf(
    "AM", "AZ", "GE", "KG", "KZ", "TJ", "TM", "UZ",
  )

  private val asiaPacificCountries = setOf(
    "AF", "BD", "BN", "BT", "CC", "CN", "CX", "HK", "ID", "IN", "IO", "JP",
    "KH", "KP", "KR", "LA", "LK", "MM", "MN", "MO", "MV", "MY", "NP", "PH",
    "PK", "SG", "TH", "TL", "TW", "VN", "IR", "TR", "IL",
  )

  private val caribbeanOceaniaCountries = setOf(
    "AG", "AI", "AS", "AU", "AW", "BB", "BL", "BQ", "BS", "BZ", "CK", "CR",
    "CU", "CW", "DM", "DO", "FJ", "FM", "GD", "GP", "GT", "GU", "HN", "HT",
    "JM", "KI", "KN", "KY", "LC", "MF", "MH", "MP", "MQ", "MS", "NC", "NF",
    "NI", "NR", "NU", "NZ", "PA", "PF", "PG", "PN", "PR", "PW", "SB", "SV",
    "SX", "TC", "TK", "TO", "TT", "TV", "UM", "VC", "VG", "VI", "VU", "WF",
    "WS",
  )

  private val arabicOverrides = mapOf(
    "PS" to "فلسطين",
    "AE" to "الإمارات العربية المتحدة",
    "SA" to "المملكة العربية السعودية",
    "EG" to "مصر",
    "JO" to "الأردن",
    "KW" to "الكويت",
    "QA" to "قطر",
    "BH" to "البحرين",
    "OM" to "عُمان",
    "MA" to "المغرب",
    "DZ" to "الجزائر",
    "TN" to "تونس",
    "IQ" to "العراق",
    "LB" to "لبنان",
    "US" to "الولايات المتحدة",
    "GB" to "المملكة المتحدة",
    "DE" to "ألمانيا",
    "FR" to "فرنسا",
    "NL" to "هولندا",
    "CH" to "سويسرا",
    "SE" to "السويد",
    "JP" to "اليابان",
    "SG" to "سنغافورة",
    "TR" to "تركيا",
    "CA" to "كندا",
    "AU" to "أستراليا",
    "XK" to "كوسوفو",
  )

  private val englishOverrides = mapOf(
    "PS" to "Palestine",
    "XK" to "Kosovo",
  )

  private val extraIsoCodes = setOf(
    "AQ", "AX", "BL", "BQ", "CW", "GG", "IM", "JE", "MF", "PS", "RS", "ME",
    "SS", "SX", "TL", "XK",
  )

  fun flagEmojiForIso(isoCode: String): String {
    val upper = isoCode.uppercase(Locale.US)
    if (upper.length != 2 || !upper.all { it in 'A'..'Z' }) return "🌐"
    val first = Character.codePointAt(upper, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(upper, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
  }

  fun regionForIso(isoCode: String): GeographicalRegion {
    val upper = isoCode.uppercase(Locale.US)
    return when {
      upper in menaCountries -> GeographicalRegion.MENA
      upper in europeCountries -> GeographicalRegion.EUROPE
      upper in northAmericaCountries -> GeographicalRegion.NORTH_AMERICA
      upper in southAmericaCountries -> GeographicalRegion.SOUTH_AMERICA
      upper in centralAsiaCaucasusCountries -> GeographicalRegion.CENTRAL_ASIA
      upper in asiaPacificCountries -> GeographicalRegion.ASIA_PACIFIC
      upper in caribbeanOceaniaCountries -> GeographicalRegion.CARIBBEAN_OCEANIA
      else -> GeographicalRegion.SUB_SAHARAN_AFRICA
    }
  }

  fun buildAllCountries(): List<CountryLocation> {
    val codes = (Locale.getISOCountries().toSet() + extraIsoCodes)
      .map { it.uppercase(Locale.US) }
      .filter { it.length == 2 && it.all { ch -> ch in 'A'..'Z' } }
      .sorted()

    val arLocale = Locale.forLanguageTag("ar")
    val enLocale = Locale.ENGLISH

    return codes.map { code ->
      val locale = Locale.Builder().setRegion(code).build()
      val nameEn = englishOverrides[code] ?: locale.getDisplayCountry(enLocale).ifBlank { code }
      val nameAr = arabicOverrides[code] ?: locale.getDisplayCountry(arLocale).ifBlank { nameEn }
      CountryLocation(
        isoCode = code,
        nameEn = nameEn,
        nameAr = nameAr,
        flagEmoji = flagEmojiForIso(code),
        region = regionForIso(code),
        cities = emptyList(),
        isFavorite = false,
        lastConnectedEpochMs = null,
      )
    }
  }
}
