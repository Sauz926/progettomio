const API_BASE = "/api";
const CONSULTATION_WELCOME = "Ciao! Sono il tuo assistente per la scelta del dispositivo perfetto. Per iniziare, dimmi: stai cercando uno smartphone, uno smartwatch o un tablet?";
const CHATBOT_WELCOME = "Fai pure una domanda su smartphone, smartwatch o tablet. Rispondo usando le schede tecniche PDF caricate nel sistema.";

const state = {
    isAdmin: false,
    devices: [],
    documents: [],
    consultation: {
        sessionId: "",
        history: [],
        phase: "RACCOLTA_DATI",
        recommendation: []
    },
    chatbot: {
        history: []
    }
};

const dom = {};
let toastTimer = null;

document.addEventListener("DOMContentLoaded", async () => {
    cacheDom();
    initNavigation();
    initConsultation();
    initChatbot();
    initDeviceModal();
    initAdminActions();

    await detectAdmin();
    await loadConsultationHistory();
    startNewConsultation();
});

function cacheDom() {
    dom.navButtons = Array.from(document.querySelectorAll(".nav-pill"));
    dom.sections = Array.from(document.querySelectorAll(".section"));
    dom.roleBadge = document.getElementById("roleBadge");
    dom.toast = document.getElementById("toast");

    dom.consultMessages = document.getElementById("consultMessages");
    dom.consultForm = document.getElementById("consultForm");
    dom.consultInput = document.getElementById("consultInput");
    dom.consultResetBtn = document.getElementById("consultResetBtn");
    dom.consultSaveBtn = document.getElementById("consultSaveBtn");
    dom.consultPhaseBadge = document.getElementById("consultPhaseBadge");
    dom.recommendationStrip = document.getElementById("recommendationStrip");
    dom.recommendationCards = document.getElementById("recommendationCards");
    dom.sessionCode = document.getElementById("sessionCode");
    dom.sessionPhase = document.getElementById("sessionPhase");
    dom.sessionRecommendation = document.getElementById("sessionRecommendation");
    dom.historyList = document.getElementById("historyList");

    dom.deviceTableBody = document.getElementById("deviceTableBody");
    dom.deviceCategoryFilter = document.getElementById("deviceCategoryFilter");
    dom.deviceBudgetFilter = document.getElementById("deviceBudgetFilter");
    dom.deviceOsFilter = document.getElementById("deviceOsFilter");
    dom.deviceFilterBtn = document.getElementById("deviceFilterBtn");
    dom.deviceAddBtn = document.getElementById("deviceAddBtn");
    dom.deviceImportBtn = document.getElementById("deviceImportBtn");
    dom.deviceImportInput = document.getElementById("deviceImportInput");
    dom.statsGrid = document.getElementById("statsGrid");

    dom.documentUploadBtn = document.getElementById("documentUploadBtn");
    dom.documentInput = document.getElementById("documentInput");
    dom.documentTableBody = document.getElementById("documentTableBody");

    dom.deviceModal = document.getElementById("deviceModal");
    dom.deviceModalTitle = document.getElementById("deviceModalTitle");
    dom.deviceModalClose = document.getElementById("deviceModalClose");
    dom.deviceCancelBtn = document.getElementById("deviceCancelBtn");
    dom.deviceForm = document.getElementById("deviceForm");
    dom.deviceId = document.getElementById("deviceId");
    dom.deviceName = document.getElementById("deviceName");
    dom.deviceBrand = document.getElementById("deviceBrand");
    dom.deviceModel = document.getElementById("deviceModel");
    dom.deviceCategory = document.getElementById("deviceCategory");
    dom.devicePrice = document.getElementById("devicePrice");
    dom.deviceOs = document.getElementById("deviceOs");
    dom.deviceAvailable = document.getElementById("deviceAvailable");
    dom.deviceDescription = document.getElementById("deviceDescription");
    dom.deviceSpecs = document.getElementById("deviceSpecs");
    dom.deviceStrengths = document.getElementById("deviceStrengths");
    dom.deviceWeaknesses = document.getElementById("deviceWeaknesses");

    dom.chatbotMessages = document.getElementById("chatbotMessages");
    dom.chatbotForm = document.getElementById("chatbotForm");
    dom.chatbotInput = document.getElementById("chatbotInput");
    dom.chatbotResetBtn = document.getElementById("chatbotResetBtn");
}

