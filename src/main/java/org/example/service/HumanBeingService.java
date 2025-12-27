package org.example.service;

import org.example.dto.HumanBeingDTO;
import org.example.entity.*;
import org.example.mapper.HumanBeingMapper;
import org.example.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class HumanBeingService {

    @Autowired
    private HumanBeingRepository humanBeingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CoordinatesRepository coordinatesRepository;

    @Autowired
    private HumanBeingMapper mapper;

    public Page<HumanBeingDTO> getAllHumanBeings(Pageable pageable, String filterBy, String filterValue) {
        Page<HumanBeing> result;

        if (filterBy != null && filterValue != null && !filterValue.trim().isEmpty()) {
            switch (filterBy) {
                case "name":
                    result = humanBeingRepository.findByNameExact(filterValue.trim(), pageable);
                    break;
                case "car.name":
                    result = humanBeingRepository.findByCarNameExact(filterValue.trim(), pageable);
                    break;
                case "mood":
                    result = humanBeingRepository.findByMoodExact(filterValue.trim(), pageable);
                    break;
                case "weaponType":
                    result = humanBeingRepository.findByWeaponTypeExact(filterValue.trim(), pageable);
                    break;
                default:
                    result = humanBeingRepository.findAll(pageable);
            }
        } else {
            result = humanBeingRepository.findAll(pageable);
        }

        return result.map(mapper::toDTO);
    }

    public HumanBeingDTO getHumanBeingById(Long id) {
        return humanBeingRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("HumanBeing not found with id: " + id));
    }

    public HumanBeingDTO createHumanBeing(HumanBeingDTO dto) {
        HumanBeing entity = mapper.toEntity(dto);

        if (dto.getCoordinates() != null) {
            Coordinates coords;
            if (dto.getCoordinates().getId() == null) {
                coords = mapper.toCoordinatesEntity(dto.getCoordinates());
                if (coords.getY() != null && coords.getY() <= -926) {
                    throw new IllegalArgumentException("Y coordinate must be greater than -926");
                }
                if (coords.getX() != null && coords.getX() > 112) {
                    throw new IllegalArgumentException("X coordinate cannot exceed 112");
                }
                coords = coordinatesRepository.save(coords);
            } else {
                coords = coordinatesRepository.findById(dto.getCoordinates().getId())
                        .orElseThrow(() -> new RuntimeException("Coordinates not found"));
                if (dto.getCoordinates().getX() != null) coords.setX(dto.getCoordinates().getX());
                if (dto.getCoordinates().getY() != null) coords.setY(dto.getCoordinates().getY());
                if (coords.getY() != null && coords.getY() <= -926) {
                    throw new IllegalArgumentException("Y coordinate must be greater than -926");
                }
                if (coords.getX() != null && coords.getX() > 112) {
                    throw new IllegalArgumentException("X coordinate cannot exceed 112");
                }
                coords = coordinatesRepository.save(coords);
            }
            entity.setCoordinates(coords);
        }

        if (dto.getCar() != null && dto.getCar().getId() == null) {
            Car car = mapper.toCarEntity(dto.getCar());
            car = carRepository.save(car);
            entity.setCar(car);
        } else if (dto.getCar() != null && dto.getCar().getId() != null) {
            Car car = carRepository.findById(dto.getCar().getId())
                    .orElseThrow(() -> new RuntimeException("Car not found"));
            entity.setCar(car);
        }

        if (entity.getImpactSpeed() != null && entity.getImpactSpeed() > 345) {
            throw new IllegalArgumentException("Impact speed cannot exceed 345");
        }

        List<HumanBeing> existingByNameAndCoords = humanBeingRepository.findByNameAndCoordinates(
                entity.getName(), entity.getCoordinates().getX(), entity.getCoordinates().getY());

        if (!existingByNameAndCoords.isEmpty()) {
            throw new IllegalArgumentException(
                    "HumanBeing with name '" + entity.getName() + "' and coordinates (" +
                            entity.getCoordinates().getX() + ", " + entity.getCoordinates().getY() +
                            ") already exists");
        }

        if (Boolean.TRUE.equals(entity.getRealHero())) {
            List<HumanBeing> existingBySpeedAndWaiting = humanBeingRepository.findByHeroSpeedAndWaiting(
                    entity.getImpactSpeed(), entity.getMinutesOfWaiting());

            if (!existingBySpeedAndWaiting.isEmpty()) {
                throw new IllegalArgumentException(
                        "Hero with impactSpeed " + entity.getImpactSpeed() +
                                " and minutesOfWaiting " + entity.getMinutesOfWaiting() + " already exists");
            }
        }

        HumanBeing saved = humanBeingRepository.save(entity);
        return mapper.toDTO(saved);
    }

    public HumanBeingDTO updateHumanBeing(Long id, HumanBeingDTO dto) {
        HumanBeing existing = humanBeingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HumanBeing not found with id: " + id));

        existing.setName(dto.getName());
        existing.setRealHero(dto.getRealHero());
        existing.setHasToothpick(dto.getHasToothpick());
        existing.setMood(dto.getMood());
        existing.setImpactSpeed(dto.getImpactSpeed());
        existing.setMinutesOfWaiting(dto.getMinutesOfWaiting());
        existing.setWeaponType(dto.getWeaponType());

        if (dto.getCoordinates() != null) {
            Coordinates coords;
            if (dto.getCoordinates().getId() != null) {
                coords = coordinatesRepository.findById(dto.getCoordinates().getId())
                        .orElseThrow(() -> new RuntimeException("Coordinates not found"));
                if (dto.getCoordinates().getX() != null) coords.setX(dto.getCoordinates().getX());
                if (dto.getCoordinates().getY() != null) coords.setY(dto.getCoordinates().getY());
            } else {
                coords = mapper.toCoordinatesEntity(dto.getCoordinates());
            }

            if (coords.getY() != null && coords.getY() <= -926) {
                throw new IllegalArgumentException("Y coordinate must be greater than -926");
            }
            if (coords.getX() != null && coords.getX() > 112) {
                throw new IllegalArgumentException("X coordinate cannot exceed 112");
            }

            coords = coordinatesRepository.save(coords);
            existing.setCoordinates(coords);
        }

        if (dto.getCar() != null) {
            if (dto.getCar().getId() != null) {
                Car car = carRepository.findById(dto.getCar().getId())
                        .orElseThrow(() -> new RuntimeException("Car not found"));
                car.setName(dto.getCar().getName());
                carRepository.save(car);
                existing.setCar(car);
            } else {
                Car car = mapper.toCarEntity(dto.getCar());
                car = carRepository.save(car);
                existing.setCar(car);
            }
        }

        if (existing.getImpactSpeed() != null && existing.getImpactSpeed() > 345) {
            throw new IllegalArgumentException("Impact speed cannot exceed 345");
        }

        List<HumanBeing> existingByNameAndCoords = humanBeingRepository.findByNameAndCoordinates(
                existing.getName(), existing.getCoordinates().getX(), existing.getCoordinates().getY());

        if (!existingByNameAndCoords.isEmpty() && existingByNameAndCoords.stream().anyMatch(h -> !h.getId().equals(existing.getId()))) {
            throw new IllegalArgumentException(
                    "HumanBeing with name '" + existing.getName() + "' and coordinates (" +
                            existing.getCoordinates().getX() + ", " + existing.getCoordinates().getY() +
                            ") already exists");
        }

        if (Boolean.TRUE.equals(existing.getRealHero())) {
            List<HumanBeing> existingBySpeedAndWaiting = humanBeingRepository.findByHeroSpeedAndWaiting(
                    existing.getImpactSpeed(), existing.getMinutesOfWaiting());

            if (!existingBySpeedAndWaiting.isEmpty() && existingBySpeedAndWaiting.stream().anyMatch(h -> !h.getId().equals(existing.getId()))) {
                throw new IllegalArgumentException(
                        "Hero with impactSpeed " + existing.getImpactSpeed() +
                                " and minutesOfWaiting " + existing.getMinutesOfWaiting() + " already exists");
            }
        }

        HumanBeing updated = humanBeingRepository.save(existing);
        return mapper.toDTO(updated);
    }

    public void deleteHumanBeing(Long id, Long replacementId) {
        HumanBeing toDelete = humanBeingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HumanBeing not found with id: " + id));

        if (replacementId != null) {
            HumanBeing replacement = humanBeingRepository.findById(replacementId)
                    .orElseThrow(() -> new RuntimeException("Replacement HumanBeing not found with id: " + replacementId));

            Long carUsageCount = humanBeingRepository.countByCarId(toDelete.getCar().getId());
            if (carUsageCount == 1) {
                replacement.setCar(toDelete.getCar());
            }

            Long coordinatesUsageCount = humanBeingRepository.countByCoordinatesId(toDelete.getCoordinates().getId());
            if (coordinatesUsageCount == 1) {
                replacement.setCoordinates(toDelete.getCoordinates());
            }

            humanBeingRepository.save(replacement);
        }

        humanBeingRepository.delete(toDelete);
    }

    public HumanBeing deleteOneByWeaponType(WeaponType weaponType) {
        try {
            List<HumanBeing> found = humanBeingRepository.findByWeaponType(weaponType);
            if (found.isEmpty()) {
                return null;
            }

            HumanBeing toDelete = found.get(0);
            HumanBeing result = new HumanBeing();
            result.setId(toDelete.getId());
            result.setName(toDelete.getName());
            result.setWeaponType(toDelete.getWeaponType());

            humanBeingRepository.deleteById(toDelete.getId());

            return result;

        } catch (EmptyResultDataAccessException | ObjectOptimisticLockingFailureException e) {
            return null;
        }
    }

    public Double getAverageImpactSpeed() {
        Double avg = humanBeingRepository.getAverageImpactSpeed();
        return avg != null ? avg : 0.0;
    }

    public List<HumanBeingDTO> findByNameContaining(String substring) {
        return humanBeingRepository.findByNameContaining(substring)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public int setAllHeroesMoodToSadness() {
        int totalChanged = 0;
        int batchSize = 1000;
        long lastId = 0L;

        while (true) {
            List<Long> ids = humanBeingRepository.findHeroIdsAfter(
                    lastId,
                    Mood.SADNESS,
                    PageRequest.of(0, batchSize)
            );

            if (ids.isEmpty()) {
                break;
            }

            int changedInBatch = updateMoodChunk(ids);
            totalChanged += changedInBatch;

            lastId = ids.get(ids.size() - 1);
        }

        return totalChanged;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateMoodChunk(List<Long> ids) {
        return humanBeingRepository.updateMoodByIds(ids, Mood.SADNESS);
    }


    public int setAllHeroesWithoutCarToRedLadaKalina() {
        List<HumanBeing> heroesWithoutCar = humanBeingRepository.findAllHeroesWithoutCar();
        Car redLadaKalina = carRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equals("Lada Kalina"))
                .findFirst()
                .orElseGet(() -> {
                    Car newCar = new Car();
                    newCar.setName("Lada Kalina");
                    return carRepository.save(newCar);
                });

        for (HumanBeing hero : heroesWithoutCar) {
            hero.setCar(redLadaKalina);
        }
        humanBeingRepository.saveAll(heroesWithoutCar);
        return heroesWithoutCar.size();
    }

    public void deleteAllHumanBeings() {
        humanBeingRepository.deleteAll();
    }

    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    public List<Coordinates> getAllCoordinates() {
        return coordinatesRepository.findAll();
    }
}