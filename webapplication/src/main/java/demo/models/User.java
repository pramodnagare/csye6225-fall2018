package demo.models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Table
public class User {

	//
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", length = 40)
	private int id;
	@Column(name = "email", length = 40)
	private String email;
	@Column(name = "password", length = 300)
	private String password;

	@OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<UserTransaction> ut;

	public User() {

		ut = new ArrayList<UserTransaction>();
	}

	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public List<UserTransaction> getUt() {
		return ut;
	}

	public void setUt(List<UserTransaction> ut) {
		this.ut = ut;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
