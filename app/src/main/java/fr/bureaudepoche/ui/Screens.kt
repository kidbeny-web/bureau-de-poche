package fr.bureaudepoche.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.bureaudepoche.BuildConfig
import fr.bureaudepoche.DEMARCHES
import fr.bureaudepoche.DOSSIERS
import fr.bureaudepoche.Demarche
import fr.bureaudepoche.Doc
import fr.bureaudepoche.Dossier
import fr.bureaudepoche.GRATUIT_PAR_JOUR
import fr.bureaudepoche.Job
import fr.bureaudepoche.MainActivity
import fr.bureaudepoche.OutFmt
import fr.bureaudepoche.Processor
import fr.bureaudepoche.demarche
import fr.bureaudepoche.dossier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Accueil : Screen
    data object Demarches : Screen
    data class DemarcheS(val id: String) : Screen
    data class Outil(val type: TypeOutil) : Screen
    data class Resultat(val job: Job) : Screen
    data object Dossiers : Screen
    data class DossierS(val id: String) : Screen
    data object Fichiers : Screen
    data object Pro : Screen
    data object Reglages : Screen
}

enum class TypeOutil { CONVERTIR, COMPRESSER, ASSEMBLER }

@Composable
fun AppRoot(app: MainActivity) {
    val p = LocalPal.current
    val ecran = app.pile.lastOrNull() ?: Screen.Accueil
    BackHandler(enabled = app.pile.size > 1) { app.retour() }
    Column(Modifier.fillMaxSize().background(p.bg).systemBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(app.pile.size, ecran) {
                when (ecran) {
                    Screen.Accueil -> AccueilEcran(app)
                    Screen.Demarches -> DemarchesEcran(app)
                    is Screen.DemarcheS -> DemarcheEcran(app, ecran.id)
                    is Screen.Outil -> OutilEcran(app, ecran.type)
                    is Screen.Resultat -> ResultatEcran(app, ecran.job)
                    Screen.Dossiers -> DossiersEcran(app)
                    is Screen.DossierS -> DossierEcran(app, ecran.id)
                    Screen.Fichiers -> FichiersEcran(app)
                    Screen.Pro -> ProEcran(app)
                    Screen.Reglages -> ReglagesEcran(app)
                }
            }
        }
        if (ecran == Screen.Accueil || ecran == Screen.Dossiers || ecran == Screen.Fichiers || (ecran == Screen.Pro && app.pile.size == 1)) {
            BarreOnglets(app, ecran)
        }
    }
}

@Composable
private fun BarreOnglets(app: MainActivity, ecran: Screen) {
    val p = LocalPal.current
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.border))
        Row(Modifier.fillMaxWidth().background(p.surface).padding(top = 6.dp, bottom = 8.dp)) {
            Onglet("Accueil", Icons.Outlined.Home, ecran == Screen.Accueil, Modifier.weight(1f)) { app.onglet(Screen.Accueil) }
            Onglet("Dossiers", Icons.Outlined.Folder, ecran == Screen.Dossiers, Modifier.weight(1f)) { app.onglet(Screen.Dossiers) }
            Onglet("Fichiers", Icons.Outlined.Description, ecran == Screen.Fichiers, Modifier.weight(1f)) { app.onglet(Screen.Fichiers) }
            Onglet("Pro", Icons.Outlined.Star, ecran == Screen.Pro, Modifier.weight(1f)) { app.onglet(Screen.Pro) }
        }
    }
}

