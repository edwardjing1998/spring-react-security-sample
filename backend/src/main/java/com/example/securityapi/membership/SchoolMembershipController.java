package com.example.securityapi.membership;

import com.example.securityapi.membership.SchoolMembershipDtos.MembershipRequest;
import com.example.securityapi.membership.SchoolMembershipDtos.MembershipResponse;
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
@RequestMapping("/api/admin/school-memberships")
public class SchoolMembershipController {
    private final SchoolMembershipService service;

    public SchoolMembershipController(SchoolMembershipService service) {
        this.service = service;
    }

    @GetMapping
    public List<MembershipResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MembershipResponse findOne(@PathVariable Long id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@Valid @RequestBody MembershipRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MembershipResponse update(@PathVariable Long id,
                                     @Valid @RequestBody MembershipRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
