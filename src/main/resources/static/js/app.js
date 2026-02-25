// API Base URL
const API_BASE = '/api';

// DOM Elements
let documentsTable, machinesGrid, assessmentsList;
let dropZone, fileInput, uploadProgress, progressFill, uploadStatus;
let loadingOverlay, assessmentModal, assessmentDetails;
let machineManualUploadBtn, machineManualFileInput, machineManualStatus;
let machineForm, machineFormTitle, machineSubmitBtn, machineResetBtn;

// State
let editingMachineId = null;
let machinesById = new Map();

// Chat state (UI-only)
let currentAssessmentContext = null;
let assessmentChatThreads = new Map();

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    initElements();
    initAssessmentModalUi();
    initNavigation();
    initFileUpload();
    initMachineForm();
    initMachineManualUpload();
    loadDocuments();
    loadMachines();
    loadAssessments();
});

function initElements() {
    documentsTable = document.getElementById('documentsBody');
    machinesGrid = document.getElementById('machinesGrid');
    assessmentsList = document.getElementById('assessmentsList');
    dropZone = document.getElementById('dropZone');
    fileInput = document.getElementById('fileInput');
    uploadProgress = document.getElementById('uploadProgress');
    progressFill = document.getElementById('progressFill');
    uploadStatus = document.getElementById('uploadStatus');
    loadingOverlay = document.getElementById('loadingOverlay');
    assessmentModal = document.getElementById('assessmentModal');
    assessmentDetails = document.getElementById('assessmentDetails');
    machineManualUploadBtn = document.getElementById('machineManualUploadBtn');
    machineManualFileInput = document.getElementById('machineManualFileInput');
    machineManualStatus = document.getElementById('machineManualStatus');

    machineForm = document.getElementById('machineForm');
    machineFormTitle = document.getElementById('machineFormTitle');
    machineSubmitBtn = document.getElementById('machineSubmitBtn');
    machineResetBtn = document.getElementById('machineResetBtn');
}

function initAssessmentModalUi() {
    if (assessmentDetails) {
        assessmentDetails.addEventListener('click', (e) => {
            const sourceButton = e.target.closest?.('.source-btn');
            if (sourceButton) {
                const panelId = sourceButton.getAttribute('aria-controls');
                if (!panelId) return;

                const panel = document.getElementById(panelId);
                if (!panel) return;

                const isExpanded = sourceButton.getAttribute('aria-expanded') === 'true';
                sourceButton.setAttribute('aria-expanded', String(!isExpanded));
                panel.hidden = isExpanded;
                return;
            }

            const tabButton = e.target.closest?.('.assessment-tab');
            if (tabButton) {
                const layout = tabButton.closest('.assessment-layout');
                const panel = tabButton.getAttribute('data-panel');
                if (!layout || !panel) return;
                setAssessmentActivePanel(layout, panel);
                return;
            }

            const askButton = e.target.closest?.('.ask-assistant-btn');
            if (askButton) {
                const layout = askButton.closest('.assessment-layout');
                const question = askButton.getAttribute('data-question') || '';
                if (!layout || !question) return;
                setAssessmentActivePanel(layout, 'chat');
                prefillAssessmentChatInput(layout, question);
                return;
            }

            const suggestionButton = e.target.closest?.('.chat-suggestion');
            if (suggestionButton) {
                const layout = suggestionButton.closest('.assessment-layout');
                const question = suggestionButton.getAttribute('data-question') || '';
                if (!layout || !question) return;
                setAssessmentActivePanel(layout, 'chat');
                prefillAssessmentChatInput(layout, question);
                return;
            }

            const resetButton = e.target.closest?.('.chat-reset-btn');
            if (resetButton) {
                const layout = resetButton.closest('.assessment-layout');
                const assessmentId = Number(layout?.getAttribute('data-assessment-id'));
                if (!layout || !Number.isFinite(assessmentId)) return;

                if (!confirm('Vuoi cancellare la chat per questo assessment?')) return;
                resetAssessmentChatThread(assessmentId);
                renderAssessmentChatIntoLayout(layout, assessmentId);
                return;
            }
        });

        assessmentDetails.addEventListener('submit', (e) => {
            const form = e.target.closest?.('.chat-composer');
            if (!form) return;
            e.preventDefault();
            submitAssessmentChat(form).catch((err) => console.error('Chat submit error:', err));
        });

        assessmentDetails.addEventListener('keydown', (e) => {
            const input = e.target.closest?.('.chat-input');
            if (!input) return;

            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                input.closest('form')?.requestSubmit();
            }
        });

        assessmentDetails.addEventListener('input', (e) => {
            const input = e.target.closest?.('.chat-input');
            if (!input) return;
            autoResizeChatInput(input);
        });
    }

    if (assessmentModal) {
        assessmentModal.addEventListener('click', (e) => {
            if (e.target === assessmentModal) closeModal();
        });
    }

    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;
        if (!assessmentModal?.classList.contains('active')) return;
        closeModal();
    });
}

// Navigation
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const sectionId = this.dataset.section;

            // Update active states
            navLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');

            document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
            document.getElementById(sectionId).classList.add('active');
        });
    });
}

