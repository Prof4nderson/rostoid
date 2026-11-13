package com.rostoid.backend.people;

import com.rostoid.backend.common.ApiViews;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/people")
public class PeopleController {
    private final PeopleService service;

    public PeopleController(PeopleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiViews.PersonView create(@Valid @RequestBody PeopleRequests.CreatePersonRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ApiViews.PersonView> list() {
        return service.list();
    }

    @PutMapping("/{id}/profile")
    public ApiViews.PersonView updateProfile(@PathVariable Long id,
                                             @Valid @RequestBody PeopleRequests.UpdateProfileRequest request) {
        return service.updateProfile(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
