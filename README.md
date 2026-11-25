# SeniorAgenda (Dagoverzicht)

Een native Android-applicatie in **Kotlin + Jetpack Compose**, speciaal gebouwd als een overzichtelijk, hoog-contrast, altijd-aan agendadashboard op een **Samsung Galaxy Tab S9 FE** (10,9" TFT LCD-scherm, vaste liggende weergave) met **100% Nederlandse interface en spraak (`nl-NL`)**.

## Belangrijkste functies

- **100% Nederlands (`nl-NL`)**: Alle teksten, dag- en maandnamen (bijv. `DONDERDAG 26 NOVEMBER`, `Za 28 nov`), 24-uurs tijden (`10:15 - 11:00`), lege-statusmeldingen (`Geen afspraken vandaag` / `Een rustige dag`), knoppen en foutmeldingen zijn volledig in natuurlijk Nederlands.
- **Vaste liggende indeling zonder drukke menu's**:
  - **Bovenbalk**: Nederlandse dag en datum, knop **Spreek voor**, knop **Afspraak toevoegen** (microfoon + Gemini), en grote 24-uurs klok.
  - **Links (`VANDAAG`)**: Grote weergave van hele-dag-items (`HELE DAG - Hapjes komen aan`) én meerdere tijdsgebonden afspraken met zowel start- als eindtijd.
  - **Rechtsboven (`MORGEN`)**: Dagnaam, datum en afspraken van morgen (`MORGEN - VRIJDAG 27 NOVEMBER`).
  - **Rechtsonder (`DAARNA`)**: Compact overzicht van de komende 4 dagen.
- **Nederlandse spraak (`Spreek voor`)**: Android `TextToSpeech` strikt op `nl-NL` met rustig spreektempo (`0.88f`), levenscyclusbeheer en controle op beschikbaarheid van de Nederlandse stem.
- **Tijdige herinneringen + 5-minuten herhaallus + persistente overlay**:
  - Begint exact **30 minuten vóór de begintijd** van elke tijdsgebonden afspraak (bijv. vanaf `13:30` voor `14:00 - 15:00`).
  - Herhaalt elke **5 minuten** (spraak + actieve overlay) totdat de afspraak begint of op **Begrepen** wordt getikt.
  - De overlay (`HERINNERING VOOR VANDAAG`) kan **uitsluitend** worden gesloten met de grote knop **Begrepen** (tikken buiten het venster en de terugknop sluiten de melding niet) en bevat een grote knop **Opnieuw voorlezen**.
- **Nachtstand voor TFT LCD (Samsung Galaxy Tab S9 FE)**:
  - Tussen **23:30 en 05:30** wordt de schermhelderheid van het venster naar het LCD-minimum (`0.01f`) gedimd over een zeer donker rustscherm, zodat de kamer donker blijft en warmte/stroomverbruik beperkt blijven.
  - Tikken op het scherm wekt het dashboard tijdelijk voor **30 seconden**.
  - Om **05:30** keert het scherm automatisch terug naar de dagstand.
  - Elke actieve herinnering tijdens de nachtstand wekt het scherm met voorrang.
- **Google Agenda & Gemini-workflow**:
  - Synchronisatie met Google Calendar REST API v3 (OAuth 2.0 via Google Play Services `AuthorizationClient`) zodat familieleden afspraken op afstand kunnen beheren (via Google Agenda of Gemini op hun eigen telefoon).
  - In-app **Afspraak toevoegen** met Nederlandse spraakherkenning (`nl-NL`) en Gemini-parsing naar gestructureerde datum, begintijd en eindtijd (standaardduur **1 uur** als geen eindtijd is genoemd), plus een ingebouwde offline Nederlandse terugvalparser.

---

## Projectstructuur

```text
SeniorAgenda/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── res/
│   │   ├── drawable/ic_launcher_agenda.xml
│   │   ├── values/strings.xml
│   │   ├── values/themes.xml
│   │   └── xml/locales_config.xml
│   └── java/nl/senioragenda/app/
│       ├── MainActivity.kt                  # Vaste landscape-kiosk, schermhelderheid, wake-flags
│       ├── SeniorAgendaApplication.kt       # Dependency container
│       ├── domain/
│       │   ├── model/Appointment.kt         # Afsprakenmodel (hele dag & start-/eindtijd)
│       │   ├── formatting/
│       │   │   ├── DutchDateTimeFormatter.kt
│       │   │   └── DutchSpeechScriptBuilder.kt
│       │   └── parser/LocalDutchAppointmentParser.kt
│       ├── data/
│       │   ├── AgendaRepository.kt
│       │   ├── SampleDutchAgendaProvider.kt
│       │   ├── local/AgendaPreferences.kt
│       │   └── remote/
│       │       ├── GoogleCalendarAuthManager.kt
│       │       ├── GoogleCalendarApiClient.kt
│       │       └── GeminiAppointmentParser.kt
│       ├── speech/
│       │   ├── DutchSpeechManager.kt        # Android TextToSpeech (nl-NL)
│       │   └── DutchVoiceInputManager.kt    # Android SpeechRecognizer (nl-NL)
│       ├── reminder/
│       │   ├── ReminderPolicy.kt            # 30-min vooraf + 5-min herhaallogica
│       │   ├── ReminderAlarmScheduler.kt    # Android AlarmManager
│       │   └── ReminderAlarmReceiver.kt     # Wekt scherm bij systeemalarm
│       ├── nightmode/NightModePolicy.kt     # 23:30-05:30 LCD-nachtstand & 30s wake
│       └── ui/
│           ├── AgendaViewModel.kt
│           ├── AgendaDashboardScreen.kt
│           ├── theme/Theme.kt
│           └── components/
│               ├── TopDashboardBar.kt
│               ├── AgendaPanels.kt
│               ├── ReminderOverlay.kt
│               ├── NightModeOverlay.kt
│               ├── AddAppointmentModal.kt
│               └── FamilySettingsModal.kt
└── app/src/test/java/...                    # Unit tests voor formatting, reminder, nachtstand & parser
```

---

## Eerste installatie en build (Android Studio + ADB)

### 1. Benodigdheden op uw pc
1. Installeer **Android Studio** (bevat JDK, Android SDK en ADB).
2. Open de map `SeniorAgenda` in Android Studio; Gradle 9.3.1 en AGP 9.1.0 zijn voorgeconfigureerd via de inbegrepen Gradle Wrapper (`gradlew` / `gradlew.bat`).
3. (Optioneel) Kopieer `local.properties.example` naar `local.properties` (dit bestand staat in `.gitignore` en wordt nooit naar GitHub gepusht) en vul indien gewenst `GEMINI_API_KEY` en `GOOGLE_CALENDAR_ID` in.
   - Zonder sleutels start de app direct in een volledige Nederlandse **voorbeeldweergave** waarin u het hele dashboard, voorlezen, toevoegen, de herinneringsoverlay en de nachtstand direct kunt testen.

### 2. Installeren op de Samsung Galaxy Tab S9 FE via ADB
1. Activeer op de Galaxy Tab S9 FE **Ontwikkelaarsopties**:
   - Ga naar **Instellingen → Over tablet → Softwaregegevens** en tik 7 keer op **Buildnummer**.
2. Ga naar **Instellingen → Ontwikkelaarsopties** en schakel **USB-foutopsporing** (USB debugging) in.
3. Verbind de Tab S9 FE met een USB-C-kabel met uw pc en accepteer de melding *"USB-foutopsporing toestaan"* op de tablet.
4. Controleer de verbinding en bouw/installeer:
   ```powershell
   adb devices
   .\gradlew.bat installDebug
   ```
   *(Of klik in Android Studio op **Run ▶** met de Galaxy Tab S9 FE geselecteerd.)*

### 3. Aanbevolen tabletinstellingen op de Galaxy Tab S9 FE
- **Nederlandse TTS-stem controleren**: Ga op de tablet naar **Instellingen → Algemeen beheer → Tekst-naar-spraak** → kies **Spraakservices van Google** als voorkeursengine en controleer dat **Nederlands (Nederland)** is gedownload.
- **Exacte alarmen**: Sta onder **Instellingen → Apps → Dagoverzicht → Alarmen en herinneringen** exacte alarmen toe zodat herinneringen tijdens diepe slaap altijd exact op tijd afgaan.
- **Batterijoptimalisatie**: Zet batterijgebruik voor **Dagoverzicht** op **Onbeperkt** zodat Samsung One UI de achtergrond-synchronisatie en alarmen niet afremt.

---

## Google Cloud OAuth & gedeelde Google Agenda instellen

Om familieleden op afstand afspraken te laten toevoegen (via Google Agenda of Gemini op hun eigen telefoon):

1. **Gedeelde Google Agenda aanmaken**:
   - Maak op een Google-account een gedeelde agenda aan (bijv. *"Agenda Papa"*) en deel die met bewerkrechten met familieleden én het Google-account dat op de Tab S9 FE staat ingelogd.
   - Kopieer de **Agenda-ID** uit *Agenda-instellingen → Agenda integreren* (of gebruik `primary` voor de hoofdagenda van het tablet-account).
2. **Google Cloud Console instellen**:
   - Maak een nieuw project aan in [Google Cloud Console](https://console.cloud.google.com/).
   - Schakel de **Google Calendar API** in onder *APIs & Services → Library*.
   - Configureer het **OAuth consent screen** (*External* → *Testing*):
     - Voeg de scopes `.../auth/calendar.events` en `.../auth/calendar.readonly` toe.
     - Voeg onder **Test users** het Gmail-adres van de tablet (en eventuele testaccounts) toe.
   - Maak onder **Credentials → Create Credentials → OAuth client ID** een **Android** client aan:
     - **Package name**: `nl.senioragenda.app`
     - **SHA-1 certificate fingerprint**: haal uw lokale debug-SHA-1 op met:
       ```powershell
       .\gradlew.bat signingReport
       ```
3. **Koppelen in de app**:
   - Tik linksboven op het kleine statuslabel (`Voorbeeldweergave · Beheer`) om het familiebeheer-venster te openen.
   - Vul eventueel de gedeelde **Google Agenda-ID** in en tik op **Koppel / Vernieuw Google Agenda**.
   - Alle afspraken die familieleden daarna via Google Agenda of Gemini op hun telefoon in deze agenda zetten, verschijnen automatisch binnen enkele minuten op de tablet.

### Gemini API-sleutel toevoegen (optioneel voor in-app spraakverwerking)
- Maak een API-sleutel aan in [Google AI Studio](https://aistudio.google.com/) en voeg deze toe aan uw lokale `local.properties`:
  ```properties
  GEMINI_API_KEY=uw_sleutel_hier
  GOOGLE_CALENDAR_ID=primary
  ```
- Omdat `local.properties` in `.gitignore` staat, worden uw sleutels nooit in Git opgeslagen.

---

## Tests uitvoeren

Zodra JDK 17 / Android Studio aanwezig is:
```powershell
.\gradlew.bat testDebugUnitTest
```

## Pushen naar GitHub

Controleer vóór het pushen altijd dat er geen geheime bestanden (`local.properties`, keystores) worden meegenomen:
```powershell
git init
git status
git add .
git commit -m "Initial SeniorAgenda Android app (nl-NL)"
git branch -M main
git remote add origin https://github.com/<gebruikersnaam>/SeniorAgenda.git
git push -u origin main
```
