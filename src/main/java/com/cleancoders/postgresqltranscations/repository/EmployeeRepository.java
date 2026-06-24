package com.cleancoders.postgresqltranscations.repository;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>,
                                           JpaSpecificationExecutor<Employee> {

    /** JPQL: returns employees whose salary exceeds the given threshold. */
    @Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
    List<Employee> findBySalaryGreaterThan(@Param("salary") BigDecimal salary);

    /** Native SQL: same filter using the fully-qualified schema.table reference. */
    @Query(value = "SELECT * FROM company.employee WHERE emp_salary > :salary", nativeQuery = true)
    List<Employee> findBySalaryGreaterThanNative(@Param("salary") BigDecimal salary);

    @Modifying
    @Query("DELETE FROM Employee e WHERE e.empSalary > ?1")
    void deleteUsersBySalaryGreater(BigDecimal salary);
}