function initNavigation() {
    dom.navButtons.forEach((button) => {
        button.addEventListener("click", () => showSection(button.dataset.section));
    });
}

function showSection(sectionId) {
    dom.navButtons.forEach((button) => button.classList.toggle("active", button.dataset.section === sectionId));
    dom.sections.forEach((section) => section.classList.toggle("active", section.id === sectionId));
}

function initConsultation() {
    dom.consultForm.addEventListener("submit", onConsultationSubmit);
    dom.consultResetBtn.addEventListener("click", startNewConsultation);
    dom.consultSaveBtn.addEventListener("click", saveConsultation);
    dom.historyList.addEventListener("click", (event) => {
        const button = event.target.closest("[data-download-session]");
        if (!button) {
            return;
        }
        triggerDownload(`/api/consultazione/${encodeURIComponent(button.dataset.downloadSession)}/pdf`);
    });
}

function startNewConsultation() {
    state.consultation.sessionId = createSessionId();
    state.consultation.history = [{ role: "assistant", content: CONSULTATION_WELCOME }];
    state.consultation.phase = "RACCOLTA_DATI";
    state.consultation.recommendation = [];
    renderConsultation();
}

async function onConsultationSubmit(event) {
    event.preventDefault();
    const message = dom.consultInput.value.trim();
    if (!message) {
        return;
    }

    const historyBefore = state.consultation.history.map(stripMessage);
    state.consultation.history.push({ role: "user", content: message });
    dom.consultInput.value = "";
    renderConsultation();

    try {
        const response = await fetchJson(`${API_BASE}/consultazione/chat`, {
            method: "POST",
            body: JSON.stringify({
                sessioneId: state.consultation.sessionId,
                userMessage: message,
                conversationHistory: historyBefore
            })
        });

        state.consultation.phase = response.fase;
        state.consultation.recommendation = response.dispositiviConsigliati || [];
        state.consultation.history.push({
            role: "assistant",
            content: response.risposta,
            cards: response.dispositiviConsigliati || [],
            sources: response.fonti || []
        });
        renderConsultation();
    } catch (error) {
        showToast(error.message || "Errore durante la consultazione");
    }
}

function renderConsultation() {
    dom.sessionCode.textContent = state.consultation.sessionId;
    dom.sessionPhase.textContent = prettyPhase(state.consultation.phase);
    dom.consultPhaseBadge.textContent = prettyPhase(state.consultation.phase);
    dom.sessionRecommendation.textContent = state.consultation.recommendation[0]?.nome || "Nessuno";

    dom.consultSaveBtn.hidden = state.consultation.phase !== "RACCOMANDAZIONE";
    dom.recommendationStrip.hidden = !state.consultation.recommendation.length;
    dom.recommendationCards.innerHTML = state.consultation.recommendation
        .map((card) => renderDeviceCard(card))
        .join("");

    dom.consultMessages.innerHTML = state.consultation.history
        .map((message) => renderChatMessage(message))
        .join("");
    scrollToBottom(dom.consultMessages);
}

async function saveConsultation() {
    try {
        await fetchJson(`${API_BASE}/consultazione/salva`, {
            method: "POST",
            body: JSON.stringify({
                sessioneId: state.consultation.sessionId,
                conversationHistory: state.consultation.history.map(stripMessage)
            })
        });

        showToast("Raccomandazione salvata. Sto preparando il PDF.");
        await loadConsultationHistory();
        triggerDownload(`/api/consultazione/${encodeURIComponent(state.consultation.sessionId)}/pdf`);
    } catch (error) {
        showToast(error.message || "Impossibile salvare la raccomandazione");
    }
}

async function loadConsultationHistory() {
    try {
        const history = await fetchJson(`${API_BASE}/consultazione/storico`);
        renderConsultationHistory(history);
    } catch (error) {
        dom.historyList.className = "history-list empty-state";
        dom.historyList.textContent = "Storico non disponibile.";
    }
}

