# LinkedIn-Beitrag zu einem Open-Source-Release: Sichtbarkeit, Umfang, KI-Transparenz

Stand: 2026-11-15 (alle Abrufe an diesem Tag). Recherche ohne Aenderung am Repo ausser dieser Datei.

Algorithmus-Aussagen veralten schnell: LinkedIn hat laut eigener Veroeffentlichung das Feed-Ranking im Maerz 2026 umgebaut (Abschnitt 1.1). Alles, was vor 2026-03 gemessen oder geschrieben wurde, beschreibt moeglicherweise ein anderes System.

Webzugriff war verfuegbar. Es gab aber Luecken, die offen benannt sind (Abschnitt 7). Insbesondere liefen alle Abrufe ueber ein Werkzeug, das Seiten durch ein kleines Modell zusammenfasst. Woertliche Zitate unten stammen aus diesen Zusammenfassungen und sind vor einer oeffentlichen Verwendung an der Originalseite zu pruefen.

Kennzeichnung der Belegklassen (strikt getrennt):

- **(A)** von LinkedIn selbst belegt (Engineering-Blog, Hilfe-Center, LinkedIn-News/Nonprofit-Hub, User Agreement, LinkedIn-Mitarbeiter in eigener Veroeffentlichung).
- **(B)** Sekundaerquelle oder empirische Drittstudie. Methodik, Stichprobe und Interessenlage der Quelle sind dazugeschrieben.
- **(C)** Annahme, Spekulation, Folklore oder nicht belegbar.

## Kurzantwort

1. LinkedIn belegt selbst nur wenig Konkretes zu Post-Taktik. Belegt (A): Der Feed wird seit Maerz 2026 per LLM-gestuetzter Suche und sequenziellem Transformer-Ranking bedient, ausgewertet werden u. a. Verweildauer, Likes, Kommentare, Shares. LinkedIn will Engagement-Bait und recycelte "Thought Leadership" ohne Substanz abwerten. Es gibt keine offizielle Aussage zu Hashtag-Zahl, Post-Laenge, Posting-Zeit, "erste Stunde" oder Link-Abschlag in den abgerufenen Primaerquellen (eine Link-Aussage nur ueber Dritte, siehe 1.4).
2. Die meisten verbreiteten Zahlen (60 % weniger Reichweite mit Link, Carousel-Multiplikatoren, ideale Zeichenzahl, ideale Uhrzeit) stammen aus Marketing-Blogs oder Tool-Anbietern mit teils widerspruechlichen oder undokumentierten Methoden (B) bzw. sind Folklore (C).
3. Fuer einen Release-Post spricht belegt wenig fuer "mehr stopfen": LinkedIn-eigene Hinweise sind nur "ersten Satz ansprechend machen", "grosse Textbloecke vermeiden", Dokument mit kurzem Begleittext (A). Alles Weitere ist Handwerksempfehlung (B/C).
4. KI-Transparenz: LinkedIn verlangt nach den abgerufenen Quellen **keine** Kennzeichnung von KI-Nutzung in Texten (A, User Agreement, Stand 2025-11-03). Es gibt ein C2PA-Symbol fuer Bilder/Videos mit signierten Content Credentials (A). Der Autor haftet fuer Richtigkeit und Policy-Konformitaet (A). Ob man KI-Nutzung nennt, ist damit eine Stil- und Glaubwuerdigkeitsentscheidung, keine Pflicht.
5. Das Repo enthaelt keinen Motivationstext: `docs/agents/wayfinding.md` beschreibt nur Prozessregeln der Wayfinder-Maps (Merging, Grilling-, Prototype- und Research-Tickets), keine Projektmotivation. Die Map selbst (GitHub-Issue) wurde nicht gelesen; die Motivation muss aus ihr oder vom Owner kommen (Abschnitt 5).

## 1. Was LinkedIn ueber Ranking und Reichweite selbst belegt (A)

### 1.1 Feed-Architektur seit 2026

Quelle: LinkedIn Engineering, Hristo Danchev, "Engineering the next generation of LinkedIn's Feed", 2026-03-12, `https://www.linkedin.com/blog/engineering/feed/engineering-the-next-generation-of-linkedins-feed`.

- Das Ranking nutzt einen "Generative Recommender": Die Interaktionshistorie eines Mitglieds wird als Sequenz behandelt (mehr als tausend vergangene Interaktionen), statt jede Impression einzeln zu bewerten.
- Eingabe pro Post (Item): Format, Autoreninfo (Name, Headline, Unternehmen, Branche), Engagement-Zaehler, Artikel-Metadaten, Posttext.
- Mitgliederaktionen: "long dwells, likes, comments, shares" werden mit den Posts verwoben. Passive Ziele sind "click, skip, long-dwell", aktive "like, comment, share".
- Die Seite sagt nichts zu Links als Rankingfaktor (nur "article metadata" als Feld). Sie sagt nichts zu Hashtags, Postlaenge, Uhrzeit oder KI-Inhalten.
- Auditmodelle sollen sicherstellen, dass "posts from different creators compete on equal footing". Das Ranking nutzt nach eigener Aussage keine demografischen Merkmale.

