import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ToastComponent } from '../../components/toast/toast';
import { RegistroRequest } from '../../models/registro-request.model';
import { RegistroService } from '../../services/registro.service';

type PasswordMatchState = 'empty' | 'match' | 'mismatch';

/**
 * Página de registro de usuario, transcrita desde el prototipo de UI-UX.
 */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, ToastComponent],
  templateUrl: './registro.html',
  styleUrl: './registro.css',
})
export class Registro {
  private readonly formBuilder = inject(FormBuilder);
  private readonly registroService = inject(RegistroService);
  private readonly destroyRef = inject(DestroyRef);
  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly showPassword = signal(false);
  protected readonly showPasswordConfirm = signal(false);
  protected readonly loading = signal(false);
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: [
        '',
        [Validators.required, Validators.minLength(8), Validators.maxLength(100)],
      ],
      password_confirm: ['', [Validators.required]],
      terms: [false, [Validators.requiredTrue]],
    },
    { validators: [passwordsMatch] },
  );

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
  }

  /** Estado visual de la coincidencia entre contraseñas. */
  protected get matchState(): PasswordMatchState {
    const confirmation = this.form.controls.password_confirm.value;
    if (!confirmation) {
      return 'empty';
    }
    return this.form.errors?.['passwordsMismatch'] ? 'mismatch' : 'match';
  }

  /** Alterna la visibilidad del campo de contraseña indicado. */
  protected togglePassword(field: 'password' | 'password_confirm'): void {
    if (field === 'password') {
      this.showPassword.update((visible) => !visible);
    } else {
      this.showPasswordConfirm.update((visible) => !visible);
    }
  }

  /** Valida y envía el formulario al endpoint POST /api/registro. */
  protected onSubmit(): void {
    if (this.loading()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.matchState === 'mismatch') {
        this.showToast('Las contraseñas no coinciden. Por favor verifícalas.', 'error');
      }
      return;
    }

    const { username, email, password, password_confirm } = this.form.getRawValue();
    const request: RegistroRequest = { username, email, password, password_confirm };

    this.loading.set(true);
    this.registroService.registrar(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.showToast(`¡Usuario @${username} registrado con éxito!`, 'success');
        this.form.reset();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const message =
          typeof error.error?.message === 'string'
            ? error.error.message
            : 'No se pudo completar el registro. Inténtalo de nuevo.';
        this.showToast(message, 'error');
      },
    });
  }

  /** Muestra un toast y programa su ocultamiento automático. */
  private showToast(message: string, type: 'success' | 'error'): void {
    clearTimeout(this.toastTimer);
    this.toastType.set(type);
    this.toastMessage.set(message);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3500);
  }
}

/** Valida que la contraseña y su confirmación coincidan. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmation = group.get('password_confirm')?.value;
  if (!confirmation) {
    return null;
  }
  return password === confirmation ? null : { passwordsMismatch: true };
}
