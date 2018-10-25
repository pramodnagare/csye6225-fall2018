package demo.models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table
public class UserTransaction {
	@Id
	@Column(name = "id", length = 40)
	private String id;
	@Column(name = "description", length = 40)
	private String description;
	@Column(name = "merchant", length = 40)
	private String merchant;
	@Column(name = "amount", length = 40)
	private String amount;
	@Column(name = "date", length = 40)
	private String date;
	@Column(name = "category", length = 40)
	private String category;

	@OneToMany(mappedBy = "userTransaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<Attachments> attachments;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private User user;

	public UserTransaction() {
		attachments = new ArrayList<>();
	}

	public List<Attachments> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachments> attachments) {
		this.attachments = attachments;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMerchant() {
		return merchant;
	}

	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

}