Quelle: LinkedIn-Ankuendigung, wiedergegeben von Social Media Today, 2026-03-12, `https://www.socialmediatoday.com/news/linkedin-updates-its-feed-algorithm/814638/` (Social Media Today ist eine Zeitschrift, die LinkedIns Aussage zitiert; als Wiedergabe zu lesen, die Originalseite wurde hier nicht abgerufen):

- LinkedIn will "repetitive, click-driven posts" reduzieren, Engagement-Bait filtern (Beispiel: "Kommentiere Yes, wenn du zustimmst") und recycelte Thought Leadership ohne Substanz abwerten. Umsetzung "over the next few months".
- Relevante Posts zu aktuellen Themen sollen "within minutes, not hours" sichtbar werden. Das ist die einzige zeitbezogene Aussage; sie ist keine Aussage zur "ersten Stunde nach dem Posten".

### 1.2 Aeltere Engineering-Posts (Hintergrund, evtl. ueberholt)

- "A Look Behind the AI that Powers LinkedIn's Feed", Tim Jurka et al., 2018-03-29, `https://www.linkedin.com/blog/engineering/feed/a-look-behind-the-ai-that-powers-linkedins-feed-sifting-through`: Klick allein gilt als "very simplistic" Ziel, "time spent reading" ist ein Signal. Keine Aussage zu Links oder Kommentaren.
- "Community-focused Feed optimization", 2019-06-25, `https://www.linkedin.com/blog/engineering/feed/community-focused-feed-optimization`: Zielgroesse "contribution" (Teilen, Kommentieren, Reagieren), Ersteller sollen zeitnahe Reaktionen bekommen. Die Architektur ist durch die 2026er Umstellung ueberholt.

### 1.3 360Brew (Papier, zurueckgezogen)

Quelle: `https://arxiv.org/abs/2501.16450` (abgerufen 2026-11-15): LinkedIn-Autoren beschreiben ein 150B-Parameter-Decoder-Modell fuer Ranking und Empfehlung, v1 vom 2025-01-27, letzte Version 2025-08-23. Die Seite markiert das Papier als **zurueckgezogen** (Lizenzgrund laut arXiv, nicht inhaltlich). Das Papier belegt nur, dass LinkedIn daran forscht; es belegt nicht, dass 360Brew den Produktiv-Feed bedient. Die Engineering-Seite von 2026 (1.1) nennt "Generative Recommender", nicht 360Brew. Die vielen Marketing-Blogs zu "360Brew im Feed" sind damit nicht durch diese Primaerquellen gedeckt (B/C).

### 1.4 Links im Post

- Keine der abgerufenen LinkedIn-Primaerquellen sagt, dass Links Reichweite senken oder nicht senken.
- (B, ungeprueft) Ein Zoomsphere-Blog schreibt LinkedIns Senior Director Product Management Rishi Jobanputra zu: "We don't intentionally limit the reach of posts with links in it." Die Seite nennt weder Datum noch Originalquelle (nur interner Link). Siehe Abschnitt 2.
- (B, ungeprueft) Buffer schreibt Laura Lorenzetti (LinkedIn News) zu: "Our algorithm is really focusing on the value and content that's being shared in the post." Das ist ein Zitat ueber Dritte; die Originalquelle habe ich nicht gefunden (Abschnitt 7).

### 1.5 Hashtags, Postlaenge, Posting-Zeit, Kommentare

Keine offizielle Aussage gefunden zu: Hashtag-Anzahl, Zeichenlaenge, Uhrzeit, Antworten auf Kommentare. Die LinkedIn-Hilfeseite zu Hashtags war unter der probierten URL (`/help/linkedin/answer/92930`) nicht abrufbar (HTTP 404). Eine Zeichengrenze fuer Posts konnte ich nicht belegen.

### 1.6 Format-Hinweise von LinkedIn selbst

Quelle: LinkedIn Nonprofit Resource Hub, "Types of posts", `https://nonprofit.linkedin.com/resource-hub/posting/types-of-posts` (Fusszeile "(c) LinkedIn Corporation 2026"; Richtung Nonprofits, trotzdem von LinkedIn):

- Text: "Avoid big blocks of text", "Make your first sentence engaging".
- Einzelbild: mindestens 552 x 276 px, maximal 5 MB; immer Text und Alt-Text dazu.
- Mehrere Bilder: einheitliche Groessen, Alt-Text, Begleittext.
- Video: "Aim for videos between 30 and 60 seconds long", Untertitel.
- Dokumente: relevanten Titel, kurzer Begleitpost, Executive Summary bei grossen Dateien, @Erwaehnungen fuer Mitwirkende.
- Reposts mit eigenem Kontext.
- Die Seite sagt **nichts** zu Hashtags, Links oder Uhrzeiten.

