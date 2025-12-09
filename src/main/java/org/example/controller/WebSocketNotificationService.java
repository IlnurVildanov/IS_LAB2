package org.example.controller;

import org.example.dto.HumanBeingDTO;
import org.example.dto.ImportProgressDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyAll(String action, Object data) {
        Map<String, Object> message = Map.of(
                "action", action,
                "data", data
        );
        messagingTemplate.convertAndSend("/topic/updates", message);
    }

    public void notifyImportProgress(ImportProgressDTO progress) {
        Map<String, Object> message = Map.of(
                "action", "import_progress",
                "data", progress
        );
        messagingTemplate.convertAndSend("/topic/import-progress", message);
    }
}