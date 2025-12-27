let selectedFiles = [];
let activeImports = new Map();
let currentUserName = 'user';
let currentIsAdmin = false;

document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('file-input');
    const selectFilesBtn = document.getElementById('select-files-btn');
    const uploadBtn = document.getElementById('upload-btn');
    const selectedFilesDiv = document.getElementById('selected-files');

    selectFilesBtn.addEventListener('click', () => {
        fileInput.click();
    });

    fileInput.addEventListener('change', (e) => {
        const newFiles = Array.from(e.target.files);

        for (const file of newFiles) {
            if (!selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
                if (selectedFiles.length >= 5) {
                    alert('Maximum 5 files');
                    break;
                }
                if (file.name.toLowerCase().endsWith('.csv') || file.name.toLowerCase().endsWith('.json')) {
                    selectedFiles.push(file);
                } else {
                    alert('File ' + file.name + ' is not CSV or JSON');
                }
            }
        }

        fileInput.value = '';
        updateSelectedFilesDisplay();
        uploadBtn.disabled = selectedFiles.length === 0;
    });

    uploadBtn.addEventListener('click', async () => {
        if (selectedFiles.length === 0) return;

        const userName = prompt('Enter your user name:', 'user') || 'user';
        const isAdmin = confirm('Are you an admin?');

        currentUserName = userName;
        currentIsAdmin = isAdmin;

        if (selectedFiles.length === 1) {
            await uploadSingleFile(selectedFiles[0], userName, isAdmin);
        } else {
            await uploadMultipleFiles(selectedFiles, userName, isAdmin);
        }

        selectedFiles = [];
        fileInput.value = '';
        updateSelectedFilesDisplay();
        uploadBtn.disabled = true;
    });

    function updateSelectedFilesDisplay() {
        if (selectedFiles.length === 0) {
            selectedFilesDiv.innerHTML = '<p>No files selected</p>';
            return;
        }

        let html = '<ul style="list-style: none; padding: 0;">';
        selectedFiles.forEach((file, index) => {
            html += `<li style="display: flex; justify-content: space-between; align-items: center; padding: 8px; margin: 5px 0; background: #2a2a2a; border-radius: 5px;">
                <span>${file.name} (${(file.size / 1024).toFixed(2)} KB)</span>
                <button class="btn btn-danger btn-small" onclick="removeFile(${index})" style="margin-left: 10px; padding: 4px 8px; font-size: 12px;">Remove</button>
            </li>`;
        });
        html += '</ul>';
        selectedFilesDiv.innerHTML = html;
    }

    window.removeFile = function (index) {
        selectedFiles.splice(index, 1);
        updateSelectedFilesDisplay();
        uploadBtn.disabled = selectedFiles.length === 0;
    };
});

async function uploadSingleFile(file, userName, isAdmin) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userName', userName);
    formData.append('isAdmin', isAdmin);

    try {
        const response = await fetch('/api/import/upload', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Upload failed');
        }

        const result = await response.json();

        const importId = result.importId;
        const fileName = result.fileName || file.name;

        if (importId) {
            createProgressElement(importId, fileName);
            startPolling(importId);
        }
    } catch (error) {
        console.error('Upload error:', error);
        alert('Upload failed: ' + error.message);
    }
}

async function uploadMultipleFiles(files, userName, isAdmin) {
    const formData = new FormData();
    for (const file of files) {
        formData.append('files', file);
    }
    formData.append('userName', userName);
    formData.append('isAdmin', isAdmin);

    try {
        const response = await fetch('/api/import/upload-multiple', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Upload failed');
        }

        const result = await response.json();
        const imports = result.imports || [];

        for (const importInfo of imports) {
            const importId = importInfo.importId;
            const fileName = importInfo.fileName;

            if (importId) {
                createProgressElement(importId, fileName);
                startPolling(importId);
            }
        }

    } catch (error) {
        console.error('Upload error:', error);
        alert('Upload failed: ' + error.message);
    }
}

function createProgressElement(importId, fileName) {
    const container = document.getElementById('import-progress-container');
    if (document.getElementById(`progress-${importId}`)) {
        return;
    }

    const progressDiv = document.createElement('div');
    progressDiv.id = `progress-${importId}`;
    progressDiv.className = 'import-progress-item';
    progressDiv.innerHTML = `
        <div class="progress-header">
            <span class="progress-file-name">${fileName}</span>
            <span class="progress-status" id="status-${importId}">Starting...</span>
        </div>
        <div class="progress-bar-container">
            <div class="progress-bar" id="bar-${importId}" style="width: 0%">0%</div>
        </div>
        <div class="progress-details" id="details-${importId}">
            <span>0 / 0 records</span>
            <span>Successful: 0</span>
            <span>Failed: 0</span>
        </div>
    `;
    container.appendChild(progressDiv);
}