function renderConsultationHistory(history) {
    if (!Array.isArray(history) || history.length === 0) {
        dom.historyList.className = "history-list empty-state";
        dom.historyList.textContent = "Nessuna raccomandazione salvata.";
        return;
    }

    dom.historyList.className = "history-list";
    dom.historyList.innerHTML = history
        .slice(0, 6)
        .map((item) => `
            <article class="history-item">
                <h4>${escapeHtml(item.raccomandazionePrincipale || "Raccomandazione salvata")}</h4>
                <p>${escapeHtml(item.esigenzeSummary || "Profilo senza riepilogo")}</p>
                <div class="history-actions">
                    <span>${formatDate(item.dataCreazione)}</span>
                    <button class="btn btn-secondary" data-download-session="${escapeHtml(item.sessioneId)}">Scarica PDF</button>
                </div>
            </article>
        `)
        .join("");
}

async function detectAdmin() {
    try {
        const stats = await fetchJson(`${API_BASE}/dispositivi/statistiche`);
        state.isAdmin = true;
        dom.roleBadge.textContent = "Admin";
        document.querySelectorAll(".admin-only").forEach((element) => {
            element.hidden = false;
        });
        document.querySelectorAll(".admin-only-section").forEach((element) => {
            element.hidden = false;
        });
        renderStats(stats);
        await Promise.all([loadDevices(), loadDocuments()]);
    } catch (error) {
        state.isAdmin = false;
        dom.roleBadge.textContent = "Utente";
    }
}

function initAdminActions() {
    dom.deviceFilterBtn.addEventListener("click", loadDevices);
    dom.deviceAddBtn.addEventListener("click", () => openDeviceModal());
    dom.deviceImportBtn.addEventListener("click", () => dom.deviceImportInput.click());
    dom.deviceImportInput.addEventListener("change", importDeviceFromPdf);
    dom.documentUploadBtn.addEventListener("click", () => dom.documentInput.click());
    dom.documentInput.addEventListener("change", uploadDocument);

    dom.deviceTableBody.addEventListener("click", async (event) => {
        const editButton = event.target.closest("[data-edit-device]");
        if (editButton) {
            const device = state.devices.find((item) => String(item.id) === editButton.dataset.editDevice);
            if (device) {
                openDeviceModal(device);
            }
            return;
        }

        const deleteButton = event.target.closest("[data-delete-device]");
        if (!deleteButton) {
            return;
        }

        if (!window.confirm("Eliminare questo dispositivo dal catalogo?")) {
            return;
        }

        try {
            await fetchJson(`${API_BASE}/dispositivi/${deleteButton.dataset.deleteDevice}`, {
                method: "DELETE"
            });
            showToast("Dispositivo eliminato");
            await loadDevices();
            await refreshStats();
        } catch (error) {
            showToast(error.message || "Errore durante l'eliminazione");
        }
    });

    dom.documentTableBody.addEventListener("click", async (event) => {
        const downloadButton = event.target.closest("[data-download-document]");
        if (downloadButton) {
            triggerDownload(`/api/documents/${downloadButton.dataset.downloadDocument}/download`);
            return;
        }

        const deleteButton = event.target.closest("[data-delete-document]");
        if (!deleteButton) {
            return;
        }

        if (!window.confirm("Eliminare questo documento?")) {
            return;
        }

        try {
            await fetchJson(`${API_BASE}/documents/${deleteButton.dataset.deleteDocument}`, {
                method: "DELETE"
            });
            showToast("Documento eliminato");
            await loadDocuments();
        } catch (error) {
            showToast(error.message || "Errore durante l'eliminazione del documento");
        }
    });
}

async function loadDevices() {
    if (!state.isAdmin) {
        return;
    }

    const params = new URLSearchParams();
    if (dom.deviceCategoryFilter.value) {
        params.set("categoria", dom.deviceCategoryFilter.value);
    }
    if (dom.deviceBudgetFilter.value) {
        params.set("budgetMax", dom.deviceBudgetFilter.value);
    }
    if (dom.deviceOsFilter.value.trim()) {
        params.set("os", dom.deviceOsFilter.value.trim());
    }

    const url = params.toString()
        ? `${API_BASE}/dispositivi/cerca?${params.toString()}`
        : `${API_BASE}/dispositivi`;

    try {
        state.devices = await fetchJson(url);
        renderDevices();
    } catch (error) {
        showToast(error.message || "Impossibile caricare i dispositivi");
    }
}

