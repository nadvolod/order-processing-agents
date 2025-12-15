# Order Processing Agents

## Overview

This project implements a multi-agent workflow for processing orders.

**Requirements**: Java 24 or later

## Getting Started

### Prerequisites

Before running this project, ensure you have the following installed:
- Java Development Kit (JDK) 24 or later
- Maven or Gradle (depending on build system)

### Installation

1. Clone the repository
2. Navigate to the project directory
3. Build the project using Maven or Gradle

### Running the Application

```bash
# Using Maven
mvn clean install
mvn exec:java

# Using Gradle
gradle build
gradle run
```

## Architecture

This project follows a multi-agent architecture where different agents handle specific aspects of order processing:

- **Order Validation Agent**: Validates incoming orders
- **Inventory Agent**: Checks and manages inventory
- **Payment Agent**: Processes payments
- **Shipping Agent**: Handles shipping and delivery
- **Notification Agent**: Sends notifications to customers

## Development

### Project Structure

```
order-agents/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── orderprocessing/
│   │   │           ├── agents/
│   │   │           ├── models/
│   │   │           └── services/
│   │   └── resources/
│   └── test/
│       └── java/
└── pom.xml / build.gradle
```

### Building from Source

To build the project from source:

```bash
# Clone the repository
git clone https://github.com/nadvolod/order-processing-agents.git
cd order-processing-agents/order-agents

# Build with Maven
mvn clean package

# Or build with Gradle
gradle clean build
```

## Technology Stack

- **Language**: Java 24
- **Build Tool**: Maven/Gradle
- **Testing Framework**: JUnit 5
- **Logging**: SLF4J with Logback

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the terms specified in the LICENSE file.

## Contact

For questions or support, please open an issue on GitHub.
