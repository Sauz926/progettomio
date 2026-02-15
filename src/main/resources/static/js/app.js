// API Base URL
const API_BASE = '/api';

// DOM Elements
let documentsTable, machinesGrid, assessmentsList;
let dropZone, fileInput, uploadProgress, progressFill, uploadStatus;
let loadingOverlay, assessmentModal;
let machineManualUploadBtn, machineManualFileInput, machineManualStatus;

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    initElements();
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
    machineManualUploadBtn = document.getElementById('machineManualUploadBtn');
    machineManualFileInput = document.getElementById('machineManualFileInput');
    machineManualStatus = document.getElementById('machineManualStatus');
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

// Machines
function initMachineForm() {
    const form = document.getElementById('machineForm');
    form.addEventListener('submit', async (e) => {
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

        try {
            const response = await fetch(`${API_BASE}/macchinari`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(machine)
            });

            if (response.ok) {
                form.reset();
                loadMachines();
                alert('Macchinario aggiunto con successo!');
            } else {
                alert('Errore durante il salvataggio');
            }
        } catch (error) {
            console.error('Error saving machine:', error);
        }
    });
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

    document.getElementById('assessmentDetails').innerHTML = `
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
            <p>${assessment.nonConformitaRilevate || 'Nessuna non conformità rilevata'}</p>
        </div>
        
        <div class="assessment-section">
            <h5>💡 Raccomandazioni</h5>
            <p>${assessment.raccomandazioni || 'Nessuna raccomandazione'}</p>
        </div>
        
        <div class="assessment-section">
            <h5>📄 Documenti Utilizzati</h5>
            <p>${assessment.documentiUtilizzati || 'Nessun documento di riferimento'}</p>
        </div>
        
        <div style="margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--border-color); color: var(--text-secondary); font-size: 0.875rem;">
            <p>Data Assessment: ${formatDate(assessment.dataAssessment)}</p>
            <p>Versione: ${assessment.versione || '1.0'}</p>
        </div>
    `;

    assessmentModal.classList.add('active');
}

function closeModal() {
    assessmentModal.classList.remove('active');
}

// Close modal on outside click
assessmentModal?.addEventListener('click', (e) => {
    if (e.target === assessmentModal) {
        closeModal();
    }
});

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
