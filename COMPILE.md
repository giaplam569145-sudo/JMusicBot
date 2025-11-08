# How to Compile JMusicBot

This guide provides step-by-step instructions on how to compile JMusicBot from the source code.

## Prerequisites

Before you begin, ensure you have the following software installed on your system:

*   **Git:** To clone the repository.
*   **Java Development Kit (JDK) 11:** To build the Java project.
*   **Apache Maven:** To manage dependencies and run the build process.

## Compilation Steps

### 1. Clone the Repository

Open a terminal or command prompt and use the following command to clone the repository to your local machine:

```bash
git clone https://github.com/giaplam569145-sudo/JMusicBot.git
```

### 2. Navigate to the Project Directory

Change your current directory to the newly cloned `JMusicBot` folder:

```bash
cd JMusicBot
```

### 3. Compile with Maven

Run the `package` command to compile the source code, download dependencies, and package the application into a single `.jar` file.

```bash
mvn package
```

This process may take a few minutes to complete, especially on the first run as Maven downloads all the necessary dependencies.

### 4. Locate the JAR File

Once the build is successful, you will find the executable `.jar` file in the `target` directory. The file will be named `JMusicBot-Snapshot-All.jar`.

You can now move this file to a different location and run it. Remember to create a `config.txt` file in the same directory as the `.jar` file before starting the bot.
