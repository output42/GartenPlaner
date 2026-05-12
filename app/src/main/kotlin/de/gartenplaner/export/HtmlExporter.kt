package de.gartenplaner.export

import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.SectionWithPlants

object HtmlExporter {

    private val MONTH_HEADERS = listOf(
        "Jan" to "❄", "Feb" to "❄", "Mär" to "🌬", "Apr" to "🌧",
        "Mai" to "🌱", "Jun" to "☀",  "Jul" to "☀",  "Aug" to "☀",
        "Sep" to "🍂", "Okt" to "🍂", "Nov" to "❄",  "Dez" to "❄",
    )

    fun buildHtml(
        plan    : Plan,
        sections: List<SectionWithPlants>,
        entries : Map<Int, List<MonthEntry>>,   // plantId → entries
    ): String {
        val sb = StringBuilder()
        sb.appendHeader(plan)
        sb.appendTableOpen()
        for (swp in sections) {
            sb.appendSectionRow(swp.section.title)
            for (plant in swp.plants.sortedBy { it.order }) {
                val monthSlots = (0..11).map { m ->
                    entries[plant.id]?.firstOrNull { it.month == m }
                }
                sb.appendPlantRow(plant.name, plant.subtitle, monthSlots)
            }
        }
        sb.appendTableClose()
        sb.appendFooter(plan)
        return sb.toString()
    }

    // ── HTML skeleton ────────────────────────────────────────────────────────

    private fun StringBuilder.appendHeader(plan: Plan) {
        append("""
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<style>
@page { size: 297mm 210mm; margin: 0; }
* { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important;
    color-adjust: exact !important; box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: Arial, Helvetica, sans-serif; background: #fff; }
.page { width: 297mm; padding: 7mm 6mm 4mm 6mm; }
.hdr { display: flex; justify-content: space-between; align-items: flex-end;
       border-bottom: 2.5px solid #2d5016; padding-bottom: 4px; margin-bottom: 5px; }
.hdr h1 { font-size: 14pt; color: #2d5016; font-weight: 800; }
.hdr p  { font-size: 6.5pt; color: #555; margin-top: 2px; }
.hdr-r  { font-size: 6pt; color: #666; text-align: right; line-height: 1.6; }
.legend { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 5px; }
.legend b { font-size: 5.5pt; color: #333; margin-right: 2px; }
.legend span { font-size: 5.5pt; color: #333; display: flex; align-items: center; gap: 3px; }
.sq { display: inline-block; width: 8px; height: 8px; border-radius: 2px; }
.sq-v  { background-color: #f0a500; }
.sq-d  { background-color: #3a8c3f; }
.sq-p  { background-color: #2196f3; }
.sq-e  { background-color: #c0392b; }
.sq-pg { background-color: #8e44ad; }
table { width: 100%; border-collapse: collapse; table-layout: fixed; }
col.lc { width: 9.5%; }
col.mc { width: 7%; }
td, th { border: 0.6px solid #bbb; font-size: 5.5pt; line-height: 1.2;
         padding: 1px 2px; vertical-align: middle; text-align: center; height: 12mm; }
th.mh  { background-color: #2d5016; color: #fff; font-weight: 700; font-size: 6pt;
         padding: 1px 2px; height: 6mm; }
th.mh small { display: block; font-weight: 400; font-size: 5pt; opacity: 0.85; }
td.lbl { background-color: #f0f4ec; font-weight: 600; font-size: 5.5pt; text-align: left;
         padding: 1px 3px; border-right: 2px solid #a0c090; line-height: 1.3; }
td.lbl em { display: block; font-style: italic; font-weight: 400; color: #777; font-size: 4.5pt; }
.sec td { background-color: #deeece; color: #1a4010; font-weight: 700; font-size: 5.5pt;
          text-align: left; padding: 1px 4px; text-transform: uppercase;
          letter-spacing: 0.4px; border-top: 1.5px solid #8fb87a; height: 3.5mm; }
.av  { background-color: #fff0c0; color: #7a4f00; }
.ad  { background-color: #c8ecd0; color: #145220; }
.ap  { background-color: #c5e0ff; color: #00387a; }
.ae  { background-color: #fad0d3; color: #6e1219; }
.apg { background-color: #e8d0f8; color: #3d1060; }
.footer { margin-top: 5px; border-top: 1.5px solid #ccc; padding-top: 4px; display: flex; gap: 8px; }
.fb { flex: 1; }
.fb h3 { font-size: 6pt; color: #2d5016; font-weight: 800; text-transform: uppercase;
         letter-spacing: 0.4px; margin-bottom: 1px; }
.fb p  { font-size: 5pt; color: #333; line-height: 1.4; }
</style>
</head>
<body><div class="page">
<div class="hdr">
  <div>
    <h1>&#x1F331; Gartenplaner ${plan.year} &mdash; ${escHtml(plan.title)}</h1>
    <p>Aussaat &middot; Pflege &middot; Ernte</p>
  </div>
  <div class="hdr-r">
    Letzter Frost: ${escHtml(plan.frostInfoLast.ifBlank { "—" })}<br>
    Erster Frost: ${escHtml(plan.frostInfoFirst.ifBlank { "—" })}<br>
    Klimazone: ${escHtml(plan.climateZone.ifBlank { "—" })}
  </div>
</div>
<div class="legend">
  <b>Legende:</b>
  <span><span class="sq sq-v"></span> Voranzucht (innen)</span>
  <span><span class="sq sq-d"></span> Direktsaat (au&szlig;en)</span>
  <span><span class="sq sq-p"></span> Auspflanzen / Setzen</span>
  <span><span class="sq sq-e"></span> Ernte</span>
  <span><span class="sq sq-pg"></span> Pflege / Schnitt</span>
</div>
""".trimIndent())
    }

