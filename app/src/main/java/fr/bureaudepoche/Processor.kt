package fr.bureaudepoche

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class Output(val bytes: ByteArray, val ext: String, val mime: String)

class Resultat(val sorties: List<Output>, val objectifAtteint: Boolean)

/** Tout le traitement se fait sur le téléphone, sans connexion. */
object Processor {
    private const val COTE_PREPA = 2600

    class Enc(val bytes: ByteArray, val w: Int, val h: Int)

    fun estPdf(ctx: Context, uri: Uri): Boolean {
        val t = runCatching { ctx.contentResolver.getType(uri) }.getOrNull()
        if (t == "application/pdf") return true
        if (t != null && t.startsWith("image/")) return false
        return (uri.lastPathSegment ?: "").endsWith(".pdf", ignoreCase = true)
    }

    fun taille(ctx: Context, uri: Uri): Long {
        if (uri.scheme == "file") return File(uri.path ?: "").length()
        return runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    /** Transforme chaque entrée (photo, scan, page de PDF) en une image de travail propre. */
    fun preparer(ctx: Context, uris: List<Uri>, dir: File): List<File> {
        dir.deleteRecursively()
        dir.mkdirs()
        val pages = ArrayList<File>()
        for (u in uris) {
            if (estPdf(ctx, u)) lirePdf(ctx, u, dir, pages)
            else pages += ecrirePrepa(lireImage(ctx, u), dir, pages.size)
        }
        if (pages.isEmpty()) throw IllegalStateException("Aucune page trouvée dans ce fichier.")
        return pages
    }

    private fun lireImage(ctx: Context, u: Uri): Bitmap {
        val src = ImageDecoder.createSource(ctx.contentResolver, u)
        val brut = ImageDecoder.decodeBitmap(src) { dec, info, _ ->
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val s = minOf(1f, COTE_PREPA.toFloat() / maxOf(w, h))
            if (s < 1f) dec.setTargetSize((w * s).toInt().coerceAtLeast(1), (h * s).toInt().coerceAtLeast(1))
        }
        // fond blanc : une image transparente deviendrait noire en JPEG
        val r = Bitmap.createBitmap(brut.width, brut.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(r)
        c.drawColor(Color.WHITE)
        c.drawBitmap(brut, 0f, 0f, null)
        brut.recycle()
        return r
    }

    private fun lirePdf(ctx: Context, u: Uri, dir: File, pages: MutableList<File>) {
        val pfd = ctx.contentResolver.openFileDescriptor(u, "r")
            ?: throw IllegalStateException("Impossible d'ouvrir ce PDF.")
        val renderer = try {
            PdfRenderer(pfd)
        } catch (e: SecurityException) {
            pfd.close()
            throw IllegalStateException("Ce PDF est protégé par un mot de passe.")
        } catch (e: Exception) {
            pfd.close()
            throw IllegalStateException("Ce PDF est illisible ou abîmé.")
        }
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                try {
                    val echelle = minOf(2.5f, COTE_PREPA.toFloat() / maxOf(page.width, page.height))
                    val w = (page.width * echelle).toInt().coerceAtLeast(1)
                    val h = (page.height * echelle).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages += ecrirePrepa(bmp, dir, pages.size)
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
            pfd.close()
        }
    }

    private fun ecrirePrepa(bmp: Bitmap, dir: File, index: Int): File {
        val f = File(dir, "page_$index.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bmp.recycle()
        return f
    }

    fun encoder(f: File, coteMax: Int, qualite: Int, gris: Boolean, png: Boolean = false): Enc {
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bornes)
        var sample = 1
        while (maxOf(bornes.outWidth, bornes.outHeight) / (sample * 2) >= coteMax) sample *= 2
        val dec = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("Image illisible.")
        val s = minOf(1f, coteMax.toFloat() / maxOf(dec.width, dec.height))
        var bmp = dec
        if (s < 1f) {
            bmp = Bitmap.createScaledBitmap(dec, (dec.width * s).toInt().coerceAtLeast(1), (dec.height * s).toInt().coerceAtLeast(1), true)
            if (bmp !== dec) dec.recycle()
        }
        if (gris) {
            val g = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
            Canvas(g).drawBitmap(bmp, 0f, 0f, paint)
            bmp.recycle()
            bmp = g
        }
        val bos = ByteArrayOutputStream()
        bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, qualite, bos)
        val e = Enc(bos.toByteArray(), bmp.width, bmp.height)
        bmp.recycle()
        return e
    }

    private val COTES = intArrayOf(2400, 2000, 1700, 1400, 1150, 950, 800)

    /** Fabrique le ou les fichiers de sortie, en visant la taille maximale demandée (en octets). */
    fun fabriquer(pages: List<File>, fmt: OutFmt, cible: Long?, gris: Boolean): Resultat = when (fmt) {
        OutFmt.PNG -> Resultat(pages.map { Output(encoder(it, 4000, 100, gris, png = true).bytes, "png", "image/png") }, true)
        OutFmt.JPG -> {
            var tout = true
            val sorties = pages.map { p ->
                val (b, ok) = ajuster(cible) { cote, q -> encoder(p, cote, q, gris).bytes }
                if (!ok) tout = false
                Output(b, "jpg", "image/jpeg")
            }
            Resultat(sorties, tout)
        }
        else -> {
            val (b, ok) = ajuster(cible) { cote, q -> PdfWriter.write(pages.map { encoder(it, cote, q, gris) }) }
            Resultat(listOf(Output(b, "pdf", "application/pdf")), ok)
        }
    }

    /**
     * Cherche le meilleur compromis : d'abord une bonne qualité (≥ 60) en réduisant la taille de l'image,
     * puis, si ça ne suffit pas, une qualité plus basse.
     */
    private fun ajuster(cible: Long?, faire: (Int, Int) -> ByteArray): Pair<ByteArray, Boolean> {
        if (cible == null) return faire(2200, 85) to true
        var dernier: ByteArray? = null
        for (cote in COTES) {
            val b = faire(cote, 60)
            if (b.size <= cible) return affiner(cible, cote, 60, 90, b, faire) to true
            dernier = b
        }
        for (cote in intArrayOf(1150, 800, 650)) {
            val b = faire(cote, 25)
            if (b.size <= cible) return affiner(cible, cote, 25, 59, b, faire) to true
            dernier = b
        }
        return dernier!! to false
    }

    private fun affiner(cible: Long, cote: Int, bas: Int, haut: Int, dejaOk: ByteArray, faire: (Int, Int) -> ByteArray): ByteArray {
        var lo = bas
        var hi = haut
        var meilleur = dejaOk
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            val b = faire(cote, mid)
            if (b.size <= cible) { lo = mid; meilleur = b } else hi = mid - 1
        }
        return meilleur
    }
}
