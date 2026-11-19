package fr.bureaudepoche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.bureaudepoche.Source
import java.util.Locale

/** Couleurs de la maquette, en clair et en sombre. */
data class Pal(
    val bg: Color, val surface: Color, val text: Color, val muted: Color, val border: Color,
    val divider: Color, val outline: Color, val primary: Color, val soft: Color, val softText: Color,
    val link: Color, val ok: Color, val okBg: Color, val okText: Color, val warnBg: Color, val warnText: Color,
    val pdfBg: Color, val pdfText: Color, val dark: Boolean,
)

val Clair = Pal(
    bg = Color(0xFFF4F5F7), surface = Color.White, text = Color(0xFF14161A), muted = Color(0xFF4A5260),
    border = Color(0xFFE3E6EB), divider = Color(0xFFEEF0F3), outline = Color(0xFFC9CED6), primary = Color(0xFF1F4FD1),
    soft = Color(0xFFE6EEFF), softText = Color(0xFF163A9E), link = Color(0xFF1F4FD1), ok = Color(0xFF1C7C4A),
    okBg = Color(0xFFDDF3E6), okText = Color(0xFF14603A), warnBg = Color(0xFFFFE9C7), warnText = Color(0xFF7A4200),
    pdfBg = Color(0xFFFDECEC), pdfText = Color(0xFFB42318), dark = false,
)

val Sombre = Pal(
    bg = Color(0xFF0F1114), surface = Color(0xFF1A1D22), text = Color(0xFFF2F3F5), muted = Color(0xFFA7AEBB),
    border = Color(0xFF2A2E35), divider = Color(0xFF262A31), outline = Color(0xFF3A3F48), primary = Color(0xFF1F4FD1),
    soft = Color(0xFF1E2A4A), softText = Color(0xFFB9CCFF), link = Color(0xFF93B1FF), ok = Color(0xFF3DD68C),
    okBg = Color(0xFF12331F), okText = Color(0xFF8BE6B2), warnBg = Color(0xFF3A2A10), warnText = Color(0xFFFFC979),
    pdfBg = Color(0xFF3A1A1A), pdfText = Color(0xFFFF9F96), dark = true,
)

val LocalPal = staticCompositionLocalOf { Clair }

@Composable
fun BureauTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) Sombre else Clair
    val scheme = if (dark) {
        darkColorScheme(primary = p.primary, background = p.bg, surface = p.surface, onSurface = p.text, onBackground = p.text, onPrimary = Color.White)
    } else {
        lightColorScheme(primary = p.primary, background = p.bg, surface = p.surface, onSurface = p.text, onBackground = p.text, onPrimary = Color.White)
    }
    CompositionLocalProvider(LocalPal provides p) { MaterialTheme(colorScheme = scheme, content = content) }
}

fun tailleTexte(b: Long): String =
    if (b >= 1_000_000) String.format(Locale.FRANCE, "%.1f Mo", b / 1_000_000.0)
    else "${(b / 1000).coerceAtLeast(1)} Ko"

fun moTexte(v: Double): String =
    if (v == Math.floor(v)) "${v.toInt()} Mo" else String.format(Locale.FRANCE, "%.1f Mo", v)

@Composable
fun Panneau(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    pad: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPal.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(pad),
        content = content,
    )
}

@Composable
fun Titre(t: String, size: Int = 26) {
    Text(t, color = LocalPal.current.text, fontSize = size.sp, fontWeight = FontWeight.Bold, lineHeight = (size + 4).sp)
}

@Composable
fun SousTitre(t: String) {
    Text(t, color = LocalPal.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Discret(t: String, modifier: Modifier = Modifier, size: Int = 13) {
    Text(t, modifier = modifier, color = LocalPal.current.muted, fontSize = size.sp, lineHeight = (size + 5).sp)
}

@Composable
fun BarreHaut(titre: String?, onBack: () -> Unit, droite: @Composable () -> Unit = {}) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour", tint = p.text) }
        if (titre != null) {
            Text(titre, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        } else Spacer(Modifier.weight(1f))
        droite()
    }
}

@Composable
fun GrosBouton(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, icone: ImageVector? = null, actif: Boolean = true) {
    val p = LocalPal.current
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (actif) p.primary else p.outline)
            .clickable(enabled = actif, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texte, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BoutonContour(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, actif: Boolean = true) {
    val p = LocalPal.current
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, p.outline, RoundedCornerShape(16.dp))
            .background(p.surface)
            .clickable(enabled = actif, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = if (actif) p.text else p.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Choix(texte: String, choisi: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (choisi) 2.dp else 1.5.dp, if (choisi) p.primary else p.outline, RoundedCornerShape(12.dp))
            .background(if (choisi) p.soft else p.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texte, color = if (choisi) p.softText else p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun EtiquettePro() {
    val p = LocalPal.current
    Text(
        "Pro",
        color = p.warnText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(p.warnBg).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Pastille(texte: String, fond: Color, couleur: Color) {
    Text(
        texte,
        color = couleur,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fond).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun BadgeFichier(mime: String) {
    val p = LocalPal.current
    val (txt, fond, coul) = when (mime) {
        "application/pdf" -> Triple("PDF", p.pdfBg, p.pdfText)
        "image/png" -> Triple("PNG", p.soft, p.softText)
        else -> Triple("JPG", p.soft, p.softText)
    }
    Box(Modifier.size(width = 36.dp, height = 44.dp).clip(RoundedCornerShape(6.dp)).background(fond), contentAlignment = Alignment.Center) {
        Text(txt, color = coul, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChoixSource(titre: String, avecScan: Boolean = true, onChoix: (Source) -> Unit, onFermer: () -> Unit) {
    val p = LocalPal.current
    AlertDialog(
        onDismissRequest = onFermer,
        containerColor = p.surface,
        title = { Text(titre, color = p.text, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (avecScan) LigneSource("Scanner avec l'appareil photo", Icons.Outlined.DocumentScanner) { onChoix(Source.SCAN) }
                LigneSource("Choisir dans mes photos", Icons.Outlined.PhotoLibrary) { onChoix(Source.PHOTOS) }
                LigneSource("Choisir un fichier (PDF, image)", Icons.Outlined.FolderOpen) { onChoix(Source.FICHIERS) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler", color = p.link) } },
    )
}

@Composable
private fun LigneSource(texte: String, icone: ImageVector, onClick: () -> Unit) {
    val p = LocalPal.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.bg).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, contentDescription = null, tint = p.text)
        Spacer(Modifier.width(12.dp))
        Text(texte, color = p.text, fontSize = 15.sp)
    }
}

@Composable
fun Espace(h: Int) = Spacer(Modifier.height(h.dp))