function renderDevices() {
    if (!state.devices.length) {
        dom.deviceTableBody.innerHTML = `
            <tr>
                <td colspan="7">
                    <div class="empty-state">Nessun dispositivo trovato con i filtri correnti.</div>
                </td>
            </tr>
        `;
        return;
    }

    dom.deviceTableBody.innerHTML = state.devices.map((device) => `
        <tr>
            <td>
                <strong>${escapeHtml(device.nome)}</strong><br>
                <span class="text-soft">${escapeHtml(device.modello || "")}</span>
            </td>
            <td>${escapeHtml(device.marca || "-")}</td>
            <td>${renderCategoryBadge(device.categoria)}</td>
            <td>${escapeHtml(device.sistemaOperativo || "-")}</td>
            <td>${formatCurrency(device.prezzoEuro)}</td>
            <td><span class="status-dot ${device.disponibile ? "" : "off"}">${device.disponibile ? "Sì" : "No"}</span></td>
            <td>
                <div class="table-actions">
                    <button class="btn btn-secondary" data-edit-device="${device.id}">Modifica</button>
                    <button class="btn btn-secondary" data-delete-device="${device.id}">Elimina</button>
                </div>
            </td>
        </tr>
    `).join("");
}

async function refreshStats() {
    if (!state.isAdmin) {
        return;
    }
    try {
        const stats = await fetchJson(`${API_BASE}/dispositivi/statistiche`);
        renderStats(stats);
    } catch (error) {
        showToast(error.message || "Statistiche non disponibili");
    }
}

function renderStats(stats) {
    const blocks = [];
    blocks.push(`
        <div class="stat-card">
            <span>Catalogo totale</span>
            <strong>${escapeHtml(String(stats.totale ?? 0))}</strong>
        </div>
    `);

    Object.entries(stats.perCategoria || {}).forEach(([key, value]) => {
        blocks.push(`
            <div class="stat-card">
                <span>${prettyCategory(key)}</span>
                <strong>${escapeHtml(String(value))}</strong>
            </div>
        `);
    });

    dom.statsGrid.innerHTML = blocks.join("");
}

function initDeviceModal() {
    dom.deviceModalClose.addEventListener("click", closeDeviceModal);
    dom.deviceCancelBtn.addEventListener("click", closeDeviceModal);
    dom.deviceModal.addEventListener("click", (event) => {
        if (event.target === dom.deviceModal) {
            closeDeviceModal();
        }
    });

    dom.deviceForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            nome: dom.deviceName.value.trim(),
            marca: dom.deviceBrand.value.trim(),
            modello: emptyToNull(dom.deviceModel.value),
            categoria: dom.deviceCategory.value,
            prezzoEuro: emptyToNull(dom.devicePrice.value),
            sistemaOperativo: emptyToNull(dom.deviceOs.value),
            descrizione: emptyToNull(dom.deviceDescription.value),
            specificheTecniche: emptyToNull(dom.deviceSpecs.value),
            puntiDiForza: emptyToNull(dom.deviceStrengths.value),
            puntiDeboli: emptyToNull(dom.deviceWeaknesses.value),
            disponibile: dom.deviceAvailable.checked
        };

        const editingId = dom.deviceId.value;
        const url = editingId ? `${API_BASE}/dispositivi/${editingId}` : `${API_BASE}/dispositivi`;
        const method = editingId ? "PUT" : "POST";

        try {
            await fetchJson(url, {
                method,
                body: JSON.stringify(payload)
            });
            closeDeviceModal();
            showToast(editingId ? "Dispositivo aggiornato" : "Dispositivo creato");
            await loadDevices();
            await refreshStats();
        } catch (error) {
            showToast(error.message || "Errore durante il salvataggio del dispositivo");
        }
    });
}

