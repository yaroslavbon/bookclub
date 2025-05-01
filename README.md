# Book Club Management Application

A Spring Boot application for managing a book club, including books, members, ratings, and reading queue.

## Features

- Book management with metadata (title, author, etc.)
- Member management and authentication
- Reading queue with rotation
- Book rating system
- Book file storage (EPUB, PDF, MOBI)
- Theme toggling (light/dark mode)
- Google Books API integration for book info lookup

## Running the Application

### Development Mode

```
./gradlew bootRun
```

Default credentials:
- Username: admin
- Password: admin

### Setting Admin User

To create or update an admin user when starting the application:

```
./gradlew bootRun --args="--set-admin --admin-username=your_username --admin-password=your_password"
```

### Running Data Migrations

To run the data migration for completed books:

```
./gradlew bootRun --args="--run-migration"
```

You can combine both operations:

```
./gradlew bootRun --args="--run-migration --set-admin --admin-username=your_username --admin-password=your_password"
```

## Production Deployment

For production deployment, create a production-specific application.properties file and use the following command:

```
java -jar bookclub.jar --spring.profiles.active=prod
```

## Building

```
./gradlew build
```

This will create a runnable JAR file in the `build/libs` directory.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security
- Thymeleaf
- Bootstrap 5
- H2 Database
- Flyway Migrations
- Lombok