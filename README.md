# 🏋️ Smart Gym Management System

A Java-based desktop application developed to simplify and automate daily gym operations. The system uses **Java Swing, JDBC, and MySQL** to provide an organized platform for managing gym members, trainers, memberships, attendance, workout schedules, and payments.

## 📌 Project Overview

The Smart Gym Management System aims to reduce manual paperwork and make gym administration more efficient. It provides a centralized system where authorized users can manage gym-related information, track membership details, maintain payment records, and monitor daily activities.

This project is developed as an academic project for the **B.Tech Computer Science and Engineering** program.

## 🎯 Objectives

- Automate daily gym management activities.
- Maintain member and trainer records.
- Manage membership plans and validity.
- Track member attendance.
- Maintain payment and transaction records.
- Organize workout schedules.
- Generate useful reports.
- Reduce manual work and improve data accuracy.

## ✨ Key Features

- **User Authentication:** Secure login for authorized users.
- **Dashboard:** Centralized view of gym activities.
- **Member Management:** Add, update, view, and manage member details.
- **Trainer Management:** Maintain trainer information and records.
- **Membership Management:** Manage membership plans, fees, and validity.
- **Workout Management:** Maintain workout plans and schedules.
- **Attendance Management:** Record and monitor member attendance.
- **Payment Management:** Maintain payment and transaction records.
- **Reports:** Generate reports for gym operations.

*Note: The features listed above describe the intended system functionality. Update this section to reflect the modules implemented in your current project.*

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java (JDK 17) | Core application development |
| Java Swing | Graphical User Interface |
| AWT | GUI components and event handling |
| JDBC | Database connectivity |
| MySQL 8.x | Database management |
| Maven | Project and dependency management |
| IntelliJ IDEA | Development environment |

## 🏗️ System Architecture

The application follows a layered architecture to separate user interface, business logic, and database operations.

```text
          User
            |
            v
      Java Swing GUI
            |
            v
      Service Layer
            |
            v
        DAO Layer
            |
            v
        JDBC
            |
            v
       MySQL Database
```

### Architecture Components

- **GUI Layer:** Handles user interaction and displays information.
- **Service Layer:** Contains business logic and validation.
- **DAO Layer:** Handles database operations.
- **JDBC:** Connects the Java application to MySQL.
- **MySQL:** Stores gym-related information.

## 📂 Project Structure

```text
smart-gym/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/smartgym/
│   │   │       ├── model/
│   │   │       ├── dao/
│   │   │       ├── service/
│   │   │       ├── ui/
│   │   │       ├── util/
│   │   │       └── Main.java
│   │   │
│   │   └── resources/
│   │       └── db.properties.example
│   │
│   └── test/
│
├── pom.xml
├── README.md
└── .gitignore
```

*The structure above is a reference. Adjust it to match your actual source folders and class names.*

## ⚙️ Prerequisites

Before running the project, install:

1. Java Development Kit (JDK 17).
2. MySQL Server 8.x.
3. IntelliJ IDEA.
4. Maven (or use the Maven wrapper if included).

## 🚀 Installation and Setup

### 1. Clone the Repository

```bash
git clone [https://github.com/divyanshbuilds88/Gym-Management-System.git]
```

### 2. Open the Project

- Open IntelliJ IDEA.
- Select **Open**.
- Choose the cloned `smart-gym` folder.
- Allow IntelliJ to load Maven dependencies.

### 3. Create the MySQL Database

Open MySQL Workbench or the MySQL command line and create the database.

```sql
CREATE DATABASE smart_gym;
```

Import or execute the project's SQL schema file if one is included in the repository.

### 4. Configure Database Connection

Create your local `db.properties` file using `db.properties.example` as a reference.

Example configuration:

```properties
db.url=jdbc:mysql://localhost:3306/smart_gym
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

Replace the username and password with your own MySQL credentials.

**Important:** Never upload your actual database password or private credentials to GitHub.

### 5. Build the Project

Open the IntelliJ terminal and run:

```bash
mvn clean install
```

### 6. Run the Application

Run the main Java class from IntelliJ IDEA.

If your project uses a different main class, select the appropriate entry point.

## 💾 Database

The application uses MySQL to store and manage gym information.

The database is intended to maintain records related to:

- Users and login credentials
- Gym members
- Trainers
- Membership plans
- Workout schedules
- Attendance
- Payments

## 🎓 Concepts Applied

This project demonstrates practical implementation of concepts from the Java syllabus:

- Object-Oriented Programming (OOP)
- Classes and Objects
- Encapsulation
- Inheritance
- Polymorphism
- Abstraction and Interfaces
- Exception Handling
- Multithreading
- JDBC Connectivity
- MySQL Database Operations
- Java Swing GUI Programming
- Event Handling
- Collections and File Handling (where implemented)

## 🔮 Future Enhancements

- Online membership registration.
- QR-based attendance tracking.
- Email and SMS notifications.
- Automated membership expiry reminders.
- Advanced analytics and graphical reports.
- Mobile application integration.

## 👨‍💻 Project Information

**Project Name:** Smart Gym Management System  
**Project Type:** Academic / B.Tech CSE Project  
**Programming Language:** Java  
**Database:** MySQL  
**GUI Framework:** Java Swing  
**IDE:** IntelliJ IDEA

## 📄 License

This project is developed for educational and academic purposes. You may modify and improve it for learning and development.

---

⭐ If you find this project useful, consider giving the repository a star on GitHub.
