import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  HostListener,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ToastComponent } from '../../components/toast/toast';
import { Elemento } from '../../models/elemento.model';
import { CrearProyectoRequest, Proyecto } from '../../models/proyecto.model';
import { ElementoService } from '../../services/elemento.service';
import { ProyectoService } from '../../services/proyecto.service';
import { SessionService } from '../../services/session.service';

type ModalType = 'repo' | 'delete' | null;
type RepoTab = 'created' | 'shared';
type ViewMode = 'grid' | 'list';
type ToastType = 'success' | 'error';
type ChatTargetType = 'channel' | 'direct';

interface ChatMessage {
  author: string;
  initials: string;
  time: string;
  text: string;
  own: boolean;
  kind: 'text' | 'bot';
  avatarColor: string;
}

const INITIAL_CHAT_MESSAGES: ChatMessage[] = [
  {
    author: 'Marcos Rivera',
    initials: 'MR',
    time: '10:14 AM',
    text: 'Acabo de dividir el repo agile-core-platform y agregué el módulo /kanban-engine.',
    own: false,
    kind: 'text',
    avatarColor: '#0284c7',
  },
  {
    author: 'Nueva Épica Creada',
    initials: '',
    time: '',
    text: 'Sofía definió EPIC-01: Tablero Kanban Multiequipo (18 puntos de historia estimados).',
    own: false,
    kind: 'bot',
    avatarColor: '#9333ea',
  },
  {
    author: 'Elena Gómez (PO)',
    initials: 'EG',
    time: '10:22 AM',
    text: '¿Revisaron la plantilla de criterios Gherkin para la historia US-201? Ya está lista para estimación en la daily.',
    own: false,
    kind: 'text',
    avatarColor: '#d97706',
  },
  {
    author: 'Tú',
    initials: '',
    time: '10:28 AM',
    text: 'Perfecto Elena. En 10 min comenzamos el standup y asignamos los story points al sprint activo.',
    own: true,
    kind: 'text',
    avatarColor: '#1e293b',
  },
];

/**
 * Dashboard ágil y de repositorios.
 * Los repositorios y sus elementos se cargan desde la base de datos.
 */
