/**
 * Repositorio (proyecto) del workspace ágil.
 * Los contadores los calcula el backend a partir de los elementos persistidos.
 */
export interface Proyecto {
  id: number;
  titulo: string;
  descripcion: string | null;
  fechaCreacion: string;
  propietario: string;
  propio: boolean;
  epicas: number;
  historias: number;
}

/**
 * Datos enviados al endpoint de creación de repositorios.
 */
export interface CrearProyectoRequest {
  titulo: string;
  descripcion: string;
}
