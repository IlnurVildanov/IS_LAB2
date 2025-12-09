package org.example.repository;

import org.example.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findById(Long id);

    List<Car> findAll();

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Car c WHERE c.name = :name")
    Optional<Car> findByName(@org.springframework.data.repository.query.Param("name") String name);
}