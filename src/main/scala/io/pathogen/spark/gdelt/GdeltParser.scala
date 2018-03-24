package io.pathogen.spark.gdelt

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat

import scala.util.Try

object GdeltParser {

  private val DELIMITER = "\t"

  def parseEvent(str: String): Event = {

    val tokens = str.split(DELIMITER)

    val actor1Code: Actor = Try {
      Actor(
        cameoRaw = tokens(5),
        cameoName = tokens(6),
        cameoCountryCode = tokens(7),
        cameoGroupCode = tokens(8),
        cameoEthnicCode = tokens(9),
        cameoReligion1Code = tokens(10),
        cameoReligion2Code = tokens(11),
        cameoType1Code = tokens(12),
        cameoType2Code = tokens(13),
        cameoType3Code = tokens(14)
      )
    }.getOrElse(Actor())

    val actor2Code: Actor = Try {
      Actor(
        cameoRaw = tokens(15),
        cameoName = tokens(16),
        cameoCountryCode = tokens(17),
        cameoGroupCode = tokens(18),
        cameoEthnicCode = tokens(19),
        cameoReligion1Code = tokens(20),
        cameoReligion2Code = tokens(21),
        cameoType1Code = tokens(22),
        cameoType2Code = tokens(23),
        cameoType3Code = tokens(24)
      )
    }.getOrElse(Actor())

    val actor1GeoPoint: GeoPoint = Try(GeoPoint(tokens(40).toFloat, tokens(41).toFloat)).getOrElse(GeoPoint())
    val actor2GeoPoint: GeoPoint = Try(GeoPoint(tokens(48).toFloat, tokens(49).toFloat)).getOrElse(GeoPoint())
    val eventGeoPoint: GeoPoint = Try(GeoPoint(tokens(56).toFloat, tokens(57).toFloat)).getOrElse(GeoPoint())

    val actor1Geo: Location = Try {
      Location(
        geoType = geoType(tokens(35).toInt),
        geoName = tokens(36),
        countryCode = tokens(37),
        adm1Code = tokens(38),
        adm2Code = tokens(39),
        geoPoint = actor1GeoPoint,
        featureId = tokens(40)
      )
    }.getOrElse(Location())

    val actor2Geo: Location = Try {
      Location(
        geoType = geoType(tokens(43).toInt),
        geoName = tokens(44),
        countryCode = tokens(45),
        adm1Code = tokens(46),
        adm2Code = tokens(47),
        geoPoint = actor2GeoPoint,
        featureId = tokens(50)
      )
    }.getOrElse(Location())

    val eventGeo: Location = Try {
      Location(
        geoType = geoType(tokens(51).toInt),
        geoName = tokens(52),
        countryCode = tokens(53),
        adm1Code = tokens(54),
        adm2Code = tokens(55),
        geoPoint = eventGeoPoint,
        featureId = tokens(58)
      )
    }.getOrElse(Location())

    Try {
      Event(
        eventId = tokens(0).toInt,
        eventDay = new Date(new SimpleDateFormat("yyyyMMdd").parse(tokens(1)).getTime),
        actor1Code = actor1Code,
        actor2Code = actor2Code,
        isRoot = tokens(25) == "1",
        cameoEventCode = tokens(26),
        cameoEventBaseCode = tokens(27),
        cameoEventRootCode = tokens(28),
        quadClass = quadClass(tokens(29).toInt),
        goldstein = tokens(30).toFloat,
        numMentions = tokens(31).toInt,
        numSources = tokens(32).toInt,
        numArticles = tokens(33).toInt,
        avgTone = tokens(34).toFloat,
        actor1Geo = actor1Geo,
        actor2Geo = actor2Geo,
        eventGeo = eventGeo,
        dateAdded = new Date(new SimpleDateFormat("yyyyMMddHHmmss").parse(tokens(59)).getTime),
        sourceUrl = tokens(60)
      )
    }.getOrElse(Event())

  }

  def parseMention(str: String): Mention = {

    val tokens = str.split(DELIMITER)

    Try {
      Mention(
        eventId = tokens(0).toLong,
        eventTime = new Timestamp(new SimpleDateFormat("yyyyMMddHHmmss").parse(tokens(1)).getTime),
        mentionTime = new Timestamp(new SimpleDateFormat("yyyyMMddHHmmss").parse(tokens(2)).getTime),
        mentionType = sourceCollectionIdentifier(tokens(3).toInt),
        mentionSourceName = tokens(4),
        mentionIdentifier = tokens(5),
        sentenceId = tokens(6).toInt,
        actor1CharOffset = tokens(7).toInt,
        actor2CharOffset = tokens(8).toInt,
        actionCharOffset = tokens(9).toInt,
        inRawText = tokens(10).toInt,
        confidence = tokens(11).toInt,
        mentionDocLen = tokens(12).toInt,
        mentionDocTone = tokens(13).toFloat
      )
    }.getOrElse(Mention())

  }

