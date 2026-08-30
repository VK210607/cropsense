package javaproject.cropsense.repository;

import javaproject.cropsense.model.FarmData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmDataRepository extends JpaRepository<FarmData, Long> {
}