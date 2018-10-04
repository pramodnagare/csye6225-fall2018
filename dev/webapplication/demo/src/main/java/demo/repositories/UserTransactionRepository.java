package demo.repositories;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import demo.models.UserTranscation;


public interface UserTransactionRepository extends JpaRepository<UserTranscation, Integer> {

	UserTranscation save(UserTranscation usertransaction);
	
	/*
	@Query("SELECT id as id,description as description, merchant as merchant, amount as amount, date as date, category as category FROM UserTranscation u WHERE u.uid=:uid")
	Optional<List> findAllTranscationsByUserId(@Param("uid") int uid);
	*/
	
	@Query("SELECT id FROM UserTranscation u WHERE u.uid=:uid")
	List<String> findAllIDByUserId(@Param("uid") int uid);
	
	
	@Transactional
	@Modifying
	@Query("UPDATE UserTranscation u SET description=:d,merchant=:m,amount=:a,date=:dt,category=:c WHERE u.id=:id and u.uid=:uid")
	void updateTransaction(@Param("id") String id, @Param("uid") int uid, @Param("d") String d, @Param("a") String a, @Param("c") String c, @Param("dt") String dt, @Param("m") String m);
	
	
	@Transactional
	@Modifying
	@Query("DELETE FROM UserTranscation u WHERE u.id=:id and u.uid=:uid")
	void deleteTransaction(@Param("id") String id, @Param("uid") int uid);
	
}