    private fun StringBuilder.appendTableOpen() {
        append("<table>\n<colgroup>\n<col class=\"lc\">\n")
        repeat(12) { append("<col class=\"mc\">") }
        append("\n</colgroup>\n<thead>\n<tr>\n<th class=\"mh\">Pflanze</th>\n")
        for ((name, emoji) in MONTH_HEADERS) {
            append("<th class=\"mh\">${escHtml(name)}<small>${emoji}</small></th>\n")
        }
        append("</tr>\n</thead>\n<tbody>\n")
    }

    private fun StringBuilder.appendSectionRow(title: String) {
        append("<tr class=\"sec\"><td colspan=\"13\">${escHtml(title)}</td></tr>\n")
    }

    private fun StringBuilder.appendPlantRow(
        name    : String,
        subtitle: String,
        slots   : List<MonthEntry?>,
    ) {
        append("<tr>\n<td class=\"lbl\">${escHtml(name)}")
        if (subtitle.isNotBlank()) append("<em>${escHtml(subtitle)}</em>")
        append("</td>\n")
        for (entry in slots) {
            if (entry == null) {
                append("<td></td>\n")
            } else {
                val css   = entry.type.cssClass
                val label = escHtml(entry.label.ifBlank { entry.type.defaultLabel })
                append("<td class=\"${escHtml(css)}\">${label}</td>\n")
            }
        }
        append("</tr>\n")
    }

    private fun StringBuilder.appendTableClose() {
        append("</tbody>\n</table>\n")
    }

    private fun StringBuilder.appendFooter(plan: Plan) {
        append("""
<div class="footer">
  <div class="fb">
    <h3>Frostkalender</h3>
    <p>Letzter Frost: ${escHtml(plan.frostInfoLast.ifBlank { "—" })}<br>
       Erster Frost: ${escHtml(plan.frostInfoFirst.ifBlank { "—" })}<br>
       Klimazone: ${escHtml(plan.climateZone.ifBlank { "—" })}</p>
  </div>
  <div class="fb">
    <h3>Hinweise</h3>
    <p>Alle Zeitangaben sind Richtwerte. Lokale Wetterbedingungen beachten.</p>
  </div>
</div>
</div></body></html>
""".trimIndent())
    }

    private fun escHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
