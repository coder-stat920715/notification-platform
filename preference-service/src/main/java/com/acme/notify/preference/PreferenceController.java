package com.acme.notify.preference;

import com.acme.notify.common.ContactView;
import com.acme.notify.common.PreferenceView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService service;

    @GetMapping("/preferences/{userId}")
    public PreferenceView preferences(@PathVariable String userId) {
        return service.view(userId);
    }

    @PutMapping("/preferences")
    public void update(@Valid @RequestBody PreferenceUpdate update) {
        service.upsert(update);
    }

    @GetMapping("/contacts/{userId}")
    public ContactView contact(@PathVariable String userId) {
        return service.contact(userId);
    }

    @PutMapping("/contacts")
    public void saveContact(@RequestBody ContactView contact) {
        service.saveContact(contact);
    }
}
