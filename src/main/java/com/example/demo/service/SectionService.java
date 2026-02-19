package com.example.demo.service;

import com.example.demo.entity.Section;
import com.example.demo.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;

    public List<Section> findAll() {
        return sectionRepository.findAll();
    }

    public Optional<Section> findById(Long id) {
        return sectionRepository.findById(id);
    }

    public List<Section> findByDepartmentId(Long departmentId) {
        return sectionRepository.findByDepartmentId(departmentId);
    }

    public Optional<Section> findByNameAndDepartmentId(String name, Long departmentId) {
        return sectionRepository.findByNameAndDepartmentId(name, departmentId);
    }

    @Transactional
    public Section save(Section section) {
        return sectionRepository.save(section);
    }

    @Transactional
    public void deleteById(Long id) {
        sectionRepository.deleteById(id);
    }
}
