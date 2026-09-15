package com.example.securityapi.school;

import com.example.securityapi.school.SchoolDtos.SchoolRequest;
import com.example.securityapi.school.SchoolDtos.SchoolResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchoolService {
    private final SchoolRepository schools;

    public SchoolService(SchoolRepository schools) {
        this.schools = schools;
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> findAll() {
        return schools.findAll().stream()
                .sorted(Comparator.comparing(School::getSchoolName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(SchoolService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolResponse findOne(Long id) {
        return toResponse(requireSchool(id));
    }

    @Transactional
    public SchoolResponse create(SchoolRequest request) {
        assertUniqueCode(request.schoolCode(), null);
        School school = new School(
                request.schoolCode(), request.schoolName(), request.schoolType(),
                request.districtName(), request.addressLine1(), request.city(),
                request.stateCode(), request.postalCode(), request.countryCode(),
                request.status());
        return toResponse(schools.save(school));
    }

    @Transactional
    public SchoolResponse update(Long id, SchoolRequest request) {
        School school = requireSchool(id);
        assertUniqueCode(request.schoolCode(), id);
        school.update(
                request.schoolCode(), request.schoolName(), request.schoolType(),
                request.districtName(), request.addressLine1(), request.city(),
                request.stateCode(), request.postalCode(), request.countryCode(),
                request.status());
        return toResponse(schools.save(school));
    }

    @Transactional
    public void delete(Long id) {
        School school = requireSchool(id);
        schools.delete(school);
    }

    private void assertUniqueCode(String schoolCode, Long currentId) {
        boolean exists = currentId == null
                ? schools.existsBySchoolCodeIgnoreCase(schoolCode)
                : schools.existsBySchoolCodeIgnoreCaseAndIdNot(schoolCode, currentId);
        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "School code already exists");
        }
    }

    private School requireSchool(Long id) {
        return schools.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private static SchoolResponse toResponse(School school) {
        return new SchoolResponse(
                school.getId(), school.getSchoolCode(), school.getSchoolName(),
                school.getSchoolType(), school.getDistrictName(),
                school.getAddressLine1(), school.getCity(), school.getStateCode(),
                school.getPostalCode(), school.getCountryCode(), school.getStatus(),
                school.getCreatedAt(), school.getUpdatedAt());
    }
}
