/**
 * DentiBot API Client
 * Centraliza todas as chamadas para o API Gateway (porta 5000).
 */

const API_BASE = window.API_BASE || 'http://localhost:5000/api';

class DentiBotAPI {
    constructor() {
        this._token = localStorage.getItem('dentibot_token') || '';
    }

    _headers(json = true) {
        const h = { Authorization: `Bearer ${this._token}` };
        if (json) h['Content-Type'] = 'application/json';
        return h;
    }

    _setToken(token) {
        this._token = token;
        localStorage.setItem('dentibot_token', token);
    }

    _clearToken() {
        this._token = '';
        localStorage.removeItem('dentibot_token');
        localStorage.removeItem('dentibot_user');
    }

    async _request(method, path, body = null) {
        const opts = { method, headers: this._headers() };
        if (body) opts.body = JSON.stringify(body);
        const res = await fetch(`${API_BASE}${path}`, opts);
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw Object.assign(new Error(data.erro || 'Erro na API'), { status: res.status, data });
        return data;
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────
    async register(payload)       { return this._request('POST', '/auth/register', payload); }
    async login(username, password) { return this._request('POST', '/auth/login', { username, password }); }
    async verify2FA(userId, token) { return this._request('POST', '/auth/verify-2fa', { user_id: userId, token }); }
    async setup2FA(username, method) { return this._request('POST', '/auth/setup-preference', { username, method }); }
    async me()                    { return this._request('GET', '/auth/me'); }
    async listUsers()             { return this._request('GET', '/auth/users'); }
    async updateUser(id, data)    { return this._request('PUT', `/auth/users/${id}`, data); }

    logout() {
        this._clearToken();
        window.location.href = 'login.html';
    }

    async loginAndVerify(username, password, twoFAToken) {
        const step1 = await this.login(username, password);
        const step2 = await this.verify2FA(step1.user_id, twoFAToken);
        this._setToken(step2.token);
        localStorage.setItem('dentibot_user', JSON.stringify(step2.user));
        return step2;
    }

    // ─── Pacientes ────────────────────────────────────────────────────────────
    async listPatients(query = '')  { return this._request('GET', `/patients${query ? `?q=${encodeURIComponent(query)}` : ''}`); }
    async getPatient(id)            { return this._request('GET', `/patients/${id}`); }
    async createPatient(data)       { return this._request('POST', '/patients', data); }
    async updatePatient(id, data)   { return this._request('PUT', `/patients/${id}`, data); }
    async deletePatient(id)         { return this._request('DELETE', `/patients/${id}`); }

    async listRecords(patientId)    { return this._request('GET', `/patients/${patientId}/records`); }
    async createRecord(patientId, data) { return this._request('POST', `/patients/${patientId}/records`, data); }
    async updateRecord(patientId, recordId, data) { return this._request('PUT', `/patients/${patientId}/records/${recordId}`, data); }

    // ─── Agendamentos ─────────────────────────────────────────────────────────
    async listAppointments(filters = {}) {
        const qs = new URLSearchParams(filters).toString();
        return this._request('GET', `/appointments${qs ? `?${qs}` : ''}`);
    }
    async getAppointment(id)        { return this._request('GET', `/appointments/${id}`); }
    async createAppointment(data)   { return this._request('POST', '/appointments', data); }
    async updateAppointment(id, data) { return this._request('PUT', `/appointments/${id}`, data); }
    async cancelAppointment(id)     { return this._request('POST', `/appointments/${id}/cancel`); }
    async confirmAppointment(id)    { return this._request('POST', `/appointments/${id}/confirm`); }
    async getAvailableSlots(dentistId, date) {
        return this._request('GET', `/appointments/slots?dentist_id=${dentistId}&date=${date}`);
    }

    // ─── Estoque ──────────────────────────────────────────────────────────────
    async listInventory(params = {}) {
        const qs = new URLSearchParams(params).toString();
        return this._request('GET', `/inventory${qs ? `?${qs}` : ''}`);
    }
    async getItem(id)               { return this._request('GET', `/inventory/${id}`); }
    async createItem(data)          { return this._request('POST', '/inventory', data); }
    async updateItem(id, data)      { return this._request('PUT', `/inventory/${id}`, data); }
    async addMovement(id, data)     { return this._request('POST', `/inventory/${id}/movement`, data); }
    async listMovements(id)         { return this._request('GET', `/inventory/${id}/movements`); }
    async listRequests(status = '') { return this._request('GET', `/inventory/requests${status ? `?status=${status}` : ''}`); }
    async createRequest(data)       { return this._request('POST', '/inventory/requests', data); }
    async updateRequest(id, data)   { return this._request('PUT', `/inventory/requests/${id}`, data); }

    // ─── Comunicação ──────────────────────────────────────────────────────────
    async sendEmail(to, subject, body) { return this._request('POST', '/communications/email', { to, subject, body }); }
    async sendSMS(to, message)         { return this._request('POST', '/communications/sms', { to, message }); }
    async sendWhatsApp(to, message)    { return this._request('POST', '/communications/whatsapp', { to, message }); }
    async sendReminder(data)           { return this._request('POST', '/communications/appointment-reminder', data); }

    // ─── Financeiro ───────────────────────────────────────────────────────────
    async getFinancialOverview()       { return this._request('GET', '/financial/overview'); }
    async listInvoices(params = {})  {
        const qs = new URLSearchParams(params).toString();
        return this._request('GET', `/financial/invoices${qs ? `?${qs}` : ''}`);
    }
    async createInvoice(data)          { return this._request('POST', '/financial/invoices', data); }
    async getInvoice(id)               { return this._request('GET', `/financial/invoices/${id}`); }
    async updateInvoice(id, data)      { return this._request('PUT', `/financial/invoices/${id}`, data); }
    async registerPayment(invoiceId, data) { return this._request('POST', `/financial/invoices/${invoiceId}/pay`, data); }
    async listPayments()               { return this._request('GET', '/financial/payments'); }

    // ─── Auditoria ────────────────────────────────────────────────────────────
    async logEvent(data)            { return this._request('POST', '/audit/log', data); }
    async getLogs(params = {}) {
        const qs = new URLSearchParams(params).toString();
        return this._request('GET', `/audit/logs${qs ? `?${qs}` : ''}`);
    }
    async getAuditSummary()         { return this._request('GET', '/audit/logs/summary'); }

    // ─── Health ───────────────────────────────────────────────────────────────
    async health() {
        const res = await fetch(`${API_BASE.replace('/api', '')}/health`);
        return res.json();
    }

    // ─── Utilitários ──────────────────────────────────────────────────────────
    getCurrentUser() {
        const raw = localStorage.getItem('dentibot_user');
        try { return raw ? JSON.parse(raw) : null; } catch { return null; }
    }

    isAuthenticated() { return !!this._token; }

    requireAuth(redirectTo = 'login.html') {
        if (!this.isAuthenticated()) {
            window.location.href = redirectTo;
            return false;
        }
        return true;
    }
}

window.api = new DentiBotAPI();
