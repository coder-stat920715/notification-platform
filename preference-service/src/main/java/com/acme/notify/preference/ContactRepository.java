package com.acme.notify.preference;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<UserContact, String> {}