// File Upload
function initFileUpload() {
    // Drag and drop events
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('dragover');
    });

    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover');
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            uploadFile(files[0]);
        }
    });

    // File input change
    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            uploadFile(fileInput.files[0]);
        }
    });
}

async function uploadFile(file) {
    if (file.type !== 'application/pdf') {
        alert('Solo file PDF sono accettati');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    uploadProgress.style.display = 'block';
    progressFill.style.width = '0%';
    uploadStatus.textContent = 'Caricamento in corso...';

    try {
        // Simulate progress
        let progress = 0;
        const progressInterval = setInterval(() => {
            progress += 10;
            if (progress <= 90) {
                progressFill.style.width = progress + '%';
            }
        }, 200);

        const response = await fetch(`${API_BASE}/documents/upload`, {
            method: 'POST',
            body: formData
        });

        clearInterval(progressInterval);
        progressFill.style.width = '100%';

        if (response.ok) {
            const result = await response.json();
            uploadStatus.textContent = `✓ File caricato con successo! ${result.chunkCount} chunks creati.`;
            loadDocuments();

            setTimeout(() => {
                uploadProgress.style.display = 'none';
                fileInput.value = '';
            }, 3000);
        } else {
            const error = await response.json();
            uploadStatus.textContent = `✗ Errore: ${error.error}`;
        }
    } catch (error) {
        console.error('Upload error:', error);
        uploadStatus.textContent = `✗ Errore durante il caricamento`;
    }
}

// Documents
async function loadDocuments() {
    try {
        const response = await fetch(`${API_BASE}/documents`);
        const documents = await response.json();

        if (documents.length === 0) {
            documentsTable.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="empty-state-icon">📄</div>
                        <p>Nessun documento caricato</p>
                    </td>
                </tr>
            `;
            return;
        }

        documentsTable.innerHTML = documents.map(doc => `
            <tr>
                <td><strong>${doc.fileName}</strong></td>
                <td>${formatFileSize(doc.fileSize)}</td>
                <td>${formatDate(doc.uploadDate)}</td>
                <td>
                    <span class="badge ${doc.processed ? 'badge-success' : 'badge-warning'}">
                        ${doc.processed ? 'Processato' : 'In elaborazione'}
                    </span>
                </td>
                <td>${doc.chunkCount || '-'}</td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="downloadDocument(${doc.id})">
                        📥
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteDocument(${doc.id})">
                        🗑️
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error loading documents:', error);
    }
}

async function deleteDocument(id) {
    if (!confirm('Sei sicuro di voler eliminare questo documento?')) return;

    try {
        const response = await fetch(`${API_BASE}/documents/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadDocuments();
        } else {
            alert('Errore durante l\'eliminazione');
        }
    } catch (error) {
        console.error('Error deleting document:', error);
    }
}

function downloadDocument(id) {
    window.open(`${API_BASE}/documents/${id}/download`, '_blank');
}

async function downloadLatestAssessmentPdf(machineId) {
    try {
        const response = await fetch(`${API_BASE}/macchinari/${machineId}/assessments/latest/pdf`, {
            method: 'GET',
            headers: { 'Accept': 'application/pdf' },
            credentials: 'same-origin'
        });

        if (!response.ok) {
            const payload = await response.json().catch(() => ({}));
            alert(payload?.error || 'Impossibile scaricare il PDF');
            return;
        }

        const blob = await response.blob();
        const contentDisposition = response.headers.get('Content-Disposition') || '';
        const filename = getFilenameFromContentDisposition(contentDisposition) || `assessment_${machineId}.pdf`;

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (error) {
        console.error('Error downloading assessment PDF:', error);
        alert('Errore durante il download del PDF');
    }
}

// Machines
function initMachineForm() {
    if (!machineForm) return;

    machineForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const machine = {
            nome: document.getElementById('machineName').value,
            produttore: document.getElementById('machineProducer').value,
            modello: document.getElementById('machineModel').value,
            numeroSerie: document.getElementById('machineSerial').value,
            annoProduzione: document.getElementById('machineYear').value ? parseInt(document.getElementById('machineYear').value) : null,
            categoria: document.getElementById('machineCategory').value,
            descrizione: document.getElementById('machineDescription').value,
            specificheTecniche: document.getElementById('machineSpecs').value
        };

        const isEdit = editingMachineId !== null;
        const url = isEdit ? `${API_BASE}/macchinari/${editingMachineId}` : `${API_BASE}/macchinari`;
        const method = isEdit ? 'PUT' : 'POST';
        const submitBtn = machineSubmitBtn || machineForm.querySelector('button[type="submit"]');

        if (submitBtn) submitBtn.disabled = true;

        try {
            const response = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(machine)
            });

            if (response.ok) {
                if (isEdit) {
                    setMachineFormMode('create');
                    machineForm.reset();
                    loadAssessments();
                    alert('Macchinario aggiornato con successo!');
                } else {
                    machineForm.reset();
                    alert('Macchinario aggiunto con successo!');
                }

                loadMachines();
            } else {
                const error = await response.json().catch(() => ({}));
                alert(error.error || 'Errore durante il salvataggio');
            }
        } catch (error) {
            console.error('Error saving machine:', error);
            alert('Errore durante il salvataggio');
        } finally {
            if (submitBtn) submitBtn.disabled = false;
        }
    });

    machineForm.addEventListener('reset', () => {
        if (editingMachineId !== null) {
            setMachineFormMode('create');
        }
    });
}

function setMachineFormMode(mode, machine) {
    if (mode === 'edit') {
        if (!machine) return;
        editingMachineId = machine.id;
        if (machineFormTitle) machineFormTitle.textContent = `✏️ Modifica Macchinario: ${machine.nome}`;
        if (machineSubmitBtn) machineSubmitBtn.textContent = '💾 Salva Modifiche';
        if (machineResetBtn) machineResetBtn.textContent = 'Annulla';
        return;
    }

    // default: create mode
    editingMachineId = null;
    if (machineFormTitle) machineFormTitle.textContent = 'Nuovo Macchinario';
    if (machineSubmitBtn) machineSubmitBtn.textContent = 'Aggiungi Macchinario';
    if (machineResetBtn) machineResetBtn.textContent = 'Reset';
    setMachineManualStatus('', null);
}

function fillMachineForm(machine) {
    if (!machine || typeof machine !== 'object') return;

    const set = (elementId, value) => {
        const el = document.getElementById(elementId);
        if (!el) return;
        el.value = value ?? '';
    };

    set('machineName', machine.nome);
    set('machineProducer', machine.produttore);
    set('machineModel', machine.modello);
    set('machineSerial', machine.numeroSerie);
    set('machineYear', machine.annoProduzione);
    set('machineCategory', machine.categoria);
    set('machineDescription', machine.descrizione);
    set('machineSpecs', machine.specificheTecniche);
}

async function editMachine(id) {
    let machine = machinesById.get(id);

    if (!machine) {
        try {
            const response = await fetch(`${API_BASE}/macchinari/${id}`);
            if (response.ok) {
                machine = await response.json();
            }
        } catch (error) {
            console.error('Error loading machine:', error);
        }
    }

    if (!machine) {
        alert('Macchinario non trovato');
        return;
    }

    setMachineFormMode('edit', machine);
    fillMachineForm(machine);

    machineFormTitle?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    document.getElementById('machineName')?.focus();
}

function initMachineManualUpload() {
    if (!machineManualUploadBtn || !machineManualFileInput) return;

    machineManualUploadBtn.addEventListener('click', () => {
        machineManualFileInput.click();
    });

    machineManualFileInput.addEventListener('change', async () => {
        if (!machineManualFileInput.files || machineManualFileInput.files.length === 0) return;
        await uploadMachineManual(machineManualFileInput.files[0]);
    });
}

async function uploadMachineManual(file) {
    const isPdf = file.type === 'application/pdf' || file.name?.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
        setMachineManualStatus('✗ Solo file PDF sono accettati', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    setMachineManualStatus('Analisi del manuale in corso...', 'loading');
    machineManualUploadBtn.disabled = true;

    try {
        const response = await fetch(`${API_BASE}/macchinari/manual/extract`, {
            method: 'POST',
            body: formData
        });

        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            const errorMessage = payload?.error || 'Errore durante l\'analisi del manuale';
            throw new Error(errorMessage);
        }

        prefillMachineForm(payload);
        setMachineManualStatus('✓ Campi pre-compilati. Verifica e poi salva.', 'success');
    } catch (error) {
        console.error('Manual upload error:', error);
        setMachineManualStatus(`✗ ${error.message || 'Errore durante l\'analisi del manuale'}`, 'error');
    } finally {
        machineManualUploadBtn.disabled = false;
        machineManualFileInput.value = '';
    }
}

function prefillMachineForm(data) {
    if (!data || typeof data !== 'object') return;

    const setIfPresent = (elementId, value) => {
        const el = document.getElementById(elementId);
        if (!el) return;
        if (value === null || value === undefined) return;
        const stringValue = typeof value === 'string' ? value.trim() : value;
        if (stringValue === '') return;
        el.value = stringValue;
    };

    setIfPresent('machineName', data.nome);
    setIfPresent('machineProducer', data.produttore);
    setIfPresent('machineModel', data.modello);
    setIfPresent('machineSerial', data.numeroSerie);
    setIfPresent('machineDescription', data.descrizione);
    setIfPresent('machineSpecs', data.specificheTecniche);

    if (data.annoProduzione !== null && data.annoProduzione !== undefined) {
        const year = parseInt(data.annoProduzione, 10);
        if (!Number.isNaN(year)) {
            setIfPresent('machineYear', year);
        }
    }

    if (data.categoria) {
        const categorySelect = document.getElementById('machineCategory');
        if (categorySelect) {
            const optionExists = Array.from(categorySelect.options).some(o => o.value === data.categoria);
            if (optionExists) {
                categorySelect.value = data.categoria;
            }
        }
    }
}

function setMachineManualStatus(message, status) {
    if (!machineManualStatus) return;

    machineManualStatus.textContent = message || '';
    machineManualStatus.classList.remove('status-loading', 'status-success', 'status-error');
    if (status) {
        machineManualStatus.classList.add(`status-${status}`);
    }
}

async function loadMachines() {
    try {
        const response = await fetch(`${API_BASE}/macchinari`);
        const machines = await response.json();

        machinesById = new Map((machines || []).map(m => [m.id, m]));

        if (machines.length === 0) {
            machinesGrid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-state-icon">🔧</div>
                    <p>Nessun macchinario registrato</p>
                </div>
            `;
            return;
        }

        machinesGrid.innerHTML = machines.map(m => `
            <div class="machine-card">
                <div class="machine-card-header">
                    <h4>${m.nome}</h4>
                    <p>${m.categoria || 'Categoria non specificata'}</p>
                </div>
                <div class="machine-card-body">
                    <div class="machine-info">
                        <div class="machine-info-item">
                            <label>Produttore</label>
                            <span>${m.produttore || '-'}</span>
                        </div>
                        <div class="machine-info-item">
                            <label>Modello</label>
                            <span>${m.modello || '-'}</span>
                        </div>
                        <div class="machine-info-item">
                            <label>N. Serie</label>
                            <span>${m.numeroSerie || '-'}</span>
                        </div>
                        <div class="machine-info-item">
                            <label>Anno</label>
                            <span>${m.annoProduzione || '-'}</span>
                        </div>
                    </div>
                    ${m.descrizione ? `<p style="font-size: 0.875rem; color: var(--text-secondary); margin-bottom: 1rem;">${truncate(m.descrizione, 100)}</p>` : ''}
                    <div class="machine-card-actions">
                        <button class="btn btn-sm btn-success" onclick="generateAssessment(${m.id})">
                            📊 Assessment
                        </button>
                        <button class="btn btn-sm btn-secondary" onclick="viewMachineAssessments(${m.id})">
                            📋 Storico
                        </button>
                        <button class="btn btn-sm btn-secondary" type="button" onclick="downloadLatestAssessmentPdf(${m.id})" title="Scarica l'ultimo assessment in PDF">
                            📥 Scarica PDF
                        </button>
                        <button class="btn btn-sm btn-secondary" type="button" onclick="editMachine(${m.id})" title="Modifica macchinario">
                            ✏️ Modifica
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteMachine(${m.id})">
                            🗑️
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading machines:', error);
    }
}

async function generateAssessment(machineId) {
    if (!confirm('Vuoi generare un nuovo assessment per questo macchinario?')) return;

    loadingOverlay.classList.add('active');

    try {
        const response = await fetch(`${API_BASE}/macchinari/${machineId}/assessment`, {
            method: 'POST'
        });

        loadingOverlay.classList.remove('active');

        if (response.ok) {
            const assessment = await response.json();
            showAssessmentModal(assessment);
            loadAssessments();
        } else {
            const error = await response.json();
            alert('Errore: ' + (error.error || 'Impossibile generare l\'assessment'));
        }
    } catch (error) {
        loadingOverlay.classList.remove('active');
        console.error('Error generating assessment:', error);
        alert('Errore durante la generazione dell\'assessment');
    }
}

async function deleteMachine(id) {
    if (!confirm('Sei sicuro di voler eliminare questo macchinario e tutti i suoi assessment?')) return;

    try {
        const response = await fetch(`${API_BASE}/macchinari/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadMachines();
            loadAssessments();
        } else {
            alert('Errore durante l\'eliminazione');
        }
    } catch (error) {
        console.error('Error deleting machine:', error);
    }
}

async function viewMachineAssessments(machineId) {
    try {
        const response = await fetch(`${API_BASE}/macchinari/${machineId}/assessments`);
        const assessments = await response.json();

        if (assessments.length === 0) {
            alert('Nessun assessment disponibile per questo macchinario');
            return;
        }

        // Show the first (most recent) assessment
        showAssessmentModal(assessments[0]);
    } catch (error) {
        console.error('Error loading assessments:', error);
    }
}

// Assessments
async function loadAssessments() {
    try {
        const response = await fetch(`${API_BASE}/assessments`);
        const assessments = await response.json();

        if (assessments.length === 0) {
            assessmentsList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📊</div>
                    <p>Nessun assessment disponibile</p>
                    <p style="font-size: 0.9rem;">Genera un assessment dalla sezione Macchinari</p>
                </div>
            `;
            return;
        }

        assessmentsList.innerHTML = assessments.map(a => `
            <div class="assessment-card">
                <div class="assessment-header">
                    <div>
                        <h4>${a.macchinarioNome}</h4>
                        <p style="color: var(--text-secondary); font-size: 0.875rem;">
                            ${formatDate(a.dataAssessment)}
                        </p>
                    </div>
                    <div class="assessment-score">
                        <span class="risk-badge risk-${a.livelloRischio?.toLowerCase() || 'medio'}">
                            Rischio ${a.livelloRischio || 'N/D'}
                        </span>
                        <div class="score-circle ${getScoreClass(a.punteggioConformita)}">
                            ${a.punteggioConformita || '?'}
                        </div>
                    </div>
                </div>
                <div class="assessment-body">
                    <div class="assessment-section">
                        <h5>Riepilogo Conformità</h5>
                        <p>${truncate(a.riepilogoConformita || 'Non disponibile', 300)}</p>
                    </div>
                    <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
                        <button class="btn btn-sm btn-primary" onclick="showAssessmentDetail(${a.id})">
                            Visualizza Dettagli
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading assessments:', error);
    }
}

async function showAssessmentDetail(id) {
    try {
        const response = await fetch(`${API_BASE}/assessments/${id}`);
        const assessment = await response.json();
        showAssessmentModal(assessment);
    } catch (error) {
        console.error('Error loading assessment:', error);
    }
}

function showAssessmentModal(assessment) {
    document.getElementById('modalTitle').textContent = `Assessment: ${assessment.macchinarioNome}`;

    if (!assessmentDetails) {
        assessmentDetails = document.getElementById('assessmentDetails');
    }

    const assessmentId = Number(assessment?.id);
    currentAssessmentContext = Number.isFinite(assessmentId) ? {
        id: assessmentId,
        macchinarioId: assessment?.macchinarioId ?? null,
        macchinarioNome: assessment?.macchinarioNome ?? '',
        livelloRischio: assessment?.livelloRischio ?? null,
        punteggioConformita: assessment?.punteggioConformita ?? null,
        dataAssessment: assessment?.dataAssessment ?? null
    } : null;

    const nonConformitaItems = toFindingItems(assessment?.nonConformitaRilevate);
    const raccomandazioniItems = toFindingItems(assessment?.raccomandazioni);

    const chatThread = ensureAssessmentChatThread(assessmentId, assessment?.macchinarioNome);
    const chatSuggestions = buildAssessmentChatSuggestions(nonConformitaItems, raccomandazioniItems);

    assessmentDetails.innerHTML = `
        <div class="assessment-layout" data-assessment-id="${Number.isFinite(assessmentId) ? assessmentId : ''}" data-active-panel="results">
            <div class="assessment-layout__tabs" role="tablist" aria-label="Schede risultati/chat">
                <button type="button" class="assessment-tab is-active" data-panel="results" role="tab" aria-selected="true">
                    📊 Risultati
                </button>
                <button type="button" class="assessment-tab" data-panel="chat" role="tab" aria-selected="false">
                    💬 Chat
                </button>
            </div>

            <div class="assessment-layout__grid">
                <div class="assessment-results" data-panel="results" role="tabpanel">
                    <div class="assessment-score" style="justify-content: center; margin-bottom: 2rem;">
                        <div class="score-circle ${getScoreClass(assessment.punteggioConformita)}" style="width: 80px; height: 80px; font-size: 1.5rem;">
                            ${assessment.punteggioConformita || '?'}
                        </div>
                        <div style="text-align: left;">
                            <p style="font-size: 0.875rem; color: var(--text-secondary);">Punteggio Conformità</p>
                            <span class="risk-badge risk-${assessment.livelloRischio?.toLowerCase() || 'medio'}">
                                Rischio ${assessment.livelloRischio || 'N/D'}
                            </span>
                        </div>
                    </div>

                    <div class="assessment-section">
                        <h5>📋 Riepilogo Conformità</h5>
                        <p>${assessment.riepilogoConformita || 'Non disponibile'}</p>
                    </div>

                    <div class="assessment-section">
                        <h5>⚠️ Non Conformità Rilevate</h5>
                        ${renderFindingList(nonConformitaItems, 'nc', 'Nessuna non conformità rilevata')}
                    </div>

                    <div class="assessment-section">
                        <h5>💡 Raccomandazioni</h5>
                        ${renderFindingList(raccomandazioniItems, 'rec', 'Nessuna raccomandazione')}
                    </div>

                    <div class="assessment-section">
                        <h5>📄 Documenti Utilizzati</h5>
                        <p>${assessment.documentiUtilizzati || 'Nessun documento di riferimento'}</p>
                    </div>

                    <div style="margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--border-color); color: var(--text-secondary); font-size: 0.875rem;">
                        <p>Data Assessment: ${formatDate(assessment.dataAssessment)}</p>
                        <p>Versione: ${assessment.versione || '1.0'}</p>
                    </div>
                </div>

                <aside class="assessment-chat" data-panel="chat" role="tabpanel" aria-label="Chat assistente sui risultati">
                    <div class="chat-header">
                        <div class="chat-header__title">
                            <span class="badge badge-info">AI</span>
                            <div>
                                <h3>Chat sui risultati</h3>
                                <p class="chat-header__subtitle">${escapeHtml(assessment?.macchinarioNome || 'Macchinario')}</p>
                            </div>
                        </div>
                        <button type="button" class="btn btn-sm btn-secondary chat-reset-btn" title="Nuova chat" aria-label="Nuova chat">
                            🔄
                        </button>
                    </div>

                    <div class="chat-suggestions" aria-label="Domande rapide">
                        ${renderAssessmentChatSuggestionsHtml(chatSuggestions)}
                    </div>

                    <p class="chat-hint">
                        Suggerimento: usa <strong>💬 Chat</strong> accanto alle non conformità per precompilare una domanda mirata.
                    </p>

                    <div class="chat-messages" data-chat-messages role="log" aria-live="polite" aria-relevant="additions">
                        ${renderAssessmentChatMessagesHtml(chatThread?.messages || [])}
                    </div>

                    <form class="chat-composer" autocomplete="off">
                        <label class="sr-only" for="assessmentChatInput">Messaggio</label>
                        <textarea id="assessmentChatInput"
                                  class="chat-input"
                                  data-chat-input="assessment"
                                  rows="1"
                                  placeholder="Fai una domanda su difetti e raccomandazioni…"></textarea>
                        <button type="submit" class="btn btn-primary chat-send-btn">Invia</button>
                    </form>
                </aside>
            </div>
        </div>
    `;

    assessmentModal.classList.add('active');

    requestAnimationFrame(() => {
        const layout = assessmentDetails?.querySelector?.('.assessment-layout');
        if (!layout || !Number.isFinite(assessmentId)) return;
        renderAssessmentChatIntoLayout(layout, assessmentId);
    });
}

function closeModal() {
    assessmentModal.classList.remove('active');
    currentAssessmentContext = null;
}

function setAssessmentActivePanel(layout, panel) {
    if (!layout || !panel) return;
    layout.setAttribute('data-active-panel', panel);

    const tabs = layout.querySelectorAll('.assessment-tab');
    tabs.forEach((tab) => {
        const isActive = tab.getAttribute('data-panel') === panel;
        tab.classList.toggle('is-active', isActive);
        tab.setAttribute('aria-selected', String(isActive));
    });

    if (panel === 'chat') {
        const input = layout.querySelector('.chat-input');
        if (input) {
            input.focus();
            input.setSelectionRange?.(input.value.length, input.value.length);
            autoResizeChatInput(input);
        }

        const messages = layout.querySelector('[data-chat-messages]');
        if (messages) {
            requestAnimationFrame(() => {
                try {
                    messages.scrollTop = messages.scrollHeight;
                } catch (_) {
                    // ignore
                }
            });
        }
    }
}

function prefillAssessmentChatInput(layout, question) {
    const input = layout?.querySelector?.('.chat-input');
    if (!input) return;

    input.value = question;
    autoResizeChatInput(input);
    input.focus();
    input.setSelectionRange?.(input.value.length, input.value.length);
}

function autoResizeChatInput(textarea) {
    if (!textarea) return;
    textarea.style.height = 'auto';
    const max = 140;
    textarea.style.height = `${Math.min(textarea.scrollHeight, max)}px`;
}

function createChatMessageId() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `m_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function ensureAssessmentChatThread(assessmentId, macchinarioNome) {
    if (!Number.isFinite(assessmentId)) return null;

    let thread = assessmentChatThreads.get(assessmentId);
    if (thread) return thread;

    thread = {
        assessmentId,
        macchinarioNome: macchinarioNome || '',
        messages: [{
            id: createChatMessageId(),
            role: 'assistant',
            text: [
                'Ciao! Sono qui per aiutarti a capire i difetti trovati e come risolverli.',
                'Fammi una domanda sulle non conformità o sulle raccomandazioni visibili in questa pagina.'
            ].join('\n'),
            ts: Date.now()
        }]
    };

    assessmentChatThreads.set(assessmentId, thread);
    return thread;
}

function resetAssessmentChatThread(assessmentId) {
    const machineName = assessmentChatThreads.get(assessmentId)?.macchinarioNome || currentAssessmentContext?.macchinarioNome || '';
    assessmentChatThreads.delete(assessmentId);
    ensureAssessmentChatThread(assessmentId, machineName);
}

function addAssessmentChatMessage(assessmentId, role, text, options = {}) {
    const thread = ensureAssessmentChatThread(assessmentId, currentAssessmentContext?.macchinarioNome || '');
    if (!thread) return null;

    const message = {
        id: createChatMessageId(),
        role,
        text,
        ts: Date.now(),
        pending: Boolean(options.pending)
    };
    thread.messages.push(message);
    return message.id;
}

function updateAssessmentChatMessage(assessmentId, messageId, patch) {
    const thread = assessmentChatThreads.get(assessmentId);
    if (!thread) return;
    const msg = thread.messages.find(m => m.id === messageId);
    if (!msg) return;
    Object.assign(msg, patch);
}

function buildAssessmentChatHistory(assessmentId, maxMessages = 10) {
    const thread = assessmentChatThreads.get(assessmentId);
    const messages = Array.isArray(thread?.messages) ? thread.messages : [];

    return messages
        .filter(m => !m?.pending)
        .filter(m => m?.role === 'user' || m?.role === 'assistant')
        .filter(m => (m?.text || '').trim().length > 0)
        .slice(-maxMessages)
        .map(m => ({ role: m.role, content: m.text }));
}

async function submitAssessmentChat(form) {
    const layout = form.closest('.assessment-layout');
    const assessmentId = Number(layout?.getAttribute('data-assessment-id'));
    if (!layout || !Number.isFinite(assessmentId)) return;

    const input = form.querySelector('.chat-input');
    const text = input?.value?.trim?.() || '';
    if (!text) return;

    ensureAssessmentChatThread(assessmentId, currentAssessmentContext?.macchinarioNome || '');
    const history = buildAssessmentChatHistory(assessmentId, 10);

    input.value = '';
    autoResizeChatInput(input);

    addAssessmentChatMessage(assessmentId, 'user', text);
    const pendingId = addAssessmentChatMessage(assessmentId, 'assistant', 'Sto elaborando…', { pending: true });

    renderAssessmentChatIntoLayout(layout, assessmentId);

    const sendBtn = form.querySelector('.chat-send-btn');
    if (sendBtn) sendBtn.disabled = true;
    if (input) input.disabled = true;

    try {
        const response = await fetch(`${API_BASE}/assessments/${assessmentId}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ question: text, history })
        });

        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload?.error || 'Errore durante la chat');
        }

        const answer = (payload?.answer || '').toString().trim();
        updateAssessmentChatMessage(assessmentId, pendingId, {
            pending: false,
            text: answer || 'Risposta non disponibile.'
        });
    } catch (error) {
        updateAssessmentChatMessage(assessmentId, pendingId, {
            pending: false,
            text: `✗ ${error?.message || 'Errore durante la chat'}`
        });
    } finally {
        if (input) {
            input.disabled = false;
            input.focus();
        }
        if (sendBtn) sendBtn.disabled = false;
        renderAssessmentChatIntoLayout(layout, assessmentId);
    }
}

