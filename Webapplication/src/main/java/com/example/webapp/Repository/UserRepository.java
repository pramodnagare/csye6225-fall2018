package com.example.webapp.Repository;

import com.example.webapp.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,String>{

}
