/**
 * Tipos de elemento que puede contener un repositorio.
 */
export type TipoElemento = 'DIRECTORIO' | 'EPICA' | 'HISTORIA' | 'ELEMENTO';

/**
 * Elemento del backlog (épica o historia de usuario).
 * El tipo DIRECTORIO se conserva solo para leer elementos ya persistidos.
 */
export interface Elemento {
  id: number;
  titulo: string;
  descripcion: string | null;
  tipo: TipoElemento;
  fechaCreacion: string;
  fechaUltModificacion: string;
  idElementoPadre: number | null;
  tituloPadre: string | null;
  identificador: string | null;
  reglasNegocio: string | null;
  criteriosAceptacion: string | null;
  estado: string | null;
  /** Épica a la que está asociada la historia (relación EsAsociado). */
  idEpica: number | null;
  tituloEpica: string | null;
  color: string | null;
  version: number;
}

/**
 * Datos mínimos de una historia asociada a una épica.
 */
export interface HistoriaResumen {
  id: number;
  identificador: string | null;
  titulo: string;
  estado: string | null;
}

/**
 * Resumen de una épica con las historias de usuario que tiene asociadas.
 */
export interface EpicaResumen {
  id: number;
  titulo: string;
  idElementoPadre: number | null;
  tituloPadre: string | null;
  totalHistorias: number;
  historias: HistoriaResumen[];
}

/**
 * Datos enviados al endpoint de creación de directorios.
 */export interface CrearDirectorioRequest {
  titulo: string;
  idElementoPadre?: number | null;
}

/**
 * Datos enviados al endpoint de creación de épicas.
 */
export interface CrearEpicaRequest {
  titulo: string;
  descripcion: string;
  idElementoPadre?: number | null;
}

/**
 * Datos enviados al endpoint de creación de historias de usuario.
 * El identificador se toma tal cual de la plantilla y se guarda en la entidad.
 */
export interface CrearHistoriaRequest {
  identificador: string;
  titulo: string;
  idEpicaPadre: number | null;
  idElementoPadre?: number | null;
  reglasNegocio: string;
  criteriosAceptacion: string;
  estado: string;
}

/**
 * Datos enviados al endpoint de modificación de un elemento.
 */
export interface ActualizarElementoRequest {
  titulo: string;
  version: number;
  descripcion?: string;
  identificador?: string;
  reglasNegocio?: string;
  criteriosAceptacion?: string;
  estado?: string;
  idEpicaPadre?: number | null;
  idElementoPadre?: number | null;
  color?: string;
}
