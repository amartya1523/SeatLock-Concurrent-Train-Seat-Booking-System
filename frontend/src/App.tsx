import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowRight, Check, Clock3, LockKeyhole, MapPin, RefreshCw, ShieldCheck, Sparkles, Ticket, TrainFront, Users, X } from 'lucide-react';
import { api } from './api';
import type { Booking, Reservation, Seat } from './types';

type Toast = { kind: 'success' | 'error'; message: string } | null;

export default function App() {
  const [seats, setSeats] = useState<Seat[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState('');
  const [toast, setToast] = useState<Toast>(null);
  const [remaining, setRemaining] = useState(0);
  const [history, setHistory] = useState<Booking[]>([]);

  const refresh = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true);
    try { const [nextSeats, nextHistory] = await Promise.all([api.seats(), api.bookings()]); setSeats(nextSeats); setHistory(nextHistory); } catch (e) { if (!quiet) setToast({ kind: 'error', message: (e as Error).message }); }
    finally { if (!quiet) setLoading(false); }
  }, []);

  useEffect(() => { void refresh(); const poll = window.setInterval(() => void refresh(true), 8000); return () => clearInterval(poll); }, [refresh]);
  useEffect(() => {
    if (!reservation) return;
    const tick = () => setRemaining(Math.max(0, Math.ceil((new Date(reservation.expiresAt).getTime() - Date.now()) / 1000)));
    tick(); const timer = window.setInterval(tick, 1000); return () => clearInterval(timer);
  }, [reservation]);
  useEffect(() => { if (remaining === 0 && reservation) { setReservation(null); setSelected([]); setToast({ kind: 'error', message: 'Your seat hold expired. The seats are available again.' }); void refresh(true); } }, [remaining, reservation, refresh]);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(null), 4500); return () => clearTimeout(timer); }, [toast]);

  const stats = useMemo(() => ({ available: seats.filter(s => s.status === 'AVAILABLE').length, locked: seats.filter(s => s.status === 'LOCKED').length, booked: seats.filter(s => s.status === 'BOOKED').length }), [seats]);
  const toggleSeat = (seat: Seat) => {
    if (seat.status !== 'AVAILABLE' || reservation) return;
    setSelected(current => current.includes(seat.seatNumber) ? current.filter(n => n !== seat.seatNumber) : current.length < 6 ? [...current, seat.seatNumber] : current);
  };
  const reserve = async () => {
    if (!name.trim() || !/^\S+@\S+\.\S+$/.test(email)) return setToast({ kind: 'error', message: 'Enter your name and a valid email to continue.' });
    setAction('reserve');
    try { const result = await api.reserve(name, email, selected); setReservation(result); setRemaining(result.remainingSeconds); setToast({ kind: 'success', message: 'Seats held for 2 minutes. Complete your booking.' }); await refresh(true); }
    catch (e) { setToast({ kind: 'error', message: (e as Error).message }); setSelected([]); await refresh(true); }
    finally { setAction(''); }
  };
  const confirm = async () => {
    if (!reservation) return; setAction('confirm');
    try { const result = await api.confirm(reservation.reservationId); setBooking(result); setReservation(null); setSelected([]); setToast({ kind: 'success', message: `Booking ${result.reference} confirmed.` }); await refresh(true); }
    catch (e) { setToast({ kind: 'error', message: (e as Error).message }); }
    finally { setAction(''); }
  };
  const cancel = async () => {
    if (!reservation) return; setAction('cancel');
    try { await api.cancel(reservation.reservationId); setReservation(null); setSelected([]); setToast({ kind: 'success', message: 'Reservation cancelled and seats released.' }); await refresh(true); }
    catch (e) { setToast({ kind: 'error', message: (e as Error).message }); }
    finally { setAction(''); }
  };
  const clock = `${String(Math.floor(remaining / 60)).padStart(2, '0')}:${String(remaining % 60).padStart(2, '0')}`;

  return <div className="app-shell">
    <header className="navbar"><a className="brand" href="#top"><span className="brand-mark"><TrainFront size={20}/></span><span>Seat<span>Lock</span></span></a><nav><a href="#coach">Choose seats</a><a href="#history">Bookings</a><a href="#safety">How it works</a></nav><div className="secure"><ShieldCheck size={17}/> Secure booking</div></header>
    <main id="top">
      <section className="hero">
        <div className="hero-copy"><div className="eyebrow"><Sparkles size={14}/> Effortless rail travel</div><h1>Your journey,<br/><em>perfectly seated.</em></h1><p>Choose your seat, hold it securely, and board with confidence. Fast reservations built for busy routes.</p><a className="hero-cta" href="#coach">Find your seat <ArrowRight size={18}/></a><div className="trust-row"><span><ShieldCheck/>Safe & secure</span><span><Clock3/>2-minute hold</span><span><Ticket/>Instant confirmation</span></div></div>
        <div className="journey-card"><div className="route-label">TODAY'S JOURNEY</div><div className="route"><div><strong>NDLS</strong><span>New Delhi</span><small>08:30</small></div><div className="route-line"><TrainFront/><span></span><small>4h 15m</small></div><div><strong>CNB</strong><span>Kanpur</span><small>12:45</small></div></div><div className="train-detail"><span><TrainFront/> Goolluck Express</span><span>Coach C1</span></div><div className="availability"><div><span>Live availability</span><strong>{stats.available} seats</strong></div><div className="meter"><i style={{width: `${stats.available * 5}%`}}/></div><small>Updated automatically every few seconds</small></div></div>
      </section>

      <section className="booking-section" id="coach">
        <div className="section-heading"><div><span className="step">01</span><p>SELECT YOUR PLACE</p><h2>Choose your perfect seat</h2><small>Coach C1 · Air conditioned · 20 seats</small></div><button className="refresh" onClick={() => void refresh()}><RefreshCw className={loading ? 'spin' : ''}/> Refresh</button></div>
        <div className="booking-grid">
          <div className="coach-card">
            <div className="coach-top"><div><TrainFront/><span><strong>Coach C1</strong><small>Front of train</small></span></div><div className="legend"><span><i className="available"/>Available</span><span><i className="selected"/>Selected</span><span><i className="locked"/>Held</span><span><i className="booked"/>Booked</span></div></div>
            <div className="coach-body">
              <div className="window-label"><span>WINDOW</span><span>WINDOW</span></div>
              {loading && !seats.length ? <div className="skeleton">Preparing the coach...</div> : [1,2,3,4,5].map(row => <div className="seat-row" key={row}><b>{row}</b>{['A','B','C','D'].map((position, index) => { const seat = seats.find(s => s.seatNumber === `${row}${position}`); if (!seat) return null; const isSelected = selected.includes(seat.seatNumber); return <div className={index === 2 ? 'aisle-gap' : ''} key={position}><button aria-label={`Seat ${seat.seatNumber}, ${seat.status.toLowerCase()}`} className={`seat ${seat.status.toLowerCase()} ${isSelected ? 'is-selected' : ''}`} onClick={() => toggleSeat(seat)} disabled={seat.status !== 'AVAILABLE' || !!reservation}><span>{seat.seatNumber}</span>{seat.status !== 'AVAILABLE' && <LockKeyhole size={11}/>}</button></div>})}</div>)}
              <div className="coach-footer"><span><MapPin/> New Delhi</span><span>Direction of travel <ArrowRight/></span></div>
            </div>
          </div>

          <aside className="summary-card">
            {booking ? <div className="success-panel"><div className="success-icon"><Check/></div><p>YOU'RE ALL SET</p><h3>Booking confirmed</h3><span>Reference</span><strong>{booking.reference}</strong><div className="ticket-seats">{booking.seatNumbers.map(s => <b key={s}>{s}</b>)}</div><small>A confirmation has been prepared for {booking.customerEmail}.</small><button onClick={() => { setBooking(null); setName(''); setEmail(''); }}>Book another journey</button></div> : <>
              <div className="summary-title"><span className="step">02</span><div><p>YOUR SELECTION</p><h3>Booking summary</h3></div></div>
              {reservation && <div className="hold-timer"><div><Clock3/><span><small>Seats held for you</small><strong>{clock}</strong></span></div><i style={{width: `${remaining / 1.2}%`}}/></div>}
              <div className="selected-list">{selected.length ? selected.map(number => <div key={number}><span className="mini-seat">{number}</span><span><strong>Seat {number}</strong><small>Coach C1 · {['A','D'].includes(number.at(-1)!) ? 'Window' : 'Aisle'}</small></span>{!reservation && <button onClick={() => setSelected(s => s.filter(n => n !== number))}><X/></button>}</div>) : <div className="empty-selection"><div><Ticket/></div><strong>No seats selected</strong><span>Pick up to 6 available seats from the coach</span></div>}</div>
              {selected.length > 0 && <><div className="customer-fields"><label>Passenger name<input value={name} onChange={e => setName(e.target.value)} placeholder="Amartya Singh" disabled={!!reservation}/></label><label>Email address<input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" disabled={!!reservation}/></label></div><div className="fare"><span>Reservation total <small>Simulated fare</small></span><strong>₹{(selected.length * 549).toLocaleString('en-IN')}</strong></div></>}
              {!reservation ? <button className="primary-action" disabled={!selected.length || !!action} onClick={() => void reserve()}>{action ? 'Securing seats...' : 'Hold selected seats'} <LockKeyhole/></button> : <div className="confirm-actions"><button className="primary-action" disabled={!!action} onClick={() => void confirm()}>{action === 'confirm' ? 'Confirming...' : 'Confirm & pay'} <ArrowRight/></button><button className="cancel-action" disabled={!!action} onClick={() => void cancel()}>Cancel reservation</button></div>}
              <p className="safe-note"><ShieldCheck/> Seats are locked transactionally—no double bookings.</p>
            </>}
          </aside>
        </div>
      </section>

      <section className="history-section" id="history">
        <div className="section-heading"><div><span className="step">03</span><p>BOOKING HISTORY</p><h2>Confirmed bookings</h2><small>{history.length} bookings · {history.reduce((sum, b) => sum + b.seatNumbers.length, 0)} of {seats.length || 20} seats confirmed</small></div></div>
        {history.length ? <div className="history-table" role="table" aria-label="Confirmed bookings">
          <div className="history-row head" role="row"><span role="columnheader">Reference</span><span role="columnheader">Passenger</span><span role="columnheader">Seats</span><span role="columnheader">Confirmed</span></div>
          {history.map(b => <div className="history-row" role="row" key={b.bookingId}><strong role="cell">{b.reference}</strong><span role="cell">{b.customerName}</span><span role="cell" className="history-seats">{b.seatNumbers.map(s => <b key={s}>{s}</b>)}</span><span role="cell">{new Date(b.confirmedAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span></div>)}
        </div> : <div className="history-empty"><Ticket/><span>No confirmed bookings yet.</span></div>}
      </section>

      <section className="safety" id="safety"><div><p>BUILT FOR THE RUSH</p><h2>Every seat. One passenger.<br/>Guaranteed.</h2></div><div className="feature"><span><Users/></span><div><strong>Concurrency safe</strong><p>Even when 100 travellers book at once, database-level locks keep every confirmation unique.</p></div></div><div className="feature"><span><Clock3/></span><div><strong>Fair 2-minute holds</strong><p>Your chosen seats are yours while you confirm. Expired holds release automatically.</p></div></div></section>
    </main>
    <footer><div className="brand"><span className="brand-mark"><TrainFront size={18}/></span><span>Seat<span>Lock</span></span></div><p>Designed and developed by Amartya Singh</p><span>© 2026 SeatLock</span></footer>
    {toast && <div className={`toast ${toast.kind}`}>{toast.kind === 'success' ? <Check/> : <X/>}<span>{toast.message}</span><button onClick={() => setToast(null)}><X/></button></div>}
  </div>;
}
