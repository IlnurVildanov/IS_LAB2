let stompClient = null;
let connected = false;

function connectWebSocket() {
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.debug = function (str) {
            // console.log(str);
        };

        stompClient.connect({}, function (frame) {
            connected = true;
            console.log('WebSocket connected');

            stompClient.subscribe('/topic/updates', function (message) {
                try {
                    const update = JSON.parse(message.body);
                    handleWebSocketUpdate(update);
                } catch (e) {
                    console.error('Error parsing WebSocket message:', e);
                }
            });

            stompClient.subscribe('/topic/import-progress', function (message) {
                try {
                    const update = JSON.parse(message.body);
                    handleWebSocketUpdate(update);
                } catch (e) {
                    console.error('Error parsing import progress message:', e);
                }
            });
        }, function (error) {
            console.error('WebSocket connection error:', error);
            connected = false;
            setTimeout(connectWebSocket, 5000);
        });
    } catch (error) {
        console.error('Error initializing WebSocket:', error);
        connected = false;
    }
}

function handleWebSocketUpdate(update) {
    const {action, data} = update;

    switch (action) {
        case 'created':
        case 'updated':
            if (window.currentView === 'table') {
                loadTable();
            }
            break;
        case 'deleted':
            if (window.currentView === 'table') {
                loadTable();
            }
            break;
        case 'import_progress':
            if (window.updateImportProgress) {
                window.updateImportProgress(data);
            }
            break;
    }
}

function disconnectWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    connected = false;
}

window.addEventListener('beforeunload', function () {
    disconnectWebSocket();
});