@Composable
private fun Onglet(texte: String, icone: ImageVector, actif: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    val c = if (actif) p.link else p.muted
    Column(
        modifier.heightIn(min = 48.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icone, contentDescription = null, tint = c)
        Text(texte, color = c, fontSize = 11.sp, fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ==================== ACCUEIL ====================

@Composable
private fun AccueilEcran(app: MainActivity) {
    val p = LocalPal.current
    val store = app.store
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Espace(14)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bureau de Poche", color = p.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { app.aller(Screen.Reglages) }) { Icon(Icons.Outlined.Settings, contentDescription = "Réglages", tint = p.text) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.ok, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Discret("Vos papiers restent sur votre téléphone")
        }
        Espace(14)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.primary)
                .clickable { app.commencer(Job("Scanner un document", format = OutFmt.PDF, choixFormat = true), fr.bureaudepoche.Source.SCAN) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Scanner un document", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Recadrage et netteté automatiques", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
        Espace(12)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TuileOutil("Convertir", Icons.Outlined.SwapHoriz, false, Modifier.weight(1f)) { app.aller(Screen.Outil(TypeOutil.CONVERTIR)) }
            TuileOutil("Compresser", Icons.Outlined.Compress, false, Modifier.weight(1f)) { app.aller(Screen.Outil(TypeOutil.COMPRESSER)) }
            TuileOutil("Assembler", Icons.Outlined.Layers, !store.pro, Modifier.weight(1f)) {
                if (store.pro) app.aller(Screen.Outil(TypeOutil.ASSEMBLER)) else app.aller(Screen.Pro)
            }
        }
        Espace(22)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SousTitre("Pour quelle démarche ?") }
            Text("Tout voir", color = p.link, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { app.aller(Screen.Demarches) }.padding(8.dp))
        }
        Espace(8)
        DEMARCHES.chunked(2).forEach { ligne ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ligne.forEach { d -> TuileDemarche(app, d, Modifier.weight(1f)) }
                if (ligne.size == 1) Spacer(Modifier.weight(1f))
            }
            Espace(10)
        }
        Espace(12)
        SousTitre("Derniers fichiers")
        Espace(10)
        if (store.docs.isEmpty()) {
            Discret("Vos fichiers prêts apparaîtront ici.")
        } else {
            store.docs.take(3).forEach { d ->
                LigneDoc(d, onClick = { app.onglet(Screen.Fichiers) })
                Espace(8)
            }
        }
        Espace(20)
    }
}

@Composable
private fun TuileOutil(texte: String, icone: ImageVector, pro: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(p.surface).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icone, contentDescription = null, tint = p.text)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(texte, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (pro) { Spacer(Modifier.width(4.dp)); EtiquettePro() }
        }
    }
}

@Composable
private fun TuileDemarche(app: MainActivity, d: Demarche, modifier: Modifier) {
    val p = LocalPal.current
    val bloquee = !d.gratuite && !app.store.pro
    Column(
        modifier.heightIn(min = 72.dp).clip(RoundedCornerShape(14.dp)).background(p.surface)
            .clickable { if (bloquee) app.aller(Screen.Pro) else app.aller(Screen.DemarcheS(d.id)) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.nom, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (bloquee) { Spacer(Modifier.width(6.dp)); EtiquettePro() }
        }
        Discret(d.sous, size = 12)
    }
}

private val formatDate = SimpleDateFormat("d MMM, HH:mm", Locale.FRANCE)

@Composable
private fun LigneDoc(d: Doc, onClick: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    val p = LocalPal.current
    val dem = demarche(d.demarche)?.nom
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgeFichier(d.mime)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.nom, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Discret(listOfNotNull(tailleTexte(d.taille), dem, formatDate.format(Date(d.date))).joinToString(" · "), size = 12)
        }
        actions()
    }
}

// ==================== DÉMARCHES ====================

@Composable
private fun DemarchesEcran(app: MainActivity) {
    Column(Modifier.fillMaxSize()) {
        BarreHaut("Toutes les démarches", { app.retour() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Discret("Chaque démarche règle le format, la taille et le nom du fichier pour vous.")
            DEMARCHES.forEach { d -> TuileDemarche(app, d, Modifier.fillMaxWidth()) }
            Espace(20)
        }
    }
}

private val PALIERS = listOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 8.0, 10.0, 15.0, 20.0)