Quelle: LinkedIn-Hilfe "Document uploads on LinkedIn FAQ", `https://www.linkedin.com/help/linkedin/answer/a523054` und `https://www.linkedin.com/help/linkedin/answer/a518909`: Dokumente (PDF, DOC, DOCX, PPT, PPTX) bis 100 MB und 300 Seiten, ein Dokument pro Post, Titel erforderlich. Der Begriff "Carousel" kommt auf der Seite nicht vor; ein PDF-Dokument-Post wird in der Praxis so genannt (C: Sprachgebrauch, nicht Produktbegriff).

### 1.7 Engagement-Bait und Authentizitaet

Quelle: Zusammenfassung in der LinkedIn-Sales-Navigator-Community (deutsch), 2026-05-26, `https://scommunity.linkedin.com/deutschsprachige-gruppe-25/linkedin-feed-update-mehr-relevanz-weniger-engagement-bait-8941`, die auf Tim Jurkas LinkedIn-Artikel verweist (`https://www.linkedin.com/pulse/updates-linkedin-feed-focusing-authentic-relevant-tim-jurka-umwnc/`; der Artikel selbst war hier nicht abrufbar, HTTP 403/nur Vorschau): weniger Reichweite fuer "Comment YES"-Posts, Engagement-Pods und automatisierte Kommentare; Videos ohne Bezug zum Inhalt staerker eingeschraenkt; bevorzugt wird Inhalt mit konkreten Erkenntnissen. Die Community-Seite ist von einem LinkedIn-Learning-Konto, also nahe an A, aber eine Zusammenfassung. Zu KI sagt sie nichts.

## 2. Sekundaerquellen und Drittstudien (B)

Keine dieser Zahlen ist von LinkedIn bestaetigt. Jeweils Methodik und Interessenlage angegeben.

| Thema | Aussage | Quelle, Methodik, Einordnung |
|---|---|---|
| Posting-Zeit | Werktags 15 bis 20 Uhr, Mittwoch am besten, 16 Uhr Mittwoch Spitzenslot; Morgen (6 bis 11 Uhr) schwaecher; "no universal best time" | Buffer, 2026-09-09, 4,8 Mio. Posts, nur Posts die ueber Buffer verschickt wurden, Metrik Reaktionen+Kommentare+Reposts, Zeitzonen normalisiert. Buffer nennt selbst die Einschraenkungen und dass LinkedIns eigener Blog Dienstag nennt. `https://buffer.com/resources/best-time-to-post-on-linkedin/`. Aeltere Buffer-Fassungen nennen andere Stunden (laut Suchergebnis). Vendor-Daten, Anbieter verkauft Planungstool. |
| Posting-Frequenz | 2 bis 5 Posts pro Woche, kleiner Lift | Buffer, 2 Mio.+ Posts, 94.000+ Konten (laut Suchzusammenfassung, Seite selbst nicht abgerufen), `https://buffer.com/resources/how-often-to-post-on-linkedin/`. Fuer einen einzelnen Release-Post nur am Rande relevant. |
| Kommentare beantworten | Antworten auf Kommentare koennen Engagement um ca. 30 % erhoehen | Buffer, 72.000 Posts, `https://buffer.com/library/linkedin-algorithm/`. Korrelation, kein Kausalnachweis. |
| Hashtags | Drei oder weniger relevante; Hashtags seien "nice to have, not a need to have" | Buffer, gleiche Seite; die Aussage wird dort Laura Lorenzetti (LinkedIn News) zugeschrieben, die Originalquelle habe ich nicht gefunden. Andere Blogs nennen 2 bis 5, 3 bis 5 oder bis zu 30 Hashtags; keine offizielle LinkedIn-Zahl abrufbar. |
| Links | Buffer: Posts mit Link bekommen im Schnitt weniger Engagement als andere Formate; Link im Kommentar scheine nicht zu schaden | Buffer, gleiche Seite, eigene Daten ohne genannte Details. Gegenbild: andere Quellen nennen 60 % weniger Reichweite (Dataslayer, ohne Methodik, Tool-Anbieter), 18,8 % Median-Abschlag bei einem Link (Richard van der Blom, 1,3 Mio. Posts, laut wechangeminds, Report selbst nicht abgerufen), +5 % (mercermackay, Quelle unklar). Die Zahlen widersprechen sich. |
| Dokument-Posts/Carousels | Hoechste Engagement-Rate unter den Formaten (Buffer: "clear winner"); Dataslayer: 6,60 % Engagement, 3x bis 6x Multiplikatoren | Dataslayer, `https://dataslayer.ai/blog/linkedin-algorithm-february-2026-whats-working-now`, 2026-02-12 mit Update 2026-07: keine Stichprobengroesse, Zahlen ohne Basis, intern inkonsistent ("3x" in der Ueberschrift, "40 %+" im Fazit), Anbieter von Analytics-Tools. Als Richtung brauchbar, als Zahl nicht. |
| Postlaenge | 500 bis 999 Zeichen beste Relation zur Eigenbasis; unter 200 Zeichen ca. 33 % schlechter | Trevor Fox, 7.913 Posts von 63 Profilen, Einzelautor mit Analytics-Anbieter (Suchzusammenfassung, Seite selbst nicht abgerufen). Kleine Stichprobe. Andere Blogs nennen 1.200 bis 2.100 Zeichen (Text) bzw. 900 bis 1.200 (mit Bild), ohne belastbare Methodik. |
| Reichweite allgemein | Reichweite/Engagement sanken 2025 gegenueber Vorjahr (Zahlen 47 bis 50 %, je nach Quelle verschieden) | Richard van der Blom "Algorithm Insights", Sekundaerzitate; Report selbst nicht abgerufen. Van der Blom verkauft Beratung und den Report. |
| KI-Texte | Likely-AI-Posts: ca. 45 % weniger Likes/Kommentare (Studie 2024); 2025: Menschliche Posts meist besser, Technologie ungefaehr gleich, Leadership/Inspiration besser fuer KI; ueber 50 % der langen Posts seien wahrscheinlich KI | Originality.ai (Anbieter eines KI-Detektors, also interessengeleitet), `https://originality.ai/blog/linkedin-ai-study-engagement`, 2024: 8.795 Posts, 2025: 99 Profile, Posts unter 100 Woertern ausgeschlossen; Detektion ist probabilistisch, Korrelation. Zusaetzlich van der Blom (ueber 2 Mio. Posts, Methodik nicht gefunden): menschliche Originale ca. 34 % mehr Reichweite. |
| "Golden Hour" | Siehe Abschnitt 3 | |

