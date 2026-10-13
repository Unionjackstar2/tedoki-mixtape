# Vintage Transistorradio (Exakte Nachbildung im Querformat)

Wir implementieren ein neues Modell im Radiodesign-Menü: Ein authentisches analoges Transistorradio im horizontalen Querformat (Landscape), das exakt der Bildvorlage entspricht. Das Modell verzichtet komplett auf das Kassettendeck und triggert bei Auswahl automatisch die Querformat-Orientierung (`SCREEN_ORIENTATION_SENSOR_LANDSCAPE`).

### User Review & Critical Decisions

> [!IMPORTANT]
> **Festgelegte Design- und Funktionsvorgaben:**
> - **Festes Querformat (Landscape)**: Das Transistorradio ist wie die bestehende Boombox für das Querformat konzipiert und schaltet bei Aktivierung automatisch auf `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, um das 16:9-Seitenverhältnis der Fotovorlage perfekt abzubilden.
> - **Volumerad mit bidirektionaler Kopplung**:
>   - Das gerändelte Metallrad dreht sich flüssig per Touch-Drehgeste (-135° bis +135°).
>   - Beim Drücken der Lautstärketasten am Smartphone dreht sich das Rad in Echtzeit exakt synchron mit.
>   - Das Rad bildet den exakten `AudioManager.STREAM_MUSIC`-Pegel auf der MIN/MAX-Punktskala ab.
> - **FM-Skala mit Riffelschieber**:
>   - Bernsteinfarbenes Leuchtfeld (88–108 MHz) mit glühender roter Nadel.
>   - Darunter der horizontale, silbern geriffelte Daumenschieber zur Senderabstimmung.
>   - Authentisches UKW-Zwischenstationsrauschen in Sendelücken.
> - **3-Stufen-Wippschalter (FM / AM / OFF)**:
>   - **FM**: Echte Radiosender mit Suchlauf & Rauschen.
>   - **AM**: Klassisches Mittelwellen-Rauschen mit leisem Pfeifton.
>   - **OFF**: Standby (Radio stumm, Beleuchtung abgedunkelt).
> - **Keine Kassettenelemente**: Sämtliche Kassetten-Mechaniken, Spulen und Transporttasten entfallen.

---

### 1. Overview & Core Concept

- **Was es bewirkt**: Bietet ein fokussiertes Radioerlebnis im Querformat, das wie ein echtes transistorisiertes Tischradio auf einer Holzoberfläche wirkt.
- **Optische Treue**: 1:1-Übernahme der Designelemente aus dem Foto:
  - Breitformatiges Gehäuse mit abgerundeten Ecken, feiner dunkler Ledertextur und polierter Chrom-Zierleiste.
  - Oben horizontal anliegende Chrom-Teleskopantenne und seitliche Ledergurthalterungen.
  - Linke Gehäusehälfte: Vertieftes Lautsprechergitter mit dichter Matrix aus gestanzten kreisrunden Akustiklöchern.
  - Rechte Gehäusehälfte:
    - Oben: Bernsteinfarben hinterleuchtete Frequenzskala mit roter Nadel und dem silbernen horizontalen Schieber.
    - Unten links: Schwarzer gerillter 3-Stufen-Schiebeschalter (FM, AM, OFF).
    - Unten rechts: Großer Dreh-Lautstärkeregler aus konzentrisch gebürstetem Metall mit MIN/MAX-Skala.

---

### 2. User Experience & Visual Design (Querformat 16:9)

```
┌────────────────────────────────────────────────────────────────────────┐
│ [== Chrome Teleskopantenne =========================================] │
├──────────────────────────┬─────────────────────────────────────────────┤
│                          │  [ FM   88   92   96  100  104  108  MHz ]  │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │  │││││││││││││││││││││││|││││││││││││││││   │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │  [======[|||||]===========================] │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │        (Horizontaler Silber-Schieberegler)  │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   ├─────────────────────────────────────────────┤
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │    FM          ·  · VOLUME ·  ·             │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │   [===] AM    ·    ╭──────╮    ·            │
│  ○ ○ ○ ○ ○ ○ ○ ○ ○ ○ ○   │    OFF       ·    │  /   │     ·           │
│  (Perforiertes Lochgitter│              MIN   ╰──────╯    MAX          │
│   Lautsprecher-Matrix)   │   (3-Wege-     (Drehbarer Lautstärkeknopf   │
│                          │    Schalter)    mit Min/Max Indikator)      │
└──────────────────────────┴─────────────────────────────────────────────┘
```

#### Bedienungsabläufe

1. **Auswahl des Modells**:
   - Nutzer öffnet die Einstellungen -> „Radio auswählen“.
   - Wählt die neue Karte **„Transistorradio (Querformat)“**.
   - Die App rotiert das Smartphone ins Querformat und zeigt das Radio vollflächig an.
2. **Senderabstimmung**:
   - Nutzer zieht den silbernen Schieber unter der Skala horizontal nach links oder rechts.
   - Die rote Skalennadel gleitet mit; bei Sendelücken rauscht das UKW-Band, bei Sendern rastet die Wiedergabe ein.
3. **Lautstärke**:
   - **Geste am Rad**: Nutzer dreht das Rad im Uhrzeigersinn -> Systemlautstärke steigt, Markierungsstrich wandert Richtung MAX.
   - **Gerätetasten**: Nutzer drückt Lautstärke +/- am Telefon -> das Rad auf dem Bildschirm dreht sich sofort synchron mit.
4. **Band- & Ein/Aus-Schalter**:
   - Klick auf `FM`: UKW-Live-Radio aktiv.
   - Klick auf `AM`: Nostalgisches Mittelwellen-Rauschen.
   - Klick auf `OFF`: Radio stummgeschaltet, Skalenbeleuchtung schaltet ab.

---

### 3. Key Product Decisions & Trade-Offs

- **Entscheidung 1: Orientierungssteuerung (`SCREEN_ORIENTATION_SENSOR_LANDSCAPE`)**
  - *Gewählter Ansatz*: Wie bei der Boombox wird beim Wechsel auf `RadioLayoutMode.TRANSISTOR` die Querformat-Orientierung der Activity angefordert.
  - *Warum*: Garantiert, dass die exakten horizontalen Proportionen des Referenzbilds auf jedem Smartphone-Display voll zur Geltung kommen.
- **Entscheidung 2: `AudioManager` + `ContentObserver` für Tasten-Synchronisation**
  - *Gewählter Ansatz*: Registrierung eines ContentObservers auf `Settings.System.CONTENT_URI` und BroadcastReceivers für Lautstärkeänderungen.
  - *Warum*: Erkennt Änderungen durch die seitlichen Hardware-Tasten absolut verzögerungsfrei und triggert die visuelle Rotation des Lautstärkerads.
- **Entscheidung 3: Reines Radio ohne Kassetten-Zustand**
  - *Gewählter Ansatz*: Ausblenden aller Tape-Komponenten; AudioEngine läuft im reinen `PlaybackSource.RADIO`-Modus.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   CassetteDeckScreen.kt                │
│             radioLayoutMode == TRANSISTOR              │
└───────────────────────────┬────────────────────────────┘
                            │ (Schaltet auf Querformat)
                            ▼
┌────────────────────────────────────────────────────────┐
│                 TransistorRadioView.kt                 │
│  - Gehäuse mit Ledertextur & Chrom-Zierrahmen          │
│  - Teleskopantenne & Gehäusefüße                       │
│  - Perforiertes Lautsprechergitter (Canvas)            │
├───────────────────────────┬────────────────────────────┤
│ TransistorTuningScale.kt  │ TransistorVolumeKnob.kt    │
│ - 88–108 MHz Leuchtfeld   │ - Drehbares Metallrad      │
│ - Rote Nadel + Schieber   │ - Min/Max Punktskala       │
│ - FM/AM/OFF Wahlschalter  │ - AudioManager Listener    │
└───────────────────────────┴────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                   AudioEngine.kt                       │
│  - FM-Streaming + Zwischenrauschen                     │
│  - AM-Rauschgenerator                                  │
│  - Standby / OFF Muting                                │
└────────────────────────────────────────────────────────┘
```

#### Komponenten-Mapping

| Datei | Änderungen |
|---|---|
| `Cassette.kt` | Enum-Wert `RadioLayoutMode.TRANSISTOR` hinzufügen. |
| `TransistorRadioView.kt` | Neues Composable für das fotorealistische Querformat-Radio (Chassis, Antenne, Lochgitter, Bedienpanel). |
| `TransistorVolumeKnob.kt` | Drehbarer Lautstärkeknopf mit Touch-Winkelberechnung & bidirektionaler `AudioManager`-Synchronisation. |
| `TransistorTuningScale.kt` | Bernsteinfarbene FM-Skala mit horizontalem Schieberegler und 3-Wege-Schiebeschalter (FM / AM / OFF). |
| `SettingsSheet.kt` | Vorschaukarte „Transistorradio (Querformat)“ in `RadioModelSelectionSubmenu` integrieren. |
| `CassetteDeckScreen.kt` | Rendering-Zweig für `RadioLayoutMode.TRANSISTOR` mit Querformat-Aktivierung verknüpfen. |