@Composable
private fun DemarcheEcran(app: MainActivity, id: String) {
    val p = LocalPal.current
    val d = demarche(id) ?: return
    val store = app.store
    var pieceChoisie by remember { mutableStateOf<String?>(null) }
    val max = store.maxMo(d)
    Column(Modifier.fillMaxSize()) {
        BarreHaut(d.nom, { app.retour() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Discret(d.sous)
            Espace(14)
            Panneau {
                Text("Taille maximale du fichier", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Espace(4)
                Discret("Elle change selon les sites. Vérifiez la limite indiquée sur le site officiel et ajustez-la ici.", size = 12)
                Espace(10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoutonRond(Icons.Outlined.Remove, "Moins") {
                        val i = PALIERS.indexOfLast { it < max }
                        if (i >= 0) store.setMaxMo(d, PALIERS[i])
                    }
                    Text(moTexte(max), color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    BoutonRond(Icons.Outlined.Add, "Plus") {
                        val i = PALIERS.indexOfFirst { it > max }
                        if (i >= 0) store.setMaxMo(d, PALIERS[i])
                    }
                }
            }
            Espace(20)
            SousTitre("Quel document ?")
            Espace(10)
            d.pieces.forEach { piece ->
                Panneau(onClick = { pieceChoisie = piece }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(piece, color = p.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = p.muted)
                    }
                }
                Espace(8)
            }
            Espace(20)
        }
    }
    pieceChoisie?.let { piece ->
        ChoixSource(piece, onChoix = { src ->
            pieceChoisie = null
            app.commencer(Job(piece, format = OutFmt.PDF, choixFormat = true, demarche = d, piece = piece), src)
        }, onFermer = { pieceChoisie = null })
    }
}

@Composable
private fun BoutonRond(icone: ImageVector, desc: String, onClick: () -> Unit) {
    val p = LocalPal.current
    Box(Modifier.size(48.dp).clip(CircleShape).background(p.soft).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icone, contentDescription = desc, tint = p.softText)
    }
}

// ==================== OUTILS ====================

@Composable
private fun OutilEcran(app: MainActivity, type: TypeOutil) {
    val p = LocalPal.current
    var format by remember { mutableStateOf(OutFmt.PDF) }
    val (titre, texte) = when (type) {
        TypeOutil.CONVERTIR -> "Convertir" to "Transformez vos photos (HEIC, PNG, JPG…) en PDF, ou un PDF en images."
        TypeOutil.COMPRESSER -> "Compresser" to "Réduisez le poids d'une photo ou d'un PDF pour qu'il passe sur un site ou par mail."
        TypeOutil.ASSEMBLER -> "Assembler" to "Réunissez plusieurs photos ou PDF (recto, verso, plusieurs pages) dans un seul PDF."
    }
    fun job() = when (type) {
        TypeOutil.CONVERTIR -> Job(titre, format = format, choixFormat = true)
        TypeOutil.COMPRESSER -> Job(titre, format = OutFmt.AUTO, unSeulPdf = false)
        TypeOutil.ASSEMBLER -> Job(titre, format = OutFmt.PDF)
    }
    Column(Modifier.fillMaxSize()) {
        BarreHaut(titre, { app.retour() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Discret(texte, size = 14)
            if (type == TypeOutil.CONVERTIR) {
                Espace(20)
                Text("Convertir en", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Espace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(OutFmt.PDF, OutFmt.JPG, OutFmt.PNG).forEach { f ->
                        Choix(f.label, format == f, { format = f }, Modifier.weight(1f))
                    }
                }
            }
            if (type == TypeOutil.ASSEMBLER || (type == TypeOutil.CONVERTIR && format == OutFmt.PDF)) {
                Espace(14)
                Discret("Astuce : sélectionnez plusieurs fichiers d'un coup, ils seront mis dans l'ordre choisi.", size = 12)
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GrosBouton("Choisir dans mes photos", { app.commencer(job(), fr.bureaudepoche.Source.PHOTOS) })
            BoutonContour("Choisir un fichier (PDF, image)", { app.commencer(job(), fr.bureaudepoche.Source.FICHIERS) })
        }
    }
}

// ==================== RÉSULTAT ====================

private sealed interface Etat {
    data object Travail : Etat
    data class Fini(val docs: List<Doc>, val total: Long, val ok: Boolean) : Etat
    data class Erreur(val msg: String) : Etat
}

private fun resoudre(ctx: Context, job: Job): OutFmt =
    if (job.format != OutFmt.AUTO) job.format
    else if (job.inputs.any { Processor.estPdf(ctx, it) }) OutFmt.PDF else OutFmt.JPG

@Composable
private fun ResultatEcran(app: MainActivity, job: Job) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    val store = app.store
    val preset = job.demarche?.let { store.maxMo(it) }
    var cibleMo by remember { mutableStateOf<Double?>(preset ?: 2.0) }
    var gris by remember { mutableStateOf(false) }
    var format by remember { mutableStateOf(resoudre(ctx, job)) }
    var nom by remember { mutableStateOf(store.nomPropose(job)) }
    var pages by remember { mutableStateOf<List<File>?>(null) }
    var etat by remember { mutableStateOf<Etat>(Etat.Travail) }
    var docs by remember { mutableStateOf<List<Doc>>(emptyList()) }
    var compte by remember { mutableStateOf(false) }
    val avant = remember { job.inputs.sumOf { Processor.taille(ctx, it) } }
    val dossierTravail = remember { File(store.workDir, System.nanoTime().toString()) }

    DisposableEffect(Unit) { onDispose { dossierTravail.deleteRecursively() } }

    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { runCatching { Processor.preparer(ctx, job.inputs, dossierTravail) } }
        r.onSuccess { pages = it }.onFailure { etat = Etat.Erreur(it.message ?: "Ce fichier est illisible.") }
    }

    LaunchedEffect(pages, cibleMo, gris, format) {
        val pg = pages ?: return@LaunchedEffect
        etat = Etat.Travail
        val cible = if (format == OutFmt.PNG) null else cibleMo?.let { (it * 1_000_000 * 0.97).toLong() }
        val r = withContext(Dispatchers.Default) { runCatching { Processor.fabriquer(pg, format, cible, gris) } }
        r.onSuccess { res ->
            val nouveaux = store.enregistrer(res.sorties, nom, job.demarche?.id ?: "", docs)
            docs = nouveaux
            if (!compte) {
                compte = true
                store.compterUtilisation()
                job.rangement?.let { store.ranger(it, nouveaux) }
            }
            etat = Etat.Fini(nouveaux, nouveaux.sumOf { it.taille }, res.objectifAtteint)
        }.onFailure { etat = Etat.Erreur(it.message ?: "Erreur pendant la préparation du fichier.") }
    }

    Column(Modifier.fillMaxSize()) {
        BarreHaut(null, { app.retour() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // ----- En-tête -----
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (val e = etat) {
                    Etat.Travail -> {
                        CircularProgressIndicator(color = p.primary, modifier = Modifier.size(56.dp))
                        Espace(14)
                        Titre("Préparation…")
                        Espace(6)
                        Discret("Tout se fait sur votre téléphone.", size = 14)
                    }
                    is Etat.Fini -> {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(p.okBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = p.ok, modifier = Modifier.size(32.dp))
                        }
                        Espace(10)
                        Titre(if (e.docs.size > 1) "Fichiers prêts" else "Fichier prêt")
                        Espace(6)
                        Discret(
                            job.demarche?.let { "Format et taille réglés pour votre démarche ${it.nom}." } ?: "Prêt à être envoyé ou enregistré.",
                            size = 14,
                        )
                    }
                    is Etat.Erreur -> {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(p.warnBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = p.warnText, modifier = Modifier.size(32.dp))
                        }
                        Espace(10)
                        Titre("Oups")
                        Espace(6)
                        Discret(e.msg, size = 14)
                    }
                }
            }
            Espace(20)

            // ----- Fichier, tailles, réglages -----
            Panneau {
                val fini = etat as? Etat.Fini
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgeFichier(docs.firstOrNull()?.mime ?: "application/pdf")
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(docs.firstOrNull()?.nom ?: "$nom.${format.label.lowercase()}", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (docs.size > 1) Discret("+ ${docs.size - 1} autre(s) fichier(s)", size = 12)
                    }
                }
                Espace(12)
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du fichier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.text, unfocusedTextColor = p.text,
                        focusedBorderColor = p.primary, unfocusedBorderColor = p.outline,
                        focusedLabelColor = p.link, unfocusedLabelColor = p.muted,
                    ),
                    trailingIcon = {
                        TextButton(onClick = {
                            if (docs.isNotEmpty()) {
                                docs = store.renommer(docs, nom)
                                (etat as? Etat.Fini)?.let { etat = it.copy(docs = docs) }
                            }
                        }) { Text("OK", color = p.link) }
                    },
                )
                if (!store.pro && job.piece != null) {
                    Espace(4)
                    Discret("Avec Pro, le nom est créé tout seul (ex. DUPONT_Jean_Justificatif_de_domicile).", size = 12)
                }
                Espace(14)
                Row {
                    Discret("Avant : ${tailleTexte(avant)}", Modifier.weight(1f))
                    Text(
                        if (fini != null) "Après : ${tailleTexte(fini.total)}" else "Après : …",
                        color = p.ok, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Espace(8)
                val ratio = if (fini != null && avant > 0) (fini.total.toFloat() / avant).coerceIn(0.02f, 1f) else 0f
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(p.border)) {
                    Box(Modifier.fillMaxWidth(ratio).height(10.dp).clip(RoundedCornerShape(50)).background(p.primary))
                }

                if (format != OutFmt.PNG) {
                    Espace(16)
                    Text("Taille maximale visée", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Espace(8)
                    val options = buildList {
                        if (preset != null && preset !in listOf(1.0, 2.0, 5.0)) add(preset)
                        add(1.0); add(2.0); add(5.0)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { v -> Choix(moTexte(v), cibleMo == v, { cibleMo = v }) }
                        Choix("Sans limite", cibleMo == null, { cibleMo = null })
                    }
                }

                Espace(14)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Noir et blanc", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Discret("Fichier plus léger, idéal pour les papiers", size = 12)
                    }
                    Switch(
                        checked = gris, onCheckedChange = { gris = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = Color.White),
                    )
                }

                if (job.choixFormat) {
                    Espace(14)
                    Text("Format", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Espace(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(OutFmt.PDF, OutFmt.JPG, OutFmt.PNG).forEach { f -> Choix(f.label, format == f, { format = f }, Modifier.weight(1f)) }
                    }
                }
            }
            Espace(12)

            // ----- Vérifications -----
            val fini = etat as? Etat.Fini
            if (fini != null) {
                Panneau(pad = 6.dp) {
                    LigneVerif(true, "Format ${format.label}")
                    if (format != OutFmt.PNG) {
                        val c = cibleMo
                        if (c == null) LigneVerif(true, "Taille libre")
                        else if (fini.ok) LigneVerif(true, "Sous ${moTexte(c)}")
                        else LigneVerif(false, "Impossible de descendre sous ${moTexte(c)} : essayez « Noir et blanc » ou une taille plus grande")
                    }
                    if (job.rangement != null) LigneVerif(true, "Rangé dans votre dossier")
                    LigneVerif(true, "Traité sur votre téléphone, rien n'a été envoyé en ligne")
                }
            }
            Espace(16)
        }

        val pret = etat is Etat.Fini
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GrosBouton("Partager ou envoyer", { app.partager(docs) }, icone = Icons.Outlined.Share, actif = pret)
            BoutonContour(if (docs.size > 1) "Enregistrer dans un dossier" else "Enregistrer sur le téléphone", { app.enregistrer(docs) }, actif = pret)
        }
    }
}

