var currentPage = parseInt(localStorage.getItem('currentPage')) || 0;
var pageSize = parseInt(localStorage.getItem('pageSize')) || 10;
var currentSort = localStorage.getItem('currentSort') || '';
var currentSortDir = localStorage.getItem('currentSortDir') || 'asc';
var currentFilterBy = localStorage.getItem('currentFilterBy') || '';
var currentFilterValue = localStorage.getItem('currentFilterValue') || '';

async function loadTable() {
    try {
        const savedPage = parseInt(localStorage.getItem('currentPage')) || 0;
        const savedPageSize = parseInt(localStorage.getItem('pageSize')) || 10;
        const savedFilterBy = localStorage.getItem('currentFilterBy') || '';
        const savedFilterValue = localStorage.getItem('currentFilterValue') || '';
        if (savedPage !== currentPage) currentPage = savedPage;
        if (savedPageSize !== pageSize) pageSize = savedPageSize;
        if (savedFilterBy !== currentFilterBy) currentFilterBy = savedFilterBy;
        if (savedFilterValue !== currentFilterValue) currentFilterValue = savedFilterValue;

        const data = await apiService.getAllHumanBeings(currentPage, pageSize, currentSort, currentSortDir, currentFilterBy, currentFilterValue);

        const tbody = document.getElementById('table-body');
        tbody.innerHTML = '';

        if (data.content && data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="13" style="text-align: center; color: white;">No data</td></tr>';
            return;
        }

        const pageSizeSelect = document.getElementById('page-size-select');
        if (pageSizeSelect) {
            pageSizeSelect.value = pageSize;
        }

        data.content.forEach(hb => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${hb.id}</td>
                <td>${hb.name}</td>
                <td>${hb.coordinates.x}</td>
                <td>${hb.coordinates.y}</td>
                <td>${new Date(hb.creationDate).toLocaleString('en-US')}</td>
                <td>${hb.realHero ? 'Yes' : 'No'}</td>
                <td>${hb.hasToothpick === null ? 'null' : (hb.hasToothpick ? 'Yes' : 'No')}</td>
                <td>${hb.car.name || 'null'}</td>
                <td>${hb.mood}</td>
                <td>${hb.impactSpeed}</td>
                <td>${hb.minutesOfWaiting}</td>
                <td>${hb.weaponType || 'null'}</td>
                <td>
                    <button class="btn btn-primary btn-small" onclick="viewHumanBeing(${hb.id})">View</button>
                    <button class="btn btn-secondary btn-small" onclick="editHumanBeing(${hb.id})">Edit</button>
                    <button class="btn btn-danger btn-small" onclick="deleteHumanBeing(${hb.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });

        const pageInfo = document.getElementById('page-info');
        const totalPages = data.totalPages || 1;
        pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;

        document.getElementById('prev-page').disabled = currentPage === 0;
        document.getElementById('next-page').disabled = currentPage >= totalPages - 1;

        updateSortIndicators();

    } catch (error) {
        console.error('Error loading table:', error);
    }
}

function updateSortIndicators() {
    document.querySelectorAll('th').forEach(th => {
        th.classList.remove('sort-asc', 'sort-desc');
    });

    if (currentSort) {
        const sortTh = document.querySelector(`th[data-sort="${currentSort}"]`);
        if (sortTh) {
            sortTh.classList.add(currentSortDir === 'asc' ? 'sort-asc' : 'sort-desc');
        }
    }
}

function handleSort(column) {
    if (currentSort === column) {
        currentSortDir = currentSortDir === 'asc' ? 'desc' : 'asc';
    } else {
        currentSort = column;
        currentSortDir = 'asc';
    }
    currentPage = 0;
    localStorage.setItem('currentSort', currentSort);
    localStorage.setItem('currentSortDir', currentSortDir);
    localStorage.setItem('currentPage', currentPage);
    loadTable();
}

function applyFilter() {
    const filterColumn = document.getElementById('filter-column-select').value;
    const filterValue = document.getElementById('filter-input').value.trim();

    if (!filterColumn || !filterValue) {
        clearFilter();
        return;
    }

    currentFilterBy = filterColumn;
    currentFilterValue = filterValue;
    localStorage.setItem('currentFilterBy', currentFilterBy);
    localStorage.setItem('currentFilterValue', currentFilterValue);
    currentPage = 0;
    localStorage.setItem('currentPage', '0');
    loadTable();
}

function clearFilter() {
    document.getElementById('filter-column-select').value = '';
    document.getElementById('filter-input').value = '';
    currentFilterBy = '';
    currentFilterValue = '';
    localStorage.removeItem('currentFilterBy');
    localStorage.removeItem('currentFilterValue');
    currentPage = 0;
    localStorage.setItem('currentPage', '0');
    loadTable();
}

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('th[data-sort]').forEach(th => {
        th.addEventListener('click', function () {
            const sortColumn = this.getAttribute('data-sort');
            handleSort(sortColumn);
        });
    });

    document.getElementById('apply-filter-btn').addEventListener('click', applyFilter);
    document.getElementById('clear-filter-btn').addEventListener('click', clearFilter);
    document.getElementById('filter-input').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            applyFilter();
        }
    });

    const filterColumnSelect = document.getElementById('filter-column-select');
    const filterInput = document.getElementById('filter-input');
    if (currentFilterBy) {
        filterColumnSelect.value = currentFilterBy;
        filterInput.value = currentFilterValue;
    }

    document.getElementById('prev-page').addEventListener('click', function () {
        if (currentPage > 0) {
            currentPage--;
            localStorage.setItem('currentPage', currentPage);
            loadTable();
        }
    });

    document.getElementById('next-page').addEventListener('click', function () {
        currentPage++;
        localStorage.setItem('currentPage', currentPage);
        loadTable();
    });

    document.getElementById('page-size-select').addEventListener('change', function () {
        pageSize = parseInt(this.value);
        currentPage = 0;
        localStorage.setItem('pageSize', pageSize);
        localStorage.setItem('currentPage', currentPage);
        loadTable();
    });

    const pageSizeSelect = document.getElementById('page-size-select');
    if (pageSizeSelect) {
        pageSizeSelect.value = pageSize;
    }
});

