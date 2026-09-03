package vn.rikkei.exam.equipmentloan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.equipmentloan.model.ResourceInventory;
import vn.rikkei.exam.equipmentloan.model.ResourceType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResourceInventoryRepository extends JpaRepository<ResourceInventory, Long> {
    List<ResourceInventory> findByResourceType_ResourceCodeAndAvailableDateBetween(String resourceCode, LocalDate startDate, LocalDate endDate);
    List<ResourceInventory> findByResourceTypeAndAvailableDateBetween(ResourceType resourceType, LocalDate startDate, LocalDate endDate);
    Optional<ResourceInventory> findByResourceType_ResourceCodeAndAvailableDate(String resourceCode, LocalDate availableDate);
}
