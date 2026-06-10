import React from "react";
import { createRoot } from "react-dom/client";
import { Building2, CalendarDays, CreditCard, DoorOpen, LogOut, Mail, Plus, Send, UserPlus } from "lucide-react";
import "./styles.css";

const API = "http://localhost:9092/api";
const today = () => new Date().toISOString().slice(0, 10);
const firstOfMonth = () => `${today().slice(0, 7)}-01`;
const timeOptions = Array.from({ length: 15 }, (_, i) => {
  const hour = i + 6;
  return `${String(hour).padStart(2, "0")}:00`;
});
const formatTime = (value) => {
  const [h, m] = value.split(":").map(Number);
  const suffix = h >= 12 ? "PM" : "AM";
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, "0")} ${suffix}`;
};

function request(path, options = {}) {
  const token = localStorage.getItem("token");
  return fetch(`${API}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  }).then(async (res) => {
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || body.detail || body.error || res.statusText);
    }
    return res.status === 204 ? null : res.json();
  });
}

function App() {
  const [session, setSession] = React.useState(() => JSON.parse(localStorage.getItem("session") || "null"));
  const logout = () => {
    localStorage.clear();
    setSession(null);
  };
  if (!session) return <Login onLogin={setSession} />;
  return session.role === "ADMIN" ? <AdminDashboard session={session} logout={logout} /> : <ClientDashboard session={session} logout={logout} />;
}

function Login({ onLogin }) {
  const [form, setForm] = React.useState({ email: "admin@workafella.com", password: "Admin@12345" });
  const [error, setError] = React.useState("");
  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const data = await request("/auth/login", { method: "POST", body: JSON.stringify(form) });
      localStorage.setItem("token", data.token);
      localStorage.setItem("session", JSON.stringify(data));
      onLogin(data);
    } catch (err) {
      setError(err.message);
    }
  };
  return (
    <main className="login">
      <section className="login-panel">
        <div className="brand"><Building2 size={30} /> Workafella</div>
        <form onSubmit={submit}>
          <label>Email<input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          <label>Password<input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
          {error && <p className="error">{error}</p>}
          <button><DoorOpen size={18} /> Sign in</button>
        </form>
      </section>
    </main>
  );
}

function Shell({ children, title, logout }) {
  return (
    <div className="shell">
      <aside>
        <div className="brand"><Building2 /> Workafella</div>
        <p>Conference rooms, invoices, and visitor access.</p>
        <button className="ghost" onClick={logout}><LogOut size={16} /> Logout</button>
      </aside>
      <main>
        <header><h1>{title}</h1></header>
        {children}
      </main>
    </div>
  );
}