function renderAssessmentChatIntoLayout(layout, assessmentId) {
    const messagesEl = layout?.querySelector?.('[data-chat-messages]');
    if (!messagesEl) return;

    const thread = ensureAssessmentChatThread(assessmentId, currentAssessmentContext?.macchinarioNome || '');
    messagesEl.innerHTML = renderAssessmentChatMessagesHtml(thread?.messages || []);

    requestAnimationFrame(() => {
        try {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        } catch (_) {
            // ignore
        }
    });
}

function renderAssessmentChatMessagesHtml(messages) {
    const items = Array.isArray(messages) ? messages : [];
    return items.map(renderAssessmentChatMessageHtml).join('');
}

function renderAssessmentChatMessageHtml(message) {
    const role = message?.role === 'user' ? 'user' : 'assistant';
    const bubbleClass = role === 'user' ? 'chat-message--user' : 'chat-message--assistant';
    const pending = message?.pending ? ' <span class="chat-pending">•</span>' : '';
    const text = message?.text || '';
    const content = escapeHtml(text).replaceAll('\n', '<br>');
    const time = message?.ts ? new Date(message.ts).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }) : '';
    return `
        <div class="chat-message ${bubbleClass}">
            <div class="chat-bubble">${content}${pending}</div>
            <div class="chat-meta">${escapeHtml(time)}</div>
        </div>
    `;
}