Qualitaet der Quellenlage insgesamt: ueberwiegend Anbieter von Planungs-, Analytics- oder Beratungsprodukten, die vom Thema leben. Selbst Buffer (relativ transparent) misst nur seine eigenen Nutzer.

## 3. Folklore und Annahmen (C), geprueft

| Behauptung | Befund |
|---|---|
| "Links im Post senken die Reichweite" | **Nicht entscheidbar.** Keine LinkedIn-Primaerquelle bestaetigt oder widerlegt es. Ein zugeschriebenes Zitat eines LinkedIn-Managers ("we don't intentionally limit...") ist nur ueber ein Marketing-Blog ohne Originalquelle belegt. Drittdaten widersprechen sich (Abschnitt 2). Selbst wenn Links im Schnitt weniger Engagement bekommen, kann das daran liegen, dass Link-Posts andere Inhalte tragen oder Mitglieder nicht klicken wollen. Ehrliche Einordnung: unbelegt, aber plausibel moderat. |
| "Link in den ersten Kommentar verschieben hilft" | Nur Buffer (B) meint, es scheine nicht zu schaden; Dataslayer behauptet das Gegenteil ohne Quelle. Offen. |
| "Die erste Stunde ist entscheidend" | **Nicht belegt.** LinkedIn hat den Begriff nach den hier gefundenen Quellen nie offiziell genutzt. Eine Suchzusammenfassung nennt eine Auswertung 2026, nach der die erste Stunde im Median nur 7 % der 30-Tage-Impressions ausmacht; die Quelle ist Vendor-Inhalt und wurde nicht im Original geprueft. Die LinkedIn-Aussage "within minutes, not hours" (1.1) betrifft Nachrichtenthemen, nicht die erste Stunde nach dem Posten. Praktisch: Zeit fuer Antworten auf Kommentare einplanen kostet nichts, ist aber keine belegte Algorithmus-Regel. |
| "Hashtags erhoehen die Reichweite" | Eine LinkedIn-Quelle gibt es nicht; die einzige LinkedIn-Naehe ist das Buffer-Zitat "nice to have". Ein Blog nennt Hashtags "Steuerrad statt Gaspedal". Unbelegt. |
| "LinkedIn erkennt und bestraft KI-Text" | **Nicht belegt.** Zoomsphere behauptet das Gegenteil (LinkedIn "cannot detect AI-written content", ohne Quelle). LinkedIn kuendigt an, repetitive/inhaltsleere Beitraege und Engagement-Bait abzuwerten (A, 1.1), das ist kein KI-Detektor. |
| "Dokument-Posts/Carousels haben die beste Reichweite" | Aus Drittquellen (Abschnitt 2) plausibel, in LinkedIns eigenen Quellen nicht quantifiziert. |
| "Ideale Zeichenzahl 1.300 bis 2.000" | Sehr viele Blogs, keine belastbare Primaerquelle. Nur die LinkedIn-Hinweise "erste Zeile ansprechend", "keine Textbloecke" sind A. |
| "360Brew rankt den Feed" | Siehe 1.3: Papier zurueckgezogen, Engineering-Blog 2026 nennt es nicht. Unbelegt. |
| "Fruehe Uhrzeit/Hauptzeit bringt kaum Lift" (Zoomsphere) | Unzitiert (B/C). |

