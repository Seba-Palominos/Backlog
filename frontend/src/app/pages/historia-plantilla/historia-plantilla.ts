import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, afterNextRender, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { ToastComponent } from '../../components/toast/toast';
import {
  ActualizarElementoRequest,
  CrearHistoriaRequest,
  Elemento,
} from '../../models/elemento.model';
import { ElementoService } from '../../services/elemento.service';
import { ProyectoService } from '../../services/proyecto.service';
import { SessionService } from '../../services/session.service';

type ToastType = 'success' | 'error';

/**
 * Página con la plantilla de creación y modificación de una historia de usuario.
 * Compone las reglas y criterios según el formato del proyecto.
 */
@Component({
  selector: 'app-historia-plantilla',
  imports: [ReactiveFormsModule, RouterLink, ToastComponent],
  templateUrl: './historia-plantilla.html',
  styleUrl: './historia-plantilla.css',
})
export class HistoriaPlantilla {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly proyectoService = inject(ProyectoService);
  private readonly elementoService = inject(ElementoService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);

  private toastTimer?: ReturnType<typeof setTimeout>;
  private redirectTimer?: ReturnType<typeof setTimeout>;
  private readonly loadedVersion = signal<number | null>(null);

  protected readonly projectId = signal(0);
  protected readonly directorioId = signal<number | null>(null);
  protected readonly historiaId = signal(0);
  protected readonly editing = computed(() => this.historiaId() > 0);
  protected readonly projectTitle = signal('');
  protected readonly epicas = signal<Elemento[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<ToastType>('success');
  protected readonly username = signal(this.session.getUsername() || 'agileflow');

  protected readonly form = this.formBuilder.nonNullable.group({
    identificador: ['', [Validators.required, Validators.maxLength(50)]],
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    idEpicaPadre: [null as number | null],
    estado: ['en desarrollo', [Validators.required]],
    reglasNegocio: ['', [Validators.maxLength(4000)]],
    criteriosAceptacion: ['', [Validators.required, Validators.maxLength(4000)]],
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const historiaId = Number(this.route.snapshot.paramMap.get('historiaId'));
    this.projectId.set(Number.isFinite(id) && id > 0 ? id : 0);
    const directorioId = Number(this.route.snapshot.queryParamMap.get('directorioId'));
    this.directorioId.set(
      Number.isFinite(directorioId) && directorioId > 0 ? directorioId : null,
    );
    this.historiaId.set(
      Number.isFinite(historiaId) && historiaId > 0 ? historiaId : 0,
    );
    afterNextRender(() => this.loadData());
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.redirectTimer);
    });
  }

  /** Carga el repositorio, sus épicas y, si aplica, la historia a modificar. */
  private loadData(): void {
    if (this.projectId() === 0) {
      this.loading.set(false);
      this.showToast('El repositorio solicitado no es válido.', 'error');
      return;
    }

    forkJoin({
      proyecto: this.proyectoService.obtener(this.projectId()),
      // Todas las épicas del proyecto: la asociación no depende del directorio.
      epicas: this.elementoService.listarEpicas(this.projectId()),
      historia: this.editing()
        ? this.elementoService.obtenerElemento(this.projectId(), this.historiaId())
        : of(null),
    }).subscribe({
      next: ({ proyecto, epicas, historia }) => {
        this.projectTitle.set(proyecto.titulo);
        this.epicas.set(epicas);
        if (historia) {
          this.loadedVersion.set(historia.version);
          this.form.patchValue({
            identificador: historia.identificador ?? '',
            titulo: historia.titulo,
            idEpicaPadre: historia.idEpica,
            estado: historia.estado ?? 'en desarrollo',
            reglasNegocio: this.quitarPrefijoReglas(historia.reglasNegocio),
            criteriosAceptacion: historia.criteriosAceptacion ?? '',
          });
        }
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.showToast(this.errorMessage(error, 'No se pudo cargar el repositorio.'), 'error');
      },
    });
  }

  /** Quita el prefijo de lista agregado al guardar las reglas de negocio. */
  private quitarPrefijoReglas(reglas: string | null): string {
    return (reglas ?? '')
      .split('\n')
      .map((regla) => regla.trim().replace(/^-\s*/, ''))
      .join('\n');
  }

  /** Valida y guarda la historia, creándola o actualizándola según la ruta. */
  protected submit(): void {
    if (this.saving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const reglasNegocio = value.reglasNegocio
      .split('\n')
      .map((rule) => rule.trim())
      .filter((rule) => rule.length > 0)
      .map((rule) => `- ${rule}`)
      .join('\n');
    const identificador = value.identificador.trim();
    const titulo = value.titulo.trim();
    const criteriosAceptacion = value.criteriosAceptacion.trim();
    const esEdicion = this.editing();

    const crear: CrearHistoriaRequest = {
      identificador,
      titulo,
      idEpicaPadre: value.idEpicaPadre,
      idElementoPadre: this.directorioId(),
      reglasNegocio,
      criteriosAceptacion,
      estado: value.estado,
    };
    const actualizar: ActualizarElementoRequest = {
      titulo,
      version: this.loadedVersion() ?? 0,
      identificador,
      reglasNegocio,
      criteriosAceptacion,
      estado: value.estado,
      idEpicaPadre: value.idEpicaPadre,
      idElementoPadre: this.directorioId(),
    };

    const operacion = esEdicion
      ? this.elementoService.actualizarElemento(this.projectId(), this.historiaId(), actualizar)
      : this.elementoService.crearHistoria(this.projectId(), crear);

    this.saving.set(true);
    operacion.subscribe({
      next: (historia) => {
        this.saving.set(false);
        this.showToast(
          `Historia "${historia.identificador ?? historia.titulo}" ${
            esEdicion ? 'actualizada' : 'creada'
          } correctamente.`,
          'success',
        );
        this.redirectTimer = setTimeout(() => this.backToRepository(), 900);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.showToast(
          this.errorMessage(
            error,
            esEdicion ? 'No se pudo actualizar la historia.' : 'No se pudo crear la historia.',
          ),
          'error',
        );
      },
    });
  }

  /** Vuelve al detalle del repositorio sin guardar. */
  protected cancel(): void {
    this.backToRepository();
  }

  /** Cierra sesión y vuelve al login. */
  protected logout(): void {
    this.session.clear();
    this.router.navigate(['/login']);
  }

  private backToRepository(): void {
    const directorioId = this.directorioId();
    this.router.navigate(
      directorioId
        ? ['/repositorios', this.projectId(), 'directorios', directorioId]
        : ['/repositorios', this.projectId()],
    );
  }

  /** Muestra un toast y programa su ocultamiento automático. */
  private showToast(message: string, type: ToastType): void {
    clearTimeout(this.toastTimer);
    this.toastType.set(type);
    this.toastMessage.set(message);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3500);
  }

  /** Traduce un error HTTP a un mensaje para el usuario. */
  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return typeof error.error?.message === 'string' ? error.error.message : fallback;
  }
}
