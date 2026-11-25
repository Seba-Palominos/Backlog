import { Component, input } from '@angular/core';

/**
 * Notificación flotante reutilizable para mensajes de éxito o error.
 */
@Component({
  selector: 'app-toast',
  templateUrl: './toast.html',
  styleUrl: './toast.css',
})
export class ToastComponent {
  readonly message = input('');
  readonly type = input<'success' | 'error'>('success');
}
