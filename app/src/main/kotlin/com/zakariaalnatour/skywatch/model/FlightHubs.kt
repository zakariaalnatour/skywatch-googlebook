package com.zakariaalnatour.skywatch.model

object FlightHubs {
  val DXB = FlightHub(
    id = "DXB",
    name = "Dubai Intl",
    icaoCode = "OMDB",
    country = "UAE",
    latitude = 25.2532,
    longitude = 55.3657,
    defaultZoom = 7.2f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 1,
  )

  val DOH = FlightHub(
    id = "DOH",
    name = "Doha Hamad",
    icaoCode = "OTHH",
    country = "Qatar",
    latitude = 25.2731,
    longitude = 51.6081,
    defaultZoom = 7.4f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 2,
  )

  val RUH = FlightHub(
    id = "RUH",
    name = "Riyadh KKIA",
    icaoCode = "OERK",
    country = "Saudi Arabia",
    latitude = 24.9576,
    longitude = 46.6988,
    defaultZoom = 7.0f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 3,
  )

  val JED = FlightHub(
    id = "JED",
    name = "Jeddah KAIA",
    icaoCode = "OEJN",
    country = "Saudi Arabia",
    latitude = 21.6796,
    longitude = 39.1565,
    defaultZoom = 7.0f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 4,
  )

  val AMM = FlightHub(
    id = "AMM",
    name = "Amman QAIA",
    icaoCode = "OJAI",
    country = "Jordan",
    latitude = 31.7226,
    longitude = 35.9932,
    defaultZoom = 7.2f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 5,
  )

  val CAI = FlightHub(
    id = "CAI",
    name = "Cairo Intl",
    icaoCode = "HECA",
    country = "Egypt",
    latitude = 30.1219,
    longitude = 31.4056,
    defaultZoom = 7.2f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 6,
  )

  val IST = FlightHub(
    id = "IST",
    name = "Istanbul Intl",
    icaoCode = "LTFM",
    country = "Turkey",
    latitude = 41.2753,
    longitude = 28.7519,
    defaultZoom = 7.2f,
    radiusNm = 250,
    group = HubGroup.MIDDLE_EAST,
    shortcutDigit = 7,
  )

  val SFO = FlightHub(
    id = "SFO",
    name = "San Francisco",
    icaoCode = "KSFO",
    country = "USA",
    latitude = 37.6213,
    longitude = -122.3790,
    defaultZoom = 7.4f,
    radiusNm = 250,
    group = HubGroup.GLOBAL,
  )

  val JFK = FlightHub(
    id = "JFK",
    name = "New York JFK",
    icaoCode = "KJFK",
    country = "USA",
    latitude = 40.6413,
    longitude = -73.7781,
    defaultZoom = 7.4f,
    radiusNm = 250,
    group = HubGroup.GLOBAL,
  )

  val LAX = FlightHub(
    id = "LAX",
    name = "Los Angeles",
    icaoCode = "KLAX",
    country = "USA",
    latitude = 33.9416,
    longitude = -118.4085,
    defaultZoom = 7.4f,
    radiusNm = 250,
    group = HubGroup.GLOBAL,
  )

  val LHR = FlightHub(
    id = "LHR",
    name = "London Heathrow",
    icaoCode = "EGLL",
    country = "UK",
    latitude = 51.4700,
    longitude = -0.4543,
    defaultZoom = 7.3f,
    radiusNm = 250,
    group = HubGroup.GLOBAL,
  )

  val HND = FlightHub(
    id = "HND",
    name = "Tokyo Haneda",
    icaoCode = "RJTT",
    country = "Japan",
    latitude = 35.5494,
    longitude = 139.7798,
    defaultZoom = 7.2f,
    radiusNm = 250,
    group = HubGroup.GLOBAL,
  )