function buildAssessmentChatSuggestions(nonConformitaItems, raccomandazioniItems) {
    const suggestions = [];
    const nonConformita = Array.isArray(nonConformitaItems) ? nonConformitaItems : [];
    const raccomandazioni = Array.isArray(raccomandazioniItems) ? raccomandazioniItems : [];

    const topFinding = nonConformita.length > 0 ? normalizeFinding(nonConformita[0]).text : '';
    if (topFinding) {
        suggestions.push({
            label: 'Spiega la principale non conformità',
            question: buildQuestionForFinding(topFinding)
        });
    }

    suggestions.push({
        label: 'Priorità interventi',
        question: 'Quali sono le priorità di intervento e i rischi principali in base a questo assessment?'
    });

    if (raccomandazioni.length > 0) {
        suggestions.push({
            label: 'Piano di azione',
            question: 'Puoi propormi un piano di azione step-by-step per risolvere le non conformità e applicare le raccomandazioni?'
        });
    }

    if (nonConformita.length > 1) {
        const secondFinding = normalizeFinding(nonConformita[1]).text;
        if (secondFinding) {
            suggestions.push({
                label: 'Spiega un\'altra non conformità',
                question: buildQuestionForFinding(secondFinding)
            });
        }
    }

    return suggestions.slice(0, 4);
}

