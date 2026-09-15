package com.example.securityapi.school;

import com.example.securityapi.school.SchoolDtos.SchoolRequest;
import com.example.securityapi.school.SchoolDtos.SchoolResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schools")
public class SchoolController {
    private final SchoolService service;

    public SchoolController(SchoolService service) {
        this.service = service;
    }

    @GetMapping
    public List<SchoolResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SchoolResponse findOne(@PathVariable Long id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse create(@Valid @RequestBody SchoolRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SchoolResponse update(@PathVariable Long id,
                                 @Valid @RequestBody SchoolRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