  val WIDEBODY = FlightHub(
    id = "WIDEBODY",
    name = "Widebody",
    icaoCode = "HEAVY",
    country = "Global",
    latitude = 28.0,
    longitude = 42.0,
    defaultZoom = 3.2f,
    radiusNm = 250,
    group = HubGroup.SPECIAL,
    feedMode = HubFeedMode.WIDEBODY,
    shortcutDigit = 8,
  )

  val MIL = FlightHub(
    id = "MIL",
    name = "Military",
    icaoCode = "MIL",
    country = "Global",
    latitude = 34.0,
    longitude = 25.0,
    defaultZoom = 3.2f,
    radiusNm = 250,
    group = HubGroup.SPECIAL,
    feedMode = HubFeedMode.MILITARY,
    shortcutDigit = 9,
  )

  val ALL: List<FlightHub> = listOf(
    DXB, DOH, RUH, JED, AMM, CAI, IST,
    SFO, JFK, LAX, LHR, HND,
    WIDEBODY, MIL,
  )

  val MIDDLE_EAST_HUBS: List<FlightHub> = ALL.filter { it.group == HubGroup.MIDDLE_EAST }
  val GLOBAL_HUBS: List<FlightHub> = ALL.filter { it.group == HubGroup.GLOBAL }
  val SPECIAL_FEEDS: List<FlightHub> = ALL.filter { it.group == HubGroup.SPECIAL }

  val DEFAULT: FlightHub = DXB

  /** Primary global widebody ICAO type designators queried in Widebody mode. */
  val WIDEBODY_QUERY_TYPES: List<String> = listOf("A388", "B77W", "B789", "A359", "B748")

  val WIDEBODY_ICAO_TYPES: Set<String> = setOf(
    "A306", "A30B", "A310", "A332", "A333", "A337", "A338", "A339",
    "A342", "A343", "A345", "A346", "A359", "A35K", "A388",
    "B742", "B743", "B744", "B748", "B74S", "BLCF",
    "B762", "B763", "B764",
    "B772", "B773", "B77L", "B77W", "B778", "B779",
    "B788", "B789", "B78X",
    "IL96", "MD11", "DC10", "A124", "A225", "C5M",
  )

  val MILITARY_ICAO_TYPES: Set<String> = setOf(
    "C17", "C130", "C30J", "C5M", "K35R", "KC10", "KC46", "A400", "A330M",
    "P8", "E3TF", "E3CF", "E6", "RC135", "R135", "U2", "RQ4", "Q4",
    "F15", "F16", "F18", "F18S", "F22", "F35", "EUFI", "RFAL", "T38", "T6",
    "V22", "H60", "H47", "C27J", "C295", "B52", "B1", "B2", "TEX2",
  )

  val ROTORCRAFT_ICAO_TYPES: Set<String> = setOf(
    "H60", "H47", "V22", "EC35", "EC45", "EC55", "EC75", "AS32", "AS50",
    "A109", "A139", "A169", "A189", "B06", "B212", "B407", "B412", "B429",
    "R22", "R44", "R66", "S76", "S92", "S70", "NH90", "UH1",
  )

  fun findById(id: String?): FlightHub? =
    ALL.firstOrNull { it.id.equals(id, ignoreCase = true) }

  fun findByShortcutDigit(digit: Int): FlightHub? =
    ALL.firstOrNull { it.shortcutDigit == digit }

  fun isWidebodyType(icaoType: String?): Boolean =
    !icaoType.isNullOrBlank() && WIDEBODY_ICAO_TYPES.contains(icaoType.trim().uppercase())

  fun isMilitaryType(icaoType: String?): Boolean =
    !icaoType.isNullOrBlank() && MILITARY_ICAO_TYPES.contains(icaoType.trim().uppercase())

  fun isRotorcraftType(icaoType: String?): Boolean =
    !icaoType.isNullOrBlank() && ROTORCRAFT_ICAO_TYPES.contains(icaoType.trim().uppercase())
}
