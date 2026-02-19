package com.example.demo.repository;

import com.example.demo.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByDepartmentId(Long departmentId);

    Optional<Section> findByNameAndDepartmentId(String name, Long departmentId);
}