  def parseGkg(str: String): GKGEvent = {

    val values = str.split(DELIMITER, -1)

    Try {
      GKGEvent(
        gkgRecordId = buildGkgRecordId(values(0)),
        publishDate = buildPublishDate(values(1)),
        sourceCollectionIdentifier = buildSourceCollectionIdentifier(values(2)),
        sourceCommonName = values(3),
        documentIdentifier = values(4),
        counts = buildCounts(values(5)),
        enhancedCounts = buildEnhancedCounts(values(6)),
        themes = buildThemes(values(7)),
        enhancedThemes = buildEnhancedThemes(values(8)),
        locations = buildLocations(values(9)),
        enhancedLocations = buildEnhancedLocations(values(10)),
        persons = buildPersons(values(11)),
        enhancedPersons = buildEnhancedPersons(values(12)),
        organisations = buildOrganisations(values(13)),
        enhancedOrganisations = buildEnhancedOrganisations(values(14)),
        tone = buildTone(values(15)),
        enhancedDates = buildEnhancedDates(values(16)),
        gcams = buildGcams(values(17)),
        sharingImage = values(18),
        relatedImages = buildRelatedImages(values(19)),
        socialImageEmbeds = buildSocialImageEmbeds(values(20)),
        socialVideoEmbeds = buildSocialVideoEmbeds(values(21)),
        quotations = buildQuotations(values(22)),
        allNames = buildNames(values(23)),
        amounts = buildAmounts(values(24)),
        translationInfo = buildTranslationInfo(values(25)),
        extrasXML = values(26)
      )
    }.getOrElse(GKGEvent())

  }

  private def buildPublishDate(str: String) = {
    Try(new Timestamp(new SimpleDateFormat("yyyyMMddHHmmSS").parse(str).getTime)).getOrElse(new Timestamp(0L))
  }

  private def buildGkgRecordId(str: String) = {
    val split = str.split("-")
    Try {
      val isTranslingual = split(1).length > 1
      val numberInBatch = if (isTranslingual) split(1).substring(1).toInt else split(1).toInt
      GkgRecordId(publishDate = new Timestamp(new SimpleDateFormat("yyyyMMddHHmmSS").parse(split(0)).getTime), translingual = isTranslingual, numberInBatch = numberInBatch)
    } getOrElse GkgRecordId()
  }

  private def buildTranslationInfo(str: String) = {
    val values = str.split(";")
    Try(TranslationInfo(SRCLC = values(0), ENG = values(1))).getOrElse(TranslationInfo())
  }

  private def buildAmounts(str: String) = {
    str.split(";").map(buildAmount).filter(_.isDefined).map(_.get).toList
  }

  private def buildAmount(str: String) = {
    val values = str.split(",")
    Try(Amount(amount = values(0).toDouble, amountType = values(1), charOffset = values(2).toInt)).toOption
  }

  private def buildNames(str: String) = {
    str.split(";").map(buildName).filter(_.isDefined).map(_.get).toList
  }

  private def buildName(str: String) = {
    val values = str.split(",")
    Try(Name(name = values(0), charOffset = values(1).toInt)).toOption
  }

  private def buildQuotations(str: String) = {
    str.split("#").map(buildQuotation).filter(_.isDefined).map(_.get).toList
  }

  private def buildQuotation(str: String) = {
    val values = str.split("\\|")
    Try(Quotation(charOffset = values(0).toInt, charLength = values(1).toInt, verb = values(2), quote = values(3))).toOption
  }

  private def buildSocialImageEmbeds(str: String) = str.split(";").toList

  private def buildSocialVideoEmbeds(str: String) = str.split(";").toList

  private def buildRelatedImages(str: String) = str.split(";").toList

  private def buildGcams(str: String) = {
    str.split(",").map(buildGcam).filter(_.isDefined).map(_.get).toList
  }

  private def buildGcam(str: String) = {
    val split = str.split(":")
    Try(Gcam(gcamCode = split(0), gcamValue = split(1).toDouble)).toOption
  }

