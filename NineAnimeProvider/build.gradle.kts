// use an integer for version numbers
version = 21


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Watch 9anime with the help of the Consumet API. Use 9anime 2 to fix the issue of loading 9anime if you have it, if it doesn't report an issue on the github or DM blonde_one on discord."
    authors = listOf("Stormunblessed, commandertolerance")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/Nain.png"
}
