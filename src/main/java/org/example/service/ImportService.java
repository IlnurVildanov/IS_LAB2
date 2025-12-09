package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.*;
import org.example.controller.WebSocketNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ImportService {

    @Autowired
    private HumanBeingRepository humanBeingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CoordinatesRepository coordinatesRepository;

    @Autowired
    private ImportHistoryRepository importHistoryRepository;

    @Autowired
    private WebSocketNotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<Long, ImportProgressDTO> progressMap = new ConcurrentHashMap<>();

    public ImportHistory createImportHistory(String fileName, String fileType, String userName, Boolean isAdmin) {
        ImportHistory history = new ImportHistory();
        history.setFileName(fileName);
        history.setFileType(fileType);
        history.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        history.setUserName(userName);
        history.setIsAdmin(isAdmin);
        history.setStartTime(new Date());
        history.setCurrentProgress(0);
        history.setTotalRecords(0);
        history.setSuccessfulRecords(0);
        history.setFailedRecords(0);

        history = importHistoryRepository.save(history);

        ImportProgressDTO dto = new ImportProgressDTO();
        dto.setImportId(history.getId());
        dto.setFileName(history.getFileName());
        dto.setCurrentProgress(0);
        dto.setTotalRecords(0);
        dto.setProcessedRecords(0);
        dto.setSuccessfulRecords(0);
        dto.setFailedRecords(0);
        dto.setStatus("IN_PROGRESS");
        dto.setErrorMessage(null);

        progressMap.put(history.getId(), dto);

        return history;
    }

    public ImportHistory importFileAsync(MultipartFile file, String userName, Boolean isAdmin) {
        String fileName = file.getOriginalFilename();
        String fileType = fileName != null && fileName.toLowerCase().endsWith(".json") ? "JSON" : "CSV";

        ImportHistory history = createImportHistory(fileName != null ? fileName : "unknown", fileType, userName, isAdmin);
        final Long historyId = history.getId();

        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] fileBytes = file.getBytes();
                MultipartFile tempFile = new ByteArrayMultipartFile(
                        fileBytes, file.getName(), fileName, file.getContentType() != null ? file.getContentType() : "application/octet-stream"
                );

                return processImport(tempFile, history);
            } catch (Exception e) {
                updateHistoryError(historyId, e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);

        return history;
    }

    private ImportHistory processImport(MultipartFile file, ImportHistory history) {
        Long historyId = history.getId();
        String fileName = history.getFileName();

        try {
            List<HumanBeingDTO> dtos;
            if ("JSON".equals(history.getFileType())) {
                dtos = parseJSON(file);
            } else {
                dtos = parseCSV(file);
            }

            history.setTotalRecords(dtos.size());
            importHistoryRepository.save(history);

            importRecords(dtos, historyId, fileName);

            history = importHistoryRepository.findById(historyId).orElse(history);
            history.setStatus(ImportHistory.ImportStatus.COMPLETED);
            history.setCurrentProgress(100);
            history.setEndTime(new Date());
            importHistoryRepository.save(history);

            sendProgress(historyId, fileName, dtos.size(), dtos.size(),
                    history.getSuccessfulRecords(), history.getFailedRecords(), "COMPLETED");

            return history;
        } catch (Exception e) {
            updateHistoryError(historyId, e.getMessage());
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void importRecords(List<HumanBeingDTO> dtos, Long historyId, String fileName) {
        int successful = 0;
        int failed = 0;

        for (int i = 0; i < dtos.size(); i++) {
            HumanBeingDTO dto = dtos.get(i);
            try {
                validateDTO(dto);
                createHumanBeing(dto);
                successful++;

                int progress = (int) ((i + 1) * 100.0 / dtos.size());

                if (progress < 100) {
                    updateProgressAsync(historyId, fileName, i + 1, dtos.size(), successful, failed, progress);
                }

                Thread.sleep(5);
            } catch (Exception e) {
                failed++;
                int progress = (int) ((i + 1) * 100.0 / dtos.size());
                if (progress < 100) {
                    updateProgressAsync(historyId, fileName, i + 1, dtos.size(), successful, failed, progress);
                }
                throw new RuntimeException("Error importing record: " + e.getMessage(), e);
            }
        }

        ImportHistory history = importHistoryRepository.findById(historyId).orElse(null);
        if (history != null) {
            history.setSuccessfulRecords(successful);
            history.setFailedRecords(failed);
            importHistoryRepository.save(history);
        }
    }

    private void validateDTO(HumanBeingDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (dto.getCoordinates() == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }

        if (dto.getCoordinates().getX() == null || dto.getCoordinates().getY() == null) {
            throw new IllegalArgumentException("Coordinates x and y cannot be null");
        }

        if (dto.getCoordinates().getX() > 112) {
            throw new IllegalArgumentException("X coordinate cannot exceed 112");
        }

        if (dto.getCoordinates().getY() <= -926) {
            throw new IllegalArgumentException("Y coordinate must be greater than -926");
        }

        if (dto.getImpactSpeed() == null) {
            throw new IllegalArgumentException("Impact speed cannot be null");
        }

        if (dto.getImpactSpeed() > 345) {
            throw new IllegalArgumentException("Impact speed cannot exceed 345");
        }

        if (dto.getMinutesOfWaiting() == null) {
            throw new IllegalArgumentException("Minutes of waiting cannot be null");
        }

        if (dto.getMood() == null) {
            throw new IllegalArgumentException("Mood cannot be null");
        }

        if (dto.getRealHero() == null) {
            throw new IllegalArgumentException("Real hero cannot be null");
        }

        if (dto.getCar() == null) {
            throw new IllegalArgumentException("Car cannot be null");
        }

        List<HumanBeing> existing = humanBeingRepository.findByNameAndCoordinates(
                dto.getName(), dto.getCoordinates().getX(), dto.getCoordinates().getY());
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("HumanBeing with name '" + dto.getName() +
                    "' and coordinates (" + dto.getCoordinates().getX() + ", " + dto.getCoordinates().getY() + ") already exists");
        }

        if (Boolean.TRUE.equals(dto.getRealHero())) {
            List<HumanBeing> existingHero = humanBeingRepository.findByHeroSpeedAndWaiting(
                    dto.getImpactSpeed(), dto.getMinutesOfWaiting());
            if (!existingHero.isEmpty()) {
                throw new IllegalArgumentException("Hero with impactSpeed " + dto.getImpactSpeed() +
                        " and minutesOfWaiting " + dto.getMinutesOfWaiting() + " already exists");
            }
        }
    }

    private void createHumanBeing(HumanBeingDTO dto) {
        Coordinates coords = new Coordinates();
        coords.setX(dto.getCoordinates().getX());
        coords.setY(dto.getCoordinates().getY());

        Car car = new Car();
        car.setName(dto.getCar().getName());

        HumanBeing entity = new HumanBeing();
        entity.setName(dto.getName());
        entity.setCoordinates(coords);
        entity.setCar(car);
        entity.setRealHero(dto.getRealHero());
        entity.setHasToothpick(dto.getHasToothpick());
        entity.setMood(dto.getMood());
        entity.setImpactSpeed(dto.getImpactSpeed());
        entity.setMinutesOfWaiting(dto.getMinutesOfWaiting());
        entity.setWeaponType(dto.getWeaponType());
        entity.setCreationDate(new Date());

        humanBeingRepository.save(entity);
    }

    private List<HumanBeingDTO> parseJSON(MultipartFile file) throws Exception {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            HumanBeingDTO[] dtos = objectMapper.readValue(content, HumanBeingDTO[].class);
            return Arrays.asList(dtos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    private List<HumanBeingDTO> parseCSV(MultipartFile file) throws Exception {
        List<HumanBeingDTO> dtos = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return dtos;
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), i);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                try {
                    String[] values = parseCSVLine(line);
                    HumanBeingDTO dto = parseCSVRow(values, headerMap);
                    dtos.add(dto);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error parsing CSV line " + lineNum + ": " + e.getMessage(), e);
                }
            }
        }
        return dtos;
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private HumanBeingDTO parseCSVRow(String[] values, Map<String, Integer> headerMap) throws Exception {
        HumanBeingDTO dto = new HumanBeingDTO();

        dto.setName(getCSVValue(values, headerMap, "name"));

        CoordinatesDTO coords = new CoordinatesDTO();
        coords.setX(Integer.parseInt(getCSVValue(values, headerMap, "coordinates.x", "x")));
        coords.setY(Double.parseDouble(getCSVValue(values, headerMap, "coordinates.y", "y")));
        dto.setCoordinates(coords);

        dto.setRealHero(Boolean.parseBoolean(getCSVValue(values, headerMap, "realHero", "false")));

        String hasToothpick = getCSVValue(values, headerMap, "hasToothpick");
        if (hasToothpick != null && !hasToothpick.isEmpty() && !hasToothpick.equalsIgnoreCase("null")) {
            dto.setHasToothpick(Boolean.parseBoolean(hasToothpick));
        }

        CarDTO car = new CarDTO();
        String carName = getCSVValue(values, headerMap, "car.name", "carName");
        car.setName(carName);
        dto.setCar(car);

        String moodStr = getCSVValue(values, headerMap, "mood");
        if (moodStr == null || moodStr.isEmpty()) {
            throw new IllegalArgumentException("Mood cannot be null");
        }
        try {
            dto.setMood(Mood.valueOf(moodStr));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mood value: " + moodStr + ". Valid values: SADNESS, CALM, RAGE");
        }

        dto.setImpactSpeed(Float.parseFloat(getCSVValue(values, headerMap, "impactSpeed")));

        dto.setMinutesOfWaiting(Float.parseFloat(getCSVValue(values, headerMap, "minutesOfWaiting")));

        String weaponType = getCSVValue(values, headerMap, "weaponType");
        if (weaponType != null && !weaponType.isEmpty() && !weaponType.equalsIgnoreCase("null")) {
            try {
                dto.setWeaponType(WeaponType.valueOf(weaponType));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid weaponType value: " + weaponType +
                        ". Valid values: HAMMER, RIFLE, MACHINE_GUN, BAT");
            }
        }

        return dto;
    }

    private String getCSVValue(String[] values, Map<String, Integer> headerMap, String key) {
        return getCSVValue(values, headerMap, key, null);
    }

    private String getCSVValue(String[] values, Map<String, Integer> headerMap, String key, String altKey) {
        Integer index = headerMap.get(key);
        if (index == null && altKey != null) {
            index = headerMap.get(altKey);
        }
        if (index != null && index < values.length) {
            String value = values[index].trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private void updateProgressAsync(Long historyId, String fileName, int processed, int total,
                                     int successful, int failed, int progress) {
        try {
            updateProgress(historyId, fileName, processed, total, successful, failed, progress);
        } catch (Exception e) {
        }
    }

    private void updateProgress(Long historyId, String fileName, int processed, int total,
                                int successful, int failed, int progress) {

        ImportProgressDTO current = progressMap.get(historyId);
        if (current != null) {
            String status = current.getStatus();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                return;
            }
        }

        if (progress >= 100) {
            return;
        }

        sendProgress(historyId, fileName, processed, total, successful, failed, "IN_PROGRESS");
    }

    private void sendProgress(Long historyId, String fileName, int processed, int total,
                              int successful, int failed, String status) {
        ImportProgressDTO progress = new ImportProgressDTO();
        progress.setImportId(historyId);
        progress.setFileName(fileName);
        progress.setCurrentProgress(total > 0 ? (int) (processed * 100.0 / total) : 0);
        progress.setTotalRecords(total);
        progress.setProcessedRecords(processed);
        progress.setSuccessfulRecords(successful);
        progress.setFailedRecords(failed);
        progress.setStatus(status);

        progressMap.put(historyId, progress);
        notificationService.notifyImportProgress(progress);
    }

    private void updateHistoryError(Long historyId, String errorMessage) {
        try {
            ImportHistory history = importHistoryRepository.findById(historyId).orElse(null);
            if (history != null) {
                history.setStatus(ImportHistory.ImportStatus.FAILED);
                history.setErrorMessage(errorMessage);
                history.setEndTime(new Date());
                importHistoryRepository.save(history);

                ImportProgressDTO errorProgress = new ImportProgressDTO();
                errorProgress.setImportId(historyId);
                errorProgress.setFileName(history.getFileName());
                errorProgress.setStatus("FAILED");
                errorProgress.setErrorMessage(errorMessage);
                progressMap.put(historyId, errorProgress);
                notificationService.notifyImportProgress(errorProgress);
            }
        } catch (Exception e) {
        }
    }

    public ImportProgressDTO getProgress(Long importId) {
        ImportProgressDTO progress = progressMap.get(importId);
        if (progress != null) {
            return progress;
        }

        return importHistoryRepository.findById(importId)
                .map(history -> {
                    ImportProgressDTO dto = new ImportProgressDTO();
                    dto.setImportId(history.getId());
                    dto.setFileName(history.getFileName());

                    int total = history.getTotalRecords() != null ? history.getTotalRecords() : 0;
                    int success = history.getSuccessfulRecords() != null ? history.getSuccessfulRecords() : 0;
                    int failed = history.getFailedRecords() != null ? history.getFailedRecords() : 0;

                    dto.setTotalRecords(total);
                    dto.setSuccessfulRecords(success);
                    dto.setFailedRecords(failed);
                    dto.setProcessedRecords(success + failed);
                    dto.setCurrentProgress(history.getCurrentProgress() != null
                            ? history.getCurrentProgress()
                            : (total > 0 ? (int) ((success + failed) * 100.0 / total) : 0));
                    dto.setStatus(history.getStatus() != null ? history.getStatus().name() : "IN_PROGRESS");
                    dto.setErrorMessage(history.getErrorMessage());
                    return dto;
                })
                .orElse(null);
    }

    public List<ImportHistoryDTO> getImportHistory(String userName) {
        boolean isAdmin = "admin".equalsIgnoreCase(userName);

        List<ImportHistory> history;
        if (isAdmin) {
            history = importHistoryRepository.findAllOrderedById();
        } else {
            history = importHistoryRepository.findAllByUserName(userName).stream()
                    .filter(h -> !Boolean.TRUE.equals(h.getIsAdmin()))
                    .toList();
        }

        return history.stream().map(this::toDTO).toList();
    }

    public void clearImportHistory(String userName) {
        if (!"admin".equalsIgnoreCase(userName)) {
            throw new SecurityException("Only admin can clear import history");
        }
        importHistoryRepository.deleteAll();
    }

    private ImportHistoryDTO toDTO(ImportHistory history) {
        ImportHistoryDTO dto = new ImportHistoryDTO();
        dto.setId(history.getId());
        dto.setFileName(history.getFileName());
        dto.setFileType(history.getFileType());
        dto.setStatus(history.getStatus());
        dto.setUserName(history.getUserName());
        dto.setIsAdmin(history.getIsAdmin());
        dto.setStartTime(history.getStartTime());
        dto.setEndTime(history.getEndTime());
        dto.setTotalRecords(history.getTotalRecords());
        dto.setSuccessfulRecords(history.getSuccessfulRecords());
        dto.setFailedRecords(history.getFailedRecords());
        dto.setErrorMessage(history.getErrorMessage());
        dto.setCurrentProgress(history.getCurrentProgress());
        return dto;
    }
}