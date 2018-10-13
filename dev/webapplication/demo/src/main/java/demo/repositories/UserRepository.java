package demo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import demo.models.User;

public interface UserRepository extends CrudRepository<User, Integer> {
	@Query("SELECT id FROM User u WHERE u.email=:email")
	Optional<Integer> findIdByUserName(@Param("email") String email);

	// findIdByUserName(@Param("email") String email);
	
}