async function viewHumanBeing(id) {
    try {
        const hb = await apiService.getHumanBeingById(id);
        const modal = document.getElementById('view-modal');
        const content = document.getElementById('view-content');

        content.innerHTML = `
            <div><strong>ID:</strong> ${hb.id}</div>
            <div><strong>Name:</strong> ${hb.name}</div>
            <div><strong>Coordinates:</strong> X=${hb.coordinates.x}, Y=${hb.coordinates.y}</div>
            <div><strong>Creation Date:</strong> ${new Date(hb.creationDate).toLocaleString('en-US')}</div>
            <div><strong>Real Hero:</strong> ${hb.realHero ? 'Yes' : 'No'}</div>
            <div><strong>Has Toothpick:</strong> ${hb.hasToothpick === null ? 'null' : (hb.hasToothpick ? 'Yes' : 'No')}</div>
            <div><strong>Car:</strong> ${hb.car.name || 'null'} (ID: ${hb.car.id})</div>
            <div><strong>Mood:</strong> ${hb.mood}</div>
            <div><strong>Impact Speed:</strong> ${hb.impactSpeed}</div>
            <div><strong>Minutes Of Waiting:</strong> ${hb.minutesOfWaiting}</div>
            <div><strong>Weapon Type:</strong> ${hb.weaponType || 'null'}</div>
        `;

        document.getElementById('edit-from-view-btn').onclick = function () {
            modal.style.display = 'none';
            editHumanBeing(id);
        };

        modal.style.display = 'block';
    } catch (error) {
        console.error('Error loading object:', error);
    }
}

async function deleteHumanBeing(id) {
    try {
        const allData = await apiService.getAllHumanBeings(0, 1000, '', '', '', '');
        const allObjects = allData.content || [];
        const availableReplacements = allObjects.filter(hb => hb.id !== id);

        let replacementId = null;
        if (availableReplacements.length > 0) {
            const replacementOptions = availableReplacements.map(hb =>
                `${hb.id}: ${hb.name}`
            ).join('\n');

            const userInput = prompt(
                `Select replacement object ID (or leave empty):\n\nAvailable objects:\n${replacementOptions}`
            );

            if (userInput && userInput.trim()) {
                const selectedId = parseInt(userInput.trim());
                if (availableReplacements.find(hb => hb.id === selectedId)) {
                    replacementId = selectedId;
                }
            }
        }

        await apiService.deleteHumanBeing(id, replacementId);
        loadTable();
    } catch (error) {
        console.error('Error deleting:', error);
    }
}

window.loadTable = loadTable;
window.viewHumanBeing = viewHumanBeing;
window.deleteHumanBeing = deleteHumanBeing;
window.currentPage = currentPage;
window.pageSize = pageSize;