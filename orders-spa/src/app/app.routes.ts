import { Routes } from '@angular/router';
import { Orders } from './pages/orders/orders';
import { Forbidden } from './pages/forbidden/forbidden';

export const routes: Routes = [
  { path: 'forbidden', component: Forbidden },

  { path: '', redirectTo: 'orders', pathMatch: 'full' },
  {
    path: 'orders',
    component: Orders,
  },
  { path: '**', redirectTo: 'orders' },
];
