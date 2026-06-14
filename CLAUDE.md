# Slagalica — Dizajn sistem (CLAUDE.md)

Android implementacija mobilne kviz aplikacije **Slagalica**. Vizuelni jezik je
**„Sunny pop" (pravac C)** — razigran, game-show stil: zaobljeni oblici, topli
narandžasto→pink gradijenti, topla neutralna pozadina, dve teme (svetla + tamna).

**Izvor istine za dizajn** je `design/slagalica-mobilne-aplikacije-projekat/project/`:
- `Slagalica - Dizajn sistem.html` — tokeni (boje, tipografija, oblik, razmak, senke, komponente, principi).
- `slagalica.css` — kompletan CSS sa svim CSS varijablama i klasama komponenti (najdetaljniji izvor).
- `Slagalica - Ekrani.html` / `*-print.html` i `sl-*.jsx` — prototipovi svih 21 ekrana.

HTML/CSS/JSX su **prototipovi, ne produkcioni kod**. Cilj je pixel-perfect rekreacija u
Android XML/Java — ne kopirati strukturu prototipa, već vizuelni rezultat. Ne renderovati
fajlove u browseru; sve dimenzije/boje su u CSS-u.

Mobilni okvir prototipa: **384×832 px** ekran (telefon).

---

## 0. Mapiranje CSS → Android resursi

CSS varijable su već prevedene u Android resurse. **Koristi postojeće resurse**, ne hardkoduj vrednosti.

| CSS | Android | Fajl |
|-----|---------|------|
| `--accent`, `--accent-end`, `--accent2` | `@color/accent`, `@color/accent_end`, `@color/accent2` | `values/colors.xml` |
| `--text`, `--muted`, `--faint` | `@color/text`, `@color/muted`, `@color/faint` | `values/colors.xml` |
| `--g0..--g3` | `@color/g0..g3` | `values/colors.xml` |
| `--correct(-bg)`, `--wrong(-bg)` | `@color/correct`, `@color/correct_bg`, `@color/wrong`, `@color/wrong_bg` | `values/colors.xml` |
| `--radius-sm/--radius/--radius-lg` | `@dimen/radius_sm` (13dp), `@dimen/radius` (20dp), `@dimen/radius_lg` (28dp) | `values/dimens.xml` |
| `--gap` (13px) | `@dimen/gap` (13dp), `@dimen/screen_pad` (18dp) | `values/dimens.xml` |
| `--font-display` (Baloo 2) | `@font/baloo2` | `font/baloo2.xml` |
| `--font-body` (Nunito) | `@font/nunito` | `font/nunito.xml` |
| tema na `<html data-sl-theme>` | `values/` (light) + `values-night/` (dark) | `themes.xml` |

Gradijenti, kartice, polja, čipovi → `drawable/bg_*.xml` (vidi §6).
Tema, dugmad, tekst stilovi → `values/themes.xml` (`Theme.Slagalica`, `Widget.Slagalica.*`, `Text.*`).

---

## 1. Boje

### Brend i akcenat (isto u obe teme)
- **Accent** `#FF6A3D` (`--accent`) — primarna boja / glavna akcija.
- **Accent End** `#FF4D7D` (`--accent-end`) — kraj gradijenta.
- **Accent gradijent** 135°: `#FF6A3D → #FF4D7D` — hero, primarna dugmad, bedževi.
- **Ring / Zlatna** `#FFB020` (`--ring`) — okviri avatara, liga.

### Game boje — kodiranje 6 mini-igara i kategorija (boja = značenje)
| Var | Light | Dark | Igra |
|-----|-------|------|------|
| `--g0` | `#FF5A5F` | `#FF6A5F` | Ko zna zna |
| `--g1` | `#13B6A6` | `#2FD3C0` | Spojnice |
| `--g2` | `#FFB020` | `#FFC24B` | Asocijacije |
| `--g3` | `#7A6BFF` | `#9A8BFF` | Skočko |

