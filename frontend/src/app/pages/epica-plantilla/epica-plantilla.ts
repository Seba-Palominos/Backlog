import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, afterNextRender, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastComponent } from '../../components/toast/toast';
import { CrearEpicaRequest } from '../../models/elemento.model';
import { ElementoService } from '../../services/elemento.service';
import { ProyectoService } from '../../services/proyecto.service';
import { SessionService } from '../../services/session.service';

type ToastType = 'success' | 'error';

/**
 * Página con la plantilla de creación de una épica.
 * Guarda la épica en el repositorio mediante el endpoint existente.
 */
@Component({
  selector: 'app-epica-plantilla',
  imports: [ReactiveFormsModule, RouterLink, ToastComponent],
  templateUrl: './epica-plantilla.html',
  styleUrl: './epica-plantilla.css',
})
export class EpicaPlantilla {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly proyectoService = inject(ProyectoService);
  private readonly elementoService = inject(ElementoService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);

  private toastTimer?: ReturnType<typeof setTimeout>;
  private redirectTimer?: ReturnType<typeof setTimeout>;

  protected readonly projectId = signal(0);
  protected readonly directorioId = signal<number | null>(null);
  protected readonly projectTitle = signal('');
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<ToastType>('success');
  protected readonly username = signal(this.session.getUsername() || 'agileflow');

  protected readonly form = this.formBuilder.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.maxLength(2000)]],
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.projectId.set(Number.isFinite(id) && id > 0 ? id : 0);
    const directorioId = Number(this.route.snapshot.queryParamMap.get('directorioId'));
    this.directorioId.set(
      Number.isFinite(directorioId) && directorioId > 0 ? directorioId : null,
    );
    afterNextRender(() => this.loadProject());
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.redirectTimer);
    });
  }

  /** Carga el nombre del repositorio para mostrarlo en la plantilla. */
  private loadProject(): void {
    if (this.projectId() === 0) {
      this.loading.set(false);
      this.showToast('El repositorio solicitado no es válido.', 'error');
      return;
    }

    this.proyectoService.obtener(this.projectId()).subscribe({
      next: (project) => {
        this.projectTitle.set(project.titulo);
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

  /** Valida y envía la épica al backend. */
  protected submit(): void {
    if (this.saving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: CrearEpicaRequest = {
      titulo: value.titulo.trim(),
      descripcion: value.descripcion.trim(),
      idElementoPadre: this.directorioId(),
    };

    this.saving.set(true);
    this.elementoService.crearEpica(this.projectId(), request).subscribe({
      next: (epica) => {
        this.saving.set(false);
        this.showToast(`Épica "${epica.titulo}" creada correctamente.`, 'success');
        this.redirectTimer = setTimeout(() => this.backToRepository(), 900);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.showToast(this.errorMessage(error, 'No se pudo crear la épica.'), 'error');
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
