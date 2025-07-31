import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

data class EmployeeData(
    var id: String,
    var firstName: String,
    var lastName: String,
    var role: String
)

data class CheckInOutRecord(
    val employeeId: String,
    var checkIn: LocalDateTime,
    var checkOut: LocalDateTime? = null
)

open class EmployeeRepository {
    protected val employeeList = mutableListOf<EmployeeData>()
    private var idCounter = 1

    fun generateEmployeeId(): String = "E" + idCounter++.toString().padStart(3, '0')

    fun addEmployeeData(emp: EmployeeData) {
        employeeList.add(emp)
    }

    fun updateEmployeeData(id: String, newFirstName: String, newLastName: String, newRole: String) {
        val emp = employeeList.find { it.id == id }
        if (emp != null) {
            emp.firstName = newFirstName
            emp.lastName = newLastName
            emp.role = newRole
            println(" Employee updated.")
        } else println(" Employee not found.")
    }

    fun deleteEmployee(id: String) {
        val removed = employeeList.removeIf { it.id == id }
        if (removed) println(" Employee deleted.") else println(" Employee not found.")
    }

    fun getEmployeeById(id: String): EmployeeData? = employeeList.find { it.id == id }

    fun getAllEmployees(): List<EmployeeData> = employeeList
}

open class AttendanceRepository {
    protected val records = mutableListOf<CheckInOutRecord>()

    fun addRecord(record: CheckInOutRecord) {
        records.add(record)
    }

    fun getLastCheckIn(id: String): CheckInOutRecord? = records.findLast { it.employeeId == id && it.checkOut == null }

    fun completeRecord(id: String): Boolean {
        val rec = getLastCheckIn(id)
        return if (rec != null) {
            rec.checkOut = LocalDateTime.now()
            true
        } else false
    }

    fun getCompletedRecords(): List<CheckInOutRecord> = records.filter { it.checkOut != null }
}

class EmployeeManager : EmployeeRepository() {

    fun addEmployee() {
        println("=== Add New Employee ===")
        print("Enter First Name: ")
        val firstName = readLine()?.trim().orEmpty()
        print("Enter Last Name: ")
        val lastName = readLine()?.trim().orEmpty()
        print("Enter Role: ")
        val role = readLine()?.trim().orEmpty()

        val newId = generateEmployeeId()
        val newEmployee = EmployeeData(newId, firstName, lastName, role)
        addEmployeeData(newEmployee)

        println(" Employee added successfully!")
        println("Your Employee ID is: $newId (Please save this for Check-In/Check-Out)")
    }

    fun updateEmployee() {
        println("=== Update Employee ===")
        print("Enter Employee ID to update: ")
        val id = readLine()?.trim().orEmpty()
        print("Enter new First Name: ")
        val fn = readLine()?.trim().orEmpty()
        print("Enter new Last Name: ")
        val ln = readLine()?.trim().orEmpty()
        print("Enter new Role: ")
        val role = readLine()?.trim().orEmpty()
        updateEmployeeData(id, fn, ln, role)
    }

    fun displayAllEmployees() {
        println("\n=== Current Employees ===")
        if (employeeList.isEmpty()) println("No employees added yet.")
        else employeeList.forEach {
            println("${it.id}: ${it.firstName} ${it.lastName} - ${it.role}")
        }
    }

    fun deleteEmployeePrompt() {
        println("=== Delete Employee ===")
        print("Enter Employee ID to delete: ")
        val id = readLine()?.trim().orEmpty()
        deleteEmployee(id)
    }

    fun isValidEmployee(id: String): Boolean = employeeList.any { it.id == id }
}

class AttendanceTracker(private val empManager: EmployeeManager) : AttendanceRepository() {

    fun parseDateTimeInput(prompt: String): LocalDateTime {
        print("$prompt (yyyy-MM-dd HH:mm or hit Enter for now): ")
        val input = readLine()?.trim().orEmpty()
        return if (input.isBlank()) LocalDateTime.now()
        else try {
            LocalDateTime.parse(input, formatter)
        } catch (e: Exception) {
            println("Invalid format. Using current time.")
            LocalDateTime.now()
        }
    }

    fun checkInEmployee() {
        println("\n=== Employee Check-In ===")
        print("Enter your Employee ID: ")
        val id = readLine()?.trim().orEmpty()

        if (!empManager.isValidEmployee(id)) {
            println(" Invalid ID. Please add employee first.")
            return
        }

        if (getLastCheckIn(id) != null) {
            println(" Already checked in. Please check out first.")
            return
        }

        val now = parseDateTimeInput("Enter check-in date/time")
        addRecord(CheckInOutRecord(id, now))
        val emp = empManager.getEmployeeById(id)
        println(" ${emp?.firstName} ${emp?.lastName} checked in at ${now.format(formatter)}")
    }

    fun checkOutEmployee() {
        println("\n=== Employee Check-Out ===")
        print("Enter your Employee ID: ")
        val id = readLine()?.trim().orEmpty()

        val record = getLastCheckIn(id)

        if (record == null) {
            println("No active check-in found for this ID.")
            return
        }

        val outTime = parseDateTimeInput("Enter check-out date/time")
        record.checkOut = outTime
        val emp = empManager.getEmployeeById(id)
        println(" ${emp?.firstName} ${emp?.lastName} checked out at ${outTime.format(formatter)}")
    }

    fun showWorkingHours() {
        println("\n=== Working Hours Report ===")
        val completedRecords = getCompletedRecords()

        if (completedRecords.isEmpty()) {
            println("No completed attendance records.")
            return
        }

        completedRecords.forEach { rec ->
            val emp = empManager.getEmployeeById(rec.employeeId)
            val duration = Duration.between(rec.checkIn, rec.checkOut)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            println("👤 ${emp?.firstName} ${emp?.lastName} |  ${hours}h ${minutes}m |  In: ${rec.checkIn.format(formatter)} | Out: ${rec.checkOut?.format(formatter)}")
        }
    }
}

fun main() {
    val employeeManager = EmployeeManager()
    val attendanceTracker = AttendanceTracker(employeeManager)

    println("=== Welcome to the Employee Attendance System ===")

    loop@ while (true) {
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
            8. Exit
            --------------------------
        """.trimIndent()
        )
        print("Choose an option (1-8): ")
        when (readLine()?.trim()) {
            "1" -> employeeManager.addEmployee()
            "2" -> employeeManager.displayAllEmployees()
            "3" -> employeeManager.updateEmployee()
            "4" -> employeeManager.deleteEmployeePrompt()
            "5" -> attendanceTracker.checkInEmployee()
            "6" -> attendanceTracker.checkOutEmployee()
            "7" -> attendanceTracker.showWorkingHours()
            "8" -> {
                println(" Exiting...!")
                break@loop
            }
            else -> println(" Invalid choice. Please try again.")
        }
    }
}
