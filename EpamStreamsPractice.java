package infy;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * EPAM Java 8 Streams practice — fill in every TODO.
 * Pure Java 8: no records, no List.of / Map.of, no stream().toList().
 *
 * Eclipse: put this file in the default package (src/), right-click -> Run As -> Java Application.
 * Command line:  javac EpamStreamsPractice.java && java EpamStreamsPractice
 *
 * Each method returns null until you implement it. main() checks your answer
 * against the expected output and prints PASS / FAIL / TODO.
 *
 * Rules for interview practice:
 *   - Use streams only (no for-loops).
 *   - Try each one in under 5 minutes, without autocomplete.
 *   - After solving, say the time complexity out loud.
 *   - In Java 8 use .collect(Collectors.toList()), not .toList().
 */
public class EpamStreamsPractice {

    // ---------- Data model ----------
    static class Employee {
        private final int id;
        private final String name;
        private final String city;
        private final String dept;
        private final double salary;
        private final int age;

        Employee(int id, String name, String city, String dept, double salary, int age) {
            this.id = id; this.name = name; this.city = city;
            this.dept = dept; this.salary = salary; this.age = age;
        }
        public int getId()         { return id; }
        public String getName()    { return name; }
        public String getCity()    { return city; }
        public String getDept()    { return dept; }
        public double getSalary()  { return salary; }
        public int getAge()        { return age; }
        @Override public String toString() { return name + "(" + id + ")"; }
    }

    static class Student {
        private final String name;
        private final double grade;
        Student(String name, double grade) { this.name = name; this.grade = grade; }
        public String getName()  { return name; }
        public double getGrade() { return grade; }
        @Override public String toString() { return name; }
    }

    static class CartItem {
        private final String name;
        private final double price;
        private final int qty;
        CartItem(String name, double price, int qty) { this.name = name; this.price = price; this.qty = qty; }
        public String getName()  { return name; }
        public double getPrice() { return price; }
        public int getQty()      { return qty; }
        @Override public String toString() { return name; }
    }

    static final List<Employee> EMPLOYEES = Arrays.asList(
            new Employee(5, "Zoya",   "Noida",     "Finance", 88000, 41),
            new Employee(2, "Priya",  "Chennai",   "HR",      52000, 31),
            new Employee(8, "Bala",   "Noida",     "HR",      45000, 24),
            new Employee(1, "Arjun",  "Noida",     "IT",      75000, 28),
            new Employee(7, "Divya",  "Bangalore", "HR",      58000, 33),
            new Employee(3, "Kiran",  "Noida",     "IT",      98000, 35),
            new Employee(6, "Ravi",   "Chennai",   "IT",      61000, 26),
            new Employee(4, "Meena",  "Bangalore", "Finance", 67000, 29)
    );

    // =====================================================================
    // Q1. First non-repeating character in a string.  "swiss" -> 'w'
    // Hint: chars() -> groupingBy with a map that keeps insertion order.
    // =====================================================================
    static Character firstNonRepeatingChar(String s) {
        // TODO
    	
        Map<Character, Long> map = s.chars()
        		.mapToObj(ch -> (char) ch)
        		.collect(
        				Collectors.groupingBy(
        						Function.identity(), LinkedHashMap::new , Collectors.counting()
        						)
        				);
        return map.entrySet().stream().filter( x-> x.getValue()==1).map(x->x.getKey()).findFirst().orElse(null);
    }

    // =====================================================================
    // Q2. Filter palindromes (ignore case).
    // =====================================================================
    static List<String> filterPalindromes(List<String> words) {
        // TODO
        return words
        		.stream()
        		.filter(
        				word -> word.equals(
        						new StringBuilder(word).reverse()
        						))
        		.collect(Collectors.toList());
        		
    }