function AdminDashboard({ logout }) {
  const [stats, setStats] = React.useState({});
  const [companies, setCompanies] = React.useState([]);
  const [users, setUsers] = React.useState([]);
  const [invoices, setInvoices] = React.useState([]);
  const [bookings, setBookings] = React.useState([]);
  const [companyForm, setCompanyForm] = React.useState({ name: "", email: "", ownerName: "", seatCount: 39 });
  const [invoiceForm, setInvoiceForm] = React.useState({ companyId: "", billingMonth: firstOfMonth(), dueDate: "", sendNow: true });
  const [message, setMessage] = React.useState("");
  const [error, setError] = React.useState("");
  const selectedCompany = companies.find((c) => String(c.id) === String(invoiceForm.companyId));

  const load = async () => {
    const [s, c, u, i, b] = await Promise.all([
      request("/admin/dashboard"), request("/admin/companies"), request("/admin/companies/users"),
      request("/admin/invoices"), request("/admin/bookings")
    ]);
    setStats(s); setCompanies(c); setUsers(u); setInvoices(i); setBookings(b);
    if (!invoiceForm.companyId && c[0]) setInvoiceForm((f) => ({ ...f, companyId: c[0].id }));
  };
  React.useEffect(() => { load(); }, []);

  const createCompany = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const created = await request("/admin/companies", { method: "POST", body: JSON.stringify({ ...companyForm, seatCount: Number(companyForm.seatCount) }) });
      setMessage(`Client created for ${created.email}. Temporary password: ${created.temporaryPassword}`);
      setCompanyForm({ name: "", email: "", ownerName: "", seatCount: 39 });
      load();
    } catch (err) {
      setError(err.message);
    }
  };
  const generateInvoice = async (e) => {
    e.preventDefault();
    setError("");
    if (!invoiceForm.companyId) {
      setError("Create or select a company before generating an invoice.");
      return;
    }
    try {
      const invoice = await request("/admin/invoices", { method: "POST", body: JSON.stringify({ ...invoiceForm, companyId: Number(invoiceForm.companyId) }) });
      setMessage(`Invoice ${invoice.invoiceNumber} generated for ${invoice.company} and sent to ${invoice.companyEmail}. It is visible in that client's dashboard.`);
      load();
    } catch (err) {
      setError(err.message);
    }
  };
  const markPaid = async (invoice) => {
    await request(`/admin/invoices/${invoice.id}/paid`, { method: "POST", body: JSON.stringify({ amount: invoice.totalAmount, reference: "Manual admin update" }) });
    load();
  };

  return (
    <Shell title="Admin Dashboard" logout={logout}>
      <div className="stats">
        <Metric label="Companies" value={stats.companies || 0} />
        <Metric label="Paid bills" value={stats.paidBills || 0} />
        <Metric label="Pending bills" value={stats.pendingBills || 0} />
        <Metric label="Bookings" value={stats.totalBookings || 0} />
      </div>
      {message && <p className="notice">{message}</p>}
      {error && <p className="error">{error}</p>}
      <section className="grid two">
        <Panel icon={<UserPlus />} title="Add Client Company">
          <form onSubmit={createCompany} className="form-grid">
            <input placeholder="Company name" value={companyForm.name} onChange={(e) => setCompanyForm({ ...companyForm, name: e.target.value })} required />
            <input placeholder="Owner name" value={companyForm.ownerName} onChange={(e) => setCompanyForm({ ...companyForm, ownerName: e.target.value })} required />
            <input placeholder="Login email" type="email" value={companyForm.email} onChange={(e) => setCompanyForm({ ...companyForm, email: e.target.value })} required />
            <input placeholder="Seats" type="number" value={companyForm.seatCount} onChange={(e) => setCompanyForm({ ...companyForm, seatCount: e.target.value })} required />
            <button><Plus size={16} /> Create company</button>
          </form>
        </Panel>
        <Panel icon={<CreditCard />} title="Generate Invoice">
          <form onSubmit={generateInvoice} className="form-grid">
            <select value={invoiceForm.companyId} onChange={(e) => setInvoiceForm({ ...invoiceForm, companyId: e.target.value })} required>
              <option value="">Select company to bill</option>
              {companies.map((c) => <option key={c.id} value={c.id}>{c.name} - {c.email} - {c.seatCount} seats</option>)}
            </select>
            {selectedCompany && (
              <div className="summary-box">
                <span>Invoice receiver</span>
                <b>{selectedCompany.email}</b>
                <span>Amount: INR {(selectedCompany.seatCount * 11000).toLocaleString("en-IN")} ({selectedCompany.seatCount} seats x 11,000)</span>
              </div>
            )}
            <input type="date" value={invoiceForm.billingMonth} onChange={(e) => setInvoiceForm({ ...invoiceForm, billingMonth: e.target.value })} />
            <input type="date" value={invoiceForm.dueDate} onChange={(e) => setInvoiceForm({ ...invoiceForm, dueDate: e.target.value })} required />
            <label className="check"><input type="checkbox" checked={invoiceForm.sendNow} onChange={(e) => setInvoiceForm({ ...invoiceForm, sendNow: e.target.checked })} /> Send now</label>
            <button disabled={!companies.length}><Send size={16} /> Generate</button>
          </form>
        </Panel>
      </section>
      <Data title="Invoices" rows={invoices} action={markPaid} />
      <Data title="Companies" rows={companies} />
      <Data title="Users" rows={users} />
      <Data title="Recent Bookings" rows={bookings} />
    </Shell>
  );
}

