import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  PLATFORM_ID,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ToastComponent } from '../../components/toast/toast';
import { GoogleCredentialResponse } from '../../models/google-identity.model';
import { LoginRequest } from '../../models/login-request.model';
import { LoginResponse } from '../../models/login-response.model';
import { AuthService } from '../../services/auth.service';
import { GoogleIdentityService } from '../../services/google-identity.service';
import { SessionService } from '../../services/session.service';

/**
 * Página de inicio de sesión, transcrita desde el prototipo de UI-UX.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, ToastComponent],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements AfterViewInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly googleIdentity = inject(GoogleIdentityService);
  private toastTimer?: ReturnType<typeof setTimeout>;
  private navigationTimer?: ReturnType<typeof setTimeout>;

  /** Contenedor donde Google Identity Services dibuja su botón. */
  @ViewChild('googleButton') private googleButton?: ElementRef<HTMLDivElement>;

  protected readonly showPassword = signal(false);
  protected readonly loading = signal(false);
  protected readonly googleLoading = signal(false);
  protected readonly toastMessage = signal('');
  protected readonly toastType = signal<'success' | 'error'>('success');
  /** Indica si existe un Client ID configurado para mostrar el botón de Google. */
  protected readonly googleConfigured = environment.googleClientId.trim().length > 0;

  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
    rememberMe: [false],
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.navigationTimer);
    });
  }

  /**
   * Carga Google Identity Services y dibuja su botón.
   * Solo se ejecuta en el navegador para no romper el renderizado en SSR.
   */
  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId) || !this.googleConfigured) {
      return;
    }

    const target = this.googleButton?.nativeElement;
    if (!target) {
      return;
    }

    this.googleIdentity
      .initialize(environment.googleClientId.trim(), (response) =>
        this.onGoogleCredential(response),
      )
      .then(() => this.googleIdentity.renderButton(target))
      .catch(() =>
        this.showToast(
          'No se pudo cargar el inicio de sesión con Google. Recarga la página.',
          'error',
        ),
      );
  }

  /** Alterna la visibilidad del campo de contraseña. */
  protected togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  /** Avisa que la recuperación de contraseña fue solicitada al administrador. */
  protected notifyRecovery(): void {
    this.showToast('Enlace de recuperación enviado al administrador', 'success');
  }

  /** Valida y envía las credenciales al endpoint POST /api/auth/login. */
  protected onSubmit(): void {
    if (this.loading() || this.googleLoading()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { username, password, rememberMe } = this.form.getRawValue();
    const request: LoginRequest = { username, password };

    this.loading.set(true);
    this.authService.login(request).subscribe({
      next: (response: LoginResponse) => {
        this.loading.set(false);
        this.session.save(response.token, response.username, rememberMe);
        this.showToast(`¡Sesión iniciada correctamente como @${response.username}!`, 'success');
        // Se navega a la ruta solicitada o al dashboard después de mostrar el aviso.
        this.navigationTimer = setTimeout(
          () => this.router.navigateByUrl(this.resolveReturnUrl()),
          700,
        );
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const message =
          typeof error.error?.message === 'string'
            ? error.error.message
            : 'No se pudo iniciar sesión. Inténtalo de nuevo.';
        this.showToast(message, 'error');
      },
    });
  }

  /**
   * Recibe el ID token de Google y lo valida en el backend.
   * El token de Google nunca se usa como sesión local: el backend emite su JWT.
   */
  private onGoogleCredential(response: GoogleCredentialResponse): void {
    if (this.loading() || this.googleLoading()) {
      return;
    }

    const credential = response?.credential;
    if (!credential) {
      this.showToast('Google no devolvió una credencial válida', 'error');
      return;
    }

    const rememberMe = this.form.controls.rememberMe.getRawValue();

    this.googleLoading.set(true);
    this.authService.googleLogin(credential).subscribe({
      next: (loginResponse: LoginResponse) => {
        this.googleLoading.set(false);
        this.session.save(loginResponse.token, loginResponse.username, rememberMe);
        this.showToast(
          `¡Sesión iniciada correctamente como @${loginResponse.username}!`,
          'success',
        );
        this.navigationTimer = setTimeout(
          () => this.router.navigateByUrl(this.resolveReturnUrl()),
          700,
        );
      },
      error: (error: HttpErrorResponse) => {
        this.googleLoading.set(false);
        const message =
          typeof error.error?.message === 'string'
            ? error.error.message
            : 'No se pudo iniciar sesión con Google. Inténtalo de nuevo.';
        this.showToast(message, 'error');
      },
    });
  }

  /** Resuelve la ruta interna a la que se debe volver después del login. */
  private resolveReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    if (
      !returnUrl ||
      !returnUrl.startsWith('/') ||
      returnUrl.startsWith('//') ||
      returnUrl === '/login' ||
      returnUrl === '/registro'
    ) {
      return '/dashboard';
    }

    return returnUrl;
  }

  /** Muestra un toast y programa su ocultamiento automático. */
  private showToast(message: string, type: 'success' | 'error'): void {
    clearTimeout(this.toastTimer);
    this.toastType.set(type);
    this.toastMessage.set(message);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3500);
  }
}
