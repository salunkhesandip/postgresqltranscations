package com.cleancoders.postgresqltranscations.repository;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findById(Long empId);
    // JPQL query
    @Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
    List<Employee> findBySalaryGreaterThan(@Param("salary") Long salary);

    // Native SQL Query
    @Query(value = "SELECT * FROM Employee  WHERE emp_salary > :salary", nativeQuery = true)
    List<Employee> findBySalaryGreaterThanNative(@Param("salary") Long salary);

    @Modifying
    @Query("DELETE FROM Employee e WHERE e.empSalary > ?1")
    void deleteUsersBySalaryGreater(Long salary);
}