@Composable
private fun LigneVerif(ok: Boolean, texte: String) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Outlined.Check else Icons.Outlined.Warning, contentDescription = null, tint = if (ok) p.ok else p.warnText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texte, color = p.text, fontSize = 14.sp)
    }
}

// ==================== DOSSIERS ====================

@Composable
private fun DossiersEcran(app: MainActivity) {
    val p = LocalPal.current
    val store = app.store
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Espace(18)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titre("Mes dossiers")
            if (!store.pro) { Spacer(Modifier.width(8.dp)); EtiquettePro() }
        }
        Espace(6)
        Discret("Rassemblez toutes les pièces d'un dossier, puis envoyez-les en un seul PDF.", size = 14)
        Espace(16)
        DOSSIERS.forEach { d ->
            val prets = d.pieces.count { store.docsRanges("${d.id}/${it.id}").size >= it.nombre }
            Panneau(onClick = { if (store.pro) app.aller(Screen.DossierS(d.id)) else app.aller(Screen.Pro) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.nom, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Discret("$prets pièce(s) prête(s) sur ${d.pieces.size}", size = 12)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = p.muted)
                }
            }
            Espace(10)
        }
    }
}

@Composable
private fun DossierEcran(app: MainActivity, id: String) {
    val p = LocalPal.current
    val d: Dossier = dossier(id) ?: return
    val store = app.store
    var ajoutPour by remember { mutableStateOf<fr.bureaudepoche.Piece?>(null) }
    val tous = store.docsDuDossier(d)
    val prets = d.pieces.count { store.docsRanges("${d.id}/${it.id}").size >= it.nombre }
    Column(Modifier.fillMaxSize()) {
        BarreHaut(null, { app.retour() }) { EtiquettePro() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Titre(d.nom)
            Espace(6)
            Discret(d.intro, size = 14)
            Espace(16)
            Panneau {
                Row {
                    Text("$prets pièces prêtes sur ${d.pieces.size}", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Discret("${prets * 100 / d.pieces.size} %")
                }
                Espace(10)
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(p.border)) {
                    Box(Modifier.fillMaxWidth(prets.toFloat() / d.pieces.size).height(10.dp).clip(RoundedCornerShape(50)).background(p.primary))
                }
            }
            Espace(12)
            Panneau(pad = 4.dp) {
                d.pieces.forEachIndexed { i, piece ->
                    val cle = "${d.id}/${piece.id}"
                    val n = store.docsRanges(cle).size
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(piece.nom, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Discret(if (piece.nombre > 1) "${piece.detail} · $n sur ${piece.nombre} ajouté(s)" else piece.detail, size = 12)
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            n >= piece.nombre -> {
                                Pastille("Prêt", p.okBg, p.okText)
                                IconButton(onClick = { store.viderPiece(cle) }) { Icon(Icons.Outlined.Close, contentDescription = "Retirer", tint = p.muted) }
                            }
                            n > 0 -> {
                                Pastille("En cours", p.warnBg, p.warnText)
                                IconButton(onClick = { ajoutPour = piece }) { Icon(Icons.Outlined.Add, contentDescription = "Ajouter", tint = p.link) }
                            }
                            else -> Text(
                                "Ajouter",
                                color = p.softText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(p.soft).clickable { ajoutPour = piece }.padding(horizontal = 14.dp, vertical = 12.dp),
                            )
                        }
                    }
                    if (i < d.pieces.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 12.dp).background(p.divider))
                }
            }
            Espace(16)
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GrosBouton("Réunir en un seul PDF", {
                if (!store.peutTraiter()) { app.aller(Screen.Pro); return@GrosBouton }
                val nomBase = listOf(store.nom.uppercase(Locale.FRANCE), store.prenom, d.nom).filter { it.isNotBlank() }.joinToString("_")
                app.aller(
                    Screen.Resultat(
                        Job(
                            d.nom,
                            inputs = tous.map { Uri.fromFile(File(it.chemin)) },
                            format = OutFmt.PDF,
                            demarche = demarche(d.demarche),
                            nomImpose = nomBase,
                        ),
                    ),
                )
            }, actif = tous.isNotEmpty())
            BoutonContour("Exporter les fichiers séparés", { app.enregistrer(tous) }, actif = tous.isNotEmpty())
        }
    }
    ajoutPour?.let { piece ->
        ChoixSource(piece.nom, onChoix = { src ->
            ajoutPour = null
            app.commencer(
                Job(piece.nom, format = OutFmt.PDF, choixFormat = true, demarche = demarche(d.demarche), piece = piece.nom, rangement = "${d.id}/${piece.id}"),
                src,
            )
        }, onFermer = { ajoutPour = null })
    }
}

