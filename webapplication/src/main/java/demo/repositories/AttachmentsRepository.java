package demo.repositories;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

import demo.models.Attachments;
import demo.models.User;
import demo.models.UserTransaction;

public interface AttachmentsRepository extends CrudRepository<Attachments, String> {

	@Query("select attachment_id from Attachments a WHERE a.userTransaction.id= :user_trans_id and a.attachment_id= :attachment_id")
	Optional<String> findIdByAttachIds(@Param("user_trans_id") String user_trans_id,
			@Param("attachment_id") String attachment_id);

	@Query("from Attachments a WHERE a.attachment_id= :attachment_id")
	Attachments findIdByAttachIdswa(@Param("attachment_id") String attachment_id);

	@Query("select url from Attachments a WHERE a.attachment_id= :attachment_id")
	List<Attachments> findIdByAttachuserIds(@Param("attachment_id") String attachment_id);

	@Query("from Attachments a WHERE a.userTransaction.id= :user_transaction_id")
	List<Attachments> findIdByu(@Param("user_transaction_id") String user_transaction_id);

	@Query("select url from Attachments a WHERE a.attachment_id= :attachment_id")
	List<String> findIdu(@Param("attachment_id") String attachment_id);

	@Transactional
	@Modifying
	@Query("DELETE FROM Attachments u WHERE u.attachment_id= :attachment_id")
	void deleteTransaction(@Param("attachment_id") String attachment_id);

	@Transactional
	@Modifying
	@Query("UPDATE Attachments u SET url= :url WHERE u.attachment_id= :attachment_id and u.userTransaction.id= :user_transaction_id")
	void updateAttachments(@Param("attachment_id") String attachment_id,
			@Param("user_transaction_id") String user_transaction_id);

}