  private def buildEnhancedDates(str: String) = {
    str.split(";").map(buildEnhancedDate).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedDate(str: String) = {
    val values = str.split("#")
    Try(EnhancedDate(dateResolution = values(0).toInt, month = values(1).toInt, day = values(2).toInt, year = values(3).toInt, charOffset = values(4).toInt)).toOption
  }

  private def buildTone(str: String) = {
    val values = str.split(",")
    Try(Tone(tone = values(0).toFloat, positiveScore = values(1).toFloat, negativeScore = values(2).toFloat, polarity = values(3).toFloat, activityReferenceDensity = values(4).toFloat, selfGroupReferenceDensity = values(5).toFloat, wordCount = values(6).toInt)).getOrElse(Tone())
  }

  private def buildEnhancedOrganisations(str: String) = {
    str.split(";").map(buildEnhancedOrganisation).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedOrganisation(str: String) = {
    val blocks = str.split(",")
    Try(EnhancedOrganisation(organisation = blocks(0), charOffset = blocks(1).toInt)).toOption
  }

  private def buildOrganisations(str: String) = str.split(";").toList

  private def buildEnhancedPersons(str: String) = {
    str.split(";").map(buildEnhancedPerson).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedPerson(str: String) = {
    val blocks = str.split(",")
    Try(EnhancedPerson(person = blocks(0), charOffset = blocks(1).toInt)).toOption
  }

  private def buildPersons(str: String) = str.split(";").toList

  private def buildSourceCollectionIdentifier(str: String) = sourceCollectionIdentifier(str.toInt)

  private def buildEnhancedLocations(str: String) = {
    str.split(";").map(buildEnhancedLocation).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedLocation(str: String) = {
    val blocks = str.split("#")
    Try {
      val geoPoint = GeoPoint(latitude = blocks(5).toFloat, longitude = blocks(6).toFloat)
      val location = Location(geoType = geoType(blocks(0).toInt), geoName = blocks(1), countryCode = blocks(2), adm1Code = blocks(3), adm2Code = blocks(4), geoPoint = geoPoint, featureId = blocks(7))
      EnhancedLocation(location = location, charOffset = blocks(8).toInt)
    }.toOption
  }

  private def buildLocations(str: String) = {
    str.split(";").map(buildLocation).filter(_.isDefined).map(_.get).toList
  }

  private def buildLocation(str: String) = {
    val blocks = str.split("#")
    Try {
      val geoPoint = GeoPoint(latitude = blocks(4).toFloat, longitude = blocks(5).toFloat)
      Location(geoType = geoType(blocks(0).toInt), geoName = blocks(1), countryCode = blocks(2), adm1Code = blocks(3), geoPoint = geoPoint, featureId = blocks(6))
    }.toOption
  }

  private def buildEnhancedThemes(str: String) = {
    str.split(";").map(buildEnhancedTheme).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedTheme(str: String) = {
    val blocks = str.split(",")
    Try(EnhancedTheme(theme = blocks(0), charOffset = blocks(1).toInt)).toOption
  }

  private def buildThemes(str: String) = str.split(";").toList

  private def buildEnhancedCounts(str: String) = {
    str.split(";").map(buildEnhancedCount).filter(_.isDefined).map(_.get).toList
  }

  private def buildEnhancedCount(str: String) = {
    Try {
      val count = buildCount(str).get
      EnhancedCount(count = count, charOffset = str.substring(str.lastIndexOf('#') + 1).toInt)
    }.toOption
  }

  private def buildCount(str: String) = {
    val blocks = str.split("#")
    Try {
      val geoPoint = GeoPoint(latitude = blocks(7).toFloat, longitude = blocks(8).toFloat)
      val location = Location(geoType = geoType(blocks(3).toInt), geoName = blocks(4), countryCode = blocks(5), adm1Code = blocks(6), geoPoint = geoPoint, featureId = blocks(9))
      Count(countType = blocks(0), count = blocks(1).toLong, objectType = blocks(2), location = location)
    }.toOption
  }

  private def buildCounts(str: String) = {
    str.split(";").map(buildCount).filter(_.isDefined).map(_.get).toList
  }

  private def quadClass(quadClass: Int): String = quadClass match {
    case 1 => "VERBAL_COOPERATION"
    case 2 => "MATERIAL_COOPERATION"
    case 3 => "VERBAL_CONFLICT"
    case 4 => "MATERIAL_CONFLICT"
    case _ => "UNKNOWN"
  }

  private def sourceCollectionIdentifier(sourceCollectionIdentifier: Int): String = sourceCollectionIdentifier match {
    case 1 => "WEB"
    case 2 => "CITATION_ONLY"
    case 3 => "CORE"
    case 4 => "DTIC"
    case 5 => "JSTOR"
    case 6 => "NON_TEXTUAL_SOURCE"
    case _ => "UNKNOWN"
  }

  private def geoType(geoType: Int): String = geoType match {
    case 1 => "COUNTRY"
    case 2 => "USSTATE"
    case 3 => "USCITY"
    case 4 => "WORLDCITY"
    case 5 => "WORLDSTATE"
    case _ => "UNKNOWN"
  }

}
