package com.project.moduleserviceadmin.service;

import com.project.moduleserviceadmin.entity.Person;
import com.project.moduleserviceadmin.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {
    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person createPerson(String name) {
        Person person = new Person(name);
        return personRepository.save(person);
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
}
