import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException



// --- Enums ---
enum class Role(val displayName: String) {
    DEVELOPER("Developer"), TESTER("Tester"),
    MANAGER("Manager"), DESIGNER("Designer");

    override fun toString(): String = displayName
}

enum class SupervisorRole(val displayName: String) {
    MANAGER("Manager"), CEO("CEO"), CTO("CTO");
    override fun toString(): String = displayName
}

// --- Utility ---
val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
val dateOnlyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Duration.toHourMinuteString(): String {
    val h = toHours()
    val m = toMinutes() % 60
    return "${h}h ${m}m"
}

// --- Data Classes ---
data class EmployeeData(
    var id: String,
    var firstName: String,
    var lastName: String,
    var role: Role,
    var reportTo: SupervisorRole
) {
    override fun toString(): String {
        return "ID: $id | Name: $firstName $lastName | Role: $role | Reports To: $reportTo"
    }
}

data class CheckInOutRecord(
    val employeeId: String,
    var checkIn: LocalDateTime,
    var checkOut: LocalDateTime? = null
) {
    override fun toString(): String {
        val checkOutStr = checkOut?.format(formatter) ?: "Still Checked-In"
        return "Employee ID: $employeeId | In: ${checkIn.format(formatter)} | Out: $checkOutStr"
    }
}

// --- Base Classes ---
open class EmployeeList {
    protected val employeeList = mutableListOf<EmployeeData>()
    private var idCounter = 1

    fun generateEmployeeId(): String = "E" + idCounter++.toString().padStart(3, '0')

    fun addEmployeeData(emp: EmployeeData) { employeeList.add(emp) }

    fun updateEmployeeData(id: String, newFirst: String, newLast: String, newRole: Role, newSupervisor: SupervisorRole) {
        val emp = employeeList.find { it.id == id }
        if (emp != null) {
            emp.firstName = newFirst
            emp.lastName = newLast
            emp.role = newRole
            emp.reportTo = newSupervisor
            println("Employee updated.")
        } else println("Employee not found.")
    }

    fun deleteEmployee(id: String) {
        val removed = employeeList.removeIf { it.id == id }
        if (removed) println("Employee deleted.") else println("Employee not found.")
    }

    fun getEmployeeById(id: String): EmployeeData? = employeeList.find { it.id == id }
}

open class AttendanceList {
    protected val records = mutableListOf<CheckInOutRecord>()

    fun addRecord(record: CheckInOutRecord) { records.add(record) }

    fun getLastCheckIn(id: String): CheckInOutRecord? =
        records.findLast { it.employeeId == id && it.checkOut == null }

    fun getCompletedRecords(): List<CheckInOutRecord> = records.filter { it.checkOut != null }

    fun getRecordsBetweenDates(from: LocalDate, to: LocalDate): List<CheckInOutRecord> {
        return getCompletedRecords().filter {
            val checkInDate = it.checkIn.toLocalDate()
            checkInDate >= from && checkInDate <= to
        }
    }
}

// --- Manager Classes ---
class EmployeeManager : EmployeeList() {
    private fun chooseRole(): Role {
        println("Select Role:")
        Role.entries.forEachIndexed { index, role -> println("${index + 1}. $role") }
        while (true) {
            print("Enter choice: ")
            val idx = readLine()?.toIntOrNull()
            if (idx != null && idx in 1..Role.entries.size) return Role.entries[idx - 1]
            println("Invalid choice.")
        }
    }

    private fun chooseSupervisor(): SupervisorRole {
        println("Select Supervisor:")
        SupervisorRole.entries.forEachIndexed { i, s -> println("${i + 1}. $s") }
        while (true) {
            print("Enter choice: ")
            val idx = readLine()?.toIntOrNull()
            if (idx != null && idx in 1..SupervisorRole.entries.size) return SupervisorRole.entries[idx - 1]
            println("Invalid choice.")
        }
    }

    fun addEmployee() {
        println("=== Add New Employee ===")
        print("Enter First Name: ")
        val firstName = readLine()?.trim().orEmpty()
        print("Enter Last Name: ")
        val lastName = readLine()?.trim().orEmpty()
        val role = chooseRole()
        val supervisor = chooseSupervisor()

        val id = generateEmployeeId()
        addEmployeeData(EmployeeData(id, firstName, lastName, role, supervisor))
        println(" Employee added! ID: $id")
    }

    fun updateEmployee() {
        println("=== Update Employee ===")
        print("Enter Employee ID to update: ")
        val id = readLine()?.trim().orEmpty()

        val emp = getEmployeeById(id)
        if (emp == null) {
            println("Employee not found.")
            return
        }

        print("First Name (${emp.firstName}): ")
        val first = readLine().takeIf { !it.isNullOrBlank() } ?: emp.firstName
        print("Last Name (${emp.lastName}): ")
        val last = readLine().takeIf { !it.isNullOrBlank() } ?: emp.lastName
        val role = chooseRole()
        val supervisor = chooseSupervisor()

        updateEmployeeData(id, first, last, role, supervisor)
    }

    fun deleteEmployeePrompt() {
        println("=== Delete Employee ===")
        print("Enter ID: ")
        val id = readLine()?.trim().orEmpty()
        deleteEmployee(id)
    }