function renderAssessmentChatSuggestionsHtml(suggestions) {
    const items = Array.isArray(suggestions) ? suggestions : [];
    if (items.length === 0) return '';
    return items.map(s => {
        const label = truncate(s.label || 'Domanda rapida', 42);
        const question = s.question || '';
        return `
            <button type="button" class="chat-suggestion" data-question="${escapeHtml(question)}" title="${escapeHtml(question)}">
                ${escapeHtml(label)}
            </button>
        `;
    }).join('');
}

// Utility functions
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('it-IT', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function truncate(str, length) {
    if (!str) return '';
    return str.length > length ? str.substring(0, length) + '...' : str;
}

function getScoreClass(score) {
    if (score >= 70) return 'score-high';
    if (score >= 40) return 'score-medium';
    return 'score-low';
}

function getFilenameFromContentDisposition(headerValue) {
    if (!headerValue) return null;

    const filenameStarMatch = headerValue.match(/filename\*=UTF-8''([^;]+)/i);
    if (filenameStarMatch?.[1]) {
        try {
            return decodeURIComponent(filenameStarMatch[1]);
        } catch (_) {
            return filenameStarMatch[1];
        }
    }

    const filenameMatch = headerValue.match(/filename=\"?([^\";]+)\"?/i);
    if (filenameMatch?.[1]) {
        return filenameMatch[1];
    }

    return null;
}

function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function toFindingItems(raw) {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;

    if (typeof raw !== 'string') return [raw];

    const text = raw.trim();
    if (!text) return [];

    try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed)) return parsed;
    } catch (_) {
        // ignore
    }

    return text
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(Boolean)
        .map(line => line
            .replace(/^[-*•]\s+/, '')
            .replace(/^\d+[.)]\s+/, '')
            .trim())
        .filter(Boolean);
}