function ClientDashboard({ logout }) {
  const [rooms, setRooms] = React.useState([]);
  const [roomId, setRoomId] = React.useState("");
  const [date, setDate] = React.useState(today());
  const [slots, setSlots] = React.useState([]);
  const [usage, setUsage] = React.useState(null);
  const [bookings, setBookings] = React.useState([]);
  const [invoices, setInvoices] = React.useState([]);
  const [passes, setPasses] = React.useState([]);
  const [passForm, setPassForm] = React.useState({
  visitorName: "",
  visitorEmail: "",
  hostName: "",
  purpose: "",
  visitingDate: date,
  entryTime: "09:00",
  exitTime: "18:00"
});
  const [message, setMessage] = React.useState("");
  const [error, setError] = React.useState("");
  const currentHour = new Date().getHours();

const visibleSlots = slots.map((slot) => {
  const slotHour = Number(slot.startTime.split(":")[0]);

  const pastTime =
    date === today() &&
    slotHour <= currentHour;

  return {
    ...slot,
    disabled: !slot.free || pastTime
  };
});
  const load = async () => {
    const [r, b, i, p] = await Promise.all([request("/rooms"), request("/client/bookings"), request("/client/invoices"), request("/client/gatepasses")]);
    setRooms(r); setBookings(b); setInvoices(i); 
    setPasses(p);
    if (!roomId && r[0]) setRoomId(r[0].id);
  };
  React.useEffect(() => { load(); }, []);
  React.useEffect(() => {
    if (!roomId) return;
    Promise.all([
      request(`/bookings/availability?roomId=${roomId}&date=${date}`),
      request(`/client/bookings/usage?roomId=${roomId}&date=${date}`)
    ]).then(([slotData, usageData]) => {
      setSlots(slotData);
      setUsage(usageData);
    }).catch((err) => setError(err.message));
  }, [roomId, date]);

  const book = async (slot) => {
    setError("");
    try {
      await request("/client/bookings", { method: "POST", body: JSON.stringify({ roomId: Number(roomId), date, startTime: slot.startTime }) });
      setMessage(`Booked ${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}.`);
      load();
      const [slotData, usageData] = await Promise.all([
        request(`/bookings/availability?roomId=${roomId}&date=${date}`),
        request(`/client/bookings/usage?roomId=${roomId}&date=${date}`)
      ]);
      setSlots(slotData);
      setUsage(usageData);
    } catch (err) {
      setError(err.message);
    }
  };
  const cancelBooking = async (id) => {
  setError("");

  try {
    await request(`/client/bookings/${id}/cancel`, {
      method: "PATCH"
    });

    setMessage("Booking cancelled successfully.");

    await load();

    const [slotData, usageData] = await Promise.all([
      request(`/bookings/availability?roomId=${roomId}&date=${date}`),
      request(`/client/bookings/usage?roomId=${roomId}&date=${date}`)
    ]);

    setSlots(slotData);
    setUsage(usageData);

  } catch (err) {
    setError(err.message);
  }
};
  const generatePass = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await request("/client/gatepasses", { method: "POST", body: JSON.stringify(passForm) });
      setMessage(`Gate pass generated and sent to ${passForm.visitorEmail}.`);
      setPassForm({
  visitorName: "",
  visitorEmail: "",
  hostName: "",
  purpose: "",
  visitingDate: date,
  entryTime: "09:00",
  exitTime: "18:00"
});
      load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Shell title="Client Dashboard" logout={logout}>
      {message && <p className="notice">{message}</p>}
      {error && <p className="error">{error}</p>}
      <section className="grid two">
        <Panel icon={<CalendarDays />} title="Book Conference Room">
          <div className="toolbar">
            <select value={roomId} onChange={(e) => setRoomId(e.target.value)}>{rooms.map((r) => <option key={r.id} value={r.id}>{r.capacity} seater room</option>)}</select>
            <input type="date" min={today()} value={date} onChange={(e) => setDate(e.target.value < today() ? today() : e.target.value)} />
          </div>
          {usage && (
            <div className="usage-strip">
              <span>{usage.capacity} seater monthly usage</span>
              <b>{usage.remainingThisMonth} hours remaining</b>
              <span>{usage.usedThisMonth} used / {usage.monthlyLimit} allowed</span>
            </div>
          )}
          <div className="slots">
  {slots.map((s) => {
    const currentHour = new Date().getHours();
    const slotHour = Number(s.startTime.split(":")[0]);

    const pastTime =
      date === today() &&
      slotHour <= currentHour;

    const disabled = !s.free || pastTime;

    return (
      <button
        key={s.startTime}
        disabled={disabled}
        className={disabled ? "slot booked" : "slot free"}
        onClick={() => book(s)}
      >
        {formatTime(s.startTime)} - {formatTime(s.endTime)}
        <span>
          {pastTime
            ? "Expired"
            : s.free
            ? "Free"
            : "Booked"}
        </span>
      </button>
    );
  })}
</div>
        </Panel>
        <Panel icon={<Mail />} title="Generate Visitor Gate Pass">
          <form onSubmit={generatePass} className="form-grid">
            <input placeholder="Visitor name" value={passForm.visitorName} onChange={(e) => setPassForm({ ...passForm, visitorName: e.target.value })} required />
            <input placeholder="Visitor email" type="email" value={passForm.visitorEmail} onChange={(e) => setPassForm({ ...passForm, visitorEmail: e.target.value })} required />
            <input
  placeholder="Host Name"
  value={passForm.hostName}
  onChange={(e) =>
    setPassForm({
      ...passForm,
      hostName: e.target.value
    })
  }
  required
/>

<input
  placeholder="Purpose"
  value={passForm.purpose}
  onChange={(e) =>
    setPassForm({
      ...passForm,
      purpose: e.target.value
    })
  }
  required
/>
            <input type="date" min={today()} value={passForm.visitingDate} onChange={(e) => setPassForm({ ...passForm, visitingDate: e.target.value < today() ? today() : e.target.value })} />
            <select value={passForm.entryTime} onChange={(e) => setPassForm({ ...passForm, entryTime: e.target.value })}>
              {timeOptions.slice(0, -1).map((t) => <option key={t} value={t}>{formatTime(t)}</option>)}
            </select>
            <select value={passForm.exitTime} onChange={(e) => setPassForm({ ...passForm, exitTime: e.target.value })}>
              {timeOptions.slice(1).map((t) => <option key={t} value={t}>{formatTime(t)}</option>)}
            </select>
            <button><Send size={16} /> Generate pass</button>
          </form>
        </Panel>
      </section>
      <Data title="Bill Progress & History" rows={invoices} />
      <BookingTable
  rows={bookings}
  onCancel={cancelBooking}
/>
      <Data title="Visitor Passes" rows={passes} />
    </Shell>
  );
}

