# Java 21 – 25 Language & Platform Features

A comprehensive reference organized by Java version.
Each section covers the JEP number, description, practical examples, when to use the feature,
common pitfalls, and — where applicable — real examples pulled directly from this codebase.

---

## Table of Contents

| Version | Status | Key features |
|---------|--------|-------------|
| [Java 21 (LTS)](#java-21-lts--september-2023) | Current LTS | Sealed Classes, Pattern-Matching Switch, Virtual Threads, Record Patterns, Sequenced Collections |
| [Java 22](#java-22--march-2024) | Feature release | Unnamed Variables, Structured Concurrency (preview), Stream Gatherers (preview), Foreign Function & Memory API |
| [Java 23](#java-23--september-2024) | Feature release | Markdown Javadoc, Module Import Declarations (preview), Scoped Values (preview), Primitive Patterns (preview) |
| [Java 24](#java-24--march-2025) | Feature release | Stream Gatherers (final), Class-File API, ZGC Generational Mode, Scoped Values (preview) |
| [Java 25 (LTS)](#java-25-lts--september-2025) | Next LTS | Module Import Declarations (final), Scoped Values (final), Structured Concurrency (final), Compact Source Files |

---

## Java 21 (LTS) — September 2023

### JEP 409 — Sealed Classes *(final)*

**Description**  
Sealed classes restrict which other classes or interfaces may extend or implement them.
The `permits` clause is an explicit, compiler-enforced exhaustiveness contract.

**When to use it**  
Model closed domain hierarchies where every subtype is known at design time:
exception hierarchies, algebraic-data-type style value objects, command/event types.

**Practical example**

```java
public abstract sealed class Shape
        permits Circle, Rectangle, Triangle { }

public final class Circle    extends Shape { double radius; }
public final class Rectangle extends Shape { double width, height; }
public final class Triangle  extends Shape { double base, height; }
```

**Common pitfalls**
- Forgetting to add a new subtype to `permits` when extending the hierarchy — this is a feature, not a bug; the compiler tells you exactly what to fix.
- Mixing `sealed` with open extension: a `non-sealed` permitted type opts the subtree back open.
- Sealed interfaces require all implementations to be in the same compilation unit (same package or module in most configurations).

**Codebase example** — [`EmployeeDomainException.java`](../src/main/java/com/cleancoders/postgresqltranscations/exception/EmployeeDomainException.java)

```java
/// Sealed base class for all domain exceptions in the Employee service.
/// Because this hierarchy is sealed, a pattern-matching switch in
/// GlobalExceptionHandler is exhaustive — adding a new subtype without
/// a matching case causes a compile error, not a silent 500.
public abstract sealed class EmployeeDomainException extends RuntimeException
        permits EmployeeNotFoundException, EmployeeConflictException, ServiceUnavailableException {

    protected EmployeeDomainException(String message) { super(message); }
    protected EmployeeDomainException(String message, Throwable cause) { super(message, cause); }
}
```

The Resilience4j retry config also leverages this: one `ignoreExceptions` entry covers all three
subtypes because `isAssignableFrom` respects the sealed hierarchy.

---

### JEP 441 — Pattern Matching for `switch` *(final)*

**Description**  
`switch` expressions and statements accept type patterns, guarded patterns (`when`), and
`null` cases. When the selector type is a sealed type the compiler verifies exhaustiveness.

**When to use it**  
Replacing chains of `instanceof` + cast, multi-catch exception handlers, and visitor patterns.
Especially powerful when the switch type is a sealed class or interface.

**Practical example**

```java
// Type patterns + guarded pattern
String describe(Object obj) {
    return switch (obj) {
        case Integer i when i < 0 -> "negative int: " + i;
        case Integer i            -> "positive int: " + i;
        case String s             -> "string of length " + s.length();
        case null                 -> "null";
        default                   -> "other: " + obj;
    };
}
```

**Common pitfalls**
- Ordering matters: more specific patterns (guarded) must appear before general ones.
- `default` is required unless the selector type is sealed and all subtypes are covered.
- Mixing classic `case CONSTANT:` with type patterns in the same switch is illegal.

**Codebase example** — [`GlobalExceptionHandler.java`](../src/main/java/com/cleancoders/postgresqltranscations/exception/GlobalExceptionHandler.java)

```java
// One @ExceptionHandler covers the entire sealed hierarchy.
// No default branch needed — the compiler enforces exhaustiveness.
@ExceptionHandler(EmployeeDomainException.class)
protected ResponseEntity<ErrorResponse> handleDomainException(EmployeeDomainException ex) {
    return switch (ex) {
        case EmployeeNotFoundException e ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));

        case EmployeeConflictException e ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage()));

        case ServiceUnavailableException e ->
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage()));
    };
}
```

---

### JEP 444 — Virtual Threads *(final)*

**Description**  
Virtual threads are lightweight threads managed by the JVM, not the OS.
They can be parked during blocking I/O at near-zero cost, enabling millions of concurrent
threads without the memory and scheduling overhead of platform (OS) threads.
This is the core deliverable of Project Loom.

**When to use it**  
Any server application that does blocking I/O (JDBC, HTTP calls, file reads).
Spring Boot 3.2+ supports opt-in activation; Spring Boot 4.x recommends it as the default.

**Practical example**

```java
// Creating a virtual thread explicitly
Thread.ofVirtual().name("worker").start(() -> {
    // block on I/O freely — the carrier thread is released
    String data = fetchFromDatabase();
    process(data);
});

// Using a virtual-thread executor
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doWork());
}
```

**Common pitfalls**
- **Thread-local pollution**: heavy use of `ThreadLocal` with large values wastes memory per virtual thread. Prefer `ScopedValue` (JEP 506).
- **Pinning**: holding a `synchronized` monitor while blocking pins the virtual thread to the carrier thread. Use `ReentrantLock` in hot paths.
- **Thread pools**: wrapping virtual threads in a fixed-size pool defeats the purpose — use an unbounded `VirtualThreadPerTaskExecutor`.

**Codebase example** — [`application.yml`](../src/main/resources/application.yml)

```yaml
spring:
  threads:
    virtual:
      enabled: true   # Java 21 Virtual Threads (JEP 444)
                      # Activates virtual threads for Tomcat's request pool AND
                      # Spring's async executor. Every @Transactional JDBC call
                      # parks a virtual thread, freeing the carrier for other requests.
```

All `@Transactional` service methods in `EmployeeService` run on virtual threads automatically
once this property is set.

---

### JEP 440 — Record Patterns *(final)*

**Description**  
Extends pattern matching to destructure record components inline inside `instanceof` checks
and `switch` expressions, eliminating boilerplate accessor calls.

**When to use it**  
When working with record types inside `instanceof` or `switch` and you need multiple
components simultaneously.

**Practical example**

```java
record Point(double x, double y) {}
record ColoredPoint(Point point, String color) {}

// Destructure nested records in one expression
if (obj instanceof ColoredPoint(Point(double x, double y), String color)) {
    System.out.printf("(%f, %f) in %s%n", x, y, color);
}

// Same in a switch
String describe(Object o) {
    return switch (o) {
        case Point(double x, double y) when x == y -> "diagonal at " + x;
        case Point(double x, double y)              -> "point (" + x + ", " + y + ")";
        default                                     -> "unknown";
    };
}
```

**Common pitfalls**
- Record patterns require the target to be a record. They do not work on regular classes.
- Deeply nested destructuring can hurt readability — use local variables for intermediate components.

---

### JEP 431 — Sequenced Collections

**Description**  
New interfaces added to the collections framework: `SequencedCollection`, `SequencedSet`,
and `SequencedMap`. Each exposes `getFirst()`, `getLast()`, `addFirst()`, `addLast()`,
`removeFirst()`, `removeLast()`, and a `reversed()` view.

**When to use it**  
Wherever you need first/last element access without depending on implementation details of
`LinkedList` or `Deque`.

**Practical example**

```java
SequencedCollection<String> list = new ArrayList<>(List.of("a", "b", "c"));
System.out.println(list.getFirst()); // a
System.out.println(list.getLast());  // c

// Reversed view (lazy, no copy)
list.reversed().forEach(System.out::println); // c, b, a

SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.put("one", 1);
map.put("two", 2);
System.out.println(map.firstEntry()); // one=1
```

**Common pitfalls**
- `reversed()` returns a *view* — mutations to the reversed view affect the original collection.
- Not all collections implement `SequencedCollection` (e.g., `HashSet`). Use `LinkedHashSet` for ordering.

---

## Java 22 — March 2024

### JEP 456 — Unnamed Variables and Patterns *(final)*

**Description**  
The underscore `_` can replace any variable name or pattern component when the value
is not needed, making the intent explicit and suppressing unused-variable warnings.

**When to use it**  
Loop variables, catch blocks, lambda parameters, and record-pattern components you
don't need to reference.

**Practical example**

```java
// Unused catch variable
try {
    riskyOperation();
} catch (IOException _) {
    log.warn("I/O error, retrying");
}

// Unused loop variable
for (var _ : items) {
    counter++;
}

// Unused pattern component
if (obj instanceof Point(double x, _)) {
    System.out.println("x = " + x);
}
```

**Common pitfalls**
- `_` cannot be read — it is a write-only sink. Any attempt to reference it causes a compile error.

---

### JEP 454 — Foreign Function & Memory API *(final)*

**Description**  
A safe, efficient API for calling native code and managing off-heap memory without JNI.
Key types: `Arena`, `MemorySegment`, `MethodHandles.lookup().findStatic(...)`.

**When to use it**  
Interoperating with native libraries (C/C++), managing large off-heap buffers for performance-critical
data structures, or replacing JNI boilerplate.

**Practical example**

```java
// Allocate off-heap memory, interop with a C function
try (Arena arena = Arena.ofConfined()) {
    MemorySegment cString = arena.allocateFrom("Hello, native!");
    // pass cString to a native method via a downcall handle
}
```

**Common pitfalls**
- Always use `Arena.ofConfined()` or `Arena.ofShared()` — `Arena.global()` never releases memory.
- Off-heap memory is invisible to the GC; leaks are silent.

---

### JEP 461 — Stream Gatherers *(preview in 22, final in 24)*

> See the [Java 24 section](#jep-485--stream-gatherers-final) for the final spec and codebase examples.

---

## Java 23 — September 2024

### JEP 467 — Markdown Documentation Comments *(final)*

**Description**  
Javadoc comments can now be written with `///` prefix lines and full Markdown syntax
(headings, bold, code fences, links). The `@link` Javadoc tag also supports Markdown-style
`[ClassName#method]` references.

**When to use it**  
Any new or updated API documentation. Prefer `///` over `/** */` in new code.
Text inside `///` blocks renders as Markdown in IDEs and the generated Javadoc HTML.

**Practical example**

```java
/// Represents an employee in the HR system.
///
/// **Usage example:**
/// ```java
/// Employee emp = new Employee(1L, "Alice", new BigDecimal("75000"));
/// ```
///
/// See also [EmployeeDTO] for the API contract and [EmployeeMapper] for conversions.
public class Employee { … }
```

**Common pitfalls**
- Mixing `/** */` and `///` on the same declaration is illegal — use one style per element.
- The Markdown renderer is strict: a blank `///` line is needed between sections (same as CommonMark rules for paragraphs).
- Backtick code spans inside `///` do not generate `<code>` tags if the content looks like a Javadoc `{@code}` tag — use triple-backtick fences for multi-line examples.

**Codebase examples**

Found throughout the service, context, and config layers, for example in
[`RequestContext.java`](../src/main/java/com/cleancoders/postgresqltranscations/context/RequestContext.java):

```java
/// Holds per-request **Scoped Values** — immutable, inheritable-by-child-threads
/// values bound for the lifetime of one HTTP request.
///
/// ### Usage
/// ```java
/// String reqId = RequestContext.CORRELATION_ID.orElse("n/a");
/// ```
public final class RequestContext { … }
```

And in [`EmployeeDomainException.java`](../src/main/java/com/cleancoders/postgresqltranscations/exception/EmployeeDomainException.java):

```java
/// Sealed base class for all domain exceptions in the Employee service.
///
/// Permitted subtypes: [EmployeeNotFoundException], [EmployeeConflictException],
/// [ServiceUnavailableException]. Because this hierarchy is **sealed**, the compiler
/// guarantees that a pattern-matching `switch` in [GlobalExceptionHandler] is
/// **exhaustive**.
public abstract sealed class EmployeeDomainException extends RuntimeException
        permits EmployeeNotFoundException, EmployeeConflictException, ServiceUnavailableException { … }
```

---

### JEP 476 — Module Import Declarations *(preview in 23, final in 25)*

> See the [Java 25 section](#jep-511--module-import-declarations-final) for the final spec and codebase examples.

---

## Java 24 — March 2025

### JEP 485 — Stream Gatherers *(final)*

**Description**  
`Stream.gather(Gatherer)` is a new intermediate stream operation that allows custom
stateful, short-circuiting, and parallel-aware pipeline stages.
The `java.util.stream.Gatherers` utility class ships four built-in gatherers:

| Gatherer | Behaviour |
|----------|-----------|
| `fold` | Sequential stateful reduction (like `reduce`, but emits the running state) |
| `scan` | Running prefix (emits each intermediate accumulation) |
| `windowFixed` | Consecutive, non-overlapping fixed-size windows |
| `windowSliding` | Overlapping sliding windows |
| `mapConcurrent` | Maps each element concurrently on up to *n* virtual threads |

**When to use it**  
Custom pipeline stages that are impossible to express with `map`, `filter`, `flatMap` alone:
sliding aggregates, concurrent fan-out with back-pressure, grouped transformations.

**Practical example**

```java
// Sliding window of size 3
Stream.of(1, 2, 3, 4, 5)
      .gather(Gatherers.windowSliding(3))
      .forEach(System.out::println);
// [1, 2, 3]
// [2, 3, 4]
// [3, 4, 5]

// Concurrent mapping (up to 4 virtual threads at once)
List<EmployeeDTO> dtos = employees.stream()
        .gather(Gatherers.mapConcurrent(4, mapper::convertToEmployeeDTO))
        .toList();

// Custom gatherer: take elements until a condition fails
Gatherer<Integer, ?, Integer> takeWhilePositive = Gatherer.ofSequential(
        () -> new boolean[]{true},         // initializer
        (state, element, downstream) -> {  // integrator
            if (state[0] && element > 0) {
                return downstream.push(element);
            }
            state[0] = false;
            return false;                  // short-circuit
        }
);
```

**Common pitfalls**
- `mapConcurrent` forks tasks onto virtual threads — ensure the mapping function is thread-safe.
- Custom gatherers that maintain mutable state must use the sequential form (`Gatherer.ofSequential`) unless they correctly implement the `combiner` for parallel streams.
- Unlike `parallel()`, `mapConcurrent` preserves encounter order while still executing concurrently.

**Codebase example** — [`EmployeeService.java`](../src/main/java/com/cleancoders/postgresqltranscations/service/EmployeeService.java)

```java
// Java 24: Stream Gatherers — mapConcurrent (JEP 485).
// Applies the mapping function concurrently across up to 4 virtual threads.
// Most valuable when the mapping function performs I/O (e.g. an external enrichment call).
return employees.stream()
        .gather(Gatherers.mapConcurrent(4, mapper::convertToEmployeeDTO))
        .toList();
```

Used in both `findEmployeesBySalary` (JPQL path) and `findEmployeesBySalaryNative` (native SQL path).

---

### JEP 484 — Class-File API *(final)*

**Description**  
A standard API for reading, writing, and transforming Java `.class` files, replacing
third-party bytecode libraries (ASM, Javassist) for many use cases.

**When to use it**  
Build-time or agent-based bytecode transformation, instrumentation, and analysis tasks
that previously required ASM or similar libraries.

**Practical example**

```java
// Read a class file and print all method names
ClassFile cf = ClassFile.of();
ClassModel cm = cf.parse(Path.of("MyClass.class"));
cm.methods().forEach(m -> System.out.println(m.methodName().stringValue()));

// Transform: rename all occurrences of a field
byte[] transformed = cf.transformClass(cm, ClassTransform.transformingMethods(
        MethodTransform.transformingCode(
                (builder, element) -> {
                    if (element instanceof FieldInstruction fi && fi.name().stringValue().equals("oldName"))
                        builder.getfield(fi.owner(), "newName", fi.typeSymbol());
                    else
                        builder.with(element);
                }
        )
));
```

**Common pitfalls**
- The API models `.class` file structure directly — familiarity with the JVM specification helps.
- `ClassFile.of()` reuses a shared instance; it is thread-safe.

---

### JEP 490 — ZGC: Generational Mode by Default

**Description**  
ZGC now defaults to generational mode, collecting young and old generations separately.
This improves throughput and reduces memory footprint for most workloads without any code changes.

**When to use it**  
Production workloads that already use ZGC (`-XX:+UseZGC`). The generational mode
improves allocation-heavy applications.

**Practical example (JVM flag)**

```bash
# Generational ZGC is now the default when using ZGC
java -XX:+UseZGC -jar app.jar

# Explicit (same as default post JDK 24)
java -XX:+UseZGC -XX:+ZGenerational -jar app.jar
```

**Common pitfalls**
- If you previously pinned `-XX:-ZGenerational` to disable it, that flag is now deprecated and will be removed.

---

## Java 25 (LTS) — September 2025

### JEP 511 — Module Import Declarations *(final)*

**Description**  
`import module <module-name>;` imports all exported packages of a named module in a single
declaration, replacing a long list of individual `import` statements.
The most common use is `import module java.base;`, which covers `java.lang`, `java.util`,
`java.math`, `java.time`, `java.io`, `java.net`, `java.util.stream`, and every other
exported package of the platform's base module.

**When to use it**  
Service and utility classes that use many standard library types. It reduces noise at the
top of the file without compromising explicitness — IDEs can still resolve which types come
from which package.

**Practical example**

```java
import module java.base;           // replaces ~10 individual imports

// All of the following types are now available without explicit imports:
// BigDecimal, List, Map, Optional, LocalDate, Duration, Path, UUID, etc.

public class Example {
    public Optional<BigDecimal> computeSalary(List<Employee> employees) {
        return employees.stream()
                .map(Employee::getSalary)
                .max(Comparator.naturalOrder());
    }
}
```

**Common pitfalls**
- Ambiguity: if two modules export a type with the same simple name, the compiler reports an
  error and you must use a single-type import to resolve it.
- `import module java.base` does **not** include third-party or application modules — those
  still require explicit imports.
- Some IDEs may not yet fully support module import declarations; verify IDE support before adoption.

**Codebase examples**

[`EmployeeService.java`](../src/main/java/com/cleancoders/postgresqltranscations/service/EmployeeService.java):

```java
// Java 25: Module Import Declaration (JEP 511) — imports all public packages of java.base
// in one line, replacing individual imports of BigDecimal, LocalDate, List, Gatherers, etc.
import module java.base;
```

[`EmployeeController.java`](../src/main/java/com/cleancoders/postgresqltranscations/controller/EmployeeController.java):

```java
// Java 25: Module Import Declaration (JEP 511) — covers BigDecimal, List, and
// any other java.base type used in this controller without individual imports.
import module java.base;
```

---

### JEP 506 — Scoped Values *(final)*

**Description**  
`ScopedValue<T>` provides an immutable, per-thread (and per-virtual-thread), automatically
inherited binding for a value.
Unlike `ThreadLocal`, a scoped value:
- Is **immutable** once bound — no `set()` after initial binding.
- Is bound for a **bounded scope** (`ScopedValue.where(...).call(...)`) — automatically
  unbound when the scope exits, even if an exception is thrown.
- **Inherits automatically** into child virtual threads forked via `StructuredTaskScope`.

**When to use it**  
Propagating implicit context (request IDs, auth principals, trace IDs, tenant IDs)
through a call stack without parameter threading. Replaces `ThreadLocal` in server
applications that use virtual threads.

**Practical example**

```java
// 1. Declare a static constant (no initial value)
public final class RequestContext {
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
}

// 2. Bind it for the duration of a call chain
ScopedValue.where(RequestContext.USER_ID, "user-42")
           .call(() -> {
               processRequest();      // USER_ID readable anywhere in the call stack
               return null;
           });

// 3. Read it anywhere downstream (no parameter passing needed)
void processRequest() {
    String userId = RequestContext.USER_ID.get();        // throws NoSuchElementException if unbound
    String userId = RequestContext.USER_ID.orElse("anon"); // safe fallback
}
```

**Common pitfalls**
- Calling `.get()` outside a bound scope throws `NoSuchElementException`. Prefer `.orElse()` for defensive code.
- A scoped value cannot be mutated — you must re-bind with a new `where(...).call(...)` for nested overrides, which creates a *new* binding visible only in the inner scope.
- Do not store a scoped value's current value in a `ThreadLocal` — this breaks the inheritance model.

**Codebase examples**

[`RequestContext.java`](../src/main/java/com/cleancoders/postgresqltranscations/context/RequestContext.java):

```java
/// Holds per-request Scoped Values — immutable, inheritable-by-child-threads
/// values bound for the lifetime of one HTTP request.
public final class RequestContext {
    /// ScopedValue is in java.lang — no import required.
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
}
```

[`CorrelationIdFilter.java`](../src/main/java/com/cleancoders/postgresqltranscations/config/CorrelationIdFilter.java) — binding the scoped value for every HTTP request:

```java
@Override
public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
        throws IOException, ServletException {
    String correlationId = resolveCorrelationId((HttpServletRequest) req);
    ((HttpServletResponse) resp).setHeader("X-Correlation-Id", correlationId);

    // ScopedValue.where(key, value).call(callable) — binds CORRELATION_ID immutably
    // for the duration of this request's entire call-stack, including fallback methods.
    try {
        ScopedValue.where(RequestContext.CORRELATION_ID, correlationId)
                   .call(() -> { chain.doFilter(req, resp); return null; });
    } catch (IOException | ServletException e) {
        throw e;
    } catch (Exception e) {
        throw new ServletException("Unexpected error in filter chain", e);
    }
}
```

[`EmployeeService.java`](../src/main/java/com/cleancoders/postgresqltranscations/service/EmployeeService.java) — reading the correlation ID in every circuit-breaker fallback:

```java
EmployeeDTO findEmployeeFallback(Long id, Throwable t) {
    // Readable without any parameter threading — bound upstream by CorrelationIdFilter
    String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
    throw new ServiceUnavailableException(
            "Employee lookup temporarily unavailable. id=" + id
            + " correlationId=" + correlationId, t);
}
```

---

### JEP 505 — Structured Concurrency *(final)*

**Description**  
`StructuredTaskScope` provides a scoped, hierarchical model for running concurrent subtasks.
When the scope closes, it guarantees all subtasks have either completed or been cancelled.
This eliminates the "task leak" problem common with `ExecutorService`.

Key implementations:

| Class | Behaviour |
|-------|-----------|
| `StructuredTaskScope.ShutdownOnFailure` | Cancels all subtasks as soon as any one fails |
| `StructuredTaskScope.ShutdownOnSuccess` | Cancels remaining subtasks as soon as the first succeeds |

**When to use it**  
Fan-out / fan-in patterns: fetching data from multiple sources in parallel and waiting for
all (or the first successful) result. Replaces `CompletableFuture` chains for many use cases.

**Practical example**

```java
// Fan-out: call two services in parallel, fail fast if either fails
record UserAndOrder(User user, Order order) {}

UserAndOrder fetch(long userId, long orderId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<User>  userTask  = scope.fork(() -> userService.findById(userId));
        Subtask<Order> orderTask = scope.fork(() -> orderService.findById(orderId));

        scope.join()          // wait for both
             .throwIfFailed(); // propagate the first failure

        return new UserAndOrder(userTask.get(), orderTask.get());
    }
}
```

**Common pitfalls**
- Always use `StructuredTaskScope` in a `try-with-resources` block — failure to close causes resource leaks.
- Subtask results are only accessible *after* `scope.join()` — calling `.get()` before join throws.
- `ShutdownOnSuccess` is appropriate for "first-wins" patterns (e.g., querying multiple replicas); it cancels remaining tasks, not kills them.

---

### JEP 512 — Compact Source Files and Instance Main Methods *(final)*

**Description**  
Two related simplifications:
1. **Instance main method**: `main` no longer needs to be `static` or accept a `String[]` argument.
2. **Implicitly declared classes**: A source file with no explicit class declaration is treated as
   a class with the file's name, making single-file scripts far less boilerplate-heavy.

**When to use it**  
Quick scripts, educational code, exploratory prototypes, and small utilities that do not
need a full class declaration.

**Practical example**

```java
// HelloWorld.java — no class declaration, no static, no String[] args needed
void main() {
    System.out.println("Hello, Java 25!");
}
```

```java
// Or with an explicit class but an instance main
class Greeter {
    void main() {
        System.out.println("Hello from an instance main!");
    }
}
```

**Common pitfalls**
- Implicitly declared classes cannot be imported or referenced by other classes — they are top-level scripts only.
- The launcher searches for `main` in this priority order: `static void main(String[])` → `static void main()` → `instance void main(String[])` → `instance void main()`.

---

## Cross-Cutting Feature: Text Blocks (JEP 378 — Java 15, final)

Although introduced before Java 21, text blocks are heavily used in this codebase and deserve
explicit documentation.

**Description**  
`"""..."""` delimiters create multi-line string literals with automatic indentation
stripping and without the need to escape inner double-quotes or newlines.

**When to use it**  
SQL strings, JSON/XML fixtures in tests, multi-line log messages, Javadoc-embedded code samples.

**Practical example**

```java
// SQL query in a repository
@Query(value = """
        SELECT * FROM company.employee
        WHERE emp_salary > :salary
        ORDER BY emp_salary DESC
        """, nativeQuery = true)
List<Employee> findBySalaryGreaterThanNative(@Param("salary") BigDecimal salary);

// JSON fixture in a test
String patchBody = """
        [{"op":"replace","path":"/empName","value":"Updated Name"}]
        """;
```

**Common pitfalls**
- The closing `"""` position controls indentation stripping — align it with the content to strip leading whitespace.
- Text blocks include the final newline before `"""` unless you append `\` at the end of the last content line.

**Codebase example** — [`EmployeeControllerTest.java`](../src/test/java/com/cleancoders/postgresqltranscations/controller/EmployeeControllerTest.java):

```java
private String patchBody() {
    return """
            [{"op":"replace","path":"/empName","value":"Updated Name"}]""";
}
```

---

## Cross-Cutting Feature: `var` — Local-Variable Type Inference (JEP 286 — Java 10, final)

**Description**  
`var` instructs the compiler to infer the type of a local variable from its initializer,
reducing verbosity without removing type safety.

**When to use it**  
Local variables with obvious types (constructor calls, stream operations, `try-with-resources`).
Avoid `var` when the inferred type is not immediately clear from reading the right-hand side.

**Practical example**

```java
// Clear — type is obvious from the constructor
var employees = new ArrayList<EmployeeDTO>();

// Clear — type is obvious from the method name
var config = new HikariDataSource();

// Avoid — type of result is not obvious without checking the return type
var result = service.process(request); // what is result?
```

**Common pitfalls**
- `var` is a reserved type name, not a keyword — it can still be used as a variable/method name (not recommended).
- Cannot be used for fields, method parameters, or return types.

**Codebase example** — [`CorrelationIdFilter.java`](../src/main/java/com/cleancoders/postgresqltranscations/config/CorrelationIdFilter.java):

```java
var httpReq  = (HttpServletRequest)  req;
var httpResp = (HttpServletResponse) resp;
```

---

## Feature Summary Matrix

| Feature | JEP | First preview | Final | Used in this codebase |
|---------|-----|--------------|-------|-----------------------|
| Text Blocks | 378 | Java 13 | **Java 15** | ✅ `EmployeeControllerTest` |
| `var` type inference | 286 | — | **Java 10** | ✅ `CorrelationIdFilter` |
| Sealed Classes | 409 | Java 15 | **Java 21** | ✅ `EmployeeDomainException` |
| Record Patterns | 440 | Java 19 | **Java 21** | — |
| Pattern Matching for `switch` | 441 | Java 17 | **Java 21** | ✅ `GlobalExceptionHandler` |
| Virtual Threads | 444 | Java 19 | **Java 21** | ✅ `application.yml` |
| Sequenced Collections | 431 | — | **Java 21** | — |
| Unnamed Variables & Patterns | 456 | Java 21 | **Java 22** | — |
| Foreign Function & Memory API | 454 | Java 19 | **Java 22** | — |
| Stream Gatherers | 461/485 | Java 22 | **Java 24** | ✅ `EmployeeService` |
| Class-File API | 466/484 | Java 22 | **Java 24** | — |
| ZGC Generational Mode default | 490 | — | **Java 24** | — |
| Markdown Javadoc (`///`) | 467 | — | **Java 23** | ✅ Multiple classes |
| Module Import Declarations | 476/511 | Java 23 | **Java 25** | ✅ `EmployeeService`, `EmployeeController` |
| Scoped Values | 446/506 | Java 20 | **Java 25** | ✅ `RequestContext`, `CorrelationIdFilter`, `EmployeeService` |
| Structured Concurrency | 453/505 | Java 19 | **Java 25** | — |
| Compact Source Files | 512 | Java 21 | **Java 25** | — |

---

## Recommended Reading

- [JEP Index](https://openjdk.org/jeps/0) — authoritative spec for every JEP listed above
- [Project Loom](https://openjdk.org/projects/loom/) — Virtual Threads, Structured Concurrency, Scoped Values
- [Spring Boot Virtual Threads guide](https://docs.spring.io/spring-boot/reference/web/spring-mvc.html#web.spring-mvc.embedded-container.virtual-threads) — activation and configuration
- [Inside Java](https://inside.java) — tutorials and deep-dives from the OpenJDK team