Unwissen offen benannt: Es gibt hier keine belastbare Aussage dazu, wie das Ranking nach dem Maerz-2026-Umbau mit einem Link im Post oder mit einem PDF-Dokument umgeht, und keine fuer die Zielgruppe "Java-Entwickler im deutschsprachigen Raum". Alle Reichweitenzahlen stammen aus dem allgemeinen LinkedIn-Publikum (Marketing, Vertrieb, Creator).

## 4. Umfang und Format fuer einen Release-Post

Hier mischen sich Belege und Handwerk; jede Zeile ist klassifiziert.

- **Ein Post, eine Aussage (C, Handwerksempfehlung).** Belegt ist nur, dass LinkedIn den ersten Satz und fehlende Textbloecke empfiehlt (A, 1.6). Fuer die Begruendung "nicht zu viel stopfen" gibt es keine Primaerquelle; sie ergibt sich aus Lesbarkeit auf dem Handy und der LinkedIn-Praxis, Dokumente fuer Tiefe zu nutzen (A: "executive summary" bei grossen Dokumenten).
- **Was ein Release-Post sinnvoll trägt (Vorschlag, C):** (1) was ist neu (ein bis zwei Kernpunkte), (2) fuer wen nuetzlich, (3) ein konkretes Beispiel (Code vorher/nachher als Bild), (4) wo man es findet (Link), (5) eine ehrliche Frage oder Einladung zum Feedback. Alles andere (Changelog, Architektur, Tooling-Details) gehoert in den Link/README/Release-Notes. Der Post ist Eintrittsstelle, nicht Dokumentation.
- **Laenge (B/C):** Primaerquellen geben keine Zahl. Drittdaten streuen von ca. 500 bis ca. 2.000 Zeichen (Abschnitt 2). Eine Zahl dazwischen zu waehlen ist Geschmack, keine Evidenz.
- **Grafik/Format (alle B/C, ausser den A-Eckwerte aus 1.6):**
  - Ein einzelnes Bild mit Code-Vergleich (Annotation vorher, generierter Matcher nachher). A-Eckwert: min. 552 x 276 px, max. 5 MB, Alt-Text.
  - Dokument-Post (PDF, "Carousel"): geeignet, wenn die Geschichte in 4 bis 8 Folien erzaehlt werden soll (Motivation, Loesung, Beispiel, Link). A: bis 100 MB und 300 Seiten, ein Dokument pro Post, Titel und kurzer Begleittext. Nachteil laut Praxis: Aufwand; Quellen zur besten Folienzahl (5 bis 12) sind unbelegt (B/C).
  - Video 30 bis 60 s: A-Empfehlung nur fuer Videos allgemein; fuer ein Bibliotheks-Release unnoetig aufwendig.
  - Code als Screenshot: Bild braucht Alt-Text mit dem Code (A: Alt-Text fuer Bilder). Barrierefreiheit: Code nicht nur als Bild liefern, Kernzeilen auch im Text oder im verlinkten README.
