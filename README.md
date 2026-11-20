# Phoenix Pro Bureau (ex-Bureau de Poche)

Appli Android de la gamme Phoenix Pro qui prépare vos papiers pour les démarches administratives : scan, conversion, compression à la bonne taille, PDF bien nommé, dossiers à compléter. Tout se fait sur le téléphone, sans connexion Internet.

## Télécharger l'APK

À chaque modification, GitHub fabrique l'APK tout seul. Il se trouve dans **Releases → derniere-version → PhoenixProBureau.apk**.

## Fonctions

- **Scanner** avec recadrage automatique (scanner de documents Google, traité sur le téléphone)
- **Convertir** : photos (HEIC, PNG, JPG…) → PDF, PDF → images
- **Compresser** une photo ou un PDF sous une taille maximale (1, 2, 5 Mo…)
- **Assembler** plusieurs pages dans un seul PDF (Pro)
- **Démarches préréglées** : CAF, ANTS, Impôts, France Travail, Location, Mutuelle
- **Dossiers à compléter** : location, carte grise, aide au logement (Pro)
- **Nom de fichier automatique** : `DUPONT_Jean_Justificatif_de_domicile.pdf` (Pro)
- Interface Phoenix Pro (sombre, accent orange)
- **Dossiers personnels** : créez vos propres dossiers et ajoutez des pièces à n'importe quel dossier

## À faire avant la publication sur le Play Store

1. **Vérifier les tailles maximales** de chaque démarche sur les sites officiels (valeur par défaut prudente : 2 Mo, dans `Catalog.kt`).
2. **Vérifier les listes de pièces** des dossiers (`Catalog.kt`).
3. Créer le produit intégré **`bureau_pro`** (achat unique) dans la Play Console.
4. Passer `TEST_BUILD` à `false` dans `app/build.gradle.kts` (retire l'interrupteur « débloquer Pro » des réglages).
5. Signer avec une **clé privée** gardée hors du dépôt : la clé `signature/bureau-test.jks` est publique, elle sert uniquement aux essais.
6. Fabriquer un fichier **.aab** (`gradle bundleRelease`) pour le Play Store.
