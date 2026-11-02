export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED';
export interface Seat { id: number; seatNumber: string; rowNumber: number; position: string; status: SeatStatus; lockedUntil?: string }
export interface Reservation { reservationId: string; status: string; seatNumbers: string[]; expiresAt: string; remainingSeconds: number }
export interface Booking { bookingId: string; reference: string; customerName: string; customerEmail: string; seatNumbers: string[]; confirmedAt: string }
export interface ApiError { code: string; message: string; details?: Record<string, string> }
