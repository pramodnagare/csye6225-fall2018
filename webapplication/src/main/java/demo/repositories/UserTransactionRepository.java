package demo.repositories;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import demo.models.UserTransaction;

public interface UserTransactionRepository extends CrudRepository<UserTransaction, String> {

	@Query("from UserTransaction u WHERE u.user.id=:user_id")
	List<UserTransaction> findIdByUserId(@Param("user_id") int user_id);

	@Query("select id from UserTransaction u WHERE u.user.id=:user_id")
	Optional<String> findIdByUserIdsss(@Param("user_id") int user_id);

	@Query("select id from UserTransaction u WHERE u.user.id= :user_id and u.id= :id")
	Optional<String> findIdByUserIdwa(@Param("id") String id, @Param("user_id") int user_id);

	// @Query("FROM UserTransaction u WHERE u.id=:id and u.user.id=:userid")
	// Optional<UserTransaction> findIdByUserId_Name(@Param("id") String id,
	// @Param("user_id") int user_id);

	@Transactional
	@Modifying
	@Query("DELETE FROM UserTransaction u WHERE u.id=:id and u.user.id=:userid")
	void deleteTransaction(@Param("id") String id, @Param("userid") int userid);

	@Query("select id FROM UserTransaction u WHERE u.id=:id and u.user.id=:userid")
	Optional<String> findUid(@Param("id") String id, @Param("userid") int userid);

	@Transactional
	@Modifying
	@Query("UPDATE UserTransaction u SET description=:d,merchant=:m,amount=:a,date=:dt,category=:c WHERE u.id=:id and u.user.id=:userid")
	void updateTransaction(@Param("id") String id, @Param("userid") int userid, @Param("d") String d,
			@Param("a") String a, @Param("c") String c, @Param("dt") String dt, @Param("m") String m);

}