@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule, ToastComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly formBuilder = inject(FormBuilder);
  private readonly proyectoService = inject(ProyectoService);
  private readonly elementoService = inject(ElementoService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly username = signal(this.readUsername());
  protected readonly initials = computed(
    () => this.username().replace(/[^a-zA-Z0-9]/g, '').slice(0, 2).toUpperCase() || 'AF',
  );

  protected readonly proyectosPropios = signal<Proyecto[]>([]);
  protected readonly proyectosCompartidos = signal<Proyecto[]>([]);
  protected readonly elementos = signal<Elemento[]>([]);
  protected readonly selectedProjectId = signal<number | null>(null);

  protected readonly activeTab = signal<RepoTab>('created');
  protected readonly viewMode = signal<ViewMode>('grid');
  protected readonly searchTerm = signal('');
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly deleting = signal(false);

  protected readonly profileMenuOpen = signal(false);
  protected readonly chatOpen = signal(true);
  protected readonly chatChannelsOpen = signal(false);
  protected readonly chatTarget = signal('devs-frontend');
  protected readonly chatTargetType = signal<ChatTargetType>('channel');

  protected readonly modal = signal<ModalType>(null);
  /** Repositorio seleccionado para confirmar su eliminación. */
  protected readonly projectToDelete = signal<Proyecto | null>(null);

  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<ToastType>('success');

  protected readonly repoForm = this.formBuilder.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.maxLength(1000)]],
  });

  protected readonly selectedProject = computed(() => {
    const id = this.selectedProjectId();
    if (id === null) {
      return null;
    }
    return (
      [...this.proyectosPropios(), ...this.proyectosCompartidos()].find(
        (project) => project.id === id,
      ) ?? null
    );
  });

  protected readonly artefactos = computed(() =>
    this.elementos().filter(
      (element) => element.tipo === 'EPICA' || element.tipo === 'HISTORIA',
    ),
  );

  protected readonly filteredOwned = computed(() => this.filterProjects(this.proyectosPropios()));
  protected readonly filteredShared = computed(() =>
    this.filterProjects(this.proyectosCompartidos()),
  );
  protected readonly filteredArtifacts = computed(() => this.filterArtifacts(this.artefactos()));

  protected readonly totalRepos = computed(
    () => this.proyectosPropios().length + this.proyectosCompartidos().length,
  );
  protected readonly totalEpicas = computed(() =>
    [...this.proyectosPropios(), ...this.proyectosCompartidos()].reduce(
      (total, project) => total + project.epicas,
      0,
    ),
  );
  protected readonly totalHistorias = computed(() =>
    [...this.proyectosPropios(), ...this.proyectosCompartidos()].reduce(
      (total, project) => total + project.historias,
      0,
    ),
  );

  protected readonly chatMessages = signal<ChatMessage[]>(INITIAL_CHAT_MESSAGES);
  protected readonly chatInput = signal('');

  constructor() {
    afterNextRender(() => this.loadProjects());
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
  }

  /** Carga los repositorios propios y compartidos desde la API. */
  protected loadProjects(selectId?: number): void {
    this.loading.set(true);
    forkJoin({
      owned: this.proyectoService.listarPropios(),
      shared: this.proyectoService.listarCompartidos(),
    }).subscribe({
      next: ({ owned, shared }) => {
        this.proyectosPropios.set(owned);
        this.proyectosCompartidos.set(shared);
        this.loading.set(false);

        const available = [...owned, ...shared];
        const currentId = selectId ?? this.selectedProjectId();
        const currentExists = currentId !== null && available.some((p) => p.id === currentId);
        const nextId = currentExists ? currentId : (owned[0]?.id ?? shared[0]?.id ?? null);

        this.selectedProjectId.set(nextId);
        if (nextId !== null) {
          this.loadElements(nextId);
        } else {
          this.elementos.set([]);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.handleError(error, 'No se pudieron cargar los repositorios.');
      },
    });
  }

  /** Carga los elementos del repositorio seleccionado. */
  private loadElements(projectId: number): void {
    this.elementoService.listarPorProyecto(projectId).subscribe({
      next: (elements) => this.elementos.set(elements),
      error: (error: HttpErrorResponse) => {
        this.elementos.set([]);
        this.handleError(error, 'No se pudieron cargar los elementos del repositorio.');
      },
    });
  }

  /** Selecciona un repositorio y muestra su contenido. */
  protected selectProject(project: Proyecto): void {
    this.selectedProjectId.set(project.id);
    this.loadElements(project.id);
  }

  /** Abre el detalle de un repositorio compartido. */
  protected openSharedProject(project: Proyecto): void {
    this.router.navigate(['/repositorios', project.id]);
  }

  /** Abre el detalle del repositorio seleccionado. */
  protected openProject(project: Proyecto): void {
    this.router.navigate(['/repositorios', project.id]);
  }

  /** Cambia la pestaña de repositorios. */
  protected switchTab(tab: RepoTab): void {
    this.activeTab.set(tab);
  }

  /** Cambia entre vista de cuadrícula y de lista. */
  protected setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
  }

  /** Actualiza el término de búsqueda del dashboard. */
  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  /** Alterna el menú de perfil. */
  protected toggleProfileMenu(): void {
    this.profileMenuOpen.update((open) => !open);
  }

  /** Alterna el panel de chat lateral. */
  protected toggleChatDrawer(): void {
    this.chatOpen.update((open) => !open);
  }

  /** Alterna la lista de canales del chat. */
  protected toggleChatChannels(): void {
    this.chatChannelsOpen.update((open) => !open);
  }

  /** Abre el modal de creación de repositorio. */
  protected openRepoModal(): void {
    this.repoForm.reset();
    this.modal.set('repo');
  }

  

  /** Cierra el modal activo. */
  protected closeModal(): void {
    if (this.deleting()) {
      return;
    }
    this.projectToDelete.set(null);
    this.modal.set(null);
  }

  /** Abre el modal para confirmar la eliminación de un repositorio. */
  protected openDeleteModal(project: Proyecto): void {
    this.projectToDelete.set(project);
    this.modal.set('delete');
  }

  /**
   * Elimina el repositorio confirmado mediante el borrado recursivo del backend.
   * El backend aplica el patrón Composite sobre todos sus elementos.
   */
  protected confirmDelete(): void {
    const project = this.projectToDelete();
    if (!project || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.proyectoService.eliminar(project.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.projectToDelete.set(null);
        this.modal.set(null);
        this.showToast(`Repositorio "${project.titulo}" eliminado correctamente.`, 'success');
        this.loadProjects();
      },
      error: (error: HttpErrorResponse) => {
        this.deleting.set(false);
        this.handleError(error, 'No se pudo eliminar el repositorio.');
      },
    });
  }

  /** Crea un repositorio y refresca el dashboard. */
  protected submitRepo(): void {
    if (this.repoForm.invalid) {
      this.repoForm.markAllAsTouched();
      return;
    }

    const request: CrearProyectoRequest = this.repoForm.getRawValue();
    this.saving.set(true);
    this.proyectoService.crear(request).subscribe({
      next: (project) => {
        this.saving.set(false);
        this.modal.set(null);
        this.showToast(`Repositorio "${project.titulo}" creado correctamente.`, 'success');
        this.loadProjects(project.id);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.handleError(error, 'No se pudo crear el repositorio.');
      },
    });
  }

  /** Devuelve un color oscuro para el icono de la carpeta según su posición. */
  protected folderColor(index: number): string {
    const palette = ['#09090b', '#27272a', '#3f3f46', '#18181b', '#52525b'];
    return palette[index % palette.length];
  }

  /** Muestra un aviso para las secciones que aún no están implementadas. */
  protected notifyComingSoon(label: string): void {
    this.profileMenuOpen.set(false);
    this.showToast(`${label} estará disponible próximamente.`, 'success');
  }

  /** Cambia el canal o usuario activo del chat. */
  protected selectChatTarget(target: string, type: ChatTargetType): void {
    this.chatTarget.set(target);
    this.chatTargetType.set(type);
    this.chatChannelsOpen.set(false);
    this.showToast(
      type === 'channel' ? `Cambiado al canal #${target}.` : `Conversación con ${target}.`,
      'success',
    );
  }

  /** Inserta una etiqueta ágil en el campo de mensaje. */
  protected insertChatTag(tag: string): void {
    this.chatInput.update((value) => `${value}${value ? ' ' : ''}${tag} `);
  }

  /** Actualiza el borrador del mensaje de chat. */
  protected updateChatInput(event: Event): void {
    this.chatInput.set((event.target as HTMLInputElement).value);
  }

  /** Envía un mensaje al hilo local del chat. */
  protected sendChatMessage(event: Event): void {
    event.preventDefault();
    const text = this.chatInput().trim();
    if (!text) {
      return;
    }

    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.chatMessages.update((messages) => [
      ...messages,
      {
        author: 'Tú',
        initials: this.initials(),
        time,
        text,
        own: true,
        kind: 'text',
        avatarColor: '#1e293b',
      },
    ]);
    this.chatInput.set('');
  }

  /** Cierra sesión y vuelve al login. */
  protected logout(): void {
    this.session.clear();
    this.router.navigate(['/login']);
  }

  /** Cierra menús al hacer clic fuera de ellos. */
  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('[data-menu="profile"]')) {
      this.profileMenuOpen.set(false);
    }
  }

  /** Cierra modales y menús con la tecla Escape. */
  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeModal();
    this.profileMenuOpen.set(false);
  }

  /** Filtra los repositorios por título o descripción. */
  private filterProjects(projects: Proyecto[]): Proyecto[] {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) {
      return projects;
    }
    return projects.filter(
      (project) =>
        project.titulo.toLowerCase().includes(term) ||
        (project.descripcion ?? '').toLowerCase().includes(term),
    );
  }

  /** Filtra los artefactos por título o identificador. */
  private filterArtifacts(elements: Elemento[]): Elemento[] {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) {
      return elements;
    }
    return elements.filter(
      (element) =>
        element.titulo.toLowerCase().includes(term) ||
        (element.identificador ?? '').toLowerCase().includes(term),
    );
  }

  /** Muestra un toast y programa su ocultamiento automático. */
  protected showToast(message: string, type: ToastType): void {
    clearTimeout(this.toastTimer);
    this.toastType.set(type);
    this.toastMessage.set(message);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3500);
  }

  /** Traduce un error HTTP a un mensaje para el usuario. */
  private handleError(error: HttpErrorResponse, fallback: string): void {
    if (error.status === 401) {
      this.logout();
      return;
    }
    const message =
      typeof error.error?.message === 'string' ? error.error.message : fallback;
    this.showToast(message, 'error');
  }

  /** Lee el nombre de usuario guardado durante el login. */
  private readUsername(): string {
    return this.session.getUsername() || 'agileflow';
  }
}