function normalizeFinding(finding) {
    if (typeof finding === 'string') {
        return { text: finding, sources: [] };
    }

    if (finding && typeof finding === 'object') {
        const rawText = finding.testo || finding.text || finding.descrizione || finding.messaggio || '';
        const text = rawText === null || rawText === undefined ? '' : String(rawText);
        const sources = finding.fonti || finding.sources || finding.fonte || [];
        const sourcesArray = Array.isArray(sources) ? sources : [sources];
        return { text, sources: sourcesArray.filter(Boolean) };
    }

    return { text: String(finding ?? ''), sources: [] };
}

function normalizeConfidencePercent(raw) {
    const num = Number(raw);
    if (!Number.isFinite(num)) return null;

    if (num > 1) {
        return Math.max(0, Math.min(100, num));
    }

    return Math.max(0, Math.min(1, num)) * 100;
}

function getConfidenceClass(percent) {
    if (percent === null) return 'confidence-unknown';
    if (percent >= 80) return 'confidence-high';
    if (percent >= 50) return 'confidence-medium';
    return 'confidence-low';
}

function renderConfidenceBadge(confidence) {
    const percent = normalizeConfidencePercent(confidence);
    const label = percent === null ? 'Pertinenza: N/D' : `Pertinenza: ${Math.round(percent)}%`;
    return `<span class="confidence-badge ${getConfidenceClass(percent)}">🎯 ${escapeHtml(label)}</span>`;
}

