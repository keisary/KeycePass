"""
Script de nettoyage de la base de données KeycePass.
Supprime les étudiants, émargements et séances pour repartir proprement.
La DB est dans desktopApp/database/keycepass_central.db
"""

import sqlite3
import os

# Chemin de la DB (relatif à la racine du projet)
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "desktopApp", "database", "keycepass_central.db")

def clean_db():
    if not os.path.exists(DB_PATH):
        print(f"[!] Base de données introuvable : {DB_PATH}")
        return

    print(f"[*] Connexion à : {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)
    cur  = conn.cursor()

    # Désactiver les contraintes FK temporairement pour simplifier
    cur.execute("PRAGMA foreign_keys = OFF")

    tables_a_vider = {
        "emargements":    "Emargement",
        "seances":        "Seance",
        "semaines":       "SeanceSemaine",
        "etudiants":      "Etudiant",
    }

    for label, table in tables_a_vider.items():
        try:
            cur.execute(f"DELETE FROM {table}")
            print(f"  [OK] Table '{table}' vidée ({cur.rowcount} lignes supprimées)")
        except sqlite3.OperationalError as e:
            print(f"  [!] Table '{table}' ignorée : {e}")

    # Réinitialiser les auto-incréments
    for table in tables_a_vider.values():
        try:
            cur.execute(f"DELETE FROM sqlite_sequence WHERE name='{table}'")
        except Exception:
            pass

    cur.execute("PRAGMA foreign_keys = ON")
    conn.commit()
    conn.close()
    print("\n[OK] Base de données nettoyée. Vous pouvez relancer generate_etudiants.py et importer.")

if __name__ == "__main__":
    clean_db()
