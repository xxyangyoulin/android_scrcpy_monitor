package com.xxyangyoulin.scrcpymonitor

object EndpointFormatter {
    fun format(endpoint: String?): String? {
        if (endpoint == null) {
            return null
        }
        IPV4_MAPPED_ENDPOINT_REGEX.matchEntire(endpoint)?.let { match ->
            return match.groupValues[1]
        }
        BRACKETED_ENDPOINT_REGEX.matchEntire(endpoint)?.let { match ->
            return match.groupValues[1]
        }
        IPV4_ENDPOINT_REGEX.matchEntire(endpoint)?.let { match ->
            return match.groupValues[1]
        }
        return endpoint
    }

    private val IPV4_MAPPED_ENDPOINT_REGEX =
        Regex("""^\[::ffff:([0-9.]+)]:(\d+)$""")
    private val BRACKETED_ENDPOINT_REGEX =
        Regex("""^\[([0-9a-fA-F:]+)]:(\d+)$""")
    private val IPV4_ENDPOINT_REGEX =
        Regex("""^([0-9.]+):(\d+)$""")
}
