package com.hangry.foodStandards

data class FSAData(
    val xml: Xml,
    val FHRSEstablishment: FHRSEstablishment
)

data class Xml(
    val version: String
)

data class FHRSEstablishment(
    val EstablishmentCollection: EstablishmentCollection,
    val Header: Header
)

data class EstablishmentCollection(
    val xmlns_xsd: String,
    val xmlns_xsi: String,
    val EstablishmentDetail: List<EstablishmentDetail>
)

data class Header(
    val text: String,
    val ExtractDate: String,
    val ItemCount: String,
    val PageCount: String,
    val PageNumber: String,
    val PageSize: String,
    val ReturnCode: String
)

data class EstablishmentDetail(
    val AddressLine1: String,
    val AddressLine2: String,
    val AddressLine3: Any,
    val AddressLine4: String,
    val BusinessName: String,
    val BusinessType: String,
    val BusinessTypeID: String,
    val Distance: String,
    val FHRSID: String,
    val Geocode: Geocode,
    val LocalAuthorityBusinessID: String,
    val LocalAuthorityCode: String,
    val LocalAuthorityEmailAddress: String,
    val LocalAuthorityName: String,
    val LocalAuthorityWebSite: String,
    val NewRatingPending: String,
    val PostCode: String,
    val RatingDate: String,
    val RatingKey: String,
    val RatingValue: String,
    val RightToReply: Any,
    val SchemeType: String,
    val Scores: Scores
)

data class Geocode(
    val Latitude: String,
    val Longitude: String
)

data class Scores(
    val ConfidenceInManagement: String,
    val Hygiene: String,
    val Structural: String
)