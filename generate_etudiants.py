"""
Script de génération du fichier Excel de test pour KeycePass.
Produit : etudiants_test.xlsx à la racine du projet.

Format attendu par ImportService :
  Colonne 0 : matricule
  Colonne 1 : nom
  Colonne 2 : prenom
  Colonne 3 : classe_id
"""

import subprocess
import sys

# ── Installer openpyxl si absent ──────────────────────────────────────────────
try:
    import openpyxl
except ImportError:
    print("[*] openpyxl absent — installation en cours...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "openpyxl"])
    import openpyxl

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
import os

# ── Données des 17 étudiants couvrant les 6 classes ──────────────────────────
ETUDIANTS = [
    # matricule          nom             prénom          classe_id
    ("2026_B2IT_001",  "Koffi",        "Jean-Emmanuel", "B2_IT"),
    ("2026_B2IT_002",  "Traore",       "Mariam",        "B2_IT"),
    ("2026_B2IT_003",  "Kouadio",      "Adjoua",        "B2_IT"),
    ("2026_B2IT_004",  "Diallo",       "Amadou",        "B2_IT"),
    ("2026_B2IT_005",  "Bamba",        "Kadiatou",      "B2_IT"),
    ("2026_B1IT_001",  "N'Guessan",    "Serge",         "B1_IT"),
    ("2026_B1IT_002",  "Coulibaly",    "Ibrahim",       "B1_IT"),
    ("2026_B1IT_003",  "Yao",          "Sandra",        "B1_IT"),
    ("2026_B1IT_004",  "Achi",         "David",         "B1_IT"),
    ("2026_B3IT_001",  "Allou",        "Olive",         "B3_IT"),
    ("2026_B3IT_002",  "Ekra",         "Therese",       "B3_IT"),
    ("2026_B3M_001",   "Ouattara",     "Awa",           "B3_MANAGEMENT"),
    ("2026_B3M_002",   "Soro",         "Herve",         "B3_MANAGEMENT"),
    ("2026_B3M_003",   "Kassi",        "Ketty",         "B3_MANAGEMENT"),
    ("2026_B2M_001",   "Ahou",         "Jean-Marc",     "B2_MANAGEMENT"),
    ("2026_B2M_002",   "Degny",        "Rachel",        "B2_MANAGEMENT"),
    ("2026_B1M_001",   "Gbongue",      "Ulrich",        "B1_MANAGEMENT"),
]

# ── Couleurs ──────────────────────────────────────────────────────────────────
COULEUR_HEADER   = "1E3A5F"   # Bleu foncé
COULEUR_TEXTE_H  = "FFFFFF"   # Blanc
COULEUR_LIGNE_P  = "EBF3FB"   # Bleu très clair (lignes paires)
COULEUR_LIGNE_I  = "FFFFFF"   # Blanc (lignes impaires)

COULEUR_CLASSE = {
    "B2_IT":         "D6EAF8",  # Bleu clair
    "B1_IT":         "D5F5E3",  # Vert clair
    "B3_IT":         "D2B4DE",  # Violet clair
    "B3_MANAGEMENT": "FEF9E7",  # Jaune clair
    "B2_MANAGEMENT": "FDEDEC",  # Rouge clair
    "B1_MANAGEMENT": "F4ECF7",  # Lavande
}

def build_border():
    side = Side(style="thin", color="CCCCCC")
    return Border(left=side, right=side, top=side, bottom=side)

def build_excel(output_path: str):
    wb = Workbook()
    ws = wb.active
    ws.title = "Etudiants"

    # ── En-tête ───────────────────────────────────────────────────────────────
    headers = ["matricule", "nom", "prenom", "classe_id"]
    header_font   = Font(bold=True, color=COULEUR_TEXTE_H, name="Calibri", size=11)
    header_fill   = PatternFill("solid", fgColor=COULEUR_HEADER)
    header_align  = Alignment(horizontal="center", vertical="center")
    border        = build_border()

    for col_idx, header in enumerate(headers, start=1):
        cell = ws.cell(row=1, column=col_idx, value=header)
        cell.font      = header_font
        cell.fill      = header_fill
        cell.alignment = header_align
        cell.border    = border

    ws.row_dimensions[1].height = 20

    # ── Données ───────────────────────────────────────────────────────────────
    for row_idx, (matricule, nom, prenom, classe_id) in enumerate(ETUDIANTS, start=2):
        row_data = [matricule, nom, prenom, classe_id]
        couleur_bg = COULEUR_CLASSE.get(classe_id, "FFFFFF") if row_idx % 2 == 0 else COULEUR_LIGNE_I
        fill = PatternFill("solid", fgColor=couleur_bg)

        for col_idx, val in enumerate(row_data, start=1):
            cell = ws.cell(row=row_idx, column=col_idx, value=val)
            cell.fill      = fill
            cell.border    = border
            cell.alignment = Alignment(vertical="center")
            cell.font      = Font(name="Calibri", size=10)

        ws.row_dimensions[row_idx].height = 16

    # ── Largeurs des colonnes ─────────────────────────────────────────────────
    col_widths = [20, 16, 18, 20]
    for i, width in enumerate(col_widths, start=1):
        ws.column_dimensions[get_column_letter(i)].width = width

    # ── Figer la ligne d'en-tête ──────────────────────────────────────────────
    ws.freeze_panes = "A2"

    # ── Filtre automatique ────────────────────────────────────────────────────
    ws.auto_filter.ref = f"A1:D{len(ETUDIANTS) + 1}"

    wb.save(output_path)
    return output_path


if __name__ == "__main__":
    racine = os.path.dirname(os.path.abspath(__file__))
    sortie = os.path.join(racine, "etudiants_test.xlsx")

    print(f"[*] Génération de {sortie} ...")
    build_excel(sortie)
    print(f"[OK] Fichier cree : {sortie}")
    print(f"[INFO] {len(ETUDIANTS)} etudiants repartis dans les classes :")

    classes = {}
    for m, n, p, c in ETUDIANTS:
        classes.setdefault(c, []).append(f"{p} {n}")

    for classe, noms in sorted(classes.items()):
        print(f"    {classe} ({len(noms)}) : {', '.join(noms)}")
