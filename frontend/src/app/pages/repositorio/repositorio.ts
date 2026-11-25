import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { combineLatest, forkJoin, of } from 'rxjs';
import { ToastComponent } from '../../components/toast/toast';
import { ActualizarElementoRequest, CrearDirectorioRequest, Elemento, EpicaResumen } from '../../models/elemento.model';
import { Proyecto } from '../../models/proyecto.model';
import { ElementoService } from '../../services/elemento.service';
import { ProyectoService } from '../../services/proyecto.service';
import { SessionService } from '../../services/session.service';

type ContentFilter = 'all' | 'directorio' | 'epica' | 'historia';
type ElementModal = 'directorio' | 'ver' | 'editar' | 'eliminar' | null;
type ToastType = 'success' | 'error';

/**
 * Página de detalle de un repositorio.
 * Muestra el contenido del backlog y los accesos a las plantillas de creación.
 */
@Component({
  selector: 'app-repositorio',
  imports: [RouterLink, ReactiveFormsModule, ToastComponent],
  templateUrl: './repositorio.html',
  styleUrl: './repositorio.css',
})
export class Repositorio {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly proyectoService = inject(ProyectoService);
  private readonly elementoService = inject(ElementoService);
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly directorioForm = this.formBuilder.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
  });
  protected readonly editForm = this.formBuilder.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.maxLength(2000)]],
    identificador: ['', [Validators.maxLength(50)]],
    reglasNegocio: ['', [Validators.maxLength(4000)]],
    criteriosAceptacion: ['', [Validators.maxLength(4000)]],
    estado: ['en desarrollo', [Validators.required, Validators.maxLength(20)]],
    idEpicaPadre: [null as number | null],
    color: ['', [Validators.maxLength(50)]],
  });
  protected readonly modal = signal<ElementModal>(null);
  protected readonly selectedElement = signal<Elemento | null>(null);
  protected readonly viewElement = signal<Elemento | null>(null);
  protected readonly saving = signal(false);

  protected readonly projectId = signal(0);
  protected readonly directorioId = signal<number | null>(null);
  protected readonly proyecto = signal<Proyecto | null>(null);
  protected readonly directorio = signal<Elemento | null>(null);
  protected readonly elementos = signal<Elemento[]>([]);
  protected readonly resumenEpicas = signal<EpicaResumen[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal('');
  protected readonly filter = signal<ContentFilter>('all');
  protected readonly searchTerm = signal('');

  protected readonly username = signal(this.session.getUsername() || 'agileflow');
  protected readonly initials = computed(
    () => this.username().replace(/[^a-zA-Z0-9]/g, '').slice(0, 2).toUpperCase() || 'AF',
  );
  protected readonly ownerInitials = computed(
    () =>
      (this.proyecto()?.propietario ?? '')
        .replace(/[^a-zA-Z0-9]/g, '')
        .slice(0, 2)
        .toUpperCase() || 'AF',
  );

  protected readonly directorios = computed(() =>
    this.elementos().filter((element) => element.tipo === 'DIRECTORIO'),
  );
  protected readonly epicas = computed(() =>
    this.elementos().filter((element) => element.tipo === 'EPICA'),
  );
  protected readonly historias = computed(() =>
    this.elementos().filter((element) => element.tipo === 'HISTORIA'),
  );

  protected readonly treeDirectorios = computed(() =>
    this.elementos().filter((element) => element.tipo === 'DIRECTORIO'),
  );
  protected readonly treeEpicas = computed(() =>
    this.elementos().filter((element) => element.tipo === 'EPICA'),
  );
  protected readonly treeHistorias = computed(() =>
    this.elementos().filter((element) => element.tipo === 'HISTORIA'),
  );

  protected readonly filtered = computed(() => {
    const currentFilter = this.filter();
    const term = this.searchTerm().trim().toLowerCase();

    return this.elementos().filter((element) => {
      if (
        element.tipo !== 'DIRECTORIO' &&
        element.tipo !== 'EPICA' &&
        element.tipo !== 'HISTORIA'
      ) {
        return false;
      }
      if (currentFilter === 'directorio' && element.tipo !== 'DIRECTORIO') {
        return false;
      }
      if (currentFilter === 'epica' && element.tipo !== 'EPICA') {
        return false;
      }
      if (currentFilter === 'historia' && element.tipo !== 'HISTORIA') {
        return false;
      }
      if (!term) {
        return true;
      }
      return (
        element.titulo.toLowerCase().includes(term) ||
        (element.identificador ?? '').toLowerCase().includes(term) ||
        (element.tituloPadre ?? '').toLowerCase().includes(term)
      );
    });
  });

  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<ToastType>('success');

  /** Épicas desplegadas en el árbol de "Historias por épica". */
  protected readonly expandedEpics = signal<ReadonlySet<number>>(new Set<number>());
  protected readonly allEpicsExpanded = computed(() => {
    const epicas = this.resumenEpicas();
    return epicas.length > 0 && epicas.every((epica) => this.expandedEpics().has(epica.id));
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.projectId.set(Number.isFinite(id) && id > 0 ? id : 0);

    const routeSubscription = combineLatest([
      this.route.paramMap,
      this.route.queryParamMap,
    ]).subscribe(([params, queryParams]) => {
      const fromPath = Number(params.get('directorioId'));
      const fromQuery = Number(queryParams.get('directorioId'));
      const directorioId = fromPath > 0 ? fromPath : fromQuery;
      this.directorioId.set(
        Number.isFinite(directorioId) && directorioId > 0 ? directorioId : null,
      );
      this.filter.set('all');
      this.searchTerm.set('');
      this.load();
    });

    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      routeSubscription.unsubscribe();
    });
  }

  /** Carga el repositorio y sus elementos desde la API. */
  protected load(): void {
    if (this.projectId() === 0) {
      this.loading.set(false);
      this.loadError.set('El repositorio solicitado no es válido.');
      return;
    }

    this.loading.set(true);
    this.loadError.set('');
    const directorioId = this.directorioId();
    forkJoin({
      proyecto: this.proyectoService.obtener(this.projectId()),
      // El resumen es del proyecto completo, por eso no depende del directorio abierto.
      resumenEpicas: this.elementoService.listarResumenEpicas(this.projectId()),
      directorio: directorioId
        ? this.elementoService.obtenerElemento(this.projectId(), directorioId)
        : of(null),
      elementos: directorioId
        ? this.elementoService.listarPorProyecto(this.projectId(), directorioId)
        : this.elementoService.listarPorProyecto(this.projectId()),
    }).subscribe({
      next: ({ proyecto, resumenEpicas, directorio, elementos }) => {
        this.proyecto.set(proyecto);
        this.resumenEpicas.set(resumenEpicas);
        this.directorio.set(directorio);
        this.elementos.set(elementos);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.loadError.set(this.errorMessage(error, 'No se pudo cargar el repositorio.'));
        if (error.status === 401) {
          this.logout();
        }
      },
    });
  }

  /** Cambia el filtro de contenido visible. */
  protected setFilter(filter: ContentFilter): void {
    this.filter.set(filter);
  }

  /** Actualiza el término de búsqueda. */
  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  /** Abre la página propia de un directorio, igual a la del repositorio. */
  protected openDirectory(element: Elemento): void {
    if (element.tipo !== 'DIRECTORIO') {
      return;
    }
    this.router.navigate(['/repositorios', this.projectId(), 'directorios', element.id]);
  }

  /** Abre un directorio desde el árbol o filtra por el elemento elegido. */
  protected selectElement(element: Elemento): void {
    if (element.tipo === 'DIRECTORIO') {
      this.openDirectory(element);
      return;
    }
    this.filter.set('all');
    this.searchTerm.set(element.identificador ?? element.titulo);
  }

  /** Vuelve a la vista raíz del repositorio. */
  protected openRoot(): void {
    this.router.navigate(['/repositorios', this.projectId()]);
  }

  /** Limpia la búsqueda y vuelve a mostrar todo el contenido. */
  protected clearSearch(): void {
    this.searchTerm.set('');
    this.filter.set('all');
  }

  /** Abre el modal para crear un directorio en el repositorio. */
  protected openDirectorioModal(): void {
    this.directorioForm.reset();
    this.selectedElement.set(null);
    this.modal.set('directorio');
  }

  /** Cierra el modal activo. */
  protected closeModal(): void {
    this.modal.set(null);
  }

  /** Crea el directorio y refresca el contenido del repositorio. */
  protected submitDirectorio(): void {
    if (this.directorioForm.invalid || this.saving()) {
      this.directorioForm.markAllAsTouched();
      return;
    }

    const request: CrearDirectorioRequest = {
      titulo: this.directorioForm.getRawValue().titulo.trim(),
      idElementoPadre: this.directorioId(),
    };
    this.saving.set(true);
    this.elementoService.crearDirectorio(this.projectId(), request).subscribe({
      next: (directorio) => {
        this.saving.set(false);
        this.modal.set(null);
        this.showToast(`Directorio "${directorio.titulo}" creado correctamente.`, 'success');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.handleElementError(error, 'No se pudo crear el directorio.');
      },
    });
  }

  /** Muestra el detalle de un elemento cargándolo desde la API. */
  protected openView(element: Elemento): void {
    this.selectedElement.set(element);
    this.viewElement.set(element);
    this.modal.set('ver');
    this.elementoService.obtenerElemento(this.projectId(), element.id).subscribe({
      next: (detalle) => this.viewElement.set(detalle),
      error: (error: HttpErrorResponse) =>
        this.handleElementError(error, 'No se pudo cargar el detalle del elemento.'),
    });
  }

  /** Carga un elemento en el formulario para poder modificar sus campos. */
  protected openEdit(element: Elemento): void {
    if (element.tipo === 'HISTORIA') {
      this.router.navigate(
        ['/repositorios', this.projectId(), 'historias', element.id, 'editar'],
        {
          queryParams: this.directorioId() ? { directorioId: this.directorioId() } : null,
        },
      );
      return;
    }

    this.selectedElement.set(element);
    this.modal.set('editar');
    this.elementoService.obtenerElemento(this.projectId(), element.id).subscribe({
      next: (detalle) => {
        this.selectedElement.set(detalle);
        this.editForm.patchValue({
          titulo: detalle.titulo,
          descripcion: detalle.descripcion ?? '',
          identificador: detalle.identificador ?? '',
          reglasNegocio: detalle.reglasNegocio ?? '',
          criteriosAceptacion: detalle.criteriosAceptacion ?? '',
          estado: detalle.estado ?? 'en desarrollo',
          idEpicaPadre: detalle.idEpica,
          color: detalle.color ?? '',
        });
      },
      error: (error: HttpErrorResponse) =>
        this.handleElementError(error, 'No se pudo cargar el elemento para editarlo.'),
    });
  }

  /** Guarda los cambios del elemento y refresca el listado. */
  protected submitEdit(): void {
    const element = this.selectedElement();
    if (!element || this.saving()) {
      return;
    }
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const value = this.editForm.getRawValue();
    const request: ActualizarElementoRequest = {
      titulo: value.titulo.trim(),
      version: element.version,
      descripcion: value.descripcion.trim(),
    };

    if (element.tipo === 'HISTORIA') {
      request.identificador = value.identificador.trim();
      request.reglasNegocio = value.reglasNegocio;
      request.criteriosAceptacion = value.criteriosAceptacion;
      request.estado = value.estado;
      request.idEpicaPadre = value.idEpicaPadre;
    } else if (element.tipo === 'DIRECTORIO') {
      request.color = value.color.trim();
    }

    this.saving.set(true);
    this.elementoService.actualizarElemento(this.projectId(), element.id, request).subscribe({
      next: (actualizado) => {
        this.saving.set(false);
        this.modal.set(null);
        this.showToast(`Elemento "${actualizado.titulo}" actualizado correctamente.`, 'success');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        if (error.status === 409) {
          this.modal.set(null);
          this.load();
        }
        this.handleElementError(error, 'No se pudo actualizar el elemento.');
      },
    });
  }

  /** Abre la confirmación para eliminar un elemento. */
  protected openDelete(element: Elemento): void {
    this.selectedElement.set(element);
    this.modal.set('eliminar');
  }

  /** Elimina el elemento y refresca el listado. */
  protected confirmDelete(): void {
    const element = this.selectedElement();
    if (!element || this.saving()) {
      return;
    }

    this.saving.set(true);
    this.elementoService.eliminarElemento(this.projectId(), element.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.modal.set(null);
        this.showToast(`Elemento "${element.titulo}" eliminado correctamente.`, 'success');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.handleElementError(error, 'No se pudo eliminar el elemento.');
      },
    });
  }

  /** Cierra el modal con la tecla Escape. */
  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.modal.set(null);
  }

  /** Cierra sesión y vuelve al login. */
  protected logout(): void {
    this.session.clear();
    this.router.navigate(['/login']);
  }

  /** Despliega o contrae las historias de una épica del árbol. */
  protected toggleEpic(epicaId: number): void {
    const next = new Set(this.expandedEpics());
    if (!next.delete(epicaId)) {
      next.add(epicaId);
    }
    this.expandedEpics.set(next);
  }

  /** Indica si una épica está desplegada en el árbol. */
  protected isEpicExpanded(epicaId: number): boolean {
    return this.expandedEpics().has(epicaId);
  }

  /** Despliega o contrae todas las épicas del árbol. */
  protected toggleAllEpics(): void {
    if (this.allEpicsExpanded()) {
      this.expandedEpics.set(new Set<number>());
      return;
    }
    this.expandedEpics.set(new Set(this.resumenEpicas().map((epica) => epica.id)));
  }

  /** Devuelve las clases de color de la etiqueta según el estado de la historia. */
  protected estadoBadgeClass(estado: string | null): string {
    switch (estado) {
      case 'aprobada':
        return 'bg-green-100 text-green-700';
      case 'rechazada':
        return 'bg-red-100 text-red-700';
      case 'en desarrollo':
      case 'en proceso':
        return 'bg-orange-100 text-orange-700';
      default:
        return 'bg-slate-100 text-slate-600';
    }
  }

  /** Muestra un toast y programa su ocultamiento automático. */
  protected showToast(message: string, type: ToastType): void {
    clearTimeout(this.toastTimer);
    this.toastType.set(type);
    this.toastMessage.set(message);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3500);
  }

  /** Maneja errores de las operaciones sobre elementos, incluidos los 403 del proxy. */
  private handleElementError(error: HttpErrorResponse, fallback: string): void {
    if (error.status === 401) {
      this.logout();
      return;
    }
    this.showToast(this.errorMessage(error, fallback), 'error');
  }

  /** Traduce un error HTTP a un mensaje para el usuario. */
  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return typeof error.error?.message === 'string' ? error.error.message : fallback;
  }
}