function renderSourceCard(source, index) {
    if (!source || typeof source !== 'object') {
        return `
            <div class="source-card">
                <div class="source-panel-header">
                    <div class="source-title">📎 Fonte ${index + 1}</div>
                    <div class="source-meta">${renderConfidenceBadge(null)}</div>
                </div>
                <p class="source-ref"><strong>Riferimento:</strong> ${escapeHtml(source || '—')}</p>
                <div class="chunk-box"><span class="source-placeholder">— Testo chunk non disponibile</span></div>
            </div>
        `;
    }

    const reference = source.riferimento || source.reference || source.citazione || source.citation || source.ref || source.documentReference || source.documento || '—';
    const chunk = source.chunk || source.estratto || source.excerpt || source.testo || source.text || '—';
    const confidence = source.confidence ?? source.score ?? source.similarity ?? source.pertinenza ?? null;

    return `
        <div class="source-card">
            <div class="source-panel-header">
                <div class="source-title">📎 Fonte ${index + 1}</div>
                <div class="source-meta">${renderConfidenceBadge(confidence)}</div>
            </div>
            <p class="source-ref"><strong>Riferimento:</strong> ${escapeHtml(reference)}</p>
            <div class="chunk-box">${escapeHtml(chunk)}</div>
        </div>
    `;
}

