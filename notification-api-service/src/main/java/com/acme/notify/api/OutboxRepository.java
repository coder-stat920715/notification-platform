package com.acme.notify.api;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** SKIP LOCKED lets several API pods run the relay concurrently without double-publishing. */
    @Query(value = "select * from outbox_event where published = false order by id limit :n for update skip locked",
            nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("n") int n);
}
