package vn.rikkei.exam.equipmentloan.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.equipmentloan.model.AppUser;
public interface AppUserRepository extends JpaRepository<AppUser, String> { }
