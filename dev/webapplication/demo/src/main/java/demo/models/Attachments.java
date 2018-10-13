package demo.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Attachments {

	@Id
	@Column(name = "attachment_id", length = 100)
	private String attachment_id;

	@Column(name = "url", length = 400)
	private String url;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private UserTransaction userTransaction;

	public Attachments() {

	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	public String getAttachment_id() {
		return attachment_id;
	}

	public void setAttachment_id(String attachment_id) {
		this.attachment_id = attachment_id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
