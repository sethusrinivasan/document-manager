package com.app.traveldocs.data.local

import com.app.traveldocs.data.local.entity.GpsTrackEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {
    fun export(tracks: List<GpsTrackEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<gpx version=\"1.1\" creator=\"TravelDocManager\"")
        sb.appendLine("  xmlns=\"http://www.topografix.com/GPX/1/1\">")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>Travel Track</name>")
        sb.appendLine("    <trkseg>")
        for (t in tracks) {
            val time = dateFormat.format(Date(t.timestamp))
            sb.appendLine("      <trkpt lat=\"${t.latitude}\" lon=\"${t.longitude}\">")
            sb.appendLine("        <time>$time</time>")
            sb.appendLine("        <hdop>${t.accuracy}</hdop>")
            sb.appendLine("      </trkpt>")
        }
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")
        return sb.toString()
    }
}