- **Link-Platzierung (offen, siehe Abschnitt 3):** Link im Post, im ersten Kommentar oder nur im Profil/Featured. Keine belastbare Evidenz fuer eine Variante. Pragmatisch: Der Link ist das Ziel des Posts und gehoert fuer Leser gut sichtbar hin; wenn Reichweite gegen Auffindbarkeit abgewogen wird, ist das eine Entscheidung des Owners.
- **Hashtags (offen):** 0 bis 3 relevante (#Java, #OpenSource, ggf. #Hamcrest) am Ende ist Konsens der Blogs (B); LinkedIn-Quelle nur "nice to have" ueber Buffer. Niedriges Risiko, niedriger belegter Nutzen.
- **Posting-Zeit (B):** Buffer: Mittwoch bis Freitag nachmittags; keine A-Quelle. Fuer die Zielgruppe Entwickler nicht geprueft.
- **Kommentare/Interaktion:** A: Kommentare sind Signal, LinkedIn wertet aber Engagement-Bait ab (1.1). Keine Aufrufe wie "Kommentiere X". Echte Fragen (z. B. "Welche Matcher-Art fehlt euch?") sind keine Bait im Sinn der Beispiele von LinkedIn, aber die Grenze zieht LinkedIn nicht trennscharf (C). Auf Kommentare antworten: Buffer 30 % (B, Korrelation).
- **Zielgruppe/Kanal (C):** Die Release-Information erreicht Java-Entwickler vermutlich zuverlaessiger ueber Maven-Central-Seite, GitHub-Release, Reddit/Foren; LinkedIn ist eher Sichtbarkeit fuer Netzwerk und Recruiter. Die Nutzungs-Recherche (`docs/research/nutzung-und-verbreitung.md`) zeigt sehr kleine messbare Nutzung (8 Sterne, 0 externe Dependents); realistische Erwartung an Reichweite ist dementsprechend zu setzen.

## 5. Motivation, KI-Einsatz und Effizienz glaubwuerdig und knapp behandeln

Vorab: `docs/agents/wayfinding.md` enthaelt **keine** Projektmotivation, sondern Prozessregeln (Merging per Auto-Merge, Grilling-, Prototype-, Research-Tickets). Auch `docs/research/` und `GLOSSARY.md` enthalten das Wort "Motivation" nicht (grep, 2026-11-15). Die Motivation der Wayfinder-Map wurde nicht gelesen; die folgenden Hinweise sind allgemein. Wenn die Map die Motivation (z. B. "Hamcrest-Matcher ohne Boilerplate", Modernisierung auf 6.x, Kotlin/AssertJ-Backend) enthaelt, ist sie die Quelle fuer den Post, nicht diese Datei.

### 5.1 Was LinkedIn zu KI-Kennzeichnung vorgibt (A)

- **Keine Kennzeichnungspflicht fuer KI-Text.** LinkedIn User Agreement, gueltig seit 2025-11-03, `https://www.linkedin.com/legal/user-agreement`, Abschnitt 3.6: KI-generierte Inhalte "might be inaccurate, incomplete, delayed, misleading or not suitable for your purposes"; Mitglieder sind dafuer verantwortlich, dass das Ergebnis die Professional Community Policies einhaelt (auch unabhaengig davon, ob es als "AI" gekennzeichnet ist). Abschnitt 4.1: Richtigkeit selbst pruefen. Keine Offenlegungspflicht in diesem Text. Zusaetzlich verbietet 8.2(18) manipulierte Medien, die Personen etwas Falsches sagen/tun lassen (nicht einschlaegig fuer ein Release).
- **C2PA-Symbol fuer Bilder/Videos.** LinkedIn Help "Content credentials", `https://www.linkedin.com/help/linkedin/answer/a6282984`: Bilder/Videos mit signierten C2PA Content Credentials erhalten ein C2PA-Symbol; es sei "not yet possible to identify and label all AI-generated and modified content". Die Seite verlangt keine Selbstauskunft. LinkedIn-News-Meldung 2024-05-15, `https://news.linkedin.com/2024/May/linkedin-rolls-out-c2pa-ai-generated-content-standard`: Symbol soll Herkunft KI-erzeugter Medien nachvollziehbar machen. Praktisch: Wenn das Bild mit einem KI-Generator erzeugt wurde, der C2PA schreibt, kann LinkedIn automatisch ein Symbol anzeigen; ob das so passiert, haengt vom Werkzeug ab (Metadaten gehen bei Screenshot/Neukomprimierung verloren, laut Suchzusammenfassung, nicht von mir an LinkedIn-Quelle belegt).
- **Policies:** Professional Community Policies, `https://www.linkedin.com/legal/professional-community-policies` (nur ueber Suchtreffer-Zusammenfassung gelesen): falsche oder irrefuehrende Inhalte und Deepfakes sind verboten. Eine KI-Kennzeichnungspflicht fand ich dort nicht.
- Eine dritte Meinung (LinkedIn-Pulse-Artikel eines Mitglieds, kein LinkedIn-Standpunkt): Kennzeichnung sei freiwillig, Richtigkeit wichtiger. Nicht als Beleg zu verwenden, nur als Einordnung.

Daraus folgt: Es gibt keine LinkedIn-Vorgabe, den KI-Einsatz zu nennen. Wenn der Post ihn nennt, ist das freiwillige Transparenz.

### 5.2 Hinweise fuer die Darstellung (C, Handwerksempfehlung; jeweils markiert, wo Daten dafuer existieren)

- **Konkret statt Behauptung.** "KI hat mir geholfen" ist leer. Besser eine ueberpruefbare Aussage, z. B. welcher Teil (Tickets in der Wayfinder-Map, Tests, Doku) mit Agenten entstand und wie er abgesichert wurde (Mutation-Testing, Golden-Files, Verify-Matrix; sie stehen in `CLAUDE.md` und `docs/agents/testing.md`). Der Repo-Stand gibt das her: 910 Commits von `marmer`, 17 von `claude` (Quelle: `docs/research/nutzung-und-verbreitung.md`, Stand 2026-11-15). Solche Zahlen nur nennen, wenn sie zum Veroeffentlichungszeitpunkt noch stimmen.
- **Effizienzgewinn nur mit Beleg.** Keine Faktor-Behauptung ohne Messung ("10x schneller"). Wenn gemessen: Was, womit verglichen, wie lange. Andernfalls qualitativ und ehrlich ("hat Routinearbeit uebernommen, Review blieb bei mir"). Das passt zur Linie von LinkedIn gegen inhaltsleere Beitraege (A, 1.1) und zur Erfahrung der Drittstudien, dass generische KI-Texte schlechter laufen (B, korrelativ, interessengeleitete Quelle).
- **Grenzen nennen.** Eine Zeile, was nicht gut ging oder wo Handarbeit noetig war, wirkt glaubwuerdiger als reine Erfolgsmeldung (C, keine Studie dazu gefunden).
- **Haftung und Pruefung (A).** Autor haftet fuer Richtigkeit (User Agreement 3.6/4.1). Der Post-Text und jedes Code-Beispiel muessen gegen die Release-Version geprueft sein, bevor sie veroeffentlicht werden.
- **Eigene Stimme (B/C).** Studien (Originality.ai, van der Blom) und die LinkedIn-Linie "Substanz statt Wiederverwertung" sprechen fuer persoenlich geschriebenen Text; wenn KI beim Entwurf geholfen hat, ist ein vom Owner ueberarbeiteter Text, der eigene Entscheidungen benennt, die sicherere Linie. Das ist keine Detektionsfrage (C: LinkedIn-Erkennung von KI-Text ist unbelegt).
- **Reihenfolge im Post (Vorschlag):** Hook = Nutzen/Neuheit des Releases; dann ein Satz Motivation (Problem, das der Matcher-Generator loest); KI/Effizienz hoechstens ein bis zwei Saetze oder ein Absatz in einem Dokument-Post; Link; Frage. KI/Effizienz als Hauptthema machen die Bibliothek zur Nebensache. Das ist eine Entscheidung (siehe Abschnitt 6), keine Evidenz.
- **Kennzeichnung:** Wenn Bilder KI-generiert sind (z. B. Titelgrafik), kurz im Alt-Text oder Fliesstext nennen; fuer Code-Screenshots aus echten Laeufen ist das unnoetig. Keine Pflicht (A).

## 6. Offene Entscheidungen fuers Ticket/Grilling

1. **Ziel des Posts:** Auffindbarkeit der Bibliothek, Feedback von Java-Entwicklern, Sichtbarkeit des Maintainers/der Methode, oder Nachweis fuer Recruiter/Netzwerk? Das bestimmt Ton, Link und Kanal. Reicht LinkedIn allein, oder zusaetzlich Reddit/Foren/GitHub-Release (letztere Kanaele wurden hier nicht recherchiert)?
2. **Umfang:** Ein Post, oder Post + Dokument (PDF mit 4 bis 8 Folien) + Kommentar? Wie viele Kernpunkte (Vorschlag 1 bis 2)? Was gehoert explizit nicht hinein?
3. **Themenmix:** Release-Nutzen zuerst, Motivation als Nebensatz, KI/Effizienz als eigener kurzer Absatz oder als eigener Folge-Post? Oder ist die Entwicklungsmethode (Wayfinder-Map + Agenten) der eigentliche Inhalt, und die Bibliothek das Beispiel? Beides gleichzeitig auf einem Handy-Post ist selten lesbar.
4. **KI-Offenlegung:** Nennen wir sie (freiwillig, keine LinkedIn-Pflicht)? Mit welchem konkreten Beleg (Anteil Commits, Reviews, Tests)? Welche Grenzen/Fehlschlaege kommen mit rein? Wird der Post-Text selbst mit KI entworfen, und wird das erwaehnt?
5. **Effizienzaussage:** Gibt es eine echte Messung (Zeit, Aufwand, Fehler), oder bleibt die Aussage qualitativ? Soll eine Zahl genannt werden, die der Owner belegen kann?
6. **Grafikart:** Code-Vergleich als Einzelbild, PDF-Dokument ("Carousel"), Architekturskizze (arc42/Structurizr), Kurzvideo oder gar keine Grafik? Wer erstellt sie, wie wird Alt-Text/Barrierefreiheit sichergestellt? Wird die Grafik KI-generiert (dann C2PA-Hinweis klaeren)?
7. **Link-Platzierung:** Im Post, im ersten Kommentar oder nur im Profil/Featured? Gewichtung Reichweite (unklar belegt) gegen Auffindbarkeit (sicher).
8. **Hashtags:** 0, 1 bis 3? Welche (#Java, #OpenSource, #Hamcrest, #Kotlin, #AssertJ)? Hashtags im Text oder am Ende?
9. **Zeitpunkt:** Direkt nach dem Release oder spaeter? Wochentag/Uhrzeit nach Buffer (B) oder nach eigener Erfahrung? Wer ist verfuegbar, um in den ersten Stunden auf Kommentare zu antworten (ohne Annahme, dass dies belegt wirkt)?
10. **Interaktion:** Welche echte Frage stellt der Post am Ende? Keine Aufforderungen im Stil "Kommentiere Ja". Wer antwortet, in welcher Sprache?
11. **Sprache und Zielgruppe:** Deutsch, Englisch oder zweisprachig? Java-Entwickler oder breiteres Netzwerk? Ist der Post persoenlich (Maintainer-Profil) oder ueber eine Seite/Organisation?
12. **Erfolgsmessung:** Was zaehlt als Erfolg (Impressions, Klicks, Sterne, Maven-Central-Abrufe, Feedback)? Nutzungsdaten sind laut `docs/research/nutzung-und-verbreitung.md` nur eingeschraenkt messbar (keine GitHub-Traffic-Rechte, keine Download-Zahlen); Link mit UTM-Parameter waere ein Weg, der den Release-Link veraendert.
13. **Aktualitaet:** Wann wird die Algorithmus-Einordnung erneuert (Quellen vom Maerz bis September 2026)? Vor Veroeffentlichung die Jurka- und Lorenzetti-Originalquellen direkt lesen (Abschnitt 7).

## 7. Nicht belegbar oder nicht abgerufen

- Tim Jurkas LinkedIn-Artikel vom Maerz 2026 (`.../updates-linkedin-feed-focusing-authentic-relevant-tim-jurka-umwnc/`) war per WebFetch nicht lesbar (HTTP 403 bei der aehnlichen Jurka-URL; die eigentliche URL nur ueber Community-Seite bekannt, nicht abgerufen). Alle Engagement-Bait-Details stammen aus der LinkedIn-Community-Zusammenfassung und Social Media Today.
- Laura Lorenzettis Zitate (Links, Hashtags) nur ueber Buffer; eine Suche nach der Originalquelle fand sie nicht. Das Jobanputra-Zitat nur ueber Zoomsphere ohne Originalquelle.
- LinkedIn-Hilfeseite zu Hashtags (`/help/linkedin/answer/92930`): HTTP 404. Eine offizielle Hashtag-Empfehlung konnte ich nicht belegen. Ebenso keine offizielle Zeichengrenze fuer Posts und keine offizielle Aussage zu Posting-Zeit, "erste Stunde", Postlaenge.
- Die `a527313`-Hilfeseite (Feed-Funktionsweise) war nicht auffindbar (HTTP 404); die LinkedIn-Hilfeseiten zu Professional Community Policies und "False or misleading content" habe ich nur ueber Suchtreffer-Zusammenfassungen gelesen, nicht im Wortlaut abgerufen.
- Richard van der Bloms Report, Trevor Foxs Auswertung, Buffers Frequenz-Studie: nur ueber Suchzusammenfassungen oder Zweitzitate, Originale nicht im Detail geprueft. Ihre Zahlen sind als Richtung, nicht als Fakt zu behandeln.
- Wirkung von Links, Dokument-Posts und KI-Texten auf die Reichweite im **neuen** Ranking (ab 2026-03): nicht belegt, weder durch LinkedIn noch durch eine saubere Drittstudie; Drittzahlen vor und nach dem Umbau sind nicht trennbar.
- Wayfinder-Map (GitHub-Issue) und ihre Motivation: nicht gelesen; `docs/agents/wayfinding.md` enthaelt dazu nichts.
- Zielgruppenspezifische Daten (deutschsprachige Java-Community auf LinkedIn): keine gefunden.

## Fazit

LinkedIn selbst belegt nur das Grobe: Der Feed bewertet Verweildauer und Interaktionen, wurde im Maerz 2026 auf LLM-Suche und einen sequenziellen Ranker umgestellt und soll Engagement-Bait und inhaltsleere Wiederverwertung abwerten. Fuer Links, Hashtags, Uhrzeit, "erste Stunde" und ideale Laenge gibt es keine Primaerbelege; was kursiert, ist Marketing-Datenlage mit teils widerspruechlichen Zahlen oder Folklore. Ein Release-Post sollte deshalb nach Lesbarkeit und Inhalt gebaut werden, nicht nach vermeintlichen Algorithmus-Tricks: ein bis zwei Kernpunkte, ein konkretes Beispiel, ein Link, eine echte Frage. KI-Nutzung und Effizienz sind frei (keine Kennzeichnungspflicht, Haftung beim Autor) und wirken am glaubwuerdigsten, wenn sie kurz, konkret, belegt und mit Grenzen genannt werden. Die offenen Punkte aus Abschnitt 6, vor allem Ziel, Themenmix und Link-Platzierung, muss der Owner entscheiden.
