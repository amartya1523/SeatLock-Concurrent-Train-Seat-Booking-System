import type { ApiError, Booking, Reservation, Seat } from './types';
const renderApiHost = import.meta.env.VITE_API_HOST;
const BASE_URL = import.meta.env.VITE_API_URL ?? (renderApiHost ? `https://${renderApiHost}/api/v1` : 'http://localhost:8080/api/v1');
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options?.headers } });
  if (!response.ok) { const error = (await response.json().catch(() => ({ message: 'Unable to reach SeatLock.' }))) as ApiError; throw new Error(error.message); }
  return response.json() as Promise<T>;
}
export const api = {
  seats: () => request<Seat[]>('/seats'),
  bookings: () => request<Booking[]>('/bookings'),
  reserve: (customerName: string, customerEmail: string, seatNumbers: string[]) => request<Reservation>('/reservations', { method: 'POST', body: JSON.stringify({ customerName, customerEmail, seatNumbers }) }),
  confirm: (id: string) => request<Booking>(`/reservations/${id}/confirm`, { method: 'POST' }),
  cancel: (id: string) => request<Reservation>(`/reservations/${id}/cancel`, { method: 'POST' }),
};
