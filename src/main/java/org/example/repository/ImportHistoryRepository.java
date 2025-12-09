package org.example.repository;

import org.example.entity.ImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    Optional<ImportHistory> findById(Long id);

    @Query("SELECT i FROM ImportHistory i WHERE i.userName = :userName ORDER BY i.startTime DESC")
    Page<ImportHistory> findByUserName(@Param("userName") String userName, Pageable pageable);

    @Query("SELECT i FROM ImportHistory i ORDER BY i.startTime DESC")
    Page<ImportHistory> findAllOrdered(Pageable pageable);

    @Query("SELECT i FROM ImportHistory i WHERE i.userName = :userName ORDER BY i.id DESC")
    List<ImportHistory> findAllByUserName(@Param("userName") String userName);

    @Query("SELECT i FROM ImportHistory i ORDER BY i.id DESC")
    List<ImportHistory> findAllOrderedById();

    void deleteAll();
}