    fun displayAllEmployees() {
        println("=== Employees ===")
        if (employeeList.isEmpty()) println("No records.")
        else employeeList.forEach { println(it) }
    }

    fun isValidEmployee(id: String): Boolean = employeeList.any { it.id == id }
}

class AttendanceTracker(private val empManager: EmployeeManager) : AttendanceList() {

    fun parseDateTimeInput(prompt: String): LocalDateTime {
        print("$prompt (yyyy-MM-dd HH:mm or Enter for now): ")
        val input = readLine()?.trim().orEmpty()
        return if (input.isBlank()) LocalDateTime.now()
        else try {
            LocalDateTime.parse(input, formatter)
        } catch (e: DateTimeParseException) {
            println(" Invalid format. Using current time.$e")
            LocalDateTime.now()
        }
    }

    fun parseDateInput(prompt: String): LocalDate {
        while (true) {
            print("$prompt (yyyy-MM-dd): ")
            val input = readLine()?.trim().orEmpty()
            try {
                return LocalDate.parse(input, dateOnlyFormatter)
            } catch (e: DateTimeParseException) {
                println("Invalid date. Try again.$e")
            }
        }
    }

    fun checkInEmployee() {
        println("=== Check-In ===")
        print("Enter Employee ID: ")
        val id = readLine()?.trim().orEmpty()

        if (!empManager.isValidEmployee(id)) {
            println("Invalid ID.")
            return
        }
        if (getLastCheckIn(id) != null) {
            println("Already checked in!")
            return
        }

        val now = parseDateTimeInput("Check-In Time")
        addRecord(CheckInOutRecord(id, now))
        val emp = empManager.getEmployeeById(id)
        println(" ${emp?.firstName} ${emp?.lastName} checked in at ${now.format(formatter)}")
    }

    fun checkOutEmployee() {
        println("=== Check-Out ===")
        print("Enter Employee ID: ")
        val id = readLine()?.trim().orEmpty()

        val rec = getLastCheckIn(id)
        if (rec == null) {
            println("No active check-in.")
            return
        }

        val out = parseDateTimeInput("Check-Out Time")
        rec.checkOut = out
        val emp = empManager.getEmployeeById(id)
        println(" ${emp?.firstName} ${emp?.lastName} checked out at ${out.format(formatter)}")
    }

    fun showWorkingHours() {
        println("=== Working Hours Report ===")
        val complete = getCompletedRecords()
        if (complete.isEmpty()) {
            println("No records.")
            return
        }

        complete.forEach {
            val emp = empManager.getEmployeeById(it.employeeId)
            val dur = Duration.between(it.checkIn, it.checkOut)
            println("${emp?.firstName} ${emp?.lastName} | ${dur.toHourMinuteString()} | In: ${it.checkIn.format(formatter)} | Out: ${it.checkOut?.format(formatter)}")
        }
    }

    fun showCurrentlyCheckedIn() {
        println("=== Checked-In Now ===")
        val active = records.filter { it.checkOut == null }
        if (active.isEmpty()) println("No one is checked in.")
        else active.forEach {
            val emp = empManager.getEmployeeById(it.employeeId)
            println("${emp?.firstName} ${emp?.lastName} | In: ${it.checkIn.format(formatter)}")
        }
    }

    fun showRecordsBetweenDates() {
        println("=== Records Between Dates ===")
        val from = parseDateInput("From Date")
        val to = parseDateInput("To Date")

        val filtered = getRecordsBetweenDates(from, to)
        if (filtered.isEmpty()) {
            println("No records found.")
            return
        }

        filtered.forEach {
            val emp = empManager.getEmployeeById(it.employeeId)
            val duration = Duration.between(it.checkIn, it.checkOut)
            println("${emp?.firstName} ${emp?.lastName} | ${duration.toHourMinuteString()} | In: ${it.checkIn.format(formatter)} | Out: ${it.checkOut?.format(formatter)}")
        }
    }
}

// --- Main ---
fun main() {
    val empManager = EmployeeManager()
    val tracker = AttendanceTracker(empManager)

    println("=== Welcome to Employee Attendance System ===")
    while (true) {
        println(
            """
            --------------------------
            1. Add Employee
            2. View All Employees
            3. Update Employee
            4. Delete Employee
            5. Check-In
            6. Check-Out
            7. Show Working Hours
            8. Show Currently Checked-In
            9. Show Records Between Dates
            10. Exit
            --------------------------
        """.trimIndent()
        )
        print("Choose (1-10): ")
        when (readLine()?.trim()) {
            "1" -> empManager.addEmployee()
            "2" -> empManager.displayAllEmployees()
            "3" -> empManager.updateEmployee()
            "4" -> empManager.deleteEmployeePrompt()
            "5" -> tracker.checkInEmployee()
            "6" -> tracker.checkOutEmployee()
            "7" -> tracker.showWorkingHours()
            "8" -> tracker.showCurrentlyCheckedIn()
            "9" -> tracker.showRecordsBetweenDates()
            "10" -> {
                println("Exiting...")
                return
            }
            else -> println("Invalid choice.")
        }
    }
}
