package com.example.rest_api.Repository;

import com.example.rest_api.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,String>{

}
