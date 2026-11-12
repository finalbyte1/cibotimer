# 🛡️ CiboTimer - Privacy Policy / Informativa sulla Privacy

*Read this document in: [English (EN)](#-english-version) | [Italiano (IT)](#-versione-italiana)*

- **Public Web Version (GitHub Pages):** [https://finalbyte1.github.io/cibotimer/privacy.html](https://finalbyte1.github.io/cibotimer/privacy.html)
- **Last Updated:** November 12, 2026 / 12 Novembre 2026
- **License:** Apache License 2.0
- **Package:** `com.cibotimer`

---

## 🇬🇧 English Version

### Core Philosophy
CiboTimer is designed with a strict **Offline-First** and **Privacy-First** architecture. All personal data, fasting history, body weight, measurements, and hydration logs remain **strictly on your device** within Android's private app sandbox. The app contains **no advertisements, no third-party tracking SDKs, and no telemetry**.

### 1. Data Collected & Stored Locally
- **Fasting Data:** Start and end timestamps, session duration, eating window logs, target schedules (16:8, 18:6, OMAD, custom).
- **Body & Health Metrics:** Body weight, height, optional measurements (waist, hips, neck), and locally computed Body Mass Index (BMI).
- **Hydration Log:** Daily glasses of water consumed.
- **Preferences:** Notification preferences, dark theme configuration, custom weekly intervals.

### 2. Android Permissions & Purpose
- `POST_NOTIFICATIONS`: Required on Android 13+ to notify you when fasting/eating windows finish and when metabolic stages transition.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Runs a persistent low-power chronometer in the system status bar and keeps home screen widgets (1x1 and 2x1) synchronized without battery drain.
- `SCHEDULE_EXACT_ALARM`: Triggers precise alarms when fasting intervals transition.
- `RECEIVE_BOOT_COMPLETED`: Re-registers scheduled alarms after your device restarts.

### 3. Optional Integrations (User-Initiated)
- **Google Drive Backup (Optional):** Users may link their own personal Google Drive account to export or restore JSON backups. CiboTimer only accesses its own dedicated backup file.
- **AI Coach (BYOK - Bring Your Own Key):** The AI chatbot operates on a BYOK basis. If enabled with your personal API key (OpenAI, Groq, DeepSeek, or local Ollama), messages are sent directly to that provider for text generation only. No conversations are stored on CiboTimer servers.

### 4. Zero Advertising & Zero Tracking
- No advertising networks (No AdMob, Unity, AppLovin).
- No behavioral trackers (No Firebase Analytics, Facebook SDK, Mixpanel).
- No user profiling or commercial monetization.

### 5. Data Control & Deletion
- **Export:** Export your complete history anytime in standard JSON format from *Settings ➔ Backup & Data*.
- **Erase:** Use *"Delete all data"* in Settings to permanently wipe the local SQLite database.
- **Uninstall:** Uninstalling the app instantly removes all local files stored in Android's app sandbox.

### 6. Medical Disclaimer
CiboTimer is a wellness tracking application and does not provide clinical diagnosis, medical advice, or treatment plans. Consult a qualified physician before starting intermittent fasting or making significant dietary changes.

---

## 🇮🇹 Versione Italiana

### Filosofia Fondamentale
CiboTimer adotta un approccio rigorosamente **Offline-First** e **Privacy-First**. Tutti i dati personali, lo storico dei digiuni, il peso, le misurazioni corporee e il registro dell'idratazione rimangono custoditi **esclusivamente nella memoria locale del tuo smartphone**, protetti nella sandbox di Android. L'app non contiene pubblicità, non include tracciatori di terze parti e non raccoglie dati personali.

### 1. Dati Trattati e Salvataggio Locale
- **Dati sul Digiuno:** Orari di inizio e fine, durata delle sessioni, finestre alimentari e obiettivi (16:8, 18:6, OMAD, personalizzati).
- **Salute e Parametri Corporei:** Peso, altezza, circonferenze corporee facoltative (vita, fianchi, collo) e indice BMI calcolato localmente.
- **Registro Idratazione:** Conteggio dei bicchieri d'acqua bevuti.
- **Preferenze:** Notifiche, tema scuro e orari dei piani settimanali.

### 2. Permessi Android e Finalità
- `POST_NOTIFICATIONS`: Necessario da Android 13 in poi per notificare la fine delle finestre di digiuno o pasto e i traguardi metabolici.
- `FOREGROUND_SERVICE` e `FOREGROUND_SERVICE_SPECIAL_USE`: Utilizzato dal Foreground Service per gestire il cronometro hardware a zero consumo di batteria e sincronizzare i widget 1x1 e 2x1 della home screen.
- `SCHEDULE_EXACT_ALARM`: Consente l'attivazione puntuale delle sveglie di sistema al cambio finestra.
- `RECEIVE_BOOT_COMPLETED`: Ripristina i promemoria del digiuno al riavvio del telefono.

### 3. Servizi Opzionali (Attivati dall'Utente)
- **Backup Google Drive (Facoltativo):** L'utente può facoltativamente collegare il proprio account Google Drive per salvare un backup JSON. L'app accede unicamente al file creato per CiboTimer.
- **AI Coach (BYOK - Bring Your Own Key):** Se attivato dall'utente inserendo una propria chiave API privata (OpenAI, Groq, DeepSeek, Ollama), le richieste vengono inviate direttamente al provider scelto. Nessun log viene archiviato su server intermediari.

### 4. Nessuna Pubblicità e Nessun Tracciamento
- Nessun banner pubblicitario (No AdMob, Unity, ecc.).
- Nessuna libreria di analisi o tracciamento (No Firebase Analytics, Facebook SDK, ecc.).
- Nessuna vendita o profilazione dei dati.

### 5. Controllo e Cancellazione dei Dati
- **Esportazione:** Dalla sezione *Impostazioni ➔ Backup e Dati* puoi esportare in qualsiasi momento l'intero database in formato JSON standard.
- **Cancellazione Definitiva:** Tramite il pulsante *"Elimina tutti i dati"* nelle Impostazioni puoi cancellare permanentemente tutti i record.
- **Disinstallazione:** Disinstallare l'applicazione rimuove immediatamente tutti i file memorizzati nella sandbox locale di Android.

### 6. Disclaimer Medico
CiboTimer è uno strumento di supporto al benessere e non fornisce consulenza medica o diagnosi. Prima di intraprendere regimi di digiuno prolungato, consulta sempre un medico o uno specialista.

---

### 📬 Contatti / Contacts
Per domande o segnalazioni / For questions or issues:
- **Repository Ufficiale:** [https://github.com/finalbyte1/cibotimer](https://github.com/finalbyte1/cibotimer)
