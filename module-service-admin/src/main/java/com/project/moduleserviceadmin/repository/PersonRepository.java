package com.project.moduleserviceadmin.repository;

import com.project.moduleserviceadmin.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
