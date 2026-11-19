package fr.bureaudepoche

import android.net.Uri

/**
 * Démarches préréglées.
 * ATTENTION : la taille maximale (maxMo) est une valeur prudente par défaut, PAS la limite officielle
 * de chaque site. Les limites réelles sont à relever sur les sites officiels avant publication.
 * L'utilisateur peut de toute façon la modifier dans l'écran de la démarche.
 */
data class Demarche(
    val id: String,
    val nom: String,
    val sous: String,
    val pieces: List<String>,
    val maxMo: Double = 2.0,
    val gratuite: Boolean = false,
)

val DEMARCHES = listOf(
    Demarche(
        "caf", "CAF", "Justificatifs, attestations",
        listOf("Justificatif de domicile", "Pièce d'identité", "Attestation employeur", "Avis d'imposition", "Quittance de loyer", "RIB"),
        gratuite = true,
    ),
    Demarche(
        "ants", "ANTS", "Permis, carte grise, identité",
        listOf("Pièce d'identité", "Justificatif de domicile", "Permis de conduire", "Certificat de cession", "Carte grise", "Contrôle technique"),
    ),
    Demarche(
        "impots", "Impôts", "Pièces jointes, réclamations",
        listOf("Justificatif", "Facture", "Attestation", "Avis d'imposition", "Pièce d'identité"),
        gratuite = true,
    ),
    Demarche(
        "francetravail", "France Travail", "Inscription, justificatifs",
        listOf("Pièce d'identité", "CV", "Attestation employeur", "Justificatif de domicile", "RIB"),
    ),
    Demarche(
        "location", "Location", "Dossier locataire complet",
        listOf("Pièce d'identité", "Justificatif de domicile", "Bulletin de salaire", "Avis d'imposition", "Contrat de travail", "RIB"),
    ),
    Demarche(
        "mutuelle", "Mutuelle", "Factures, remboursements",
        listOf("Facture", "Devis", "Feuille de soins", "Attestation"),
    ),
)

fun demarche(id: String?): Demarche? = DEMARCHES.firstOrNull { it.id == id }

/** Dossiers à constituer (checklists). Pièces « habituellement demandées » : à vérifier selon chaque organisme. */
data class Piece(val id: String, val nom: String, val detail: String, val nombre: Int = 1)

data class Dossier(val id: String, val nom: String, val intro: String, val demarche: String, val pieces: List<Piece>)

val DOSSIERS = listOf(
    Dossier(
        "location", "Dossier de location",
        "Les pièces habituellement demandées par un propriétaire ou une agence.",
        "location",
        listOf(
            Piece("identite", "Pièce d'identité", "Recto et verso"),
            Piece("domicile", "Justificatif de domicile", "Moins de 3 mois"),
            Piece("salaire", "3 derniers bulletins de salaire", "Un fichier par mois", 3),
            Piece("impots", "Dernier avis d'imposition", "Toutes les pages"),
            Piece("contrat", "Contrat de travail ou attestation", "Attestation d'employeur acceptée"),
            Piece("rib", "RIB", "Au nom du locataire"),
        ),
    ),
    Dossier(
        "cartegrise", "Carte grise (véhicule d'occasion)",
        "Les pièces habituellement demandées pour changer le titulaire d'une carte grise. Vérifiez la liste sur le site de l'ANTS.",
        "ants",
        listOf(
            Piece("identite", "Pièce d'identité", "Recto et verso"),
            Piece("domicile", "Justificatif de domicile", "Récent"),
            Piece("cession", "Certificat de cession", "Signé par le vendeur et l'acheteur"),
            Piece("cartegrise", "Ancienne carte grise", "Barrée et signée par le vendeur"),
            Piece("ct", "Contrôle technique", "Si le véhicule y est soumis"),
        ),
    ),
    Dossier(
        "caf", "Demande d'aide au logement",
        "Les pièces souvent demandées par la CAF. Vérifiez la liste dans votre espace CAF.",
        "caf",
        listOf(
            Piece("identite", "Pièce d'identité", "Recto et verso"),
            Piece("bail", "Bail ou quittance de loyer", "Document récent"),
            Piece("rib", "RIB", "À votre nom"),
            Piece("impots", "Avis d'imposition", "Toutes les pages"),
        ),
    ),
)

fun dossier(id: String?): Dossier? = DOSSIERS.firstOrNull { it.id == id }

enum class OutFmt(val label: String) { AUTO("Auto"), PDF("PDF"), JPG("JPG"), PNG("PNG") }

enum class Source { SCAN, PHOTOS, FICHIERS }

/** Un travail à effectuer : des fichiers d'entrée et ce qu'on veut en sortie. */
data class Job(
    val titre: String,
    val inputs: List<Uri> = emptyList(),
    val format: OutFmt = OutFmt.PDF,
    val choixFormat: Boolean = false,
    val demarche: Demarche? = null,
    val piece: String? = null,
    /** "dossierId/pieceId" : le résultat est rangé dans ce dossier. */
    val rangement: String? = null,
    val nomImpose: String? = null,
    val unSeulPdf: Boolean = true,
)
