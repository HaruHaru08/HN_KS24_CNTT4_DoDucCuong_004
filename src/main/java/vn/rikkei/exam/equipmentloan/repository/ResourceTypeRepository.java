package vn.rikkei.exam.equipmentloan.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.equipmentloan.model.ResourceType;
public interface ResourceTypeRepository extends JpaRepository<ResourceType, String> { }