function updateProgress(progress) {
    const importId = progress.importId;
    let progressDiv = document.getElementById(`progress-${importId}`);

    if (!progressDiv) {
        createProgressElement(importId, progress.fileName || 'Unknown');
        progressDiv = document.getElementById(`progress-${importId}`);
    }

    if (!progressDiv) return;

    const bar = document.getElementById(`bar-${importId}`);
    const status = document.getElementById(`status-${importId}`);
    const details = document.getElementById(`details-${importId}`);

    if (bar) {
        const progressValue = progress.currentProgress || 0;
        bar.style.width = progressValue + '%';
        bar.textContent = progressValue + '%';
    }

    if (status) {
        const statusText = progress.status || 'IN_PROGRESS';
        status.textContent = statusText;
        status.className = 'progress-status status-' + statusText.toLowerCase().replace('_', '-');
    }

    if (details) {
        details.innerHTML = `
            <span>${progress.processedRecords || 0} / ${progress.totalRecords || 0} records</span>
            <span>Successful: ${progress.successfulRecords || 0}</span>
            <span>Failed: ${progress.failedRecords || 0}</span>
        `;
    }

    if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
        stopPolling(importId);

        if (window.currentView === 'table' && typeof window.loadTable === 'function') {
            window.loadTable();
        }

        if (progress.errorMessage) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'progress-error';
            errorDiv.textContent = 'Error: ' + progress.errorMessage;
            progressDiv.appendChild(errorDiv);
        }

        setTimeout(() => {
            if (progressDiv && progressDiv.parentNode) {
                progressDiv.parentNode.removeChild(progressDiv);
            }
            activeImports.delete(importId);
        }, 10000);
    }
}

const pollingIntervals = new Map();

function startPolling(importId) {
    if (pollingIntervals.has(importId)) {
        return;
    }

    const interval = setInterval(async () => {
        try {
            const response = await fetch(`/api/import/progress/${importId}`);
            if (response.ok) {
                const progress = await response.json();
                updateProgress(progress);

                if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
                    stopPolling(importId);
                }
            } else if (response.status === 404) {
                stopPolling(importId);
            }
        } catch (error) {
            console.error('Polling error:', error);
        }
    }, 500);

    pollingIntervals.set(importId, interval);
    activeImports.set(importId, {interval: interval});
}

function stopPolling(importId) {
    const importInfo = activeImports.get(importId);
    if (importInfo && importInfo.interval) {
        clearInterval(importInfo.interval);
    }
    pollingIntervals.delete(importId);
    activeImports.delete(importId);
}

window.updateImportProgress = updateProgress;

async function loadImportHistory() {
    const userName = document.getElementById('history-user-name').value || 'user';
    const isAdmin = document.getElementById('history-is-admin').checked;

    const queryUserName = isAdmin ? 'admin' : userName;

    try {
        const response = await fetch(`/api/import/history?userName=${encodeURIComponent(queryUserName)}`);

        if (!response.ok) {
            throw new Error('Failed to load history');
        }

        const history = await response.json();
        displayHistory(history);
    } catch (error) {
        console.error('Load history error:', error);
        alert('Failed to load history: ' + error.message);
    }
}

function displayHistory(history) {
    const tbody = document.getElementById('history-table-body');
    tbody.innerHTML = '';

    if (history.length === 0) {
        tbody.innerHTML = '<tr><td colspan="12">No import history found</td></tr>';
        return;
    }

    history.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.id}</td>
            <td>${item.fileName}</td>
            <td>${item.fileType}</td>
            <td class="status-${item.status.toLowerCase()}">${item.status}</td>
            <td>${item.userName} ${item.isAdmin ? '(Admin)' : ''}</td>
            <td>${formatDate(item.startTime)}</td>
            <td>${item.endTime ? formatDate(item.endTime) : '-'}</td>
            <td>${item.totalRecords || 0}</td>
            <td>${item.successfulRecords || 0}</td>
            <td>${item.failedRecords || 0}</td>
            <td>${item.currentProgress || 0}%</td>
            <td>${item.errorMessage || '-'}</td>
        `;
        tbody.appendChild(row);
    });
}

function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString();
}

async function clearImportHistory() {
    const userName = document.getElementById('history-user-name').value || 'user';
    const isAdmin = document.getElementById('history-is-admin').checked;

    if (!isAdmin) {
        alert('Only admin can clear import history');
        return;
    }

    if (!confirm('Are you sure you want to clear all import history? This action cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch(`/api/import/history?userName=${encodeURIComponent(userName)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to clear history');
        }

        const result = await response.json();
        alert('Import history cleared successfully');
        loadImportHistory();
    } catch (error) {
        console.error('Clear history error:', error);
        alert('Failed to clear history: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const loadHistoryBtn = document.getElementById('load-history-btn');
    if (loadHistoryBtn) {
        loadHistoryBtn.addEventListener('click', loadImportHistory);
    }

    const clearHistoryBtn = document.getElementById('clear-history-btn');
    if (clearHistoryBtn) {
        clearHistoryBtn.addEventListener('click', clearImportHistory);
    }
});