// ==================== FICHIERS ====================

@Composable
private fun FichiersEcran(app: MainActivity) {
    val p = LocalPal.current
    val store = app.store
    var aSupprimer by remember { mutableStateOf<Doc?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Espace(18)
        Titre("Mes fichiers")
        Espace(6)
        Discret("Gardés uniquement sur ce téléphone.", size = 14)
        Espace(16)
        if (store.docs.isEmpty()) {
            Panneau { Discret("Aucun fichier pour l'instant. Scannez ou choisissez un document depuis l'accueil.", size = 14) }
        }
        store.docs.forEach { d ->
            LigneDoc(d) {
                IconButton(onClick = { app.partager(listOf(d)) }) { Icon(Icons.Outlined.Share, contentDescription = "Partager", tint = p.text) }
                IconButton(onClick = { aSupprimer = d }) { Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", tint = p.muted) }
            }
            Espace(8)
        }
        Espace(20)
    }
    aSupprimer?.let { d ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            containerColor = p.surface,
            title = { Text("Supprimer ce fichier ?", color = p.text) },
            text = { Text(d.nom, color = p.muted) },
            confirmButton = { TextButton(onClick = { store.supprimer(d); aSupprimer = null }) { Text("Supprimer", color = p.pdfText) } },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler", color = p.link) } },
        )
    }
}

