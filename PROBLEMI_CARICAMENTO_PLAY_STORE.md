# 🛠️ Guida alla Risoluzione dei Problemi di Caricamento su Google Play Store Console

Questa guida documenta in modo esaustivo le problematiche più frequenti riscontrate durante il caricamento, la firma e la pubblicazione di bundle Android (`.aab`) e APK su **Google Play Console** per l'applicazione **CiboTimer** (`com.cibotimer`, account sviluppatore **finalbyte**), fornendo le rispettive cause tecniche e le soluzioni operative passo-passo.

---

## 📌 Indice dei Problemi

1. [HttpError 403: "The caller does not have permission" (Service Account / API)](#1-httperror-403-the-caller-does-not-have-permission)
2. [Livello API Target (targetSdk) Obsoleto o Non Conforme](#2-livello-api-target-targetsdk-obsoleto-o-non-conforme)
3. [Avviso Dispositivi Non Supportati ("Questa release non supporta più X dispositivi")](#3-avviso-dispositivi-non-supportati)
4. [Errori di Firma Digitale e Keystore (Mismatch SHA-256 / Certificato)](#4-errori-di-firma-digitale-e-keystore)
5. [Errore "VersionCode già utilizzato o inferiore alla release precedente"](#5-errore-versioncode-già-utilizzato)
6. [Note di Rilascio Rifiutate (Superamento Limite 500 Caratteri o XML Invalido)](#6-note-di-rilascio-rifiutate)
7. [Dichiarazioni di Conformità per Foreground Services (Android 14+) e Policy Mediche](#7-dichiarazioni-di-conformità-foreground-service)
8. [Checklist Rapida di Pre-Rilascio per CiboTimer](#8-checklist-rapida-di-pre-rilascio)

---

## 1. HttpError 403: "The caller does not have permission"

### 🔍 Causa Tecnica
Questo errore si manifesta quando si tenta di pubblicare un bundle tramite Google Play Developer API (ad esempio con la skill `play-store-publish` o lo script `upload_aab.py`) ed è generato dall'endpoint:
```text
POST https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.cibotimer/edits
HttpError 403: "The caller does not have permission"
```
**Attenzione**: Avere creato la chiave JSON in Google Cloud Platform **non è sufficiente**. Un Service Account di Google Cloud è solo un'identità; Google Play Console possiede un proprio sistema di controllo accessi (RBAC). L'indirizzo email del Service Account deve essere esplicitamente invitato e autorizzato all'interno della console per l'app target.

### 💡 Soluzione Passo-Passo
1. Apri **[Google Play Console](https://play.google.com/console)** ed esegui l'accesso con l'account proprietario (**finalbyte**).
2. Nel menu laterale a sinistra, clicca su **Utenti e autorizzazioni** (*Users & permissions*).
3. Clicca sul pulsante blu **Invita nuovi utenti** (*Invite new users*).
4. Nel campo **Indirizzo email**, incolla l'indirizzo del Service Account:
   ```text
   play-publisher@play-developer-api-515116.iam.gserviceaccount.com
   ```
5. Passa alla scheda **Autorizzazioni app** (*App permissions*) e clicca su **Aggiungi app**:
   * Seleziona **CiboTimer** (`com.cibotimer`) e premi **Applica**.
6. Seleziona le seguenti autorizzazioni minime per consentire il rilascio:
   * ✅ **Rilascia app nelle tracce di test interni** (*Release apps to internal test tracks*)
   * ✅ **Gestione delle tracce di test interni** (*Manage internal test tracks*)
   * *(Opzionale per rilascio diretto in produzione)*: **Rilascia nelle tracce di produzione** (*Release to production*).
7. Clicca su **Applica**, poi sul pulsante in basso a destra **Invita utente** (*Invite user*) e conferma l'invio dell'invito.
8. **Attesa di propagazione**: Le modifiche ai permessi IAM su Google Play Developer API richiedono in genere da 2 a 15 minuti per propagarsi completamente sui server di autenticazione Google.

---

## 2. Livello API Target (targetSdk) Obsoleto o Non Conforme

### 🔍 Causa Tecnica
Google Play impone requisiti temporali stringenti per il livello API target delle nuove app e degli aggiornamenti:
* A partire da agosto/novembre di ogni anno, tutte le release devono avere come target l'ultima versione stabile di Android o quella immediatamente successiva (es. targetSdk 34, 35 o 36).
* Se `targetSdk` in `build.gradle.kts` è inferiore alla soglia richiesta, la console rifiuta il bundle con l'errore:
  > *"L'attuale livello API target della tua app è X, ma il livello API target minimo deve essere Y per garantire che l'app sia basata sulle API più recenti..."*

### 💡 Soluzione
In `app/build.gradle.kts`, allineare `targetSdk` e `compileSdk` al livello richiesto:
```kotlin
android {
    compileSdk = 36
    defaultConfig {
        applicationId = "com.cibotimer"
        minSdk = 26      // Mantenere Android 8.0+ per retrocompatibilità
        targetSdk = 36   // Conforme ai requisiti di Google Play
        versionCode = 85
        versionName = "1.0.84"
    }
}
```
Se il compilatore Kotlin o Android Gradle Plugin segnala avvisi su versioni API in anteprima, aggiungere in `gradle.properties`:
```properties
android.suppressUnsupportedCompileSdk=36
```

---

## 3. Avviso Dispositivi Non Supportati

### 🔍 Sintomo
La console Play mostra l'avviso bloccante o cautelativo:
> *"Questa release non supporta più 18.186 dispositivi presenti nella release precedente. Se procedi, su questi dispositivi non supportati non saranno disponibili la tua app per i nuovi utenti e gli aggiornamenti..."*

### 🔍 Cause Principali
1. **Innalzamento accidentale di `minSdk`**: Se `minSdk` viene portato ad esempio da 26 a 31, tutti i dispositivi con Android 8.0, 9, 10 e 11 vengono esclusi.
2. **Feature Hardware richieste implicitamente**: Dichiarare `<uses-permission>` come fotocamera, sensori biometrici o telefonia senza specificare `<uses-feature android:name="..." android:required="false" />` fa sì che Google Play escluda tutti i dispositivi privi di quell'hardware (ad esempio i tablet).
3. **Filtri ABI delle librerie native**: Mancanza di librerie a 64 bit (`arm64-v8a`, `x86_64`) o esclusione di architetture a 32 bit.

### 💡 Soluzione
* Mantenere sempre `minSdk = 26` per CiboTimer.
* Nel `AndroidManifest.xml`, specificare sempre `android:required="false"` per qualsiasi feature hardware opzionale.

---

## 4. Errori di Firma Digitale e Keystore

### 🔍 Causa Tecnica
Se il bundle caricato è firmato con una chiave diversa da quella registrata in Google Play App Signing, l'upload viene respinto con errore di *SHA-256 fingerprint mismatch*.

### 💡 Soluzione e Regole Permanenti per CiboTimer
* Usare unicamente il keystore permanente posizionato in:
  `cibotimer/keystore/cibotimer.keystore`
  * **Alias**: `cibotimer`
  * **Password**: `cibotimer123`
* **Non rigenerare mai** il keystore: il certificato contiene `CN=CiboTimer` e garantisce che l'utente possa aggiornare l'applicazione sul dispositivo senza doverla disinstallare.
* Verifica preventiva con `apksigner`:
  ```bash
  docker run --rm -v /root/github/cibotimer:/workspace --entrypoint bash fotolibro-android-builder:latest \
    -c "/opt/android-sdk/build-tools/34.0.0/apksigner verify --verbose --print-certs /workspace/CiboTimer-v*.apk"
  ```

---

## 5. Errore "VersionCode già utilizzato"

### 🔍 Causa Tecnica
Google Play Console richiede che **ogni nuovo bundle o APK** caricato abbia un `versionCode` strettamente maggiore di qualsiasi altro `versionCode` mai caricato in precedenza su **qualsiasi traccia** (inclusi Test interni, Alpha, Beta o bozze cancellate).

### 💡 Soluzione
* Incrementare rigorosamente `versionCode` in `app/build.gradle.kts` prima di ogni nuova compilazione destinata all'utente o alla console.
* Allineare contestualmente `versionName` (es. `versionCode = 85`, `versionName = "1.0.84"`).

---

## 6. Note di Rilascio Rifiutate

### 🔍 Causa Tecnica
Google Play Console impone un limite tassativo di **500 caratteri** per ogni lingua nelle note di rilascio (`recentChanges`). Testi più lunghi vengono troncati o scartati con errore `400 Bad Request`.

### 💡 Soluzione
Fornire sempre note brevissime (2-3 punti elenco concisi per lingua) all'interno di [release_notes.xml](file:///root/github/cibotimer/release_notes.xml):
```xml
<?xml version="1.0" encoding="utf-8"?>
<releases>
    <release>
        <recentChanges lang="it-IT">
• Controllo notifiche per fasi metaboliche e preferenza lingua istantanea.
• Traduzioni complete per piani, consigli coach e fasi metaboliche.
• Ottimizzazioni stabilità e compatibilità Android 15 / API 36.
        </recentChanges>
        <recentChanges lang="en-US">
• Granular metabolic stage notification toggle and instant language selection.
• Complete translations for fasting plans, coach tips, and metabolic stages.
• Stability optimizations and Android 15 / API 36 support.
        </recentChanges>
    </release>
</releases>
```

---

## 7. Dichiarazioni di Conformità Foreground Service (Android 14+)

### 🔍 Causa Tecnica
Su Android 14+ (API 34+), l'uso dei Foreground Services senza dichiarazione di conformità o con tipo errato (`FOREGROUND_SERVICE_TYPE_HEALTH` che richiederebbe sensori fisici sul corpo) causa la sospensione o il rifiuto dell'app.

### 💡 Soluzione Implementata in CiboTimer
* Dichiarazione del tipo FGS:
  `android:foregroundServiceType="specialUse"`
* Dichiarazione della proprietà nel manifest:
  ```xml
  <property
      android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
      android:value="Conto alla rovescia continuo e cronometro in tempo reale per le finestre di digiuno intermittente e alimentazione con notifica attiva a basso consumo." />
  ```
* Nella console Play (*Contenuti dell'app* &rarr; *Autorizzazioni per i servizi in primo piano*), selezionare l'uso per utilità timer/cronometro.

---

## 8. Checklist Rapida di Pre-Rilascio

Prima di avviare il comando di pubblicazione con la skill `play-store-publish`:

- [x] **VersionCode incrementato**: verificato in `app/build.gradle.kts`.
- [x] **Test unitari superati**: eseguiti con `./gradlew test`.
- [x] **Entrambi i binari compilati**: sia l'APK (`assembleDebug`) sia il bundle AAB (`bundleRelease`).
- [x] **Rimozione versioni obsolete**: nella root devono esistere solo l'ultimo `.apk` e l'ultimo `.aab`.
- [x] **Firma valida**: verificata la presenza di `CN=CiboTimer`.
- [x] **Note di rilascio bilingue concise**: [release_notes.xml](file:///root/github/cibotimer/release_notes.xml) verificato sotto i 500 caratteri per lingua.
- [x] **Service account autorizzato**: email invitata nella sezione *Utenti e autorizzazioni* di Google Play Console.
- [x] **Commit e Push Git**: codice sorgente e note committati e sincronizzati su GitHub.
