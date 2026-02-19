package com.example.demo.repository;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Assignment;
import com.example.demo.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    /** 指定資産の貸出中レコード (returnDate が null) */
    Optional<Assignment> findByAssetAndReturnDateIsNull(Asset asset);

    /** 指定社員の貸出中レコード一覧 */
    List<Assignment> findByEmployeeAndReturnDateIsNull(Employee employee);

    /** 現在貸出中の全レコード */
    List<Assignment> findByReturnDateIsNull();

    /** 全履歴 (新しい順) */
    List<Assignment> findAllByOrderByCheckoutDateDesc();
}
