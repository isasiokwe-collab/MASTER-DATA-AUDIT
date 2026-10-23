package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AuditFormType
import com.example.data.model.AuditRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportRecordsToExcel(
        context: Context,
        records: List<AuditRecord>,
        filenamePrefix: String = "Cooler_Audit_Export"
    ): File? {
        if (records.isEmpty()) {
            Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
            return null
        }

        val timestamp = fileTimestampFormatter.format(Date())
        val fileName = "${filenamePrefix}_$timestamp.xlsx"

        // Prefer Downloads directory, fallback to app external documents
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            downloadsDir
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        }

        val outputFile = File(targetDir, fileName)

        try {
            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val withCooler = records.filter { it.formType == AuditFormType.WITH_COOLER }
                    val withoutCooler = records.filter { it.formType == AuditFormType.WITHOUT_COOLER }

                    writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())
                    writeZipEntry(zos, "_rels/.rels", buildRelsXml())
                    writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
                    writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml())
                    writeZipEntry(zos, "xl/styles.xml", buildStylesXml())
                    writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildSheet1Xml(withCooler))
                    writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildSheet2Xml(withoutCooler))
                }
            }

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun shareExportedFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cooler Audit Records Export (${file.name})")
                putExtra(Intent.EXTRA_TEXT, "Here is the exported field cooler audit spreadsheet: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Excel Audit Report via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryPath: String, content: String) {
        val entry = ZipEntry(entryPath)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(Charsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(str: String?): String {
        if (str == null) return ""
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()

    private fun buildRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/package/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()

    private fun buildWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()

    private fun buildWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="With Cooler" sheetId="1" r:id="rId1"/>
    <sheet name="Without Cooler" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>""".trimIndent()

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><u/><sz val="11"/><color rgb="FF004E89"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0A3D62"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/></border>
  </borders>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>""".trimIndent()

    private fun buildSheet1Xml(records: List<AuditRecord>): String {
        val headers = listOf(
            "Record ID",
            "Audit Date",
            "Audit Time",
            "Account / Distributor",
            "Outlet Name",
            "Owner Name",
            "Owner Phone",
            "Location (Area/Town)",
            "GPS Latitude",
            "GPS Longitude",
            "GPS Accuracy (m)",
            "Cooler Make",
            "Cooler Model",
            "Cooler Serial Number",
            "Cooler Tag Number",
            "Google Maps Link"
        )

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="24" customWidth="1"/>
    <col min="5" max="5" width="24" customWidth="1"/>
    <col min="6" max="6" width="20" customWidth="1"/>
    <col min="7" max="7" width="18" customWidth="1"/>
    <col min="8" max="8" width="22" customWidth="1"/>
    <col min="9" max="9" width="16" customWidth="1"/>
    <col min="10" max="10" width="16" customWidth="1"/>
    <col min="11" max="11" width="18" customWidth="1"/>
    <col min="12" max="12" width="18" customWidth="1"/>
    <col min="13" max="13" width="18" customWidth="1"/>
    <col min="14" max="14" width="24" customWidth="1"/>
    <col min="15" max="15" width="20" customWidth="1"/>
    <col min="16" max="16" width="28" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header row
        sb.append("    <row r=\"1\">\n")
        headers.forEachIndexed { colIdx, header ->
            val colLetter = getColumnLetter(colIdx + 1)
            sb.append("      <c r=\"$colLetter 1\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(header)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        // Data rows
        records.forEachIndexed { rowIdx, record ->
            val r = rowIdx + 2
            val mapsUrl = "https://maps.google.com/?q=${record.latitude},${record.longitude}"
            val mapFormula = "HYPERLINK(\"$mapsUrl\",\"View on Google Maps\")"

            sb.append("    <row r=\"$r\">\n")
            sb.append("      <c r=\"A$r\"><v>${record.id}</v></c>\n")
            sb.append("      <c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(record.formattedDate)}</t></is></c>\n")
            sb.append("      <c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(record.formattedTime)}</t></is></c>\n")
            sb.append("      <c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(record.accountDistributor)}</t></is></c>\n")
            sb.append("      <c r=\"E$r\" t=\"inlineStr\"><is><t>${escapeXml(record.outletName)}</t></is></c>\n")
            sb.append("      <c r=\"F$r\" t=\"inlineStr\"><is><t>${escapeXml(record.ownerName)}</t></is></c>\n")
            sb.append("      <c r=\"G$r\" t=\"inlineStr\"><is><t>${escapeXml(record.ownerPhone)}</t></is></c>\n")
            sb.append("      <c r=\"H$r\" t=\"inlineStr\"><is><t>${escapeXml(record.location)}</t></is></c>\n")
            sb.append("      <c r=\"I$r\"><v>${record.latitude}</v></c>\n")
            sb.append("      <c r=\"J$r\"><v>${record.longitude}</v></c>\n")
            sb.append("      <c r=\"K$r\"><v>${record.gpsAccuracyMeters}</v></c>\n")
            sb.append("      <c r=\"L$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerMake)}</t></is></c>\n")
            sb.append("      <c r=\"M$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerModel)}</t></is></c>\n")
            sb.append("      <c r=\"N$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerSerialNumber)}</t></is></c>\n")
            sb.append("      <c r=\"O$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerTagNumber)}</t></is></c>\n")
            sb.append("      <c r=\"P$r\" s=\"2\"><f>${escapeXml(mapFormula)}</f><v>View on Google Maps</v></c>\n")
            sb.append("    </row>\n")
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet2Xml(records: List<AuditRecord>): String {
        val headers = listOf(
            "Record ID",
            "Audit Date",
            "Audit Time",
            "Trading Name",
            "Owner Name",
            "Owner Phone",
            "Location (Area/Town)",
            "GPS Latitude",
            "GPS Longitude",
            "GPS Accuracy (m)",
            "Cooler Model",
            "Cooler Serial Number",
            "Cooler Tag Number",
            "Google Maps Link"
        )

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="12" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="24" customWidth="1"/>
    <col min="5" max="5" width="20" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="22" customWidth="1"/>
    <col min="8" max="8" width="16" customWidth="1"/>
    <col min="9" max="9" width="16" customWidth="1"/>
    <col min="10" max="10" width="18" customWidth="1"/>
    <col min="11" max="11" width="18" customWidth="1"/>
    <col min="12" max="12" width="24" customWidth="1"/>
    <col min="13" max="13" width="20" customWidth="1"/>
    <col min="14" max="14" width="28" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header row
        sb.append("    <row r=\"1\">\n")
        headers.forEachIndexed { colIdx, header ->
            val colLetter = getColumnLetter(colIdx + 1)
            sb.append("      <c r=\"$colLetter 1\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(header)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        // Data rows
        records.forEachIndexed { rowIdx, record ->
            val r = rowIdx + 2
            val mapsUrl = "https://maps.google.com/?q=${record.latitude},${record.longitude}"
            val mapFormula = "HYPERLINK(\"$mapsUrl\",\"View on Google Maps\")"

            sb.append("    <row r=\"$r\">\n")
            sb.append("      <c r=\"A$r\"><v>${record.id}</v></c>\n")
            sb.append("      <c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(record.formattedDate)}</t></is></c>\n")
            sb.append("      <c r=\"C$r\" t=\"inlineStr\"><is><t>${escapeXml(record.formattedTime)}</t></is></c>\n")
            sb.append("      <c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(record.outletName)}</t></is></c>\n")
            sb.append("      <c r=\"E$r\" t=\"inlineStr\"><is><t>${escapeXml(record.ownerName)}</t></is></c>\n")
            sb.append("      <c r=\"F$r\" t=\"inlineStr\"><is><t>${escapeXml(record.ownerPhone)}</t></is></c>\n")
            sb.append("      <c r=\"G$r\" t=\"inlineStr\"><is><t>${escapeXml(record.location)}</t></is></c>\n")
            sb.append("      <c r=\"H$r\"><v>${record.latitude}</v></c>\n")
            sb.append("      <c r=\"I$r\"><v>${record.longitude}</v></c>\n")
            sb.append("      <c r=\"J$r\"><v>${record.gpsAccuracyMeters}</v></c>\n")
            sb.append("      <c r=\"K$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerModel)}</t></is></c>\n")
            sb.append("      <c r=\"L$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerSerialNumber)}</t></is></c>\n")
            sb.append("      <c r=\"M$r\" t=\"inlineStr\"><is><t>${escapeXml(record.coolerTagNumber)}</t></is></c>\n")
            sb.append("      <c r=\"N$r\" s=\"2\"><f>${escapeXml(mapFormula)}</f><v>View on Google Maps</v></c>\n")
            sb.append("    </row>\n")
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun getColumnLetter(colNum: Int): String {
        var temp = colNum
        var colName = ""
        while (temp > 0) {
            val mod = (temp - 1) % 26
            colName = (('A'.code + mod).toChar()) + colName
            temp = (temp - mod) / 26
        }
        return colName
    }
}
