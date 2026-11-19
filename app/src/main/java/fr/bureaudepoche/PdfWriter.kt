package fr.bureaudepoche

import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Petit générateur de PDF : chaque page contient une image JPEG intégrée telle quelle (/DCTDecode).
 * C'est ce qui permet de maîtriser précisément le poids du fichier final.
 */
object PdfWriter {
    fun write(images: List<Processor.Enc>): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = ArrayList<Int>()

        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun obj(n: Int, body: () -> Unit) {
            while (offsets.size < n) offsets.add(0)
            offsets[n - 1] = out.size()
            w("$n 0 obj\n")
            body()
            w("\nendobj\n")
        }

        w("%PDF-1.4\n%âãÏÓ\n")
        val n = images.size
        val kids = (0 until n).joinToString(" ") { "${3 + it * 3} 0 R" }
        obj(1) { w("<< /Type /Catalog /Pages 2 0 R >>") }
        obj(2) { w("<< /Type /Pages /Kids [$kids] /Count $n >>") }

        images.forEachIndexed { i, img ->
            val pageN = 3 + i * 3
            val imgN = pageN + 1
            val cN = pageN + 2
            val paysage = img.w > img.h
            val pw = if (paysage) 842f else 595f // A4 en points
            val ph = if (paysage) 595f else 842f
            val s = minOf(pw / img.w, ph / img.h)
            val dw = img.w * s
            val dh = img.h * s
            val x = (pw - dw) / 2f
            val y = (ph - dh) / 2f
            obj(pageN) {
                w("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${f(pw)} ${f(ph)}] /Resources << /XObject << /Im0 $imgN 0 R >> >> /Contents $cN 0 R >>")
            }
            obj(imgN) {
                w("<< /Type /XObject /Subtype /Image /Width ${img.w} /Height ${img.h} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${img.bytes.size} >>\nstream\n")
                out.write(img.bytes)
                w("\nendstream")
            }
            val contenu = "q ${f(dw)} 0 0 ${f(dh)} ${f(x)} ${f(y)} cm /Im0 Do Q"
            obj(cN) { w("<< /Length ${contenu.length} >>\nstream\n$contenu\nendstream") }
        }

        val xref = out.size()
        val total = offsets.size + 1
        w("xref\n0 $total\n0000000000 65535 f \n")
        offsets.forEach { w(String.format(Locale.US, "%010d 00000 n \n", it)) }
        w("trailer\n<< /Size $total /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }

    private fun f(v: Float) = String.format(Locale.US, "%.2f", v)
}
