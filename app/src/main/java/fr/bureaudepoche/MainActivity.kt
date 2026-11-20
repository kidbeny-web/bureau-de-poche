package fr.bureaudepoche

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import fr.bureaudepoche.ui.AppRoot
import fr.bureaudepoche.ui.BureauTheme
import fr.bureaudepoche.ui.Screen
import java.io.File

class MainActivity : ComponentActivity() {
    lateinit var store: Store
    lateinit var billing: Billing
    val pile = mutableStateListOf<Screen>(Screen.Accueil)

    private var enAttente: Job? = null
    private var aEnregistrer: List<Doc> = emptyList()

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val r = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
            recu(r?.pages?.map { it.imageUri } ?: emptyList())
        } else enAttente = null
    }
    private val photosLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { recu(it) }
    private val fichiersLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { recu(it) }
    private val savePdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { u -> u?.let { ecrireUn(it) } }
    private val saveJpg = registerForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { u -> u?.let { ecrireUn(it) } }
    private val savePng = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { u -> u?.let { ecrireUn(it) } }
    private val saveDossier = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> u?.let { ecrireDossier(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(applicationContext)
        billing = Billing(applicationContext) { runOnUiThread { store.setProAchete() } }
        billing.connecter()
        enableEdgeToEdge()
        setContent {
            val dark = true // Phoenix Pro : toujours en sombre
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                this@MainActivity.enableEdgeToEdge(style, style)
            }
            BureauTheme(dark) { AppRoot(this@MainActivity) }
        }
    }

    // ---------- Navigation ----------
    fun aller(s: Screen) { pile.add(s) }
    fun retour() { if (pile.size > 1) pile.removeAt(pile.lastIndex) }
    fun onglet(s: Screen) { pile.clear(); pile.add(s) }
    fun remplacer(s: Screen) { if (pile.isNotEmpty()) pile[pile.lastIndex] = s else pile.add(s) }

    fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

    // ---------- Choix des fichiers ----------
    fun commencer(job: Job, source: Source) {
        if (!store.peutTraiter()) {
            toast("Limite gratuite atteinte aujourd'hui ($GRATUIT_PAR_JOUR fichiers). Pro = usage illimité.")
            aller(Screen.Pro)
            return
        }
        enAttente = job
        when (source) {
            Source.SCAN -> scanner()
            Source.PHOTOS -> photosLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            Source.FICHIERS -> fichiersLauncher.launch(arrayOf("application/pdf", "image/*"))
        }
    }

    private fun scanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(if (store.pro) 20 else 1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(this)
            .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { toast("Le scanner n'est pas disponible sur ce téléphone : ${it.message}") }
    }

    private fun recu(uris: List<Uri>) {
        val job = enAttente ?: return
        enAttente = null
        if (uris.isEmpty()) return
        var liste = uris
        val unPdf = job.format == OutFmt.PDF || job.format == OutFmt.AUTO
        if (!store.pro && unPdf && job.unSeulPdf && uris.size > 1) {
            toast("Réunir plusieurs pages dans un seul PDF est une fonction Pro. Seul le premier fichier est gardé.")
            liste = uris.take(1)
        }
        aller(Screen.Resultat(job.copy(inputs = liste)))
    }

    // ---------- Partager / enregistrer ----------
    fun partager(docs: List<Doc>) {
        if (docs.isEmpty()) return
        val uris = ArrayList<Uri>(docs.map { FileProvider.getUriForFile(this, "$packageName.fichiers", File(it.chemin)) })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply { type = docs[0].mime; putExtra(Intent.EXTRA_STREAM, uris[0]) }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Envoyer le fichier"))
    }

    fun enregistrer(docs: List<Doc>) {
        if (docs.isEmpty()) return
        aEnregistrer = docs
        if (docs.size == 1) {
            val d = docs[0]
            when (d.mime) {
                "application/pdf" -> savePdf.launch(d.nom)
                "image/png" -> savePng.launch(d.nom)
                else -> saveJpg.launch(d.nom)
            }
        } else saveDossier.launch(null)
    }

    private fun ecrireUn(u: Uri) {
        val d = aEnregistrer.firstOrNull() ?: return
        runCatching {
            contentResolver.openOutputStream(u)?.use { o -> File(d.chemin).inputStream().use { it.copyTo(o) } }
        }.onSuccess { toast("Fichier enregistré") }.onFailure { toast("Impossible d'enregistrer : ${it.message}") }
    }

    private fun ecrireDossier(arbre: Uri) {
        var n = 0
        runCatching {
            val parent = DocumentsContract.buildDocumentUriUsingTree(arbre, DocumentsContract.getTreeDocumentId(arbre))
            for (d in aEnregistrer) {
                val u = DocumentsContract.createDocument(contentResolver, parent, d.mime, d.nom) ?: continue
                contentResolver.openOutputStream(u)?.use { o -> File(d.chemin).inputStream().use { it.copyTo(o) } }
                n++
            }
        }
        toast(if (n > 0) "$n fichiers enregistrés" else "Impossible d'enregistrer dans ce dossier")
    }

    fun acheterPro() {
        if (!billing.acheter(this)) {
            toast("L'achat sera disponible quand l'appli sera sur le Play Store. En attendant, utilisez le mode test dans les réglages.")
        }
    }

    fun restaurerAchats() {
        billing.restaurer { ok -> runOnUiThread { toast(if (ok) "Pro est activé, merci !" else "Aucun achat trouvé sur ce compte Google.") } }
    }
}
