package com.project.moduleserviceadmin.controller;

import com.project.moduleserviceadmin.entity.Person;
import com.project.moduleserviceadmin.service.PersonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/persons")
public class PersonController {
    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    // 새로운 Person을 생성 (INSERT)
    @PostMapping
    public ResponseEntity<Person> createPerson(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        Person person = personService.createPerson(name);
        return ResponseEntity.ok(person);
    }

    // 모든 Person 데이터 조회
    @GetMapping
    public ResponseEntity<List<Person>> getAllPersons() {
        List<Person> persons = personService.getAllPersons();
        return ResponseEntity.ok(persons);
    }
}