function Metric({ label, value }) { return <div className="metric"><b>{value}</b><span>{label}</span></div>; }
function Panel({ icon, title, children }) { return <section className="panel"><h2>{icon}{title}</h2>{children}</section>; }
function Data({ title, rows, action }) {
  const keys = rows[0] ? Object.keys(rows[0]) : [];
  return <section className="table-wrap"><h2>{title}</h2><div className="table-scroll"><table><thead><tr>{keys.map((k) => <th key={k}>{k}</th>)}{action && <th>Action</th>}</tr></thead><tbody>{rows.map((row, idx) => <tr key={idx}>{keys.map((k) => <td key={k}>{String(row[k] ?? "")}</td>)}{action && <td>{row.status !== "PAID" && <button className="small" onClick={() => action(row)}>Mark paid</button>}</td>}</tr>)}</tbody></table>{rows.length === 0 && <p className="empty">No records yet.</p>}</div></section>;
}
function BookingTable({ rows, onCancel }) {
  const keys = rows[0] ? Object.keys(rows[0]) : [];
   const canCancel = (booking) => {
    if (booking.status !== "BOOKED") return false;

    const currentDate = today();
    const currentHour = new Date().getHours();

    if (booking.date > currentDate) {
      return true;
    }

    if (
      booking.date === currentDate &&
      Number(booking.startTime.split(":")[0]) > currentHour
    ) {
      return true;
    }

    return false;
  };
  return (
    <section className="table-wrap">
      <h2>My Bookings</h2>

      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              {keys.map((k) => (
                <th key={k}>{k}</th>
              ))}
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            {rows.map((row, idx) => (
              <tr key={idx}>
                {keys.map((k) => (
                  <td key={k}>{String(row[k] ?? "")}</td>
                ))}

                <td>
  {canCancel(row) && (
    <button
      className="small"
      onClick={() => onCancel(row.id)}
    >
      Cancel
    </button>
  )}
</td>
              </tr>
            ))}
          </tbody>
        </table>

        {rows.length === 0 && (
          <p className="empty">No bookings yet.</p>
        )}
      </div>
    </section>
  );
}
createRoot(document.getElementById("root")).render(<App />);
