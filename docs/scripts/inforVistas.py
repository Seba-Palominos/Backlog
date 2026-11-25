from pathlib import Path
import sys
import ollama
from agent_chat import BASE_DIR, MODEL_NAME, build_system_prompt

UI_DIR = BASE_DIR / "UI-UX"


def analyze_view(view_folder: str) -> None:
    """Lee los archivos HTML/CSS/JS de una pantalla en UI-UX/ y pide los endpoints/DTOs."""
    target_dir = UI_DIR / view_folder
    if not target_dir.exists():
        print(f"[Error] No existe la carpeta: {target_dir}")
        return

    files_content = []
    for ext in ("*.html", "*.css", "*.js"):
        for file_path in sorted(target_dir.rglob(ext)):
            text = file_path.read_text(encoding="utf-8").strip()
            files_content.append(f"### Archivo: {file_path.relative_to(BASE_DIR)}\n```text\n{text}\n```")

    if not files_content:
        print(f"[Aviso] No se encontraron archivos .html, .css o .js en {target_dir}")
        return

    prompt = (
        f"Analiza los archivos de interfaz de la vista '{view_folder}' mostrados abajo.\n"
        "Con base en el modelo relacional y las reglas de criterios.md:\n"
        "1. Lista los endpoints REST necesarios (método HTTP, ruta, request/response).\n"
        "2. Genera las clases DTO en Java (nombres en inglés, comentarios en español).\n"
        "3. Indica qué controlador y servicio de la arquitectura debe implementarlos.\n\n"
        + "\n\n".join(files_content)
    )

    messages = [
        {"role": "system", "content": build_system_prompt()},
        {"role": "user", "content": prompt},
    ]

    print(f"Analizando vista '{view_folder}' con {MODEL_NAME}...\n")
    response = ollama.chat(model=MODEL_NAME, messages=messages)
    print(response["message"]["content"])


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python scripts/inspect_ui.py <nombre_carpeta_en_UI-UX>")
        print("Ejemplo: python scripts/inspect_ui.py raw-stitch/login")
        sys.exit(1)

    analyze_view(sys.argv[1])