package com.securitylog.repository;

import com.securitylog.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    List<LogEntry> findByLogNameOrderByRecordIdAsc(String logName);

    @Query("SELECT DISTINCT e.logName FROM LogEntry e ORDER BY e.logName")
    List<String> findDistinctLogNames();
}
