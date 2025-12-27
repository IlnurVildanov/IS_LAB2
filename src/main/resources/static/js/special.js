document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('delete-by-weapon-btn').addEventListener('click', async function () {
        const btn = this;
        if (btn.disabled) return;
        btn.disabled = true;

        const weaponType = document.getElementById('weapon-type-select').value;
        const resultDiv = document.getElementById('delete-by-weapon-result');

        try {
            const result = await apiService.deleteOneByWeaponType(weaponType);

            if (result.message) {
                if (resultDiv) {
                    resultDiv.innerHTML = `<div style="color: white; margin-top: 10px; padding: 10px; background: #1a1a1a; border-radius: 5px;">${result.message}</div>`;
                }
            } else if (result.id) {
                if (resultDiv) {
                    resultDiv.innerHTML = `<div style="color: white; margin-top: 10px; padding: 10px; background: #1a1a1a; border-radius: 5px;">
                        <strong>Deleted object:</strong><br>
                        ID: ${result.id}<br>
                        Name: ${result.name}<br>
                        Weapon Type: ${result.weaponType || 'null'}
                    </div>`;
                }
            }

            if (window.currentView === 'table') {
                loadTable();
            }
        } catch (error) {
            console.error('Error:', error);
            if (resultDiv) {
                const errorMsg = error.error || error.message || 'Failed to delete';
                resultDiv.innerHTML = `<div style="color: #ff0000; margin-top: 10px;">Error: ${errorMsg}</div>`;
            }
        } finally {
            btn.disabled = false;
        }
    });

document.getElementById('avg-speed-btn').addEventListener('click', async function () {
        try {
            const result = await apiService.getAverageImpactSpeed();
            document.getElementById('avg-speed-result').innerHTML =
                `<strong>Average Impact Speed:</strong> ${result.averageImpactSpeed.toFixed(2)}`;
        } catch (error) {
            console.error('Error:', error);
        }
    });

    document.getElementById('search-name-btn').addEventListener('click', async function () {
        const substring = document.getElementById('name-search-input').value;
        const resultsDiv = document.getElementById('search-results');

        if (resultsDiv) {
            resultsDiv.innerHTML = '';
        }

        if (!substring) {
            return;
        }

        try {
            const results = await apiService.findByNameContaining(substring);

            if (results.length === 0) {
                resultsDiv.innerHTML = '<p style="color: white;">No results found</p>';
            } else {
                let html = `<p style="color: white;"><strong>Found objects: ${results.length}</strong></p>`;
                results.forEach(hb => {
                    html += `<div style="margin: 10px 0; padding: 10px; background: #1a1a1a; border-radius: 5px; color: white;">
                        <strong>ID:</strong> ${hb.id}, <strong>Name:</strong> ${hb.name}, 
                        <strong>Impact Speed:</strong> ${hb.impactSpeed}
                    </div>`;
                });
                resultsDiv.innerHTML = html;
            }
        } catch (error) {
            console.error('Error:', error);
            if (resultsDiv) {
                resultsDiv.innerHTML = '<p style="color: #ff0000;">Error searching</p>';
            }
        }
    });

    document.getElementById('set-sadness-btn').addEventListener('click', async function () {
        const resultDiv = document.getElementById('set-sadness-result');

        try {
            const result = await apiService.setAllHeroesMoodToSadness();

            if (resultDiv) {
                resultDiv.innerHTML = `<div style="color: white; margin-top: 10px; padding: 10px; background: #1a1a1a; border-radius: 5px;">${result.message || result.count + ' objects changed to SADNESS'}</div>`;
            }

            if (window.currentView === 'table') {
                loadTable();
            }
        } catch (error) {
            console.error('Error:', error);
            if (resultDiv) {
                resultDiv.innerHTML = `<div style="color: #ff0000; margin-top: 10px;">Error: ${error.message}</div>`;
            }
        }
    });

    document.getElementById('set-lada-btn').addEventListener('click', async function () {
        const resultDiv = document.getElementById('set-lada-result');

        try {
            const result = await apiService.setAllHeroesWithoutCarToRedLadaKalina();

            if (resultDiv) {
                resultDiv.innerHTML = `<div style="color: white; margin-top: 10px; padding: 10px; background: #1a1a1a; border-radius: 5px;">${result.message || result.count + ' heroes moved to Lada Kalina'}</div>`;
            }

            if (window.currentView === 'table') {
                loadTable();
            }
        } catch (error) {
            console.error('Error:', error);
            if (resultDiv) {
                resultDiv.innerHTML = `<div style="color: #ff0000; margin-top: 10px;">Error: ${error.message}</div>`;
            }
        }
    });
});