# Fonts

Schriftdateien müssen manuell heruntergeladen und in diesem Verzeichnis abgelegt werden.
Sie sind aus lizenzrechtlichen Gründen nicht im Repository enthalten (*.ttf ist in .gitignore).

## Benötigte Dateien

### Nunito (UI-Schrift)
Quelle: https://fonts.google.com/specimen/Nunito
Lizenz: SIL Open Font License 1.1

- `nunito_regular.ttf`   — Weight 400
- `nunito_semibold.ttf`  — Weight 600
- `nunito_bold.ttf`      — Weight 700
- `nunito_extrabold.ttf` — Weight 800

### DM Mono (Monospace, Monatskürzel)
Quelle: https://fonts.google.com/specimen/DM+Mono
Lizenz: SIL Open Font License 1.1

- `dm_mono_regular.ttf`  — Weight 400
- `dm_mono_medium.ttf`   — Weight 500

## Schnell-Download (curl)

```bash
# Verzeichnis: app/src/main/res/font/
BASE="https://github.com/google/fonts/raw/main/ofl"

curl -L "$BASE/nunito/Nunito%5Bwght%5D.ttf"  -o nunito_variable.ttf
# Alternativ fixe Weights aus dem GitHub-Repo herunterladen
```

Oder: Dateien direkt über Android Studio herunterladen:
`res/font → New → More → Search "Nunito"` und `"DM Mono"`.
