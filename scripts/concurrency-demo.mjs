#!/usr/bin/env node
// Fires 100 simultaneous customers at a running SeatLock API and verifies that
// no more than 20 seats are confirmed and no seat is confirmed twice.
// Usage: node scripts/concurrency-demo.mjs [apiBaseUrl] [users]
const BASE_URL = process.argv[2] ?? process.env.API_URL ?? 'http://localhost:8080/api/v1';
const USERS = Number(process.argv[3] ?? 100);

async function post(path, body) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: response.status, body: await response.json().catch(() => null) };
}

async function customer(index, seatNumbers) {
  // Every customer competes for one seat; five customers target each of the 20 seats.
  const seat = seatNumbers[index % seatNumbers.length];
  const hold = await post('/reservations', { customerName: `Load Customer ${index}`, customerEmail: `load${index}@example.com`, seatNumbers: [seat] });
  if (hold.status !== 201) return { seat, outcome: hold.body?.code ?? `HTTP_${hold.status}` };
  const confirm = await post(`/reservations/${hold.body.reservationId}/confirm`);
  return confirm.status === 200 ? { seat, outcome: 'CONFIRMED', booking: confirm.body } : { seat, outcome: confirm.body?.code ?? `HTTP_${confirm.status}` };
}

const seats = await (await fetch(`${BASE_URL}/seats`)).json();
const before = seats.filter(seat => seat.status === 'BOOKED').length;
console.log(`Target: ${BASE_URL}\nSeats in coach: ${seats.length} (already booked: ${before})\nReleasing ${USERS} customers at once...\n`);

const started = Date.now();
const results = await Promise.all(Array.from({ length: USERS }, (_, i) => customer(i, seats.map(seat => seat.seatNumber))));
const confirmed = results.filter(r => r.outcome === 'CONFIRMED');
const confirmedSeats = confirmed.flatMap(r => r.booking.seatNumbers);
const outcomes = results.reduce((acc, r) => ({ ...acc, [r.outcome]: (acc[r.outcome] ?? 0) + 1 }), {});
const booked = (await (await fetch(`${BASE_URL}/bookings`)).json()).flatMap(b => b.seatNumbers);

const summary = {
  requests: USERS,
  elapsedMs: Date.now() - started,
  outcomes,
  confirmedThisRun: confirmed.length,
  uniqueSeatsThisRun: new Set(confirmedSeats).size,
  totalBookedSeats: booked.length,
  duplicateSeatsInDatabase: booked.length - new Set(booked).size,
};
console.log(JSON.stringify(summary, null, 2));

const ok = summary.duplicateSeatsInDatabase === 0 && summary.totalBookedSeats <= seats.length && summary.uniqueSeatsThisRun === summary.confirmedThisRun;
console.log(ok ? '\nPASS: no seat was booked twice and at most 20 seats are confirmed.' : '\nFAIL: duplicate or excess bookings detected.');
process.exit(ok ? 0 : 1);