function openDeviceModal(device = null) {
    dom.deviceModal.hidden = false;
    dom.deviceModalTitle.textContent = device ? "Modifica dispositivo" : "Nuovo dispositivo";
    dom.deviceId.value = device?.id || "";
    dom.deviceName.value = device?.nome || "";
    dom.deviceBrand.value = device?.marca || "";
    dom.deviceModel.value = device?.modello || "";
    dom.deviceCategory.value = device?.categoria || "SMARTPHONE";
    dom.devicePrice.value = device?.prezzoEuro ?? "";
    dom.deviceOs.value = device?.sistemaOperativo || "";
    dom.deviceAvailable.checked = device?.disponibile ?? true;
    dom.deviceDescription.value = device?.descrizione || "";
    dom.deviceSpecs.value = device?.specificheTecniche || "";
    dom.deviceStrengths.value = device?.puntiDiForza || "";
    dom.deviceWeaknesses.value = device?.puntiDeboli || "";
}

function closeDeviceModal() {
    dom.deviceModal.hidden = true;
    dom.deviceForm.reset();
    dom.deviceAvailable.checked = true;
    dom.deviceId.value = "";
}

async function importDeviceFromPdf(event) {
    const file = event.target.files?.[0];
    dom.deviceImportInput.value = "";
    if (!file) {
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        await fetchJson(`${API_BASE}/dispositivi/importa-pdf`, {
            method: "POST",
            body: formData
        });
        showToast("Dispositivo importato dal PDF");
        await loadDevices();
        await refreshStats();
    } catch (error) {
        showToast(error.message || "Importazione PDF non riuscita");
    }
}

async function uploadDocument(event) {
    const file = event.target.files?.[0];
    dom.documentInput.value = "";
    if (!file) {
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        await fetchJson(`${API_BASE}/documents/upload`, {
            method: "POST",
            body: formData
        });
        showToast("Documento caricato e indicizzato");
        await loadDocuments();
    } catch (error) {
        showToast(error.message || "Errore durante il caricamento del documento");
    }
}

async function loadDocuments() {
    if (!state.isAdmin) {
        return;
    }

    try {
        state.documents = await fetchJson(`${API_BASE}/documents`);
        renderDocuments();
    } catch (error) {
        showToast(error.message || "Impossibile caricare i documenti");
    }
}