    // =====================================================================
    // Q3a. Count occurrence of each word in a sentence.
    // Q3b. Count occurrence of each character in a string.
    // =====================================================================
    static Map<String, Long> wordCount(String sentence) {
        // TODO
    	return Arrays.stream(sentence.trim().split("\\s+"))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));

    }

    static Map<Character, Long> charCount(String s) {
        // TODO
        return  s.chars()
                .mapToObj(ch -> (char) ch)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));
    }

    // =====================================================================
    // Q4. Employees from a given city, sorted by name in REVERSE alphabetical order.
    // =====================================================================
    static List<Employee> employeesInCityReverseByName(List<Employee> emps, String city) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q5a. Employee IDs divisible by 2 (ascending order of ID).
    // Q5b. Sort employees by ID ascending.
    // Q5c. Sort employees by ID descending.
    // =====================================================================
    static List<Integer> evenIds(List<Employee> emps) {
        // TODO
        return null;
    }

    static List<Employee> sortByIdAsc(List<Employee> emps) {
        // TODO
        return null;
    }

    static List<Employee> sortByIdDesc(List<Employee> emps) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q6a. Group employees by department.
    // Q6b. Count employees per department.
    // Q6c. Average salary per department.
    // =====================================================================
    static Map<String, List<Employee>> groupByDept(List<Employee> emps) {
        // TODO
        return null;
    }

    static Map<String, Long> countPerDept(List<Employee> emps) {
        // TODO
        return null;
    }

    static Map<String, Double> avgSalaryPerDept(List<Employee> emps) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q7. Highest-paid employee in each department (value = Employee, not Optional).
    // Hint: collectingAndThen(maxBy(...), Optional::get)  OR  toMap(..., BinaryOperator.maxBy(...))
    // =====================================================================
    static Map<String, Employee> highestPaidPerDept(List<Employee> emps) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q8a. Average grade of all students.
    // Q8b. Names of students whose grade is below the average.
    // =====================================================================
    static Double averageGrade(List<Student> students) {
        // TODO
        return null;
    }

    static List<String> studentsBelowAverage(List<Student> students) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q9. Find duplicate elements in a list.
    // Bonus: solve it two ways (Set.add trick, and groupingBy + counting).
    // =====================================================================
    static Set<Integer> findDuplicates(List<Integer> nums) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q10a. Second highest DISTINCT number.
    // Q10b. Second highest DISTINCT salary.
    // =====================================================================
    static Integer secondHighest(List<Integer> nums) {
        // TODO
        return null;
    }

    static Double secondHighestSalary(List<Employee> emps) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q11a. Reverse each word, keep word order.  "java streams" -> "avaj smaerts"
    // Q11b. Capitalize each word.                "java streams" -> "Java Streams"
    // =====================================================================
    static String reverseEachWord(String s) {
        // TODO
        return null;
    }

    static String capitalizeWords(String s) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q12a. Join strings with ", ".
    // Q12b. Flatten List<List<Integer>> into List<Integer>.
    // =====================================================================
    static String joinWithComma(List<String> items) {
        // TODO
        return null;
    }

    static List<Integer> flatten(List<List<Integer>> nested) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q13. Partition numbers into even (true) / odd (false).
    // =====================================================================
    static Map<Boolean, List<Integer>> partitionEvenOdd(List<Integer> nums) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q14a. Longest string in a list.
    // Q14b. Sum of squares of odd numbers.
    // =====================================================================
    static String longestString(List<String> words) {
        // TODO
        return null;
    }

    static Integer sumOfSquaresOfOdds(List<Integer> nums) {
        // TODO
        return null;
    }

    // =====================================================================
    // Q15a. Shopping-cart total = sum of price * qty.
    // Q15b. Same total, but apply 10% discount if total > 50000.
    // Q15c. Name of the item line contributing the most to the total.
    // =====================================================================
    static Double cartTotal(List<CartItem> cart) {
        // TODO
        return null;
    }

    static Double cartTotalWithDiscount(List<CartItem> cart) {
        // TODO
        return null;
    }

    static String mostExpensiveLine(List<CartItem> cart) {
        // TODO
        return null;
    }

    // =====================================================================
    // Test runner — no need to edit below this line
    // =====================================================================
    public static void main(String[] args) {
        List<Integer> oneToTen = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        List<Student> students = Arrays.asList(
                new Student("Asha", 82), new Student("Ben", 67), new Student("Chitra", 91),
                new Student("Dev", 58), new Student("Esha", 74));
        List<CartItem> cart = Arrays.asList(
                new CartItem("Laptop", 55000, 1), new CartItem("Mouse", 800, 2),
                new CartItem("Keyboard", 1500, 1));

        check("Q1  first non-repeating", firstNonRepeatingChar("swiss"), 'w');
        check("Q2  palindromes",
                filterPalindromes(Arrays.asList("madam", "java", "level", "Racecar", "stream", "noon")),
                Arrays.asList("madam", "level", "Racecar", "noon"));
        check("Q3a word count", wordCount("java stream java lambda stream java"),
                mapOf("java", 3L, "stream", 2L, "lambda", 1L));
        check("Q3b char count", charCount("banana"), mapOf('b', 1L, 'a', 3L, 'n', 2L));
        check("Q4  Noida reverse by name",
                names(employeesInCityReverseByName(EMPLOYEES, "Noida")),
                Arrays.asList("Zoya", "Kiran", "Bala", "Arjun"));
        check("Q5a even ids", evenIds(EMPLOYEES), Arrays.asList(2, 4, 6, 8));
        check("Q5b sort asc", ids(sortByIdAsc(EMPLOYEES)), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        check("Q5c sort desc", ids(sortByIdDesc(EMPLOYEES)), Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1));
        Map<String, List<Employee>> grouped = groupByDept(EMPLOYEES);
        check("Q6a group by dept (sizes)", grouped == null ? null :
                grouped.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())),
                mapOf("IT", 3, "HR", 3, "Finance", 2));
        check("Q6b count per dept", countPerDept(EMPLOYEES), mapOf("IT", 3L, "HR", 3L, "Finance", 2L));
        checkApprox("Q6c avg salary per dept", avgSalaryPerDept(EMPLOYEES),
                mapOf("IT", 78000.0, "HR", 51666.67, "Finance", 77500.0));
        Map<String, Employee> top = highestPaidPerDept(EMPLOYEES);
        check("Q7  highest paid per dept", top == null ? null :
                top.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName())),
                mapOf("IT", "Kiran", "HR", "Divya", "Finance", "Zoya"));
        checkApprox("Q8a average grade", averageGrade(students), 74.4);
        check("Q8b below average", studentsBelowAverage(students), Arrays.asList("Ben", "Dev", "Esha"));
        check("Q9  duplicates", findDuplicates(Arrays.asList(4, 7, 2, 4, 9, 7, 1, 4)),
                new HashSet<>(Arrays.asList(4, 7)));
        check("Q10a second highest", secondHighest(Arrays.asList(12, 45, 7, 45, 33, 9)), 33);
        check("Q10b second highest salary", secondHighestSalary(EMPLOYEES), 88000.0);
        check("Q11a reverse each word", reverseEachWord("java streams are powerful"),
                "avaj smaerts era lufrewop");
        check("Q11b capitalize words", capitalizeWords("java streams are powerful"),
                "Java Streams Are Powerful");
        check("Q12a join", joinWithComma(Arrays.asList("Java", "Spring", "Kafka")), "Java, Spring, Kafka");
        check("Q12b flatten",
                flatten(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5))),
                Arrays.asList(1, 2, 3, 4, 5));
        check("Q13 partition even/odd", partitionEvenOdd(oneToTen),
                mapOf(true, Arrays.asList(2, 4, 6, 8, 10), false, Arrays.asList(1, 3, 5, 7, 9)));
        check("Q14a longest string",
                longestString(Arrays.asList("kafka", "spring", "microservices", "jpa")), "microservices");
        check("Q14b sum squares of odds", sumOfSquaresOfOdds(oneToTen), 165);
        checkApprox("Q15a cart total", cartTotal(cart), 58100.0);
        checkApprox("Q15b cart total w/ discount", cartTotalWithDiscount(cart), 52290.0);
        check("Q15c most expensive line", mostExpensiveLine(cart), "Laptop");

        System.out.printf("%nScore: %d / %d passed (%d still TODO)%n", passed, total, todo);
    }

    // ---------- helpers ----------
    static int passed, total, todo;

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> mapOf(Object... kv) {
        Map<K, V> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((K) kv[i], (V) kv[i + 1]);
        return m;
    }

    static List<String> names(List<Employee> list) {
        return list == null ? null : list.stream().map(Employee::getName).collect(Collectors.toList());
    }

    static List<Integer> ids(List<Employee> list) {
        return list == null ? null : list.stream().map(Employee::getId).collect(Collectors.toList());
    }

    static void check(String label, Object actual, Object expected) {
        total++;
        if (actual == null) { todo++; System.out.printf("TODO  %s%n", label); return; }
        boolean ok = Objects.equals(actual, expected);
        if (ok) passed++;
        System.out.printf("%s  %s%n", ok ? "PASS" : "FAIL", label);
        if (!ok) System.out.printf("        expected: %s%n        actual:   %s%n", expected, actual);
    }

    static void checkApprox(String label, Double actual, double expected) {
        total++;
        if (actual == null) { todo++; System.out.printf("TODO  %s%n", label); return; }
        boolean ok = Math.abs(actual - expected) < 0.01;
        if (ok) passed++;
        System.out.printf("%s  %s%n", ok ? "PASS" : "FAIL", label);
        if (!ok) System.out.printf("        expected: %s%n        actual:   %s%n", expected, actual);
    }

    static void checkApprox(String label, Map<String, Double> actual, Map<String, Double> expected) {
        total++;
        if (actual == null) { todo++; System.out.printf("TODO  %s%n", label); return; }
        boolean ok = actual.keySet().equals(expected.keySet()) && expected.entrySet().stream()
                .allMatch(e -> Math.abs(actual.get(e.getKey()) - e.getValue()) < 0.01);
        if (ok) passed++;
        System.out.printf("%s  %s%n", ok ? "PASS" : "FAIL", label);
        if (!ok) System.out.printf("        expected: %s%n        actual:   %s%n", expected, actual);
    }
}