// ==================== PRO ====================

@Composable
private fun ProEcran(app: MainActivity) {
    val p = LocalPal.current
    val store = app.store
    Column(Modifier.fillMaxSize()) {
        if (app.pile.size > 1) BarreHaut(null, { app.retour() }) else Espace(18)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(p.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Espace(10)
            Titre("Bureau de Poche Pro")
            Espace(6)
            Discret(
                if (store.pro) "Pro est activé. Merci pour votre soutien !" else "Toutes vos démarches, sans limite. Un seul paiement, pas d'abonnement.",
                size = 14,
            )
            Espace(18)
            Panneau(pad = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    Text("Gratuit", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
                    Text("Pro", color = p.softText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
                }
                listOf(
                    "Scan, conversion, compression" to "$GRATUIT_PAR_JOUR / jour",
                    "Démarches préréglées" to "2",
                    "Plusieurs pages dans un PDF" to "—",
                    "Dossiers à compléter" to "—",
                    "Nom de fichier automatique" to "—",
                    "Sans publicité" to "Oui",
                ).forEach { (label, gratuit) ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = p.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(gratuit, color = p.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
                        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, contentDescription = "Inclus", tint = p.ok, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Espace(14)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.ok, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Discret("Gratuit ou Pro, vos papiers ne quittent jamais le téléphone", size = 12)
            }
            Espace(16)
        }
        if (!store.pro) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GrosBouton("Débloquer Pro · ${app.billing.prix ?: "3,99 €"}", { app.acheterPro() })
                Espace(6)
                Discret("Paiement unique via Google Play", size = 12)
                TextButton(onClick = { app.restaurerAchats() }) { Text("Restaurer mes achats", color = p.link, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ==================== RÉGLAGES ====================

@Composable
private fun ReglagesEcran(app: MainActivity) {
    val p = LocalPal.current
    val store = app.store
    var nom by remember { mutableStateOf(store.nom) }
    var prenom by remember { mutableStateOf(store.prenom) }
    val couleurs = OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.text, unfocusedTextColor = p.text,
        focusedBorderColor = p.primary, unfocusedBorderColor = p.outline,
        focusedLabelColor = p.link, unfocusedLabelColor = p.muted,
    )
    Column(Modifier.fillMaxSize()) {
        BarreHaut("Réglages", { store.setIdentite(nom, prenom); app.retour() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Panneau {
                Text("Votre nom", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Discret("Sert à nommer vos fichiers (ex. DUPONT_Jean_RIB.pdf).", size = 12)
                Espace(10)
                OutlinedTextField(nom, { nom = it }, label = { Text("Nom") }, singleLine = true, colors = couleurs, modifier = Modifier.fillMaxWidth())
                Espace(8)
                OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, singleLine = true, colors = couleurs, modifier = Modifier.fillMaxWidth())
                Espace(10)
                BoutonContour("Enregistrer", { store.setIdentite(nom, prenom); app.toast("Nom enregistré") })
            }
            Espace(12)
            Panneau {
                Text("Thème", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Espace(10)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto", "clair" to "Clair", "sombre" to "Sombre").forEach { (v, l) ->
                        Choix(l, store.theme == v, { store.setThemeMode(v) }, Modifier.weight(1f))
                    }
                }
                Espace(6)
                Discret("« Auto » suit le réglage de votre téléphone.", size = 12)
            }
            if (BuildConfig.TEST_BUILD) {
                Espace(12)
                Panneau {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Version de test : débloquer Pro", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Discret("Pour essayer toutes les fonctions avant la mise sur le Play Store.", size = 12)
                        }
                        Switch(
                            checked = store.proTest, onCheckedChange = { store.setProTestMode(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = p.primary, checkedThumbColor = Color.White),
                        )
                    }
                }
            }
            Espace(12)
            Panneau {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.ok)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("100 % sur votre téléphone", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Discret("L'appli n'a pas accès à Internet : vos documents ne sont jamais envoyés en ligne.", size = 12)
                    }
                }
            }
            Espace(12)
            Discret("Bureau de Poche ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), size = 12)
            Espace(24)
        }
    }
}
