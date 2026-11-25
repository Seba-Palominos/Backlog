from datetime import datetime
from pathlib import Path
import ollama

# Raíz del proyecto Backlog/ (un nivel arriba de scripts/)
BASE_DIR = Path(__file__).resolve().parent.parent

# Archivos de reglas, contexto y base de datos que alimentan al agente
CONTEXT_FILES = [
    BASE_DIR / ".agent" / "context.md",
    BASE_DIR / ".agent" / "criterios.md",
    BASE_DIR / ".agent" / "feedback.md",
    BASE_DIR / "bd" / "modelo_relacional.md",
]

FEEDBACK_FILE = BASE_DIR / ".agent" / "feedback.md"

# Cambia este nombre por el modelo que descargaste en 'ollama list'
MODEL_NAME = "qwen2.5-coder:7b"


def load_project_context() -> str:
    """Lee todos los archivos .md configurados y arma el bloque de contexto."""
    sections = []
    for file_path in CONTEXT_FILES:
        if file_path.exists():
            content = file_path.read_text(encoding="utf-8").strip()
            if content:
                relative_path = file_path.relative_to(BASE_DIR)
                sections.append(f"### Archivo: {relative_path}\n{content}")
        else:
            print(f"[Aviso] No se encontró: {file_path.relative_to(BASE_DIR)}")
    return "\n\n---\n\n".join(sections)


def save_feedback(note: str) -> None:
    """Agrega una nueva entrada con fecha en .agent/feedback.md."""
    FEEDBACK_FILE.parent.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    entry = f"\n- **[{timestamp}]** {note.strip()}\n"

    with open(FEEDBACK_FILE, "a", encoding="utf-8") as f:
        f.write(entry)
    print(f"[OK] Retroalimentación guardada en {FEEDBACK_FILE.relative_to(BASE_DIR)}\n")


def build_system_prompt() -> str:
    """Construye el prompt inicial del sistema con las reglas del proyecto."""
    context = load_project_context()
    return (
        "Eres un arquitecto y desarrollador senior especializado en Java y Spring Boot. "
        "Asistes en el desarrollo del microservicio Backlog.\n"
        "Reglas obligatorias:\n"
        "1. Respeta estrictamente las reglas de criterios.md y el esquema de bd/modelo_relacional.md.\n"
        "2. Todo el código Java debe tener nombres de clases, métodos y variables en inglés, "
        "y todos los comentarios en español.\n"
        "3. Aplica el patrón Composite para directorios/elementos y Proxy para seguridad en modificaciones.\n\n"
        f"{context}"
    )


def run_chat() -> None:
    """Inicia el bucle interactivo de conversación con Ollama."""
    messages = [{"role": "system", "content": build_system_prompt()}]

    print(f"=== Agente Backlog activo ({MODEL_NAME}) ===")
    print("Comandos disponibles:")
    print("  /feedback <texto> -> Guarda una corrección o decisión en .agent/feedback.md")
    print("  /recargar         -> Vuelve a leer los archivos .md actualizados")
    print("  /salir            -> Cierra el chat\n")

    while True:
        user_input = input("Tú: ").strip()
        if not user_input:
            continue

        if user_input.lower() in {"/salir", "salir", "exit"}:
            print("Cerrando sesión del agente.")
            break

        if user_input.lower().startswith("/feedback "):
            note = user_input[len("/feedback "):].strip()
            if note:
                save_feedback(note)
                messages[0]["content"] = build_system_prompt()
            continue

        if user_input.lower() == "/recargar":
            messages[0]["content"] = build_system_prompt()
            print("[OK] Contexto recargado desde los archivos .md.\n")
            continue

        messages.append({"role": "user", "content": user_input})

        try:
            response = ollama.chat(model=MODEL_NAME, messages=messages)
            reply = response["message"]["content"]
            print(f"\nAgente:\n{reply}\n")
            messages.append({"role": "assistant", "content": reply})
        except Exception as e:
            print(f"\n[Error al conectar con Ollama]: {e}")
            print("Verifica que Ollama esté corriendo y que el modelo exista en 'ollama list'.\n")
            messages.pop()


if __name__ == "__main__":
    run_chat()