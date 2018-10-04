package demo.models;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class UserTranscation {

	@Id
	private String id;
	private String description;
	private String merchant;
	private String amount;
	private String date;
	private String category;
	private int uid;
	
	//User user;
	
	
	public String getid() {
		return id;
	}
	public int getUid() {
		return uid;
	}
	public void setUid(int uid) {
		this.uid = uid;
	}
	public void setID(String id) {
		this.id = id;
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
