package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.HumanBeingDTO;
import org.example.entity.WeaponType;
import org.example.service.HumanBeingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/human-beings")
@CrossOrigin(origins = "*")
public class HumanBeingController {

    @Autowired
    private HumanBeingService humanBeingService;

    @Autowired
    private WebSocketNotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<HumanBeingDTO>> getAllHumanBeings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String filterBy,
            @RequestParam(required = false) String filterValue) {

        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDir != null && sortDir.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<HumanBeingDTO> result = humanBeingService.getAllHumanBeings(pageable, filterBy, filterValue);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HumanBeingDTO> getHumanBeingById(@PathVariable Long id) {
        try {
            HumanBeingDTO dto = humanBeingService.getHumanBeingById(id);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllHumanBeings(
            @RequestParam(value = "confirm", required = false) String confirm
    ) {
        if (!"YES".equalsIgnoreCase(confirm)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "To delete ALL HumanBeings, call this endpoint with ?confirm=YES"));
        }

        humanBeingService.deleteAllHumanBeings();
        notificationService.notifyAll("deleted", Map.of("all", true));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> createHumanBeing(@Valid @RequestBody HumanBeingDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            HumanBeingDTO created = humanBeingService.createHumanBeing(dto);
            notificationService.notifyAll("created", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHumanBeing(@PathVariable Long id,
                                              @Valid @RequestBody HumanBeingDTO dto,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            HumanBeingDTO updated = humanBeingService.updateHumanBeing(id, dto);
            notificationService.notifyAll("updated", updated);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHumanBeing(@PathVariable Long id,
                                              @RequestParam(required = false) Long replacementId) {
        try {
            humanBeingService.deleteHumanBeing(id, replacementId);
            notificationService.notifyAll("deleted", Map.of("id", id));
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/special/delete-by-weapon-type")
    public ResponseEntity<?> deleteOneByWeaponType(@RequestParam WeaponType weaponType) {
        try {
            org.example.entity.HumanBeing deleted = humanBeingService.deleteOneByWeaponType(weaponType);
            if (deleted != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", deleted.getId());
                result.put("name", deleted.getName());
                result.put("weaponType", deleted.getWeaponType() != null ? deleted.getWeaponType().toString() : null);
                notificationService.notifyAll("deleted", Map.of("weaponType", weaponType.toString()));
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.ok(Map.of("message", "No objects found with weaponType: " + weaponType));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/special/average-impact-speed")
    public ResponseEntity<Map<String, Double>> getAverageImpactSpeed() {
        Double avg = humanBeingService.getAverageImpactSpeed();
        return ResponseEntity.ok(Map.of("averageImpactSpeed", avg));
    }

    @GetMapping("/special/search-by-name")
    public ResponseEntity<List<HumanBeingDTO>> findByNameContaining(@RequestParam String substring) {
        List<HumanBeingDTO> result = humanBeingService.findByNameContaining(substring);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/special/set-heroes-mood-sadness")
    public ResponseEntity<?> setAllHeroesMoodToSadness() {
        try {
            int count = humanBeingService.setAllHeroesMoodToSadness();
            notificationService.notifyAll("updated", Map.of("operation", "setHeroesMoodToSadness"));
            return ResponseEntity.ok(Map.of("count", count, "message", count + " objects changed to SADNESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/special/set-heroes-without-car-to-lada")
    public ResponseEntity<?> setAllHeroesWithoutCarToRedLadaKalina() {
        try {
            int count = humanBeingService.setAllHeroesWithoutCarToRedLadaKalina();
            notificationService.notifyAll("updated", Map.of("operation", "setHeroesWithoutCarToLada"));
            return ResponseEntity.ok(Map.of("count", count, "message", count + " heroes moved to Lada Kalina"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cars")
    public ResponseEntity<List<org.example.entity.Car>> getAllCars() {
        return ResponseEntity.ok(humanBeingService.getAllCars());
    }

    @GetMapping("/coordinates")
    public ResponseEntity<List<org.example.entity.Coordinates>> getAllCoordinates() {
        return ResponseEntity.ok(humanBeingService.getAllCoordinates());
    }
}