(Logo „slagalica" pločice koriste iste 4 boje.)

### Svetla tema — površine i tekst (`data-sl-theme="light"`)
- Pozadina `--bg-solid` `#FFF6EC`; radijalni gradijent `--bg-grad` (`#FFF3E4 → #FFF8F0 → #FDF2E8`).
- Panel/kartica `--panel` `#FFFFFF`; Polje `--field` `#FBF2E9`; ivica polja `#EFE0D2`.
- Tekst `--text` `#34272F`; Prigušen `--muted` `#A8968F`; Bledo `--faint` `#CDBCB0`.
- Track (prazna traka) `#F0E3D6`; ivica čipa `--pill-border` `#F1E4D6`.

### Tamna tema — topli candy dark (`data-sl-theme="dark"`)
- Pozadina `--bg-solid` `#241820`; gradijent `#3A2630 → #271A22 → #1B1218`.
- Panel `--panel-solid` `#2E2029` (`--panel` = `rgba(255,255,255,.06)`); Tekst `#FDEEDE`; Prigušen `#B59A93`.
- Senke jače/tamnije, ivice prozirno bele (`rgba(255,255,255,.08)`).

### Semantičke boje (povratne informacije)
- **Tačno** `--correct` `#1FAE6A` (bg `#E6F7EE`).
- **Netačno** `--wrong` `#EC4D52` (bg `#FDEAEA`).
- **Sekundarni akcenat / progres** `--accent2` `#13B6A6`.
- **Zvezde** `--star-c` `#FF7A3D`; **Tokeni** `#E9920A`.

---

## 2. Tipografija

Dva pisma:
- **Baloo 2** (`@font/baloo2`) — naslovi, brojevi, dugmad, imena igara. Težine 700/800. Zaobljeno, igrivo.
- **Nunito** (`@font/nunito`) — telo teksta, oznake, podnaslovi, čet. Težine 600/700/800. Čitko.
- **ui-monospace** — podaci/mape (npr. „MAPA SRBIJE · OpenStreetMap").

Telo aplikacije je Nunito (postavljeno u `Theme.Slagalica`); naslovi/dugmad eksplicitno traže `@font/baloo2`.

| Uloga | Font · težina · veličina | dimen |
|-------|--------------------------|-------|
| Display L (Bravo!) | Baloo 2 · 700 · 38–44sp | — |
| Naslov ekrana | Baloo 2 · 700 · 30sp | `text_hero` |
| Naslov sekcije | Baloo 2 · 700 · 21–22sp | `text_h2` / `text_title` |
| Kartica / pitanje | Baloo 2 · 700 · 18–19sp | `text_lg` |
| Telo / odgovor | Nunito · 700 · 14–15sp | `text_body` |
| Oznaka (label) | Nunito · 700/800 · 10–11sp · CAPS · letterSpacing | `text_xxs` |
| Mono / podaci | ui-monospace · 11–12sp | `text_xs` |

Dugmad: `textAllCaps=false`, `letterSpacing=0` (Baloo 2 je već karakteran).

---

## 3. Oblik (radius)

Skala (množilac `--rk` u CSS-u; u Androidu fiksne dp vrednosti):
- Small `@dimen/radius_sm` **13dp** — manja dugmad, ivične pločice.
- Default `@dimen/radius` **20dp** — kartice, polja, čipovi-pravougaonici.
- Large `@dimen/radius_lg` **28dp** — hero, prof/stats kartice, baneri.
- Pill **999dp** (`@dimen/radius_pill`) — čipovi, segmenti, pretraga.
- Krug **50%** — avatari, timer/win prstenovi.

---

## 4. Razmak i gustina

- Osnovni razmak između blokova `--gap` = **13dp** (`@dimen/gap`).
- Horizontalni padding ekrana **18dp** (`@dimen/screen_pad`).
- Guste liste 7–10dp; sekcije 18–28dp.
- Minimalna meta za dodir ~44dp.
- (CSS množi razmak faktorom `--dk` 0.75–1.3; u Androidu fiksno.)

---

## 5. Dubina (senke i ivice)

Meke „sticker" senke daju razigran reljef.
- Card (light) `0 6px 14px -8px rgba(120,80,40,.3)` → `--card-shadow`.
- Card (dark) `0 10px 22px -12px rgba(0,0,0,.6)`.
- Hero / accent `0 16px 30px -12px accent/.5`.
- Play (čvrsta) `0 5px 0 rgba(0,0,0,.08)` — efekat „dugmeta sa dnom".
- Ivica `--pill-border` 1px.

Android nema box-shadow kao CSS; aproksimirati sa `elevation` + zaobljenim `bg_*` drawable, ili
mekanim ivicama. Čvrstu „play" senku raditi kao layer-list sa pomerenim donjim slojem.

---

## 6. Komponente → drawables i stilovi

Gradivni elementi koji se ponavljaju kroz svih 21 ekran. Postojeći `drawable/`:

| Element | Drawable / stil |
|---------|-----------------|
| Primarno dugme (accent gradijent) | `bg_accent_button.xml` / `Widget.Slagalica.Button` |
| Sekundarno (ghost) dugme | `Widget.Slagalica.Button.Ghost` |
| Accent pill (valuta/bedž) | `bg_accent_pill.xml` |
| Kartica / panel | `bg_card.xml`, `bg_card_lg.xml` |
| Polje (input/field) | `bg_field.xml`, `bg_field_pill.xml` |
| Čip | `bg_chip.xml` |
| Hero baner | `bg_hero.xml` |
| Pozadina ekrana (gradijent) | `bg_gradient.xml` |
| Odgovor (Ko zna zna) | `bg_answer.xml`, `bg_answer_correct.xml`, `bg_answer_wrong.xml` |
| Pločica igre (game tile) | `bg_tile_g0..g3.xml` |
| Tagovi (liga/region) | `bg_tag_league.xml`, `bg_tag_region.xml` |
| Avatar okvir (ring) | `avatar_frame.xml` |
| Skočko slot/feedback | `skocko_symbol_slot.xml`, `skocko_feedback_circle.xml` |
| Moj broj pločice/operatori | `mb_numbers.xml`, `mb_numbers_big.xml`, `mb_operators.xml` |

**Obrasci komponenti** (iz `slagalica.css`):
- **Čip/valuta** (`.pill`, `.chip`): panel + ivica + senka, pill ili 20dp radius, ikonica-tačka + broj.
- **Progres bar** (`.m-bar`, `.lvl-bar`): track pozadina, popuna `accent` ili `accent2`, 5–8dp, radius 3–4dp.
- **Pločica igre** (`.gtile`): 76×92dp, simbol 40dp u game-boji, naziv ispod.
- **Avatar + okvir** (`.av`): krug, 3dp ivica `ring`, gradijent pozadina, inicijali Baloo 2.
- **Tačno/netačno** (`.ans.correct/.wrong`): obojena pozadina + 1.5dp ivica + oznaka ✓/✗.
- **Prsten** (timer/win, `.timer`/`.win-ring`): `conic-gradient` u CSS-u → u Androidu progress ring/custom view, accent popuna na track.

---

## 7. Vizuelni principi

1. **Razigrano, ne dečje** — zaobljeni oblici i topli gradijenti daju game-show energiju; tipografija i razmaci ostaju čisti i odrasli.
2. **Boja = značenje** — 6 game boja dosledno kodira igre/kategorije; akcenat je rezervisan za glavnu akciju na ekranu.
3. **Jedan fokus po ekranu** — svaki ekran ima jasnu primarnu akciju (hero, dugme); ostalo je podrška, prigušenih boja.
4. **Topla neutrala** — sive su vučene ka braon/krem da se slažu sa narandžasto-pink akcentom (nikad hladne sive).

---

## 8. Ekrani (prototipovi u `sl-*.jsx`)

21 ekran, grupisani: `sl-home` (početna), `sl-auth` (onboarding/login/register/potvrda),
`sl-games`/`sl-games2` (Ko zna zna, Spojnice, Asocijacije, Skočko, Korak po korak, Moj broj),
`sl-result` (rezultat partije + dijalog nagrade), `sl-profile` (profil + statistika),
`sl-compete` (rang, regioni, lige, turnir, izazov), `sl-social` (prijatelji, čet, notifikacije, dnevne misije).
Pri implementaciji ekrana, pronaći odgovarajući `.jsx` + CSS klase (`slagalica.css`) za tačan raspored.

---

## 9. Build

JDK 21 obavezan (toolchain pinovan na 21). PowerShell:
```
$jdk="C:\Program Files\Android\Android Studio1\jbr"   # JBR 21 (NE "Android Studio")
$env:JAVA_HOME=$jdk
& ".\gradlew.bat" :app:assembleDebug --console=plain `
  "-Dorg.gradle.java.installations.auto-download=false" `
  "-Dorg.gradle.java.installations.paths=$jdk"
```
Prvi build mora online (preuzima `aapt2`); nakon keširanja radi `--offline`.
minSdk 30, Java izvorna kompatibilnost 11, Gradle 9.4.x.

## 10. Konvencije

- Jezik: **Java** (bez Kotlin-a). UI paket: `com.example.slagalica.ui.*`.
- Arhitektura cilj: tri sloja **data / domain / ui** (trenutno postoji samo `ui`).
- Nikad ne hardkoduj boje/dimenzije u layout-ima — referenciraj `@color/*`, `@dimen/*`, `@font/*`, `@style/*`.
- Globalna navigacija (profil, statistika, prijatelji, rang liste, start) dostupna svuda **osim tokom igre**.
- Stalno vidljiv header: tokeni, broj zvezda, liga. Tokom igre: zaključaj nav, prikaži igre redom, dozvoli predaju.
