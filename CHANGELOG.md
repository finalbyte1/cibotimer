# 📋 CiboTimer — Changelog

Tutte le modifiche significative al progetto vengono documentate in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.0.0/) e il progetto aderisce al [Semantic Versioning](https://semver.org/lang/it/).

---

## [1.0.62] - 2026-11-12

### ⏱️ Risoluzione Bug Riavvio Timer & Semplificazione Dialog di Stop
- **Correzione Riavvio Timer dopo Stop**:
  - Risolta la race condition nella chiusura e riavvio del foreground service: `FastingForegroundService.stop(context)` ora arresta direttamente il servizio tramite `stopService` anziché inviare un intent asincrono `ACTION_STOP_FASTING`, prevenendo la cancellazione accidentale di una nuova sessione avviata immediatamente dopo lo stop.
  - In `stopForegroundTimer()`, aggiunto il controllo sullo stato di `_currentSession.value` per evitare doppie invocazioni di stop.
  - Centralizzato `startFasting(customStartTime)` per delegare direttamente a `startFastingNow(customStartTime)`, garantendo che la pressione del pulsante "Avvia" (Start) o il tocco sul quadrante circolare avvii sempre una sessione di digiuno attiva calcolata dal momento esatto del tap con la piena durata prevista dal piano.
  - Abilitato il tocco interattivo diretto sul quadrante circolare `FastingCircularProgress` quando il timer è fermo (`!isRunning`) per avviare istantaneamente la sessione.
- **Semplificazione Dialog Conferma Stop**:
  - Rimossa l'opzione intermedia "Switch a finestra pasto / digiuno" dal dialog di conferma stop: il dialog ora presenta una scelta pulita e diretta con solo il pulsante rosso "Ferma timer" e il pulsante "Annulla", sia durante il digiuno che durante la finestra di pasto.
  - Aggiunte stringhe dedicate e localizzate per lo stop della finestra pasto (`dialog_stop_eating_title` e `dialog_stop_eating_message`).
- **Suite di Test Unitari**:
  - Aggiunti test in `TimerStopRestartTest.kt` per verificare la corretta sequenza di stop e riavvio immediato con calcolo esatto dei tempi rimanenti e trascorsi.

---

## [1.0.61] - 2026-11-12

### 🌍 Internazionalizzazione Completa Date, Giorni e Orari (12h/24h & Locale-Aware)
- **Centralizzazione Formattazione Temporale (`DateTimeFormatters.kt`)**:
  - Creata la classe singleton di dominio `DateTimeFormatters` per la gestione unificata, locale-aware e coerente di date, orari (12h AM/PM vs 24h) e giorni della settimana:
    - `getActiveLocale(context)`: Rileva dinamicamente la lingua attiva dell'applicazione (`locales[0]`).
    - `is24Hour(context, locale)`: Rispetta sempre il formato a 24 ore per la lingua italiana (`true`) e rispetta le preferenze di sistema dell'utente (`DateFormat.is24HourFormat`) per la lingua inglese.
    - `formatShortDay(dayOfWeek, locale, capitalize)`: Utilizza `DayOfWeek.getDisplayName(TextStyle.SHORT, locale)` (es. `mer` in italiano, `Wed` in inglese).
    - `formatFullDay(dayOfWeek, locale, capitalize)`: Fornisce il nome esteso del giorno (es. `Mercoledì` in IT, `Wednesday` in EN).
    - `formatTime(...)`: Formatta l'orario a 12 ore con suffisso AM/PM (es. `8:00 PM`) o 24 ore (`20:00`).
    - `formatDayAndTime(...)`: Combina giorno abbreviato e orario (es. `mer 20:00` in IT, `Wed 8:00 PM` in EN 12h, `Wed 20:00` in EN 24h).
    - `formatFullDateTime(...)` e `formatShortDate(...)`: Formattano date complete e brevi per assi grafici, cronologia pesate e notifiche.
- **Aggiornamento Schermate & Componenti**:
  - `TimerScreen`: Card "Inizio / Fine" ("Start / Next") ora visualizza orari e giorni localizzati (es. `mer 20:00` e `gio 16:00` in italiano, `Wed 8:00 PM` e `Thu 4:00 PM` in inglese a 12 ore). Estratte tutte le stringhe di dialogo hardcoded in `strings.xml`.
  - `WeekBarChart`: Intestazioni di riga dei giorni e descrizioni orarie (es. `Lun 14:00 → Mar 07:00` in IT, `Mon 2:00 PM → Tue 7:00 AM` in EN) localizzate dinamicamente tramite `DateTimeFormatters`.
  - `FastingForegroundService`: Notifica persistente con orario del prossimo evento localizzato tramite stringhe parametriche (`notif_fgs_*`).
  - `FastingWidgetUpdater`: Visualizzazione corretta del target orario nel widget 1x1 e 2x1 ("Digiuno alle 12:00 Mer" in IT vs "Fast at 12:00 PM Wed" in EN).
  - `AppRepository`: Notifiche di transizione digiuno/alimentazione localizzate con stringhe in `strings.xml`.
  - `NotificationsScreen`, `ProgressScreen`, `MeasurementEntryDialog`, `WeightProgressChart`, `WaterTrackerScreen`, `TimeEditorModal`: Rimossi tutti i `SimpleDateFormat` e riferimenti statici a `Locale.ITALIAN`, sostituendoli con `DateTimeFormatters`.
- **Suite di Test Unitari (`DateTimeFormattersTest.kt`)**:
  - Test approfonditi per `formatDayAndTime`, `formatShortDay`, `formatFullDay`, `formatTime` a 12h e 24h, `is24Hour` e `formatShortDate` su locale italiano e inglese, verificando che la lingua italiana rimanga identica al 100% all'output precedente.

---

## [1.0.60] - 2026-11-12

### 🧭 Ottimizzazione Barra di Navigazione & Prevenzione Wrapping Etichette
- **Risoluzione Spezzamento Etichetta Notifiche in Inglese**:
  - Nel file `strings.xml` (lingua inglese di default), modificata l'etichetta del tab da `"Notifications"` a `"Alerts"` (`nav_notifications`), prevenendo lo spezzamento della parola su due righe ("Notificatio / ns") nei 72dp allocati su schermi da 360dp.
  - Creata la stringa separata `notifications_title` (`"Notifications"` in inglese, `"Notifiche"` in italiano) per il titolo della schermata di archivio notifiche, preservando intatto il titolo della schermata senza impatti visivi.
  - L'etichetta italiana `"Notifiche"` rimane invariata.
- **Rete di Sicurezza Tipografica Multi-Lingua & Font Scaling**:
  - In `Navigation.kt`, sia per `NavigationBarItem` (smartphone) sia per `NavigationRailItem` (tablet), applicati rigorosamente ai testi delle etichette:
    - `maxLines = 1`
    - `softWrap = false`
    - `overflow = TextOverflow.Ellipsis`
    - `textAlign = TextAlign.Center`
    - `fontSize = 11.sp` ($\le$ 12sp)
  - Con scale del font di sistema elevate (fino al massimo consentito da Android), il testo non va mai a capo su più righe e non altera in alcun modo l'altezza o l'allineamento della barra inferiore o laterale.
  - Tutte le altre etichette dei tab (`Fasting`/`Digiuno`, `Me`/`Io`, `Coach`, `More`/`Altro`) rimangono perfettamente leggibili e centrate sotto le rispettive icone sia su smartphone compatti (360dp) che su tablet con `NavigationRail`.
- **Suite di Test Unitari (`NavigationLabelsTest.kt`)**:
  - Aggiunti test per la verifica dell'ordine e corrispondenza dei 5 tab di navigazione, della lunghezza delle stringhe in inglese/italiano e dell'assenza di rischi di wrapping su schermi da 360dp.

---

## [1.0.59] - 2026-11-12

### ⚖️ Internazionalizzazione Unità di Misura: Selezione Peso tra kg e lb
- **Modello Dati & Retrocompatibilità Assoluta (`BackupData.kt`)**:
  - Introdotto l'enum serializzabile `@Serializable enum class WeightUnit(val symbol: String, val labelResId: Int) { KG, LB }`.
  - Aggiunto il campo `weightUnit: WeightUnit = WeightUnit.KG` a `UserProfile`.
  - **Zero migrazione dati**: tutti i valori di peso nel database, nei file JSON di stato e nei backup Google Drive restano canonicamente memorizzati in `kg` (`Double`).
  - Retrocompatibilità garantita al 100%: i backup e i dati esistenti privi del campo vengono deserializzati con default `KG`.
  - Nelle nuove installazioni (nessun file di stato pregresso) il default viene impostato automaticamente in base al Paese del Locale del dispositivo (`LB` per Stati Uniti, Liberia e Myanmar; `KG` per tutti gli altri Paesi).
- **Utility Centralizzata (`WeightUnitConverter.kt`)**:
  - Metodi `kgToDisplay(kg, unit)` e `displayToKg(value, unit)` basati sulla costante di conversione internazionale esatta `2.20462262`, preservando la massima precisione numerica dei kg salvati senza arrotondamenti preventivi.
  - Metodi `formatWeight` e `formatDelta` con rispetto del separatore decimale del `Locale`, arrotondamento a 1 cifra decimale solo in fase di visualizzazione e gestione formattata del segno esplicito (`+/-`).
  - Metodo `isValidWeight` con limiti fisiologici unificati tra 25.0 e 350.0 kg (o equivalente in lb).
- **Impostazioni (`SettingsScreen.kt`)**:
  - Aggiunto selettore a due posizioni (Segmented Button) "Unità di peso: kg / lb" che aggiorna in tempo reale tutte le schermate e i componenti dell'applicazione.
  - Campo di inserimento del peso obiettivo formattato e convertito nell'unità scelta, con validazione dei limiti e messaggio di errore.
- **Campi di Input & Dialog (`GoalEditorDialog.kt`, `MeasurementEntryDialog.kt`)**:
  - Visualizzazione e inserimento nell'unità attiva dell'utente.
  - All'apertura il valore iniziale è convertito e formattato a 1 cifra decimale.
  - Al salvataggio i dati vengono convertiti e memorizzati internamente in `kg`.
  - Etichette con simbolo dinamico dell'unità e validazione del range 25–350 kg (o equivalente lb).
- **Dashboard Progressi & Modal di Condivisione (`ProgressScreen.kt`, `ShareProgressModal.kt`)**:
  - Sostituite tutte le stringhe con "kg" hardcoded con i metodi di formattazione centralizzati.
  - Parametrizzate con l'unità di misura le stringhe di risorsa in `strings.xml` e `values-it/strings.xml` (`share_text_weight_lost`, `share_text_current_weight`, `share_text_goal`, `share_target_format`, `share_starting_format`, `share_goal_progress`, `chart_variation_format`, `dialog_delete_measurement_message`).
  - Aggiunte le risorse `unit_kg` ("kg") e `unit_lb` ("lb").
- **Grafico Evoluzione Peso (`WeightProgressChart.kt`)**:
  - `ChartMetric.WEIGHT` scala dinamicamente in base all'unità scelta (valori asse Y, tooltip e linea dell'obiettivo tracciati nell'unità attiva). Le metriche in centimetri restano invariate.
- **Trofei & AI Coach (`Trophy.kt`, `AppRepository.kt`)**:
  - Trofeo `weight_loss_1kg`: soglia interna conservata a 1.0 kg; descrizione localizzata resa dinamica in base all'unità (`1.0 kg` o `~2.2 lb`).
  - Prompt di sistema per l'AI Coach in `AppRepository.kt`: peso attuale e obiettivo comunicati all'assistente nell'unità scelta con il simbolo corretto.
- **Suite di Test Unitari (`WeightUnitConverterTest.kt`, `BackupSerializationTest.kt`)**:
  - Test di conversione andata e ritorno con stabilità matematica senza oscillazioni né drift (es. 175.2 lb).
  - Test di deserializzazione retrocompatibile per file JSON legacy privi di `weightUnit`.
  - Test di validazione range, formattazione con segno e rilevamento automatico del Locale.

---

## [1.0.58] - 2026-11-12

### 🎯 Misuratori a Segmenti Dimagrimento/Salute & Rinnovamento Schermata Coach Adattiva
- **Unica Fonte Dati Condivisa per Valutazioni Piani (`FastingPlan.kt`, `FastingPlanTemplates.kt`)**:
  - Introdotto il modello dati `PlanRatings(weightLoss: Int, health: Int)` (scala 1–4).
  - Centralizzata la mappa `PLAN_RATINGS` condivisa da tutte le schermate senza duplicazione di logica:
    - *20-4 (Dieta del guerriero)*: Dimagrimento 4, Salute 4
    - *16-8 (Classico)*: Dimagrimento 3, Salute 4
    - *18-6 (Avanzato)*: Dimagrimento 4, Salute 3
    - *Autophagy Booster*: Dimagrimento 4, Salute 4
    - *Luigi 1* (piano speciale Coach 20h/4h): Dimagrimento 4, Salute 4
- **Componente Riutilizzabile Misuratore a Segmenti (`SegmentedRatingBar.kt`)**:
  - Realizzato il componente Compose `SegmentedRatingBar` con 4 barrette arrotondate affiancate: azzurro `#22B8E8` per i segmenti attivi, blu scuro `#1F2F4B` per i segmenti inattivi.
  - Supporto configurabile per dimensioni contestuali (più grande nella schermata Coach, compatto con barrette ~14x6dp e testo 11sp nelle card dei piani).
- **Schermata Piani di Digiuno (`PlanPickerModal.kt`)**:
  - Aggiunta la riga "Dimagrimento ▰▰▰▰ · Salute ▰▰▰▰" in fondo alle card dei soli piani consigliati (es. Luigi 1, 16-8, 18-6, Autophagy Booster).
  - Separatore sottile da 0.5dp (`#22314F`) tra descrizione e misuratori.
  - Esclusione garantita per i piani personalizzati dell'utente (`category != PlanCategory.CUSTOM && !isCustom`).
- **Nuova Schermata Programmi Coach Adattiva (`PlanSelectionStartScreen.kt`)**:
  - Titolo "Programmi Coach" e sottotitolo "Il Coach ti guida lungo il tuo percorso".
  - **Tablet in Orizzontale ($\ge$ 840dp)**: Layout a 2 colonne:
    - *Colonna Sinistra*: Card in evidenza per il programma consigliato (16-8 Classico) con badge verde "Consigliato per iniziare", barra 24h (digiuno `#2A8FD0`, pasto `#1F9D72`), legenda, misuratori grandi Dimagrimento/Salute, e pulsante pieno azzurro `#22B8E8` ("Inizia settimana 1", testo scuro `#04202B`).
    - *Colonna Destra*: 3 card compatte in colonna (20-4, 18-6, Autophagy Booster con 7 pillole giornaliere alternate verde/blu), mini-barra 24h, misuratori piccoli e pulsante con contorno azzurro "Inizia".
  - **Tablet in Verticale / Schermi Medi (600–840dp)**: Colonna singola centrata con larghezza massima di 600dp.
  - **Smartphone (< 600dp)**: Colonna singola fluida a tutta larghezza.
  - **Sezione Piani Singoli**: Due tile compatte in fondo ("Piani singoli — Scegli tu il metodo di digiuno"): 16-8 Classico ("Salta la colazione") e Autophagy Booster ("Settimana intensa · Perdi peso").
  - Rimozione totale dell'arancione in favore dell'azzurro `#22B8E8`, card dark `#0F1B30` con bordo `#1C2D4A` (angoli 16dp).
  - Estrazione e localizzazione completa delle stringhe in `strings.xml` (default e `values-it`).

---

## [1.0.57] - 2026-11-12

### 📱 Supporto Nativo Tablet & Layout Adattivo Orizzontale & Verticale
- **Layout Tablet in Orizzontale (Landscape $\ge$ 840dp)**:
  - Introdotta la `NavigationRail` permanente a sinistra con branding dell'app e icone di navigazione M3 (`Digiuno`, `Io`, `Coach`, `Notifiche`, `Altro`).
  - Layout a 2 colonne affiancate:
    - **Colonna Sinistra**: Timer circolare con rapporto 1:1 e dimensione massima calibrata a 250dp, pulsante Stop/Start animato al neon, scheda delle fasi metaboliche (`FastingStagesCard`) e selettore rapido del piano.
    - **Colonna Destra**: Piano settimanale a 24 ore (`WeekBarChart`) esteso a tutta altezza (`fillMaxHeight = true`) con distribuzione uniforme `Arrangement.SpaceEvenly` dei 7 giorni, asse orario 0h-24h e indicatore dell'orario corrente in tempo reale.
- **Layout Tablet in Verticale & Schermi Medi (Portrait 600–840dp)**:
  - `NavigationRail` laterale a sinistra per massimizzare lo spazio verticale.
  - Contenuto principale centrato orizzontalmente con vincolo protettivo `widthIn(max = 600.dp)`, preservando proporzioni perfette e leggibilità ottimale.
- **Smartphone (< 600dp)**:
  - Layout compatto a colonna singola e `NavigationBar` inferiore 100% inalterati.
- **Nessuna Duplicazione di Stato**:
  - Tutta la logica reattiva, i timer, gli allarmi, le verifiche permessi e il controllo OTA rimangono unificati nel componente principale `TimerScreen`.

---

## [1.0.56] - 2026-11-12

### 🏪 Google Play Store Release (AAB) & Policy Compliance
- **Disattivazione Live Update per Rilascio Ufficiale**:
  - Impostato `enableLiveUpdate=false` in `gradle.properties`.
  - Escluso il permesso `REQUEST_INSTALL_PACKAGES` dal Manifest finale.
  - Reindirizzati i controlli degli aggiornamenti in-app direttamente al link ufficiale di Google Play Store (`market://details?id=com.cibotimer`).
- **Generazione Pacchetto Android App Bundle (`.aab`)**:
  - Compilato il bundle release firmato con keystore permanente `CN=CiboTimer`.
  - Piena conformità alle policy Google Play su Exact Alarms, Foreground Services e crittografia di esportazione.

---

## [1.0.54] - 2026-11-12

### ⏱️ Correzione Aggiornamento Indicatore Ora Main & Refresh Real-Time Widget
- **Indicatore Ora Attuale su Schermata Principale (`WeekBarChart.kt`, `TimerScreen.kt`)**:
  - Risolto il blocco dell'indicatore visivo dell'orario corrente (triangolo bianco e linea verticale sulla barra a 24 ore di Oggi): rimosso il caching statico immutabile (`remember` senza chiavi) e introdotto il calcolo dinamico in tempo reale ad ogni secondo collegato al ticker principale (`currentEpochMillis`).
  - Aggiunto fallback dinamico tramite `produceState` e aggiornamento automatico immediato del giorno corrente (`todayDayOfWeek`) per gestire senza interruzioni il passaggio della mezzanotte.
- **Aggiornamento Real-Time e Transizione Continua Widget (`FastingWidgetUpdater.kt`, `CiboTimerApp.kt`, `FastingAlarmReceiver.kt`, `FastingTimerService.kt`)**:
  - Risolto il blocco a "0h 0m" quando una finestra di digiuno o pasto scade con l'app in background: `computeState` richiama ora `syncOrAdvanceSession()`, garantendo la transizione automatica istantanea allo stato successivo prima del rendering.
  - Sincronizzazione precisa dei piani a più periodi (es. piano predefinito `Luigi 1` a 9 periodi) tramite `FastingCalculator.resolveCurrentScheduledPeriod()`.
  - Registrato un BroadcastReceiver a livello di `Application` per `ACTION_TIME_TICK` (ogni minuto a schermo acceso), `ACTION_SCREEN_ON` (all'accensione dello schermo), `ACTION_USER_PRESENT` (allo sblocco dello smartphone), `ACTION_TIME_CHANGED` e `ACTION_TIMEZONE_CHANGED`, assicurando che il widget mostri sempre il tempo residuo aggiornato senza consumi di batteria.
  - Aggiornamento istantaneo dei widget alla scadenza degli allarmi esatti (`FastingAlarmReceiver`), durante il ciclo del Foreground Service (`FastingTimerService`), al boot del sistema (`BOOT_COMPLETED`) e all'aggiornamento dell'applicazione (`MY_PACKAGE_REPLACED`).

---

## [1.0.51] - 2026-11-11

### 🚀 Flag Unico Live Update vs Google Play, Traduzione Inglese & Script Deploy
- **Flag di Configurazione Distribuzione (`gradle.properties`, `build.gradle.kts`)**:
  - Introdotto il flag globale `enableLiveUpdate=true` in `gradle.properties`.
  - Quando `enableLiveUpdate=true`, l'app attiva la gestione Live Update con download APK via DuckDNS e compila con il manifest `AndroidManifest-liveupdate.xml` che include `REQUEST_INSTALL_PACKAGES`.
  - Quando `enableLiveUpdate=false`, l'app disattiva il download APK, reindirizza gli aggiornamenti al Google Play Store ed esclude il permesso `REQUEST_INSTALL_PACKAGES` per la conformità alle policy Google Play.
- **Supporto Multi-Lingua Italiano & Inglese (`strings.xml`, `values-it/strings.xml`)**:
  - Localizzate tutte le stringhe dell'app con fallback predefinito in inglese per dispositivi con lingua non italiana.
  - Traduzione in inglese delle 7 fasi metaboliche in `FastingStage.kt` e `FastingCalculator.kt`.
- **Script di Deploy & Server Live Update (`git_exclude/live_update.py`)**:
  - Aggiornato lo script per operare direttamente dalla root del progetto GitHub `/root/github/cibotimer`.
  - Aggiunti comandi `--deploy` e gestione automatica del restart del servizio systemd `cibotimer-live-update.service`.

---

## [1.0.50] - 2026-11-11

### 🔄 Auto Backup Android Nativo, Condivisione File JSON & Reset Dati Locali
- **Rimozione Token Google Drive & Backup tramite Intent ACTION_SEND ([`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt), [`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Rimosso completamente il campo di input per l'OAuth Bearer Token manuale dalla schermata Impostazioni.
  - Mantenuto l'interruttore per il "Backup Automatico su Drive" integrato direttamente con il sottosistema Auto Backup nativo di Android.
  - Collegato il pulsante "Backup ora" e "Esporta e Condividi" all'Intent nativo `ACTION_SEND` con URI `FileProvider`, permettendo il salvataggio diretto del file JSON su Google Drive ("Salva su Drive"), Telegram, WhatsApp, Email e file manager.
- **Configurazione Auto Backup Nativo di Android ([`AndroidManifest.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/AndroidManifest.xml), [`backup_rules.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/xml/backup_rules.xml), [`data_extraction_rules.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/xml/data_extraction_rules.xml))**:
  - Verificato `android:allowBackup="true"` e aggiunti `android:fullBackupContent="@xml/backup_rules"` e `android:dataExtractionRules="@xml/data_extraction_rules"` nel tag `<application>`.
  - Configurate le regole XML per includere database Room/SQLite, file JSON di stato in `filesDir` e `sharedpref`, escludendo la cache temporanea.
- **Funzione di Reset Sicuro Dati Locali & AlertDialog di Avviso ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt), [`AppButtons.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/AppButtons.kt), [`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Creata la funzione `resetAllLocalData()` in `AppRepository` che cancella tutti i database Room/SQLite, azzera tutte le SharedPreferences, elimina ricorsivamente la cache interna/esterna, rimuove i file di stato JSON interni, azzera gli StateFlow in memoria ai valori di default, ferma il Foreground Service ongoing e cancella gli allarmi delle notifiche.
  - Aggiunto il pulsante stile avviso/pericolo `AppDangerButton` "Reset App / Elimina Dati Locali" nella nuova sezione "Area Pericolo & Reset" delle Impostazioni.
  - Introdotto un chiaro AlertDialog di conferma pre-eliminazione che avvisa l'utente della perdita definitiva dei dati non esportati.

---

## [1.0.49] - 2026-11-11

### 💬 Richiesta Orario Inizio Digiuno Prima della Creazione Piani Coach AI
- **Istruzione Mandatoria Pre-Generazione Piano ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt), [`ChatMessage.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/models/ChatMessage.kt))**:
  - Introdotta la regola vincolante nel prompt operativo del Coach AI: quando l'utente richiede di creare un nuovo piano di digiuno, l'assistente **non genera subito il blocco JSON**, ma **chiede sempre prima a che orario l'utente intende iniziare il digiuno** (o a che ora consuma abitualmente la cena/ultimo pasto della giornata).
  - Il blocco JSON (` ```fasting_plan ... ``` `) viene generato solo dopo aver raccolto la preferenza di orario dell'utente, oppure se l'orario di inizio era già stato fornito esplicitamente nella richiesta iniziale.
- **Verifica Unit Test ([`AiPlanParserTest.kt`](file:///root/src/cibotimer/cibotimer/app/src/test/java/com/cibotimer/AiPlanParserTest.kt))**:
  - Aggiunto test unitario che verifica la presenza e la conformità della regola di richiesta preventiva dell'orario di inizio nel prompt del Coach AI.

---

## [1.0.48] - 2026-11-11

### 🧠 Validazione Rigorosa Orari & JSON nei Piani Coach AI
- **Sanitizzazione & Autocorrezione Oraria Avanzata ([`OpenAiApiClient.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/remote/OpenAiApiClient.kt))**:
  - Implementato rilevamento e correzione automatica dell'inversione finestra pasti vs finestra digiuno: quando l'AI specifica erroneamente la finestra di alimentazione in `startHour`/`endHour` (es. 12:00 -> 20:00 [8h] con durata digiuno 16h [960m]), il parser inverte automaticamente gli estremi per impostare il digiuno dalle 20:00 alle 12:00.
  - Sincronizzazione matematica rigorosa: garantita al 100% l'uguaglianza `(end - start + 1440) % 1440 == fastingDurationMinutes` su ogni giorno di digiuno.
  - Conversione automatica della durata espressa in ore (`1..24`) in minuti totali (`ore * 60`).
  - Gestione pulita dei giorni di riposo/pasti liberi (`isFastingDay: false` con `duration = 0`).
  - Garanzia di completezza per tutti i 7 giorni della settimana (ISO 1=Lunedì a 7=Domenica).
- **Parser JSON Resiliente & Pulizia Sintattica ([`OpenAiApiClient.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/remote/OpenAiApiClient.kt))**:
  - Estrazione intelligente del JSON da blocchi markdown (` ```fasting_plan `, ` ```json `, ` ``` `) e fallback con scansione parentesi graffe bilanciate per JSON grezzo inline privo di backtick.
  - Rimozione preventiva di commenti su riga singola `//` e multilinea `/* */`, ed eliminazione automatica delle virgole finali (*trailing commas*) prima di parentesi graffe o quadre chiuse.
  - Configurazione Kotlinx Serialization con `isLenient = true` e `coerceInputValues = true`.
- **Rafforzamento System Prompt Coach AI ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Aggiunte regole operative tassative nel prompt dinamico: distinzione netta tra finestra digiuno e finestra pasto, verifica aritmetica interna obbligatoria prima dell'emissione, tabella di conversione ore-minuti, rispetto rigoroso delle preferenze utente (giorni liberi, orari pasti).
- **Nuova Card di Verifica Oraria & Palette Blu ([`AiChatScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/chat/AiChatScreen.kt))**:
  - Inserito badge di verifica *"Orari verificati e coerenti"* in `ProposedPlanCard` con riepilogo orario digiuno, finestra pasti, frequenza settimanale e giorni di riposo.
  - Sostituiti tutti i richiami a toni verdi con i token ufficiali `CiboTimerBlue` e `CiboTimerAzureGlow` in linea con il design system del progetto.

---

## [1.0.47] - 2026-11-11

### 📐 Rimozione Widget 1x1, Abbreviazione "Pross. digiuno" & Allineamento Testi in Alto
- **Rimozione Widget 1x1 ([`AndroidManifest.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/AndroidManifest.xml), [`FastingWidgetUpdater.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/FastingWidgetUpdater.kt))**:
  - Rimosso completamente il provider e layout 1x1, mantenendo esclusivamente il widget orizzontale compatto con tutte le informazioni e il pulsante di azione rapida.
- **Abbreviazione Etichette nella Schermata Principale ([`FastingStagesCard.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingStagesCard.kt), [`strings.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/values/strings.xml))**:
  - Abbreviata l'etichetta in alto a destra della card da *"Prossimo digiuno"* a **`"Pross. digiuno"`** (e fallback da *"Prossimo"* a *"Pross."*).
  - Impostato `maxLines = 1` e `overflow = TextOverflow.Ellipsis` per garantire che il testo occupi pochissimo spazio e non vada mai a capo né entri in collisione con la barra segmenti.
- **Allineamento Verticale in Alto ai Lati del Timer ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Allineata verticalmente in alto la scritta *"Digiuno"* a sinistra del timer (`Alignment.TopStart` con padding superiore calibrato).
  - Allineata verticalmente in alto anche la colonna di stato *"Adesso: Digiunare"* / *"Adesso: Mangiare"* a destra del timer (`Alignment.TopEnd`), garantendo simmetria e massima pulizia visiva attorno all'anello.

---

## [1.0.46] - 2026-11-11

### 📱 Doppi Widget Home "Digiuno 1x1" e "Digiuno 2x1" con Azione Rapida
- **Architettura Condivisa ([`FastingWidgetUpdater.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/FastingWidgetUpdater.kt), [`FastingWidget1x1Receiver.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/FastingWidget1x1Receiver.kt), [`FastingWidget2x1Receiver.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/FastingWidget2x1Receiver.kt))**:
  - Centralizzata l'intera logica di lettura stato, formattazione tempo residuo (`"3h 0m"`), orario prossimo digiuno (`"Digiuno alle 12 Mer"` / `"Digiuno 12 Mer"`), icone e gestione PendingIntent in `FastingWidgetUpdater`.
  - Introdotti due AppWidgetProvider dedicati con label nel manifest *"Digiuno 1x1"* e *"Digiuno 2x1"*, preservando la retrocompatibilità per le istanze widget preesistenti.
  - Aggiornamento coordinato e simultaneo di tutti i widget ad ogni evento, tick e cambio sessione.
- **Formato Compatto 1x1 ([`widget_info_fasting_1x1.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/xml/widget_info_fasting_1x1.xml), [`widget_fasting_1x1.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/layout/widget_fasting_1x1.xml))**:
  - Layout a colonna con padding 8dp: riga superiore con icona timer ciano 15dp e stato (*"Pasto"* / *"Digiuno"*), a destra pulsante circolare 28dp (area touch 40dp) con icona posate/fiamma; centro con tempo rimanente grande (24sp con autosize); riga inferiore con prossimo evento (*"Digiuno 12 Mer"*).
- **Formato Orizzontale 2x1 ([`widget_info_fasting_2x1.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/xml/widget_info_fasting_2x1.xml), [`widget_fasting_2x1.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/layout/widget_fasting_2x1.xml))**:
  - Layout orizzontale: icona timer ciano 22dp a sinistra, colonna centrale con *"Digiuno alle 12 Mer"* (14sp bianco) e *"Pasto in corso • 3h 0m"* (11sp secondario), e pulsante circolare 38dp non comprimibile a destra con icona 19dp.
- **Stile Dark Sapphire & Cyan ([`widget_bg.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/drawable/widget_bg.xml), [`widget_button_circle_bg.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/drawable/widget_button_circle_bg.xml))**:
  - Sfondo `#0B1B33`, bordo `#1C3A63`, angoli arrotondati 22dp, icone e contorni in ciano `#22D3EE`.

---

## [1.0.45] - 2026-11-11

### 📱 Widget Schermata Home a Metà Altezza (1 Cella Compatta)
- **Design Compatto Orizzontale a 1 Cella ([`cibotimer_widget_info.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/xml/cibotimer_widget_info.xml), [`widget_cibotimer.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/layout/widget_cibotimer.xml), [`CiboTimerWidgetProvider.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/CiboTimerWidgetProvider.kt))**:
  - Dimezzata l'altezza minima da `80dp` a `40dp` e `targetCellHeight` impostato a `1` (1 riga nella griglia del launcher invece di 2).
  - Riorganizzato il layout in modalità orizzontale compatta: icona a sinistra, colonna centrale con testo del prossimo periodo (`"Digiuno alle 16 Mer"` / `"Mangiare alle ore 14 Mer"`) e subtext con tempo residuo, e badge di stato a pillola sulla destra (`🍽️ MANGIARE` / `🔥 DIGIUNARE`).
  - Raggio degli angoli ottimizzato a 16dp per un inserimento visivo perfetto con la griglia dei widget Android e One UI.

---

## [1.0.44] - 2026-11-11

### 🗑️ Rimozione Catalogo, Priorità Samsung One UI, Tab Notifiche & Widget Home
- **Eliminazione Completa Sezione Catalogo ([`PlansCatalogScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/plans/PlansCatalogScreen.kt), [`PlanPickerModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/PlanPickerModal.kt), [`PlannerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/planner/PlannerScreen.kt), [`Navigation.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/Navigation.kt))**:
  - Eliminato del tutto il file dello schermo catalogo `PlansCatalogScreen.kt` e la relativa rotta `Screen.PlansCatalog`.
  - Rimossi il pulsante *"Apri catalogo completo"* e l'icona catalogo da `PlanPickerModal`.
  - In `PlannerScreen`, il pulsante *"Cambia piano"* apre direttamente il selettore rapido `PlanPickerModal`.
- **Risoluzione Notifica Samsung One UI ([`NotificationHelper.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/NotificationHelper.kt), [`FastingTimerService.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/FastingTimerService.kt))**:
  - Creato il canale `channel_cibotimer_timer_v4` con `IMPORTANCE_HIGH` e suono predefinito con `setOnlyAlertOnce(true)`: risolve il comportamento di Samsung One UI che declassava la notifica silenziosa in *"Altre notifiche"*.
  - Impostati `CATEGORY_STOPWATCH`, `setOngoing(true)`, `setAutoCancel(false)` e `FLAG_ONGOING_EVENT or FLAG_NO_CLEAR`: la notifica rimane saldamente tra le *"Notifiche di avviso"* e non può essere cancellata col tasto *"Cancella"*.
- **Nuovo Tab "Notifiche" ([`NotificationsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/notifications/NotificationsScreen.kt), [`Navigation.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/Navigation.kt), [`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Aggiunto il tab `Notifiche` nella bottom bar con storico avvisi categorizzati per emoji (fine digiuno, fine pasto, pre-avviso 15m, traguardi metabolici, acqua, peso).
  - Supporto per cancellazione della singola notifica e pulizia totale con dialog di conferma.
  - Card informativa per il timer live persistente con accesso rapido alle impostazioni di notifica di Android.
- **Widget Schermata Home per il Prossimo Periodo ([`CiboTimerWidgetProvider.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/widget/CiboTimerWidgetProvider.kt), [`widget_cibotimer.xml`](file:///root/src/cibotimer/cibotimer/app/src/main/res/layout/widget_cibotimer.xml))**:
  - Creato App Widget nativo per la schermata home con stile OLED dark e accenti ciano/blu.
  - Frase sul prossimo periodo calcolata dinamicamente: *"Digiuno alle 16 Mer"* (se in finestra pasto) oppure *"Mangiare alle ore 14 Mer"* (se in digiuno).
  - Badge di stato dinamico (`🍽️ MANGIARE` / `🔥 DIGIUNARE`) e subtext con tempo mancante.
  - Aggiornamento in tempo reale ad ogni variazione di stato, tick del timer e avvio app.

---

## [1.0.43] - 2026-11-10

### 🔔 Sincronizzazione Rinomina Piano nelle Notifiche & Notifiche Fisse ad Alta Priorità
- **Sincronizzazione Istantanea Nome Piano nelle Notifiche ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt), [`NotificationHelper.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/NotificationHelper.kt), [`FastingTimerService.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/FastingTimerService.kt))**:
  - Creata la funzione `renamePlan(planId, newName)` nel repository che aggiorna in tempo reale il nome in `_customPlans`, nel piano attivo `_activePlan`, e nella sessione in corso `_currentSession`.
  - Aggiornato istantaneamente il Foreground Service con il nuovo nome del piano e riprogrammati tutti gli allarmi di fine ciclo e promemoria con il nome aggiornato tramite `rescheduleAlarmsWithNewPlanName`.
  - Incluso il nome del piano nel titolo della notifica sia durante il digiuno che durante la finestra pasto (`CiboTimer • $planName • Finestra Pasto`).
- **Notifica Fissa & Alta Importanza su Android ([`NotificationHelper.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/NotificationHelper.kt), [`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Aggiornato il canale del timer live a `channel_cibotimer_live_v3` impostando `NotificationManager.IMPORTANCE_HIGH` (eliminando i canali obsoleti per sbloccare l'importanza su Android).
  - Impostati `NotificationCompat.PRIORITY_MAX`, `setOngoing(true)`, `setAutoCancel(false)` e i flag `FLAG_ONGOING_EVENT or FLAG_NO_CLEAR` per mantenere la notifica ancorata in cima alla tendina.
  - Aggiunta in Impostazioni l'azione rapida *"Configura Notifica Fissa di Sistema"* per accedere direttamente alla pagina di configurazione del canale Android.

---

## [1.0.42] - 2026-11-10

### 📐 Margine Sopra al Pulsante Start/Stop nella UI Principale
- **Spaziatura e Respirazione Superiore del Pulsante Start/Stop ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Calcolato il padding superiore del container delle fasi e del pulsante (`stagesBoxPaddingTop = buttonTopMargin + 12.dp`) per compensare la sporgenza dell'offset verso l'alto (`offset(y = -20.dp)`) rispetto al bordo superiore della card.
  - Risolto il contatto/sovrapposizione diretta tra il cerchio del timer e il pulsante Start/Stop, garantendo un margine visivo dedicato (14dp su schermi molto corti, 16dp su schermi corti e 20dp su schermi standard/ampi).
  - Preservata la proporzione dell'anello ingrandito e l'adattabilità della schermata senza causare overflow.

---

## [1.0.41] - 2026-11-10

### ✏️ Funzione "Rinomina Piano" nell'Anteprima dei Piani Personalizzati
- **Rinominabilità Esclusiva per Piani Personalizzati ([`PlanPickerModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/PlanPickerModal.kt), [`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Nella schermata *Anteprima piano* (`PlanPickerPreviewStep`), abilitata la funzione per rinominare il piano unicamente per i piani marcati come **Personalizzato** (`isCustomPlan == true`), lasciando inalterati i template standard predefiniti.
  - Aggiunta icona a matita `Icons.Default.Edit` in azzurro `CiboAccent` accanto al nome del piano nel titolo della card, con feedback al tocco per aprire direttamente il dialog.
  - Inserito un link/chip esplicito *"Rinomina questo piano"* con icona a matita azzurra sotto la descrizione della card per massima chiarezza e accessibilità.
  - Creato `AlertDialog` dedicato con campo `AppOutlinedTextField` precompilato con il nome attuale, pulsante *Salva* (`AppPrimaryButton`) e *Annulla* (`AppSecondaryButton`).
  - Il salvataggio aggiorna istantaneamente sia lo stato locale della schermata (`workingPlan`) sia la persistenza su disco nel repository (`repository.saveCustomPlan`), sincronizzando anche il piano attivo in caso di modifica del piano in corso.

---

## [1.0.40] - 2026-11-10

### 🎯 Correzione Colori Testo Pulsanti & Contrasto Card Impostazioni
- **Ereditarietà Dinamica del Colore del Testo nei Pulsanti ([`AppButtons.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/AppButtons.kt), [`Type.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/theme/Type.kt))**:
  - Rimossi i colori fissi hardcoded da `Type.kt` impostando `color = Color.Unspecified` per consentire ai composable `Text` di ereditare `LocalContentColor.current`.
  - In `AppPrimaryButton` e `AppSecondaryButton`, azzerato il colore esplicito sul testo con `style.copy(color = Color.Unspecified)`:
    - `AppPrimaryButton` ("Backup ora", "Esporta JSON", ecc.): testo e icona in blu notte molto scuro (`FastingBlueDarkText` `#04202E`) su sfondo azzurro `FastingBlue`, con contrasto perfetto e nitido.
    - `AppSecondaryButton` ("Gestisci Notifiche di Sistema", "Ripristina", "Importa JSON", ecc.): testo, icona e bordo in `FastingBlue` (`#38BDF8`) su sfondo trasparente.
  - Stati disabilitati configurati con trasparenza conforme (opacità 50% su testo/icona) senza mai risultare invisibili o confusi.
- **Miglioramento Contrasto Testi Descrittivi ([`Color.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/theme/Color.kt), [`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Aggiornato `TextSecondaryLight` a `#E2E8F0` garantendo un contrasto elevato (>11:1) sullo sfondo scuro delle card `#131821`.
  - Schiariti tutti i testi descrittivi nelle card delle sezioni Impostazioni.

---

## [1.0.39] - 2026-11-10

### 🎨 Design System Impostazioni: AppPrimaryButton, AppSecondaryButton & Controlli Uniformi
- **Token Colore FastingBlue Unificato ([`Color.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/theme/Color.kt))**:
  - Recuperato l'azzurro esatto della dicitura "Digiunare" (`#38BDF8`) e associato al token `FastingBlue`, sincronizzato con `CiboAccent` senza duplicazioni hex.
  - Introdotti `FastingBlueDarkText` (`#04202E`) per testo/icone ad alto contrasto su sfondo azzurro, `SlateGray` (`#6B7A90`) per bordi e switch spenti, e `TextSecondaryLight` (`#CBD5E1`) per testi descrittivi luminosi e leggibili.
- **Componenti Pulsanti Riutilizzabili a Pillola ([`AppButtons.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/AppButtons.kt))**:
  - `AppPrimaryButton`: altezza 48dp, forma pillola (RoundedCornerShape 50%), sfondo `FastingBlue`, testo e icona blu notte molto scuro (`#04202E`), stile `labelLarge` Medium, icona 18dp a sinistra con gap 8dp, stato disabilitato al 25% opacità con contrasto garantito > 4.5:1.
  - `AppSecondaryButton`: altezza 48dp, forma pillola, sfondo trasparente con bordo 1.5dp `FastingBlue`, testo e icona `FastingBlue`.
  - Icone semantiche assegnate: Backup ora (`CloudUpload`), Ripristina (`CloudDownload`), Esporta JSON (`Upload`), Importa JSON (`Download`), Notifiche (`Notifications`), Aggiornamenti (`Refresh`/`Download`).
- **Interruttori Switch & Campi di Testo Coerenti ([`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Switch: configurati con `appSwitchColors()`, spento con contorno e pallino grigio-azzurro `#6B7A90` e track trasparente; acceso con track `FastingBlue` e pallino blu notte scuro `#04202E`.
  - Campi di testo: introdotti tramite `AppOutlinedTextField` con angoli arrotondati a 12dp, bordo grigio-azzurro a riposo, bordo e label `FastingBlue` in focus e testo bianco puro.
  - Allineati i pulsanti affiancati nelle sezioni Google Drive ed Export Locale con identica larghezza (`weight(1f)`) e spazio costante di 12dp.

---

## [1.0.38] - 2026-11-10

### 💍 Anello Timer Ingrandito & Spaziatura Card Costante a 8dp
- **Anello Timer Ingrandito + Tratto e Tipografia in Proporzione ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt), [`FastingCircularProgress.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingCircularProgress.kt))**:
  - Aumentato il diametro dell'anello fino a 216dp (+16-20% in proporzione).
  - Tratto dell'anello incrementato da 11dp a 13.5dp con raggio e bagliori metabolici proporzionati.
  - Cifre tabulari del timer scalate a 35sp bold (senza andare a capo, `softWrap = false`), badge a 12sp e stato a 13sp.
  - Verificate le etichette laterali ("Digiuno" a sinistra e "Adesso: Digiunare" a destra) per evitare qualsiasi sovrapposizione.
- **Spaziatura Costante 8dp tra le Card ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Rimossa la distribuzione dispersiva `Arrangement.SpaceBetween` e gli Spacer intermedi sparsi.
  - Introdotto `Arrangement.spacedBy(8.dp)` uniforme nella `Column` principale.
  - Spazio tra la card Inizio/Fine, la card "Piano: ..." e il "Piano settimanale" esattamente di 8dp.
  - Il pulsante Start/Stop resta perfettamente agganciato al bordo superiore nel notch della card Inizio/Fine.
  - Il Piano settimanale e l'intero asse 0h-24h rimangono completamente visibili senza bisogno di scroll.

---

## [1.0.37] - 2026-11-10

### 📐 Ottimizzazione UI Dashboard Compatta & Nuovo Marcatore Piano Settimanale
- **Compattazione Verticale Zero-Scroll ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Ridotti i margini e gli spacer verticali (spazio anello ~-12dp, spacer anello/card/piano a 7-8dp).
  - L'intero contenuto della dashboard e del piano settimanale con l'asse orario 0h..24h rientra perfettamente nello schermo senza tagli né necessità di scroll.
- **Card Piano Settimanale & Testi Minimo 11sp ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt))**:
  - Calibrato il padding verticale interno a ~8dp e compattata l'altezza di ogni riga giorno del ~15-20%.
  - Portata la dimensione di tutti i testi e orari ad almeno 11sp garantendo nitidezza e piena leggibilità.
- **Nuovo Marcatore Giorno Corrente ad Alta Visibilità ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt))**:
  - Sostituita la vecchia freccia con un triangolo bianco pieno rivolto verso il basso (12dp x 8dp) posizionato subito sopra la barra 24h.
  - Aggiunta una linea verticale bianca spessa 2dp che attraversa la barra con sporgenza di 3dp sopra e sotto.
  - Aumentato lo spessore della barra del giorno corrente a ~12dp (rispetto a ~8dp per gli altri giorni), mantenendo perfetto contrasto sia sul digiuno che sul pasto.

---

## [1.0.36] - 2026-11-10

### 💍 UI Dashboard Ottimizzata: Anello Espanso, Titoli Laterali & Notifiche in Impostazioni
- **Anello Digiuno Ingrandito ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt), [`FastingCircularProgress.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingCircularProgress.kt))**:
  - Eliminato l'header superiore separato guadagnando 44dp di spazio verticale utile.
  - Incrementata la dimensione dell'anello circolare principale fino a 200dp (160dp/180dp/200dp in base all'altezza dello schermo), con maggiore respiro per countdown e percentuali interne.
  - Resa dinamica la dimensione interna di `FastingCircularProgress` rimuovendo il vincolo hardcodato a 186dp.
- **Titoli Laterali Centrati Verticalmente ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Scritta **"Digiuno"** riposizionata a sinistra dell'anello, perfettamente centrata in verticale.
  - Dicitura di stato **"Adesso: Digiunare"** / **"Adesso: Mangiare"** riposizionata a destra dell'anello, centrata in verticale e con allineamento pulito.
- **Icona Notifiche Trasferita in Impostazioni ([`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Rimosso il pulsante notifiche dall'header della dashboard per liberare spazio prezioso.
  - Aggiunta l'icona delle notifiche nella `TopAppBar` di `SettingsScreen` per aprire istantaneamente le impostazioni notifiche di sistema.
  - Aggiunto il pulsante "Gestisci Notifiche di Sistema" all'interno della card "Notifiche & Promemoria" delle Impostazioni.

---

## [1.0.35] - 2026-11-10

### 📱 Layout Adattivo Dashboard & Settimana Completa (Lun–Dom)
- **Visibilità Completa 7 Giorni ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt), [`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt))**:
  - Implementato `BoxWithConstraints` per adattare in modo reattivo l'anello del timer (144dp/160dp/176dp), i margini e le schede all'altezza esatta di qualsiasi schermo o risoluzione.
  - Compattata l'altezza di ogni riga orizzontale in `WeekBarChart` (~195dp totali) con spaziatura proporzionata, eliminando la compressione `weight(1f)` che tagliava il sabato e la domenica.
  - Abilitato lo scroll di sicurezza (`verticalScroll`) per preservare sempre l'accessibilità totale di tutti i componenti anche in split-screen o con font di sistema ingranditi.
  - Tutti i 7 giorni (Lun..Dom) con le rispettive diciture orarie sono ora sempre visibili al 100%.

---

## [1.0.34] - 2026-11-10

### 📊 Piano Settimanale a Barre Orizzontali & Ottimizzazione Modifica Piano
- **Sostituzione Capsule Verticali con Barre Orizzontali ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt))**:
  - Addio definitivo alle capsule verticali: introdotta la visualizzazione unificata a 7 istogrammi orizzontali 0-24h (Lun..Dom) con asse orario e legenda Pasto / Digiuno.
  - Aggiunta sotto ogni singolo giorno la dicitura chiara con gli intervalli orari completi (es. `"Lun 20:00 → Mar 12:00"`).
  - Indicatore dinamico dell'orario corrente sul giorno "Oggi" con cursore luminoso.
- **Risoluzione Layout Modale Modifica Piano ([`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt))**:
  - Rimossa la sezione centrale ridondante dell'elenco testuale dei periodi ("Sezione centrale: lista completa dei periodi settimanali").
  - Ridimensionato il grafico orizzontale su altezza compatta (~245dp) per evitare che occupi tutto lo schermo.
  - La scheda di controllo inferiore (`BottomEditorControlCard`: Inizio/Fine, Sposta periodi, Durata) è ora immediatamente visibile e accessibile direttamente sotto il piano senza scroll.
  - Selezione interattiva del giorno con evidenziazione neon ciano e aggiornamento dinamico del selettore d'ambito ("Periodo X" / "Tutti i").

---

## [1.0.33] - 2026-11-10

### 📱 Ottimizzazioni Interfaccia & App Info
- **Header Informativo Impostazioni ([`SettingsScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/settings/SettingsScreen.kt))**:
  - Aggiunta schermata info app in cima alle impostazioni con logo CiboTimer a 88dp, titolo e versione dinamica dell'applicazione (`BuildConfig.VERSION_NAME`).
- **Fix Anteprima Piani & Fasi del Digiuno ([`PlanPickerModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/PlanPickerModal.kt), [`FastingStagesCard.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingStagesCard.kt))**:
  - Rifiniture grafiche all'anteprima modale dei piani di digiuno e miglioramento dell'indicatore visivo delle fasi metaboliche.
- **Ciclo Continuo del Timer & Indicatore Capsula (Release Deploy)**:
  - Consolidamento del ciclo continuo e live deploy tramite server in-app e push repository.

---

## [1.0.32] - 2026-11-10

### ⏱️ Ciclo Continuo del Timer (Zero Interruzioni)
- **Transizione Continua Digiuno ↔ Alimentazione ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt), [`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Il timer **non si ferma mai** tra la finestra di digiuno e quella di alimentazione o viceversa.
  - Al termine del digiuno: archiviazione automatica della sessione completata nello storico, valutazione dei trofei, e passaggio immediato al countdown della **Finestra di Alimentazione (MANGIARE)** con notifica dedicata.
  - Al termine della finestra pasto: passaggio automatico alla nuova sessione di **Digiuno Programmato (DIGIUNARE)** con notifica e schedulazione delle fasi metaboliche.
  - Funzione `syncOrAdvanceSession()` che assicura l'allineamento automatico e istantaneo in tempo reale e alla riapertura dell'applicazione, anche a seguito di arresto del processo o deep sleep.
  - Servizio in foreground permanente (`FastingTimerService`) con notifica dinamica continua sia per il digiuno che per la finestra pasto (`🍽️ Pasto aperto fino alle HH:mm`) con cronometro hardware in live countdown.

### 🎨 Indicatore a Freccia della Capsula 24 Ore Rinnovato
- **Grafica e Posizionamento Ottimizzati ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt), [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt))**:
  - Posizionamento **più a sinistra**: la base della freccia è ora allineata all'esterno del bordo capsula (`-7dp`), con la punta di precisione che tocca la linea temporale a `+2dp`.
  - Sagoma aerodinamica a dardo/chevron con elegante intaglio posteriore (`arrowIndent`).
  - Gradiente cromatico radiante da Ciano Neon (`CiboTimerAzureGlow`) a Bianco brillante con bordo scuro OLED ad alto contrasto e alone neon soft.
  - Micro-gemma luminosa centrale alla base della freccia per una resa grafica moderna, pulita e di precisione.
  - Applicato in modo identico sia sul grafico settimanale della dashboard principale sia sul grafico del `TimeEditorModal`.

### 💙 Palette CiboTimer "Dal Blu all'Azzurro"
- Aggiornato l'anello progressivo ([`FastingCircularProgress.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingCircularProgress.kt)) per la finestra pasto, sostituendo ogni riferimento a toni verdi con le sfumature azzurre e ciano del design system.

---

## [1.0.31] - 2026-11-09

### 👤 Sezione "Io" Completamente Rinnovata (Modello BodyFast)
- **Tracciamento Avanzato Obiettivi e Peso ([`ProgressScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/progress/ProgressScreen.kt), [`BodyStatsCalculator.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/BodyStatsCalculator.kt))**:
  - Panoramica a 3 colonne ad alto contrasto: **Iniziale**, **Attuale** (hero cyan neon), **Obiettivo**.
  - Badge dedicato con freccia dinamica che traccia con esattezza i **kg persi** (es. `-3.8 kg`) e i **kg rimanenti** al raggiungimento del traguardo.
  - Barra di progresso dell'obiettivo graduata con percentuale di completamento (`38% verso l'obiettivo`) e celebrazione *"🎉 Obiettivo Raggiunto!"*.
  - Dialog di modifica degli obiettivi ([`GoalEditorDialog.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/GoalEditorDialog.kt)) per aggiornare peso iniziale, peso obiettivo e altezza con calcolo del BMI target in tempo reale.

- **Grafico Evolutivo Interattivo nel Tempo ([`WeightProgressChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeightProgressChart.kt))**:
  - Selettore di intervallo temporale: **1 Mese**, **3 Mesi**, **Tutto** (in stile BodyFast).
  - Selettore di metrica: **Peso (kg)**, **Vita (cm)**, **Fianchi (cm)**, **Torace (cm)**, **Cosce (cm)**, **Braccia (cm)**.
  - Curva morbida azzurro neon con area riempita da gradiente sfumato ed evidenziazione dei punti di pesata.
  - Linea orizzontale tratteggiata per l'**Obiettivo** con badge laterale dedicato per monitorare la convergenza.
  - Interazione touch: tocco/trascinamento per ispezionare punto per punto con tooltip fluttuante recante data esatta, peso e variazione.

- **Misure Corporee & Circonferenze ([`BodyStatsCalculator.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/BodyStatsCalculator.kt), [`MeasurementEntryDialog.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/MeasurementEntryDialog.kt))**:
  - Monitoraggio completo delle 5 circonferenze corporee: **Fianchi**, **Vita**, **Torace**, **Cosce** e **Braccia**.
  - Calcolo del delta centimetri persi rispetto alla prima misurazione per ciascuna parte del corpo.
  - **Rapporto Vita/Fianchi (WHR - Waist-to-Hip Ratio)**: calcolato istantaneamente con categorizzazione del rischio cardiometabolico e viscerale secondo i parametri dell'OMS.
  - **Circonferenza Totale Corporea**: somma delle circonferenze con indicazione dei centimetri persi complessivi.
  - Modal di inserimento e modifica misurazioni con calcolo WHR in diretta e supporto per note e retrodatazione.

- **Pulsante & Modal "Condividi Progresso" ([`ShareProgressModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/ShareProgressModal.kt))**:
  - Pulsante visibile nell'AppBar e card dedicata per condividere i risultati.
  - Scheda modale con anteprima grafica accattivante (badge CiboTimer, kg persi, percentuale obiettivo, ore totali digiunate, sessioni concluse, centimetri persi).
  - Integrazione nativa con Android `Intent.ACTION_SEND` e `createChooser` per condividere su social, WhatsApp, Telegram, email, ecc.

- **Design Dark OLED & Nessun Riferimento al Tracker Acqua**:
  - Interfaccia coerente con la palette CiboTimer dal blu cobalto all'azzurro ciano neon su sfondo OLED Dark Sapphire.
  - Rimosso qualsiasi riferimento superfluo all'acqua nella schermata "Io".

---

## [1.0.30] - 2026-11-09

### 🛡️ Vincolo Invalicabile del Periodo Anteriore nella Modifica del Piano
- **Rigorosa Impossibilità di Superare il Periodo Anteriore ([`PlanShiftEngine.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/PlanShiftEngine.kt), [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt))**:
  - Implementata la funzione `getPeriodBounds` che calcola i limiti temporali e i vincoli rispetto ai periodi adiacenti (ispirata alla funzione BodyFast `#17838 getPeriodBounds`).
  - Quando si retrocede l'orario di un periodo con gli stepper `-1h` o `-15m` o tramite TimePicker, l'orario di inizio **non può mai oltrepassare la fine del periodo anteriore** (e l'orario di fine non può oltrepassare l'inizio del successivo).
  - I pulsanti stepper `-1h` e `-15m` si disabilitano automaticamente una volta raggiunto il confine con il periodo anteriore.
  - Selezionando un orario tramite TimePicker prima del periodo anteriore, viene mostrato un Toast di avviso e l'orario viene bloccato alla fine del periodo precedente.
  - Aggiunti indicatori visivi minimi e massimi (`≥ lun 13:00`, `≤ mar 11:00`) nella scheda inferiore di modifica.

### ⏳ Fasi Metaboliche con Conteggio Ore Passate & Finestra Pasto Dinamica
- **Visualizzazione Ore Passate nelle Fasi Metaboliche ([`FastingStagesCard.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingStagesCard.kt), [`FastingStagesSheet.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingStagesSheet.kt))**:
  - La card delle fasi indica chiaramente il tempo trascorso: ad es. *"Fase 3: Combustione Grassi (9h 15m passate)"*.
  - La barra delle fasi supporta tutte le tappe metaboliche di `FastingCalculator.STAGES` (Digestione, Calo Insulina, Combustione Grassi, Chetosi, Autofagia, Ormone della Crescita, Rigenerazione Immunitaria): le fasi passate sono piene al 100%, la fase in corso avanza dinamicamente e le future rimangono vuote.
  - Nel foglio di dettaglio (`FastingStagesSheet`) ogni fase indica lo stato temporale: *"Completata • Superata da Xh"*, *"In corso • Xh Ym passate (mancano Zh)"* o *"Tra Xh"*.
  - La notifica permanente FGS riporta in tempo reale le ore passate e la fase metabolica attiva.
- **Finestra Pasto Completa di Ore Passate e Rimanenti ([`FastingCalculator.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/FastingCalculator.kt), [`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - `resolveCurrentScheduledPeriod` calcola inizio, fine, tempo trascorso (`elapsedSeconds`), tempo rimanente (`remainingSeconds`) e frazione di progresso anche per l'intera durata della finestra di alimentazione.

### 🔔 Notifiche di Transizione Finestra Pasto ⇄ Digiuno
- **Notifiche Immediate e Allarmi Esatti di Passaggio ([`NotificationHelper.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/NotificationHelper.kt), [`FastingAlarmReceiver.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/notifications/FastingAlarmReceiver.kt), [`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Allarme esatto `RTC_WAKEUP` al completamento del digiuno: *"🍽️ Finestra di Alimentazione Aperta! Complimenti! Hai completato il tuo digiuno. Puoi mangiare fino alle HH:mm."*
  - Allarme esatto `RTC_WAKEUP` alla chiusura della finestra pasto: *"🔥 Inizia il Digiuno Intermittente! La tua finestra pasto è terminata. Inizia il tuo digiuno programmato. Ricordati di bere acqua!"*
  - Promemoria anticipato 15 minuti prima dell'inizio del digiuno: *"⏰ Finestra Pasto in Chiusura: Mancano 15 minuti all'inizio del digiuno."*
  - Canale notifiche ad alta priorità con vibrazione e visibilità lockscreen.

---

## [1.0.29] - 2026-11-09

### 💾 Funzionalità "Salva con Nome" (Save As) nella Modifica Manuale del Piano
- **Salva con Nome Nativo nell'Editor Manuale ([`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt))**:
  - Quando si modifica un piano a mano nel modale di regolazione orari, il pulsante primario nell'header mostra ora **"Salva con nome"** con icona segnalibro (`Icons.Default.BookmarkBorder`).
  - Toccando il pulsante o tentando di chiudere con modifiche pendenti, viene mostrato il dialogo dedicato **"Salva con nome"** (`SaveAsDialog`).
  - Campo di testo modificabile con precompilazione intelligente (es. *"[NomePiano] Personalizzato"* o il nome corrente se già personalizzato) e campo note opzionale.
  - **Tripla Scelta di Salvataggio**:
    - **"Salva e Attiva"**: genera un nuovo ID univoco (`custom-xxxxxxxx`), imposta il piano come personalizzato e lo attiva immediatamente nel timer e nel pianificatore.
    - **"Solo Salva"**: salva il piano nei piani personalizzati senza cambiare il piano attualmente attivo.
    - **"Sovrascrivi"** (disponibile se il piano era già personalizzato): aggiorna il piano esistente in-place senza duplicare la card nel catalogo.
- **Integrazione nei Flussi Applicativi ([`PlannerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/planner/PlannerScreen.kt), [`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt), [`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - `updateActivePlan` in `AppRepository` aggiorna automaticamente anche la lista `customPlans` se il piano aggiornato è personalizzato.
  - Feedback Toast contestuale per ogni azione di salvataggio.
- **Test Unitari Dedicati ([`PlanManagementTest.kt`](file:///root/src/cibotimer/cibotimer/app/src/test/java/com/cibotimer/PlanManagementTest.kt))**:
  - Aggiunto test `testSaveAsCreatesNewCustomPlanWithUniqueIdAndName` che valida l'assegnazione del nuovo ID, del nome custom, della categoria e l'indipendenza dal piano originale.

### 🎯 Replica Algoritmo Modifica BodyFast & Sanificazione Piani AI (18h Picco & Settimana fino a Domenica)
- **Replica Fedele Algoritmo BodyFast senza Effetto Cascata ([`PlanShiftEngine.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/PlanShiftEngine.kt), [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt))**:
  - Dall'analisi REA/Hermes sul bytecode di BodyFast (`getPeriodBounds`, `shiftImmutablePeriod`, `expandPeriod`), la modifica di un record/periodo o giorno agisce esclusivamente sul record selezionato, senza alterare o spostare il giorno o periodo successivo.
  - Rimossa la propagazione a catena forzata (`cascadeNext = false`, `cascadeNextDay = false` come predefinito): la modifica degli orari di inizio/fine o della durata di un giorno preserva rigorosamente intatti i giorni successivi.
  - Sincronizzazione atomica dei giorni da periodi in `syncDaysFromPeriods` con associazione diretta al giorno di partenza.
- **Settimana Ciclica sempre fino a Domenica (7 Giorni) ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/WeekBarChart.kt), [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/TimeEditorModal.kt), [`FastingCalculator.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/domain/FastingCalculator.kt))**:
  - Eliminata la presenza dell'ottava capsula duplicata: l'istogramma 24 ore e l'editor mostrano sempre esattamente i 7 giorni della settimana da Lunedì a Domenica (`Lun`, `Mar`, `Mer`, `Gio`, `Ven`, `Sab`, `Dom`), terminando sempre regolarmente la domenica.
  - Calcolo seamless dei segmenti 24 ore in `computePeriods24hSegments` con wrapping modulare continuo della settimana ciclica (es. il digiuno iniziato domenica notte alle 20:00 prosegue naturalmente il lunedì mattina fino alle 12:00 senza discontinuità).
- **Risoluzione & Sanificazione Generazione Piani Coach AI ([`OpenAiApiClient.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/remote/OpenAiApiClient.kt), [`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Risolto il problema in cui richieste come *"modifica il piano attuale, voglio al massimo 18 ore di picco per tutti i giorni"* generavano piani con durata 0 ore per tutti i giorni.
  - Nuovo modulo `sanitizePlan` in `OpenAiApiClient`: auto-ripara e ricalcola matematicamente `fastingDurationMinutes` se l'LLM omette la durata, inserisce 0 o fornisce le ore invece dei minuti (es. 18 convertito in 1080 minuti).
  - Validazione e sincronizzazione automatica dei giorni della settimana (ISO 8601, 1=Lunedì .. 7=Domenica) con completamento dei giorni mancanti.
- **Suite di Test Unitari Dedicata ([`AiPlanParserTest.kt`](file:///root/src/cibotimer/cibotimer/app/src/test/java/com/cibotimer/AiPlanParserTest.kt), [`PlanShiftEngineTest.kt`](file:///root/src/cibotimer/cibotimer/app/src/test/java/com/cibotimer/PlanShiftEngineTest.kt))**:
  - Test automatici per auto-healing del digiuno da 18 ore con durata 0, conversione ore->minuti, preservazione giorni successivi durante modifiche singole e wrapping domenica/lunedì.

### 🧠 Consapevolezza del Piano Attivo nel Coach AI & Gestione Piani con Eliminazione
- **Consapevolezza Completa del Piano Attivo nel Coach AI ([`AppRepository.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/repository/AppRepository.kt))**:
  - Il Coach AI riceve dinamicamente nel system prompt i dettagli integrali del piano attualmente attivo dell'utente: nome piano, categoria, descrizione, orari settimanali o periodi multipli (es. per il piano Luigi 1), stato della sessione di digiuno in corso e profilo dell'utente.
  - Se l'utente chiede al Coach modifiche o variazioni sugli orari, il Coach modula la proposta sul piano reale e allega il blocco JSON ```fasting_plan```.
- **Salvataggio Piani con Altro Nome o Ridenominazione & 'Solo Salva' ([`AiChatScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/chat/AiChatScreen.kt))**:
  - Se l'utente chiede al Coach in chat di salvare il piano con un nome specifico (es. "salvalo come 'Piano Relax'"), il Coach assegna fedelmente quel nome nel campo "name".
  - Nuova opzione **"Solo Salva"** vs **"Applica e Attiva"**: l'utente può scegliere di salvare il piano proposto tra i propri piani personalizzati senza attivarlo subito, oppure attivarlo immediatamente.
  - Nuova opzione **"Rinomina o salva con altro nome..."** nella card del piano proposto (`ProposedPlanCard`): finestra modale con campo di testo e doppia scelta ("Solo Salva" o "Salva e Attiva") con nome personalizzato.
- **Eliminazione Piani con Dialogo di Conferma ([`PlansCatalogScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/plans/PlansCatalogScreen.kt), [`PlanPickerModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/PlanPickerModal.kt), [`PlannerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/planner/PlannerScreen.kt))**:
  - Aggiunta icona cestino (`Icons.Default.DeleteOutline`) in rosso brillante (`CiboStopRed`) per tutti i piani personalizzati nel catalogo, nel selettore rapido e nella schermata di dettaglio.
  - Dialog di conferma irreversibile ("Elimina Piano") con avvertimento automatico nel caso in cui il piano da eliminare sia quello attualmente in uso (con fallback automatico al piano 16-8 Classico).

## [1.0.26] - 2026-11-09

### 🤖 Potenziamento AI Chat Completo con Tecnologie MiniiChat
- **Rendering Markdown Jetpack Compose Nativo ([`MarkdownText.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/MarkdownText.kt))**:
  - Implementato parser markdown lightweight zero-dipendenze ispirato all'architettura di MiniiChat.
  - Supporto per intestazioni H1-H3, elenchi puntati e numerati, citazioni blockquote, divisori orizzontali e formattazione inline (grassetto, corsivo, codice inline, barrato, link evidenziati).
  - Blocchi di codice sintattici avanzati con testata scura Sapphire, indicatore del linguaggio di programmazione e pulsante interattivo "Copia codice" con feedback visivo.
- **Server-Sent Events (SSE) Streaming & Thinking Block ([`OpenAiApiClient.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/remote/OpenAiApiClient.kt))**:
  - Streaming real-time token-by-token su protocollo OkHttp SSE con parsing continuo riga per riga di `data:`.
  - Supporto per modelli di ragionamento avanzati (DeepSeek R1, OpenAI o1/o3, ecc.) con rilevamento di `reasoning_content` e tag `<think>...</think>`.
  - Blocco a fisarmonica espandibile/collassabile (`ThinkingAccordionBlock`) con animazione ciano/zaffiro e badge contatore di caratteri del ragionamento.
  - Pulsante dinamico Interrompi/Stop generazione (`Icons.Default.Stop`) durante lo streaming per annullare immediatamente il job di ricezione.
- **Preset Provider Multi-Modello ([`ProviderPresets.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/data/models/ProviderPresets.kt))**:
  - 10 configurazioni preimpostate pronte all'uso: OpenAI, DeepSeek, OpenRouter, Groq, Google Gemini, Mistral AI, Together AI, Ollama (localhost:11434), LM Studio (localhost:1234) e Custom endpoint.
  - Selettore rapido del modello direttamente nella TopBar tramite Chip interattivo e BottomSheet (`ModelPickerBottomSheet`).
  - Recupero automatico dei modelli disponibili dal provider via endpoint `/v1/models`.
- **Interfaccia Utente Chat Rinnovata ([`AiChatScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/chat/AiChatScreen.kt))**:
  - Barra messaggi con azioni rapide al tap: Copia risposta, Rigenera risposta, Elimina messaggio.
  - Chip con prompt rapidi di coaching nutrizionale e digiuno intermittente.
  - Rilevamento automatico di piani alimentari proposti dall'AI nel testo JSON con scheda interattiva `ProposedPlanCard` e pulsante "Applica questo Piano".
  - Indicatore di digitazione animato a tre impulsi (`TypingDotsIndicator`) e palette OLED Sapphire / Cobalto / Ciano coerente con il design system.

## [1.0.25] - 2026-11-09

### ✨ Bagliore Ottico a 5 Livelli sull'Anello & Layout Dashboard Raffinato
- **Bagliore Ottico Multi-Strato (5 Livelli di Luce)**:
  - Potenziato l'indicatore gemma su [`FastingCircularProgress.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/components/FastingCircularProgress.kt) con 5 layer concentrici: alone diffuso esterno da 13dp, bagliore ciano da 8.5dp, nucleo colorato da 5.5dp, alone bianco da 4.2dp e centro brillante puro da 3.0dp.
- **Riorganizzazione Dashboard & Scheda Fasi ([`TimerScreen.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/cibotimer/ui/screens/timer/TimerScreen.kt))**:
  - Nuova scheda Fasi metaboliche arricchita posizionata sopra al grafico settimanale con icona ingrandita e dettagli descrittivi.
  - Barra orizzontale full-width del piano corrente con pulsante rapido "Cambia".
  - Pulsante Stop con accento cromatico rosso vivo ad alta visibilità (`CiboStopRed`).

## [1.0.24] - 2026-11-09

### 🚀 Deploy & Release Completa CiboTimer
- **Package Refactoring & Unificazione Namespace**:
  - Unificato l'applicationId e il namespace Android a `com.cibotimer`.
  - Aggiornati Application class (`CiboTimerApp`), Activity principale (`MainActivity`) e intent receiver.
- **Firma Permanente & Build Verificata**:
  - Compilazione e packaging eseguiti con container builder dedicato.
  - Firma crittografica verificata con keystore permanente `CN=CiboTimer`.
  - Nuova build v1.0.24 pronta per l'aggiornamento in-place e distribuzione OTA.

## [1.0.23] - 2026-11-09

### 💫 Anello del Timer nella UI Principale Ispirato alla Nuova Icona
- **Sincronizzazione Grafica Completa tra Icona e Timer Principale ([`FastingCircularProgress.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/FastingCircularProgress.kt))**:
  - **Aura Ambientale Centrale**: gradiente radiale luminescente in ciano zaffiro (`#00E5FF` ad alpha diffusa) che fa fluttuare le cifre del countdown su sfondo OLED scuro.
  - **Orbita Esterna a 24 Ore con 4 Punti Cardinali**: anello concentrico esterno tratteggiato in zaffiro con 4 gemme ciano a ore 12, 3, 6 e 9.
  - **Traccia Sapphire Twilight**: base dell'anello in blu notte profondo (`#112338`) con stroke da 11dp per un contrasto visivo assoluto.
  - **Stato FERMO (Identico all'Icona)**: quando il timer è in standby, l'anello visualizza il cronometro metabolico 16:8 completo:
    - Arco di Digiuno (240°) in blu cobalto (`#0284C7`) con segmento superiore in sky azure (`#38BDF8`).
    - Arco Finestra Pasto (120°) in azzurro ciano neon (`#00E5FF`) con alone luminescente.
    - **Puntino Indicatore a Sinistra (Ore 9)**: gemma attiva con alone ciano pulsante (breathing animation continua a zero consumo energetico), perlina ciano e centro bianco speculare.
  - **Stato ATTIVO (Countdown in Movimento)**:
    - Multi-layer neon glow a 3 livelli (underlay glow 17dp, mid glow 14dp, core arc 11dp con stroke arrotondato).
    - **Puntino Indicatore Dinamico**: la gemma speculare viaggia in tempo reale sulla testa dell'arco di countdown, indicando visivamente il punto esatto di avanzamento.
  - **Colori dei Badge Aggiornati**: badge DIGIUNARE in ciano neon (`#00E5FF`), MANGIARE in verde smeraldo (`#10B981`) e FERMO in slate neutro.

## [1.0.22] - 2026-11-09

### 🍽️ Nuova Icona Adattiva Material 3: Posate Incrociate & Puntino Indicatore a Sinistra
- **Nuovo Glifo Vettoriale con Posate Incrociate (Forchetta & Cucchiaio a 45°)**:
  - **Forchetta Material 3**: rifinita in azzurro ciano neon (`#00E5FF`) con 3 rebbi geometrici ben distanziati, sagomatura affusolata e highlight speculare centrale in bianco ghiaccio (`#FFFFFF`), a rappresentare la finestra di alimentazione attiva.
  - **Cucchiaio Material 3**: sagomato in blu cobalto (`#0284C7`), conca affusolata con profondità interna in zaffiro scuro (`#0077B6`) e bordo celeste brillante (`#38BDF8`), a richiamare la finestra di digiuno.
  - **Stacco Visivo 3D all'Incrocio**: scanalatura scura di interruzione ad alta precisione (`#060B12`) tra forchetta e cucchiaio per una netta percezione di profondità e tridimensionalità.
- **Puntino Indicatore all'Inizio dell'Anello a Sinistra (Ore 9)**:
  - Posizionato l'indicatore gemma attiva esattamente all'inizio dell'arco a sinistra (`cx=27, cy=54`, ore 9:00):
    - Alone luminescente ciano neon a doppio raggio (`r=5.8dp`, alpha 0.28).
    - Perlina ciano centrale ad alta brillantezza (`r=3.4dp`).
    - Punto luce speculare bianco puro al centro (`r=1.6dp`).
- **Cronometro Metabolico a Doppio Arco 16:8 Ricalibrato**:
  - Arco di alimentazione ciano neon a 120° con triplo bagliore luminoso da ore 9:00 a ore 13:00.
  - Arco di digiuno blu cobalto a 240° da ore 13:00 fino al puntino a ore 9:00, con segmento sky azure per il picco mattutino.
- **Aggiornamento Completo delle Risorse Grafiche**:
  - `ic_launcher_foreground.xml`: vettore Android 108×108dp con safe-area 72dp.
  - `ic_launcher.xml`: icona unificata 48×48dp con base arrotondata per launcher legacy.
  - `cibotimer_icon_master.svg`: master grafico ad alta risoluzione 512×512 con gradienti e filtri neon.
  - `icon_preview.html`: showcase interattivo sincronizzato con la bozza grafica (anteprima Hero con parallasse 3D + griglia comparativa a 3 maschere: Circle, Squircle, Rounded Square).

## [1.0.21] - 2026-11-09

### 📐 Ottimizzazione Spazio Verticale in "Modifica Piano"
- **Layout Full-Screen Dinamico (Zero Spazio Sprecato)**:
  - Rimosso il wrapping compresso su colonna interna fissa e introdotto un layout verticale espanso (`fillMaxSize()`):
    - **Header superiore**: altezza fissa standard a 44dp con tasti touch generosi.
    - **Istogramma 24h a 8 capsule**: altezza aumentata a 145dp per una visualizzazione slanciata e leggibile delle 24 ore.
    - **Lista dei periodi centrale**: configurata con `weight(1f)` per occupare tutto lo spazio verticale disponibile tra istogramma e controlli, con scroll fluido interno e pulsante "+ Aggiungi un periodo" integrato.
    - **Scheda controlli e stepper inferiore**: saldamente ancorata alla base dello schermo, perfettamente allineata con le navigation bars di sistema.

---

## [1.0.20] - 2026-11-09

### 🥗 Palette Pasti in Verde Smeraldo & Piano "Luigi 1" con 9 Periodi BodyFast
- **Verde Smeraldo Fresco per i Pasti (Addio Giallo/Ambra)**:
  - Sostituito il colore ambra/giallo dei pasti con un brillante **verde smeraldo** (`CiboMealGreen = #10B981`) ad alto contrasto OLED:
    - Segmenti dei pasti nelle 8 capsule 24h (sia in dashboard [`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/WeekBarChart.kt) che nel pianificatore [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/TimeEditorModal.kt)).
    - Legenda pasti e badge alimentazione "MANGIARE".
    - Countdown circolare durante la finestra pasto.
    - Pulsante Stop differenziato con arancione vivido dedicato (`CiboStopOrange = #F97316`).
- **Piano "Luigi 1" con i 9 Periodi Esatti di BodyFast**:
  - Configurazione completa dei 9 periodi settimanali del coach in [`FastingPlanTemplates.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/domain/FastingPlanTemplates.kt):
    - **Periodo 1**: lun 00:00 – lun 13:00 (13h)
    - **Periodo 2**: lun 14:00 – mar 10:00 (20h)
    - **Periodo 3**: mar 11:00 – mer 07:00 (20h)
    - **Periodo 4**: mer 12:00 – gio 10:00 (22h)
    - **Periodo 5**: gio 12:00 – ven 11:00 (23h)
    - **Periodo 6**: ven 12:00 – sab 08:00 (20h)
    - **Periodo 7**: sab 21:00 – dom 10:00 (13h)
    - **Periodo 8**: dom 16:00 – lun 11:00 (19h)
    - **Periodo 9**: lun 11:00 – lun 23:00 (12h)
  - Allineamento automatico dei test unitari (`FastingCalculatorTest` e `PlanShiftEngineTest`) e sync all'avvio in `AppRepository`.

---

## [1.0.19] - 2026-11-09

### 🔄 Countdown Circolare Inverso, Grafica Capsule Premium Stadium & Nomi Giorni a 3 Caratteri
- **Grafico Circolare del Timer "All'Inverso" (Countdown)**:
  - Il ring attorno al timer adesso visualizza la frazione di **tempo rimanente** (`remainingFraction`) anziché il tempo trascorso:
    - All'inizio della sessione l'anello è pieno al 100% (cerchio perfetto continuo a 360°).
    - Man mano che il countdown scorre verso lo zero, l'arco si svuota progressivamente in senso orario, coerentemente con la dicitura "Rimanente HH:MM:SS" e lo stile BodyFast.
    - Gestione automatica sia per il digiuno (anello azzurro) che per la finestra pasto (anello ambra).
- **Grafica Capsule Premium (Stadium Pill OLED)**:
  - Le capsule verticali delle 24 ore in [`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/WeekBarChart.kt) e [`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/TimeEditorModal.kt) sono state ridisegnate con un raggio perfetto a pillola stadio (`w / 2f`).
  - Introdotto `clipPath` con sfondo scuro OLED (`#0F1A2E`) e separatori scuri netti tra segmenti pasto e digiuno, eliminando qualsiasi imprecisione visiva o spigolo sgradevole.
- **Freccia Indicatore Ingrandita e Prominente (▶)**:
  - Dimensione della freccia aumentata a 13dp di altezza e 9dp di larghezza, con contorno di contrasto scuro, riempimento neon ciano (`CiboTimerAzureGlow` `#00E5FF`) e nucleo bianco ad alta visibilità sul bordo sinistro della capsula.
  - Trattino orizzontale a 2.5dp con linea di contrasto sottostante di 5.5dp per risaltare su qualsiasi sfondo (azzurro o ambra).
- **Etichette Giorni della Settimana a 3 Caratteri**:
  - Modificate le etichette da 2 caratteri a 3 caratteri standard: **`Lun, Mar, Mer, Gio, Ven, Sab, Dom, Lun`**, con evidenziazione del giorno corrente in grassetto azzurro e dot indicatore.

---

## [1.0.18] - 2026-11-09

### 🎯 Indicatore Orario Attuale sulla Capsula 24h (Trattino e Freccia Puntatore)
- **Indicatore Real-Time Posizione nella Capsula (Trattino + Freccia ▶)**:
  - Introdotto un indicatore ad altissima visibilità posizionato con esattezza matematica (`minutiTrascorsi / 1440f`) lungo la capsula del giorno corrente sia nella dashboard principale ([`WeekBarChart.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/WeekBarChart.kt)) sia nel pianificatore a schermo intero ([`TimeEditorModal.kt`](file:///root/src/cibotimer/cibotimer/app/src/main/java/com/bodyfast/clone/ui/components/TimeEditorModal.kt)).
  - Composto da:
    - **Trattino orizzontale luminoso**: linea neon in `CiboAzureGlow` (`#00E5FF`) con alone scuro sottostante per garantire il massimo contrasto visivo sia sulle sezioni di digiuno (azzurro) che su quelle di pasto (ambra).
    - **Freccia puntatore triangolare (▶)** sul bordo sinistro della capsula che punta con precisione lungo la linea orizzontale.
    - **Punto focale bianco** per una lettura istantanea della propria posizione nelle 24 ore della giornata.

---

## [1.0.17] - 2026-11-09

### 📱 Popup a Schermo Intero, Schedulazione Wall-Clock Reale, Stop Immediato & Orari Precisi
- **Popup a Schermo Intero (Full Screen Experience)**:
  - `TimeEditorModal` ("Modifica piano" / "Pianificatore"), `PlanPickerModal` ("Piani di digiuno") e `FastingStagesSheet` convertiti in dialoghi a **schermo intero al 100%** (`DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` con padding di sistema), eliminando i margini, lo sfondo visibile in alto e il drag handle.
- **Schedulazione Wall-Clock Reale e Sincronizzazione con Orario Attuale**:
  - Il motore `FastingCalculator.resolveCurrentScheduledPeriod` calcola matematicamente l'esatto periodo in corso o il prossimo periodo in base all'orario reale (`currentEpochMillis`).
  - Se ci si trova dentro una finestra di digiuno programmata (es. Lunedì alle 14:04 nel periodo `lun 14:00 - mar 10:00`), il timer indica "Adesso: Digiunare", tempo rimanente esatto (19:55:43) e tempo trascorso reale (4 min).
- **Inizio e Fine/Prossimo Strettamente Correlati al Periodo della Popup**:
  - Sulla dashboard principale, `Inizio` e `Fine` (o `Prossimo`) mostrano esattamente gli orari del periodo programmato (es. `lun 14:00` e `mar 10:00`).
  - Icona matita cliccabile su entrambi gli orari per aprire direttamente il pianificatore a schermo intero focalizzato su quel periodo.
- **Stop Timer Immediato e Zero Countdown Quando Fermo**:
  - Quando il timer viene interrotto dall'utente ("Interrompi") o non è avviato, il timer si **ferma completamente**.
  - Nessun countdown fasullo della finestra pasto: anello fermo, badge "FERMO", orario statico con durata programmata e pulsante "INIZIA DIGIUNO".
- **Chip Nome Piano**:
  - Aggiunto badge pillola con il nome del piano attivo (es. `Luigi 1`) sotto la ghiera per aprire rapidamente il selettore piani.
- **Nuova Palette 7 Periodi BodyFast / CiboTimer**:
  - Riorganizzato `PLAN_LUIGI_1` sui 7 periodi standard da Lunedì 14:00 a Lunedì 11:00 con pause pasto coerenti e senza sovrapposizioni.

---

## [1.0.16] - 2026-11-09

### 🎯 9 Periodi BodyFast, Istogramma a 8 Capsule (lu-lu) e Gestione Periodi Completa
- **Integrazione Completa dei 9 Periodi di Digiuno**:
  - Il piano `Luigi 1` ora include fedelmente i **9 periodi** specificati dalla pianificazione BodyFast:
    - Periodo 1: lun 00:00 – lun 13:00 (13h)
    - Periodo 2: lun 14:00 – mar 10:00 (20h)
    - Periodo 3: mar 11:00 – mer 07:00 (20h)
    - Periodo 4: mer 12:00 – gio 10:00 (22h)
    - Periodo 5: gio 12:00 – ven 11:00 (23h)
    - Periodo 6: ven 12:00 – sab 08:00 (20h)
    - Periodo 7: sab 21:00 – dom 10:00 (13h)
    - Periodo 8: dom 16:00 – lun 11:00 (19h)
    - Periodo 9: lun 11:00 – lun 23:00 (12h)
- **Istogramma a 8 Capsule (lu, ma, me, gi, ve, sa, do, lu)**:
  - Il pianificatore riproduce fedelmente il layout a 8 colonne di BodyFast Coach da lunedì a lunedì successivo.
  - Ciascuna colonna calcola accuratamente i segmenti 24h di digiuno (azzurro) e pasto (ambra), evidenziando il periodo selezionato.
- **Lista Periodi Dinamica con Modifica, Cancellazione e Aggiunta**:
  - Lista scrollabile contenuta nello schermo con riga dedicata per ciascun periodo, evidenziazione del periodo selezionato, cestino per eliminare e pulsante "+ Aggiungi un periodo".
  - Scheda inferiore con Stepper di selezione scope "Tutti i / X Tutti i", spostamento orari e regolazione durata con cascading automatico ad 1 ora sul periodo successivo.

---

## [1.0.15] - 2026-11-09

### 🍽️ Transizione Automatica a "MANGIARE", Cascading Orari (+1h Giorno Successivo) & Piano "Luigi 1"
- **Timer Automatico "MANGIARE" al Termine del Digiuno**:
  - Quando il countdown del digiuno giunge a zero o al tocco di Stop, l'app completa automaticamente la sessione e avvia all'istante il timer in modalità **MANGIARE** con badge in ambra e calcolo del tempo rimanente fino all'inizio del prossimo digiuno.
  - Anello circolare con avanzamento cromatico differenziato: azzurro per digiuno attivo e ambra durante la finestra di alimentazione.
- **Regola Cascading: Inizio Giorno Successivo a +1 Ora dalla Fine**:
  - Quando viene modificato un giorno (spostamento orario o estensione della durata), l'orario di inizio del giorno successivo viene automaticamente spostato ad 1 ora dopo il termine del giorno corrente (`targetStart = (endHour + 1) % 24`), preservando la durata del giorno successivo e garantendo sempre una finestra pasto congrua.
- **Nuovo Piano Speciale "Luigi 1" (Fedele alla Screenshot BodyFast)**:
  - Introdotto `PLAN_LUIGI_1` nella lista dei piani predefiniti:
    - Lunedì: 14:00 – Mar 10:00 (20h digiuno, 1h pasto)
    - Martedì: 11:00 – Mer 07:00 (20h digiuno, 5h pasto)
    - Mercoledì: 12:00 – Gio 10:00 (22h digiuno, 2h pasto)
    - Giovedì: 12:00 – Ven 11:00 (23h digiuno, 1h pasto)
    - Venerdì: 12:00 – Sab 08:00 (20h digiuno, 13h pasto nel weekend)
    - Sabato: 21:00 – Dom 10:00 (13h digiuno, 6h pasto)
    - Domenica: 16:00 – Lun 11:00 (19h digiuno)
  - Totale 137 ore settimanali di digiuno per massima autofagia nei feriali e recupero sociale nel weekend.

---

## [1.0.14] - 2026-11-09

### 📱 Capsule 24h nella Home, Colorazione Digiuno/Pasto coerente, Layout Fit-Screen e Salvataggio Affidabile
- **Capsule 24 Ore Orizzontali anche nella Dashboard Principale**:
  - `WeekBarChart` nella Home ridisegnato con 7 capsule verticali (`Lun..Dom`) affiancate in orizzontale.
  - Asse orario Y a sinistra (`0h, 6h, 12h, 18h, 24h`) con griglia sottile.
  - Giorno corrente (*Oggi*) in evidenza con contorno neon ciano ad alto contrasto, pallino luminoso e label accent.
  - Tocco su qualunque capsula per aprire direttamente il Pianificatore sul giorno selezionato.
- **Colori Coerenti Digiuno (Azzurro) e Pasto (Ambra) in Tutte le Capsule**:
  - Calcolo matematico deterministico delle 24 ore (`FastingCalculator.compute24hSegments`):
    - Ore di **Digiuno** colorate in azzurro ciano luminoso (`#38BDF8`).
    - Ore di **Pasto** colorate in ambra caldo (`#FBBF24`).
    - Separatore arrotondato tra i segmenti.
  - Stessa palette visiva usata sia nella Home che nel popup Modifica Piano.
- **Layout "Modifica Piano" Compatto che Sta Interamente nello Schermo**:
  - Dimensionamento preciso dei componenti per entrare nel viewport senza dover scrollare (stile BodyFast):
    - Header: 44dp.
    - Istogramma a capsule 24h: 125dp.
    - Lista periodi: altezza compatta 95dp.
    - Scheda inferiore controlli & steppers: 175dp.
    - Altezza totale ~440dp (perfettamente fruibile su qualsiasi schermo Android).
- **Risoluzione Definitiva Salvataggio Piano & Allineamento Orari Home**:
  - Scrittura su disco a prova di errore: rimozione del vecchio file prima del rename e fallback a scrittura diretta.
  - Correzione visualizzazione etichette Home: "Inizio" riflette sempre l'inizio del digiuno pianificato (`startTimeFormatted`) e "Fine" il termine (`endTimeFormatted`), eliminando l'inversione durante la finestra pasto.
  - Feedback Toast immediato `"Piano salvato con successo!"` al tap su Salva.

---

## [1.0.13] - 2026-11-09

### 📊 Redesign "Modifica Piano" (Pianificatore) con Capsule 24h Orizzontali e Lista Periodi
- **Istogramma a Capsule Verticali 24 Ore Affiancate in Orizzontale**:
  - I 7 giorni della settimana (`lu, ma, me, gi, ve, sa, do`) sono disposti orizzontalmente come capsule verticali con asse orario Y a sinistra (`0h, 6h, 12h, 18h, 24h`) e linee guida orizzontali sottili.
  - All'interno di ciascuna capsula, le ore di digiuno (compreso il proseguimento della notte precedente fino al mattino) sono renderizzate con gradiente ciano, mentre le ore di pasto/riposo sono su traccia scura.
  - Giorno selezionato evidenziato con ciano vivido `#38BDF8`, contorno neon 2dp e linea di demarcazione; tocco diretto su qualunque capsula per selezionare il giorno.
- **Lista dei Periodi Settimanali (Periodo 1..7)**:
  - Sezione centrale con riepilogo orari periodo per periodo (`Periodo 1: lun 20:00 – mar 12:00 · 16h`).
  - Tocco su qualunque riga per selezionarla istantaneamente con evidenziazione azzurra.
  - Icona cestino su ogni periodo per impostare rapidamente un "Giorno libero (Riposo)" o pulsante "+" per ripristinare il digiuno.
  - Pulsante per riattivare tutti i giorni con un singolo tocco.
- **Scheda Controlli Inferiore con Orari Editabili e Steppers a Due Colonne**:
  - In alto nella card: `Inizio: lun 20:00 ✏️` e `Fine: mar 12:00 ✏️` con icona matita, cliccabili per aprire il `TimePickerDialog` nativo.
  - Colonna sinistra **"Sposta i periodi"**:
    - Stepper ambito giorni (`Tutti`, `Lun`, `Mar`, ...).
    - Stepper 1 ora (±1h).
    - Stepper 15 min (±15m).
  - Colonna destra **"Durata: X ore"**:
    - Stepper 1 ora (±1h).
    - Stepper 15 min (±15m).
- **Sincronizzazione Immediata Notifica Ongoing & Foreground Service**:
  - `syncActiveSessionWithPlan` calcola il nuovo `plannedEndTime` e aggiorna `FastingTimerService` istantaneamente al salvataggio.
- **Palette Dark OLED "Dal blu all'azzurro" Preservata**:
  - Layout identico al riferimento visuale ma coerente al 100% con la palette del brand (`#060B12`, `#101A2D`, `#172238`, `#38BDF8`, `#FBBF24`).

### 👆 "Applica a" riposizionato, Editing Diretto Orari, Gesto Drag sul Grafico e Piani Giornalieri
- **"Applica a" riposizionato sotto all'istogramma dei giorni**:
  - In `TimeEditorModal`, la riga di selezione "Tutti / Lun..Dom" è posizionata direttamente sotto alla card dell'istogramma a 7 giorni per un flusso visivo impeccabile.
- **Orari Inizio e Fine Cliccabili ed Editabili Direttamente**:
  - Toccando l'orario di Inizio o di Fine nella card di anteprima si apre il `TimePickerDialog` nativo a 24 ore per impostare direttamente l'orario desiderato.
- **Scorrimento con il Dito sul Grafico (Drag Gesture)**:
  - Trascinando orizzontalmente il dito sull'istogramma delle 24 ore, gli orari della finestra di digiuno scorrono in tempo reale a scatti di 15 minuti.
- **Sincronizzazione Immediata della UI Principale**:
  - Corretto `WeekBarChart` nella dashboard per utilizzare la linea temporale reale a 24 ore con `Fasting24hBar`, eliminando la vecchia visualizzazione statica che ignorava gli orari reali.
  - Al salvataggio del popup, l'eventuale sessione attiva viene sincronizzata all'istante aggiornando il timer circolare, i countdown e il grafico settimanale.
- **Piano Personalizzato Personalizzabile per Ogni Giorno**:
  - Nel form "+ Crea piano personalizzato", è ora possibile selezionare "Tutti" o singoli giorni della settimana per differenziare gli orari e impostare giorni di riposo/liberi senza digiuno.

---

## [1.0.11] - 2026-11-09

### 🎯 Anteprima e Conferma Piani, Piani Personalizzati & Istogramma 7 Giorni
- **Anteprima e Conferma Orari Inizio/Fine per i Piani di Digiuno**:
  - Toccando un piano nel selettore rapido, viene mostrata una schermata di anteprima con nome, badge categoria, descrizione completa e orari previsti di inizio e fine.
  - Possibilità di regolare gli orari di inizio e fine tramite stepper (±1h e ±15min) prima di confermare.
  - Pulsante dedicato in pill azzurro pieno **"Conferma ed adotta piano"** per accettare il piano con gli orari scelti.
- **Creazione Piano Personalizzato**:
  - Pulsante in evidenza **"+ Crea piano personalizzato"** in cima all'elenco dei piani.
  - Form dedicato per definire il nome del piano, l'orario di inizio digiuno e la durata desiderata (1h - 24h) con anteprima 24h in tempo reale.
  - Pulsante **"Salva ed adotta piano"** che memorizza il piano nei `customPlans` del repository e lo attiva immediatamente.
- **Istogramma 7 Giorni sempre visibile nel Popup "Modifica Piano"**:
  - La card di anteprima include ora un istogramma orizzontale completo per tutti i 7 giorni (Lun..Dom) su asse 0-24h con segmenti digiuno azzurro `#38BDF8` e pasto ambra `#FBBF24`.
  - Evidenziazione istantanea del giorno selezionato con contorno neon accent e label evidenziata; tocco sulle barre per selezionare il giorno.
  - Tutti i 7 giorni reagiscono in tempo reale alle modifiche degli stepper ("Tutti" o singolo giorno).
- **Rimozione Icona Sole nell'Header**:
  - Rimossa l'icona del sole in alto a destra nella schermata principale Digiuno, lasciando unicamente l'icona delle notifiche.
- **Rigoroso OTA Update HTTPS su `https://cibotimer.duckdns.org`**:
  - `UpdateManager`: endpoint forzato a `https://cibotimer.duckdns.org`, eliminando residui di cache locale HTTP.
  - `TimerScreen`: dialog interattivi per la notifica di nuova versione, barra di download percentuale e installazione ad 1 tocco via `FileProvider`.
  - Server `live_update.py` multi-thread su porta 8099 con supporto completo a `HEAD` e `GET`.

---

## [1.0.10] - 2026-11-09

### ⚡ Scelta Rapida Piano di Digiuno nel Timer
- **Pulsante "Piano" a sinistra in basso al timer**:
  - Inserito nella colonna sinistra del timer, simmetrico al pulsante "Stop" della colonna destra.
  - Altezza 48dp, outlined in azzurro accent `#38BDF8`, icona `Tune` + etichetta "Piano" e touch target accessibile.
- **Modal Bottom Sheet "Piani di digiuno" (PlanPickerModal)**:
  - Apertura immediata con un tocco sul pulsante "Piano".
  - Elenco completo di tutti i piani predefiniti (16-8 Classico, 14-10 Dolce, 15-9 Intermedio, 18-6 Avanzato, 20-4 Dieta del Guerriero, 23-1 OMAD, 5-2, Booster di Autofagia, 16-8 Serale) e personalizzati.
  - Indicatore di piano "ATTIVO" con checkmark in azzurro `#38BDF8` e contorno dedicato.
  - Selezione ad 1 tocco: applicazione istantanea del piano alla dashboard, al timer circolare e al grafico settimanale.
  - Opzione per aprire il catalogo completo e i programmi coach avanzati.

---

## [1.0.9] - 2026-11-09

### 🎨 Riprogettazione Bottom Sheet "Modifica Piano"
- **Header (48dp)**: Icona X a sinistra (touch target 48dp, contentDescription "Chiudi"), titolo "Modifica piano" 20sp bold al centro, e pulsante pill "Salva" pieno (accent `#38BDF8`, testo scuro `#0B1220`, altezza 40dp) a destra. Dialog di conferma per modifiche non salvate al tocco della X o swipe dismiss.
- **Selettore "Applica a"**: Riga di chip ad altezza 44dp con "Tutti" (peso maggiore) e Lun..Dom a larghezza uniforme. Stato selezionato con contorno 2dp accent, sfondo tonale e testo accent. Decide lo scope (tutti i 7 giorni o singolo giorno) per le modifiche successive.
- **Card Anteprima**:
  - Due colonne divise da un separatore: "Inizio digiuno" a sinistra e "Fine digiuno" a destra con orari 28sp extra bold in accent e badge "+1 g" quando il digiuno supera la mezzanotte.
  - Barra 0-24h alta 14dp con segmento digiuno azzurro `#38BDF8`, pasto ambra `#FBBF24`, gap 2dp ed estremità esterne arrotondate, con gestione esatta dell'attraversamento della mezzanotte (segmento digiuno mattutino, pasto centrale, digiuno serale).
  - Asse orario 0h, 6h, 12h, 18h, 24h e riepilogo colorato ("16 h digiuno · 8 h pasto") con aggiornamento reattivo istantaneo.
- **Card "Sposta orari (inizio e fine insieme)"**: Due stepper affiancati "1 ora" e "15 min" con pulsante − tonal `#26385A`, etichetta centrata e + accent `#38BDF8`.
- **Card "Durata del digiuno · 16 ore"**: Titolo dinamico con durata corrente in accent e due stepper affiancati ("1 ora" e "15 min") con limiti di durata (min 1h, max 24h) e disabilitazione visiva dei tasti al raggiungimento del limite.
- **Composable Riutilizzabile StepperRow / PlanStepperControl**: Estratto componente stepper riutilizzabile con pulsanti 44dp, contentDescription accessibili e supporto disable.
- **Design System & Risorse**: Sfondo sheet `#101A2D`, card `#172238`, angoli superiori 28dp, drag handle 40x4dp, testo minimo 12sp, stringhe in `strings.xml`.
- **@Preview**: Aggiunte preview per scope "Tutti", scope singolo giorno (Lunedì) e schermo compatto 360x640dp.

---

## [1.0.8] - 2026-11-09

### 🎨 Riprogettazione Schermata "Digiuno" (Zero Scroll & Compact)
- **Header Compatto (44dp)**: Titolo "Digiuno" 24sp bold + sottotitolo "Brucia grassi" 13sp affiancati; pulsanti circolari per Notifiche e Impostazioni con touch target accessibile a 48dp e contentDescription.
- **Riga Timer (~190dp a 3 Colonne)**:
  - **Centro**: Anello di progresso 186dp (spessore 12dp con estremità arrotondate e traccia di sfondo tenue `#162238`), badge "IN DIGIUNO" 11sp accent `#38BDF8`, etichetta "Mancano al pasto" 12sp, countdown 30sp bold con cifre tabulari anti-jitter (`tnum`), tempo trascorso e percentuale ("9h 10m · 46%"). Rimosse le decorazioni ai lati.
  - **Sinistra (72dp)**: Inizio ("Inizio" 12sp e orario "Lun 00:41" 15sp bold) agganciato in alto.
  - **Destra (72dp)**: "Fine" 12sp e orario 15sp bold agganciati in alto; pulsante "Stop" agganciato in basso con `Arrangement.SpaceBetween` (outlined ambra `#FBBF24`, altezza 48dp, icona quadrata stop + testo) con dialog di conferma (Annulla / Interrompi).
- **Piano Settimanale**: Card compatto `#141D30` con titolo "Piano settimanale" e legenda "Digiuno" (`#38BDF8`) e "Pasto" (`#FBBF24`). 7 barre orizzontali Lun-Dom (12dp di altezza, 6dp di spazio, 2dp di gap tra segmenti e angoli arrotondati), giorno corrente evidenziato con etichetta accent e contorno 2dp. Asse orario 0h, 12h, 24h. Dati reali e rimozione del badge Coaching.
- **Fasi Metaboliche**: Card sottile con barra a 4 segmenti (Digestione, Glicogeno, Grassi, Autofagia; completati pieni, corrente parziale, futuri vuoti) e descrizione sempre visibile (es. "Fase 3 · Combustione grassi. Il corpo usa le riserve di grasso come energia.").
- **Bottom Navigation**: Etichette sempre visibili (12sp), voce attiva in azzurro `#38BDF8`, icona Altro a tre puntini.
- **Accessibilità & Design System**: Sfondo `#0B1220`, contrasto elevato (>4.5:1), stringhe localizzate in `strings.xml`, colori estratti nel tema, supporto schermi piccoli (360x640dp).
- **@Preview**: Aggiunte preview per stato in digiuno, nessun digiuno attivo e schermo piccolo 360x640.

---

## [1.0.7] - 2026-11-09

### 🔧 Modifiche
- **Layout Compatto**: Rimosso scroll dalla schermata principale, tutto visibile in una singola schermata.
- **Pulsante Ridotto**: "Interrompi Digiuno" / "Inizia Digiuno" ridotto da 52dp a 42dp con font 14sp.
- **Timer Circolare Compatto**: Ridotto da 265dp a 220dp (raggio da 105 a 88dp).
- **Card Compattate**: Card Inizio/Fine con icone 28dp e padding ridotto. Card Motivator senza box icona.

### 🔒 Sicurezza
- **HTTPS via Caddy + DuckDNS**: OTA update ora tramite `https://finalhome.duckdns.org/cibotimer/`.
- **UpdateManager**: URL di default aggiornato a HTTPS.
- **Live Update Server**: `downloadUrl` nella risposta API ora punta a HTTPS.

---

## [1.0.6] - 2026-11-09

### 🔧 Modifiche
- **Grafico Timer Circolare**: Corretto il gradiente sfumato attorno al timer che appariva ovale anziché perfettamente circolare. Ora il `drawArc` utilizza un riquadro quadrato centrato per garantire un cerchio perfetto.
- **Aggiornamento al Secondo**: L'arco del timer si aggiorna ora ad ogni secondo senza ritardo di animazione (rimossa `animateFloatAsState` con lag di 600ms).

### 🆕 Aggiunto
- **Repository Gitea**: Progetto aggiunto al server Gitea locale su `http://192.168.1.3:3000/final/cibotimer`.
- **CHANGELOG.md**: Creato questo file di changelog per archiviare ogni modifica al progetto.
- **.gitignore**: Aggiunto file `.gitignore` per escludere build, IDE files e APK dalla repository.
- **Live Update Server**: Aggiornato `live_update.py` per servire su IP `192.168.1.3` con supporto changelog e release notes.

---

## [1.0.5] - 2026-11-09

### 🆕 Aggiunto
- **Sistema di Auto-Aggiornamento In-App**: `UpdateManager.kt` con controllo versione, download APK e installazione via `FileProvider`.
- **Live Update Server**: `live_update.py` con pagina web dark responsive, endpoint `/api/version`, e supporto build Docker.
- **Foreground Service**: `FastingTimerService.kt` con cronometro hardware in notifica ongoing.
- **Grafico Settimanale**: `WeekBarChart.kt` con visualizzazione 24h su 7 giorni, capsule blu/azzurro.
- **Timer Circolare**: `FastingCircularProgress.kt` con gradiente sweep, alone neon, badge MANGIARE/DIGIUNARE.
- **Design System**: Palette completa dal blu cobalto (`#0284C7`) all'azzurro ciano (`#00E5FF`) su sfondi OLED dark.
- **7 Fasi Metaboliche**: Calcolo completo delle fasi del digiuno intermittente nel `FastingCalculator.kt`.
- **Coaching AI**: Integrazione con API AI per suggerimenti personalizzati.
- **Backup & Restore**: Supporto JSON locale e Google Drive.

### 📱 Piattaforma
- **Android SDK**: compileSdk 34, minSdk 26, targetSdk 34
- **Kotlin**: JVM Target 21
- **Jetpack Compose**: BOM 2024.09.00
- **Firma**: Keystore permanente `CN=CiboTimer` (v1+v2 signing)
