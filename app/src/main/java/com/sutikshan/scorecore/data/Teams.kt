package com.sutikshan.scorecore.data

/** International (national) sides only, for now. */
data class Team(val name: String, val code: String, val fullMember: Boolean)

object Teams {
    val all: List<Team> = listOf(
        // ICC full members
        Team("Afghanistan", "AFG", true),
        Team("Australia", "AUS", true),
        Team("Bangladesh", "BAN", true),
        Team("England", "ENG", true),
        Team("India", "IND", true),
        Team("Ireland", "IRE", true),
        Team("New Zealand", "NZ", true),
        Team("Pakistan", "PAK", true),
        Team("South Africa", "SA", true),
        Team("Sri Lanka", "SL", true),
        Team("West Indies", "WI", true),
        Team("Zimbabwe", "ZIM", true),
        // Associate members that regularly play international T20Is / ODIs
        Team("Canada", "CAN", false),
        Team("Denmark", "DEN", false),
        Team("Germany", "GER", false),
        Team("Hong Kong", "HK", false),
        Team("Italy", "ITA", false),
        Team("Jersey", "JER", false),
        Team("Kenya", "KEN", false),
        Team("Malaysia", "MAS", false),
        Team("Namibia", "NAM", false),
        Team("Nepal", "NEP", false),
        Team("Netherlands", "NED", false),
        Team("Oman", "OMN", false),
        Team("Papua New Guinea", "PNG", false),
        Team("Scotland", "SCO", false),
        Team("Singapore", "SIN", false),
        Team("Uganda", "UGA", false),
        Team("UAE", "UAE", false),
        Team("USA", "USA", false),
    )

    val fullMembers: List<Team> = all.filter { it.fullMember }
    val associates: List<Team> = all.filter { !it.fullMember }

    fun code(name: String): String =
        all.firstOrNull { it.name == name }?.code ?: name.take(3).uppercase()
}

enum class MatchFormat(val label: String, val overs: Int) {
    T20I("T20I", 20),
    ODI("ODI", 50),
}
