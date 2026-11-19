package fr.bureaudepoche

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Doc(
    val id: String,
    val nom: String,
    val chemin: String,
    val mime: String,
    val taille: Long,
    val demarche: String,
    val date: Long,
)

const val GRATUIT_PAR_JOUR = 5

/** Tout ce qui est gardé sur le téléphone : réglages, fichiers prêts, dossiers. */
class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("bureau", Context.MODE_PRIVATE)
    val docsDir = File(ctx.filesDir, "docs").apply { mkdirs() }
    val workDir = File(ctx.cacheDir, "travail").apply { mkdirs() }
    private val indexFile = File(docsDir, "index.json")

    val docs = mutableStateListOf<Doc>()
    private val rangements = mutableStateMapOf<String, List<String>>()
    private val maxPerso = mutableStateMapOf<String, Double>()

    var nom by mutableStateOf(prefs.getString("nom", "") ?: "")
        private set
    var prenom by mutableStateOf(prefs.getString("prenom", "") ?: "")
        private set
    var theme by mutableStateOf(prefs.getString("theme", "auto") ?: "auto")
        private set
    var proAchete by mutableStateOf(prefs.getBoolean("proAchat", false))
        private set
    var proTest by mutableStateOf(prefs.getBoolean("proTest", false))
        private set
    var utilisesAujourdhui by mutableIntStateOf(0)
        private set

    val pro: Boolean get() = proAchete || proTest

    init {
        chargerDocs()
        chargerRangements()
        DEMARCHES.forEach { d ->
            val v = prefs.getFloat("max_${d.id}", -1f)
            if (v > 0f) maxPerso[d.id] = v.toDouble()
        }
        utilisesAujourdhui = lireCompteur()
    }

    // ---------- Réglages ----------
    fun setIdentite(n: String, p: String) {
        nom = n.trim(); prenom = p.trim()
        prefs.edit().putString("nom", nom).putString("prenom", prenom).apply()
    }

    fun setThemeMode(t: String) {
        theme = t
        prefs.edit().putString("theme", t).apply()
    }

    fun setProAchete() {
        proAchete = true
        prefs.edit().putBoolean("proAchat", true).apply()
    }

    fun setProTestMode(b: Boolean) {
        proTest = b
        prefs.edit().putBoolean("proTest", b).apply()
    }

    fun maxMo(d: Demarche): Double = maxPerso[d.id] ?: d.maxMo

    fun setMaxMo(d: Demarche, v: Double) {
        maxPerso[d.id] = v
        prefs.edit().putFloat("max_${d.id}", v.toFloat()).apply()
    }

    // ---------- Limite gratuite ----------
    private fun lireCompteur(): Int {
        val today = LocalDate.now().toString()
        return if (prefs.getString("jour", "") == today) prefs.getInt("compte", 0) else 0
    }

    fun peutTraiter(): Boolean {
        utilisesAujourdhui = lireCompteur()
        return pro || utilisesAujourdhui < GRATUIT_PAR_JOUR
    }

    fun compterUtilisation() {
        val n = lireCompteur() + 1
        prefs.edit().putString("jour", LocalDate.now().toString()).putInt("compte", n).apply()
        utilisesAujourdhui = n
    }

    // ---------- Noms de fichiers ----------
    fun nomPropose(job: Job): String {
        job.nomImpose?.let { return nettoyer(it) }
        val piece = job.piece
        if (!pro || piece == null) {
            return "Document_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date())
        }
        val parts = listOf(nom.uppercase(Locale.FRANCE), prenom, piece).filter { it.isNotBlank() }
        return nettoyer(parts.joinToString("_"))
    }

    // ---------- Fichiers prêts ----------
    fun enregistrer(sorties: List<Output>, base: String, demarcheId: String, remplacer: List<Doc>): List<Doc> {
        remplacer.forEach { supprimer(it, garderRangement = true) }
        val now = System.currentTimeMillis()
        val propre = nettoyer(base)
        val crees = sorties.mapIndexed { i, o ->
            val id = UUID.randomUUID().toString()
            val nomFichier = if (sorties.size == 1) "$propre.${o.ext}" else "${propre}_${i + 1}.${o.ext}"
            val dir = File(docsDir, id).apply { mkdirs() }
            val f = File(dir, nomFichier)
            f.writeBytes(o.bytes)
            Doc(id, nomFichier, f.absolutePath, o.mime, o.bytes.size.toLong(), demarcheId, now)
        }
        docs.addAll(0, crees)
        // un fichier recréé (autre taille, autre format) garde sa place dans les dossiers
        if (remplacer.isNotEmpty()) {
            val anciens = remplacer.map { it.id }.toSet()
            for ((k, ids) in rangements.toMap()) {
                if (ids.any { it in anciens }) {
                    rangements[k] = ids.filter { it !in anciens } + crees.map { it.id }
                }
            }
            sauverRangements()
        }
        sauverDocs()
        return crees
    }

    fun renommer(liste: List<Doc>, base: String): List<Doc> {
        val propre = nettoyer(base)
        val res = liste.mapIndexed { i, d ->
            val ext = d.nom.substringAfterLast('.', "pdf")
            val nouveau = if (liste.size == 1) "$propre.$ext" else "${propre}_${i + 1}.$ext"
            val ancien = File(d.chemin)
            val cible = File(ancien.parentFile, nouveau)
            if (ancien.renameTo(cible)) d.copy(nom = nouveau, chemin = cible.absolutePath) else d
        }
        res.forEach { r -> val idx = docs.indexOfFirst { it.id == r.id }; if (idx >= 0) docs[idx] = r }
        sauverDocs()
        return res
    }

    fun supprimer(d: Doc, garderRangement: Boolean = false) {
        File(d.chemin).parentFile?.deleteRecursively()
        docs.removeAll { it.id == d.id }
        if (!garderRangement) {
            for ((k, ids) in rangements.toMap()) if (d.id in ids) rangements[k] = ids - d.id
            sauverRangements()
        }
        sauverDocs()
    }

    fun doc(id: String): Doc? = docs.firstOrNull { it.id == id }

    // ---------- Dossiers ----------
    fun docsRanges(cle: String): List<Doc> = (rangements[cle] ?: emptyList()).mapNotNull { doc(it) }

    fun ranger(cle: String, nouveaux: List<Doc>) {
        val ids = (rangements[cle] ?: emptyList()).filter { doc(it) != null }
        rangements[cle] = ids + nouveaux.map { it.id }.filter { it !in ids }
        sauverRangements()
    }

    fun viderPiece(cle: String) {
        rangements.remove(cle)
        sauverRangements()
    }

    fun docsDuDossier(d: Dossier): List<Doc> = d.pieces.flatMap { docsRanges("${d.id}/${it.id}") }

    // ---------- Sauvegarde ----------
    private fun chargerDocs() {
        if (!indexFile.exists()) return
        runCatching {
            val arr = JSONArray(indexFile.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val d = Doc(
                    o.getString("id"), o.getString("nom"), o.getString("chemin"), o.getString("mime"),
                    o.getLong("taille"), o.optString("demarche", ""), o.getLong("date"),
                )
                if (File(d.chemin).exists()) docs.add(d)
            }
        }
    }

    private fun sauverDocs() {
        val arr = JSONArray()
        docs.forEach { d ->
            arr.put(
                JSONObject().put("id", d.id).put("nom", d.nom).put("chemin", d.chemin).put("mime", d.mime)
                    .put("taille", d.taille).put("demarche", d.demarche).put("date", d.date),
            )
        }
        indexFile.writeText(arr.toString())
    }

    private fun chargerRangements() {
        runCatching {
            val o = JSONObject(prefs.getString("rangements", "{}") ?: "{}")
            o.keys().forEach { k ->
                val a = o.getJSONArray(k)
                rangements[k] = (0 until a.length()).map { a.getString(it) }
            }
        }
    }

    private fun sauverRangements() {
        val o = JSONObject()
        rangements.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        prefs.edit().putString("rangements", o.toString()).apply()
    }

    companion object {
        fun nettoyer(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .ifBlank { "Document" }
    }
}