function renderSourcesPanel(sources) {
    const normalized = Array.isArray(sources) ? sources.filter(Boolean) : [];

    if (normalized.length === 0) {
        return `
            <div class="source-panel-header">
                <div class="source-title">📎 Fonte normativa</div>
                <div class="source-meta">${renderConfidenceBadge(null)}</div>
            </div>
            <p class="source-ref"><strong>Riferimento:</strong> <span class="source-placeholder">— (es: Regolamento UE 2023/1230, Art. 12, c. 3)</span></p>
            <div class="chunk-box"><span class="source-placeholder">— Qui comparirà il testo esatto del regolamento (chunk recuperato dal RAG)</span></div>
        `;
    }

    return normalized.map((s, idx) => renderSourceCard(s, idx)).join('');
}

function renderFindingList(findings, groupId, emptyMessage = 'Nessuna segnalazione') {
    const items = Array.isArray(findings) ? findings : [];

    if (items.length === 0) {
        return `<p>${escapeHtml(emptyMessage)}</p>`;
    }

    return `
        <ul class="finding-list">
            ${items.map((item, index) => {
                const normalized = normalizeFinding(item);
                const panelId = `source-${groupId}-${index}`;
                const findingText = normalized.text?.trim() ? normalized.text : `Segnalazione ${index + 1}`;
                const sourceLabel = normalized.sources.length > 0 ? `📎 Fonte (${normalized.sources.length})` : '📎 Fonte';
                const askQuestion = buildQuestionForFinding(findingText);
                return `
                    <li class="finding-item">
                        <div class="finding-row">
                            <div class="finding-text">${escapeHtml(findingText)}</div>
                            <div class="finding-actions">
                                <button type="button"
                                        class="btn btn-sm btn-secondary ask-assistant-btn"
                                        title="Chiedi all'assistente su questa segnalazione"
                                        data-question="${escapeHtml(askQuestion)}">
                                    💬 Chat
                                </button>
                                <button type="button"
                                        class="btn btn-sm btn-secondary source-btn"
                                        title="Mostra fonte normativa"
                                        aria-expanded="false"
                                        aria-controls="${escapeHtml(panelId)}">
                                    ${escapeHtml(sourceLabel)}
                                </button>
                            </div>
                        </div>
                        <div id="${escapeHtml(panelId)}" class="source-panel" hidden>
                            ${renderSourcesPanel(normalized.sources)}
                        </div>
                    </li>
                `;
            }).join('')}
        </ul>
    `;
}

function buildQuestionForFinding(findingText) {
    const text = String(findingText || '').trim();
    if (!text) return 'Puoi spiegarmi meglio questa segnalazione e come risolverla?';
    return `Puoi spiegarmi meglio questa segnalazione e come risolverla? "${text}"`;
}