function renderDocuments() {
    if (!state.documents.length) {
        dom.documentTableBody.innerHTML = `
            <tr>
                <td colspan="6">
                    <div class="empty-state">Nessun documento caricato.</div>
                </td>
            </tr>
        `;
        return;
    }

    dom.documentTableBody.innerHTML = state.documents.map((document) => `
        <tr>
            <td>${escapeHtml(document.fileName)}</td>
            <td>${formatFileSize(document.fileSize)}</td>
            <td>${formatDate(document.uploadDate)}</td>
            <td><span class="status-dot ${document.processed ? "" : "off"}">${document.processed ? "Processato" : "In lavorazione"}</span></td>
            <td>${escapeHtml(String(document.chunkCount ?? 0))}</td>
            <td>
                <div class="table-actions">
                    <button class="btn btn-secondary" data-download-document="${document.id}">Download</button>
                    <button class="btn btn-secondary" data-delete-document="${document.id}">Elimina</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function initChatbot() {
    dom.chatbotResetBtn.addEventListener("click", startNewChatbot);
    dom.chatbotForm.addEventListener("submit", onChatbotSubmit);
    startNewChatbot();
}

function startNewChatbot() {
    state.chatbot.history = [{ role: "assistant", content: CHATBOT_WELCOME }];
    renderChatbot();
}

async function onChatbotSubmit(event) {
    event.preventDefault();
    const message = dom.chatbotInput.value.trim();
    if (!message) {
        return;
    }

    const historyBefore = state.chatbot.history.map(stripMessage);
    state.chatbot.history.push({ role: "user", content: message });
    dom.chatbotInput.value = "";
    renderChatbot();

    try {
        const response = await fetchJson(`${API_BASE}/chatbot/chat`, {
            method: "POST",
            body: JSON.stringify({
                question: message,
                history: historyBefore,
                systemPrompt: null
            })
        });

        state.chatbot.history.push({
            role: "assistant",
            content: response.answer,
            sources: response.sources?.map((source) => source.reference) || []
        });
        renderChatbot();
    } catch (error) {
        showToast(error.message || "Errore durante la chat");
    }
}

function renderChatbot() {
    dom.chatbotMessages.innerHTML = state.chatbot.history
        .map((message) => renderChatMessage(message))
        .join("");
    scrollToBottom(dom.chatbotMessages);
}

function renderChatMessage(message) {
    const meta = message.role === "assistant" ? "Assistente" : "Tu";
    const cards = Array.isArray(message.cards) && message.cards.length
        ? `<div class="recommendation-grid">${message.cards.map((card) => renderDeviceCard(card)).join("")}</div>`
        : "";
    const sources = Array.isArray(message.sources) && message.sources.length
        ? `<div class="source-list">${message.sources.map((source) => `<span class="source-pill">${escapeHtml(source)}</span>`).join("")}</div>`
        : "";

    return `
        <article class="message ${message.role}">
            <div class="message-meta">${meta}</div>
            <div class="bubble">${escapeHtml(message.content).replace(/\n/g, "<br>")}</div>
            ${cards}
            ${sources}
        </article>
    `;
}

function renderDeviceCard(card) {
    const score = Number(card.punteggio || 0);
    return `
        <article class="device-card">
            <div>${renderCategoryBadge(card.categoria)}</div>
            <h4>${escapeHtml(card.nome)}</h4>
            <p>${escapeHtml(card.marca || "")}</p>
            <div class="price">${formatCurrency(card.prezzoEuro)}</div>
            <div class="score-meter">
                <div class="score-track">
                    <div class="score-fill" style="width: ${Math.max(0, Math.min(100, score))}%"></div>
                </div>
                <strong>${escapeHtml(String(score))}/100</strong>
            </div>
            <p>${escapeHtml(card.motivazione || "Motivazione non disponibile")}</p>
        </article>
    `;
}

function renderCategoryBadge(category) {
    const normalized = String(category || "").toUpperCase();
    const className = normalized === "SMARTWATCH"
        ? "badge badge-smartwatch"
        : normalized === "TABLET"
            ? "badge badge-tablet"
            : "badge badge-smartphone";
    return `<span class="${className}">${escapeHtml(prettyCategory(normalized || "SMARTPHONE"))}</span>`;
}

async function fetchJson(url, options = {}) {
    const config = { ...options };
    config.headers = { ...(options.headers || {}) };

    if (!(config.body instanceof FormData) && config.body && !config.headers["Content-Type"]) {
        config.headers["Content-Type"] = "application/json";
    }

    const response = await fetch(url, config);
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message = typeof payload === "string"
            ? payload
            : payload.error || `Richiesta fallita (${response.status})`;
        throw new Error(message);
    }

    return payload;
}

function showToast(message) {
    dom.toast.textContent = message;
    dom.toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        dom.toast.hidden = true;
    }, 2800);
}

function scrollToBottom(element) {
    element.scrollTop = element.scrollHeight;
}

function stripMessage(message) {
    return { role: message.role, content: message.content };
}

function triggerDownload(url) {
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
}

function prettyPhase(phase) {
    return String(phase || "").replaceAll("_", " ");
}

function prettyCategory(category) {
    const value = String(category || "").toUpperCase();
    if (value === "SMARTWATCH") return "Smartwatch";
    if (value === "TABLET") return "Tablet";
    return "Smartphone";
}

function formatCurrency(value) {
    if (value === null || value === undefined || value === "") {
        return "Prezzo non indicato";
    }
    const amount = Number(value);
    if (Number.isNaN(amount)) {
        return `${value} euro`;
    }
    return new Intl.NumberFormat("it-IT", {
        style: "currency",
        currency: "EUR"
    }).format(amount);
}

function formatFileSize(bytes) {
    if (!bytes && bytes !== 0) {
        return "-";
    }
    if (bytes < 1024) {
        return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return new Intl.DateTimeFormat("it-IT", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(date);
}

function createSessionId() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
        return window.crypto.randomUUID();
    }
    return `session-${Date.now()}`;
}

function emptyToNull(value) {
    const trimmed = value?.trim?.() ?? value;
    return trimmed === "" ? null : trimmed;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
