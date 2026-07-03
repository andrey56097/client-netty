<div align="center">
  <br/>
  <img src="https://raw.githubusercontent.com/netty/netty/4.1/misc/logo/netty-logo.png" alt="Netty" width="400"/>
  <br/>
  <h1>Netty Client</h1>
  <p><strong>Scalable TCP client application built with Spring Boot &amp; Netty</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Spring%20Boot-2.5.2-brightgreen?style=flat-square&logo=spring" alt="Spring Boot 2.5.2"/>
    <img src="https://img.shields.io/badge/Netty-4.1.65.Final-blue?style=flat-square&logo=netty" alt="Netty 4.1.65.Final"/>
    <img src="https://img.shields.io/badge/Java-11-orange?style=flat-square&logo=openjdk" alt="Java 11"/>
    <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License MIT"/>
    <img src="https://img.shields.io/badge/PRs-welcome-ff69b4?style=flat-square" alt="PRs Welcome"/>
  </p>

  <hr/>
</div>

## 📋 Table of Contents

- [About](#about)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Configuration](#configuration)
- [Built With](#built-with)
- [License](#license)

---

## 📖 About

**Netty Client** is a lightweight, non-blocking TCP client application that demonstrates how to build high-performance network clients using the [Netty](https://netty.io/) framework within a [Spring Boot](https://spring.io/projects/spring-boot) environment.

The client connects to a remote TCP server, sends structured `RequestData` payloads, and processes `ResponseData` replies through a custom Netty pipeline — all with automatic connection retry logic for resilience.

### ✨ Features

- ⚡ **Non-blocking I/O** — leverages Netty's event-driven architecture
- 🔄 **Automatic retry** — gracefully waits for the server with increasing delays (up to 10 attempts)
- 🧩 **Custom pipeline** — dedicated encoder/decoder for protocol handling
- 📦 **Spring Boot integration** — runs as a managed component with devtools hot-reload
- 🛡️ **Graceful shutdown** — proper resource cleanup via Netty's `EventLoopGroup`

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│              Netty Client App                │
│                                             │
│  ┌──────────────┐    ┌──────────────────┐   │
│  │  Spring Boot  │    │    Netty Core     │   │
│  │ (Application) │───▶│                  │   │
│  └──────────────┘    │  ┌────────────┐   │   │
│                      │  │  Bootstrap  │   │   │
│                      │  └──────┬─────┘   │   │
│                      │         │         │   │
│                      │  ┌──────▼──────┐  │   │
│                      │  │   Channel    │  │   │
│                      │  │  Pipeline    │  │   │
│                      │  │              │  │   │
│                      │  │ ┌──────────┐ │  │   │
│                      │  │ │RequestData│ │  │   │
│                      │  │ │ Decoder   │ │  │   │
│                      │  │ ├──────────┤ │  │   │
│                      │  │ │ResponseData│ │  │   │
│                      │  │ │ Encoder   │ │  │   │
│                      │  │ ├──────────┤ │  │   │
│                      │  │ │  Client   │ │  │   │
│                      │  │ │  Handler  │ │  │   │
│                      │  │ └──────────┘ │  │   │
│                      │  └──────────────┘  │   │
│                      └──────────────────┘   │
└─────────────────────┬───────────────────────┘
                      │
                      │ TCP (port 5002)
                      ▼
              ┌──────────────────┐
              │    TCP Server     │
              │  (localhost:5002) │
              └──────────────────┘
```

### Data Flow

1. **Startup** — Spring Boot launches `NettyClient` via `CommandLineRunner`
2. **Connection** — attempts to connect to `localhost:5002` with retry logic
3. **Handshake** — on successful connection, `ClientHandler.channelActive()` fires
4. **Send** — `RequestData` (int + string) is written and flushed through the pipeline
5. **Encode/Decode** — custom `ResponseDataEncoder` and `RequestDataDecoder` transform bytes ↔ objects
6. **Response** — server reply is printed to console, connection closes

---

## 📂 Project Structure

```
src/
├── main/
│   └── java/com/batsandrey/demo/
│       ├── DemoApplication.java          # Spring Boot entry point
│       ├── NettyClient.java              # Netty client with retry logic
│       ├── decoder/
│       │   └── RequestDataDecoder.java   # Inbound byte → RequestData
│       ├── encoder/
│       │   └── ResponseDataEncoder.java  # Outbound ResponseData → byte
│       ├── entity/
│       │   ├── request/
│       │   │   └── RequestData.java      # Request model (int + string)
│       │   └── response/
│       │       └── ResponseData.java     # Response model (int value)
│       └── handler/
│           └── ClientHandler.java        # Channel event handler
└── test/
    └── java/com/batsandrey/demo/
        └── DemoApplicationTests.java     # Spring context test
```

---

## ✅ Prerequisites

| Tool     | Version |
|----------|---------|
| ☕ Java   | 11+     |
| 🔨 Maven | 3.6+    |

---

## 🚀 Getting Started

### 1️⃣ Clone

```bash
git clone https://github.com/andriibats/client-netty.git
cd client-netty
```

### 2️⃣ Build

```bash
./mvnw clean package
```

> *If `mvnw` is not available, use:*
> ```bash
> mvn clean package
> ```

### 3️⃣ Run

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Or directly with Maven:

```bash
./mvnw spring-boot:run
```

> **Note:** The client will attempt to connect to `localhost:5002`. If no server is running, it will retry up to 10 times with increasing delays (2s → 4s → 6s → ...) and log a warning instead of crashing.

---

## 🎯 Usage

The client is designed to work with a TCP server listening on port `5002`. Once connected, it automatically:

1. Sends a `RequestData` object:
   - `intValue`: `123`
   - `stringValue`: `"all work and no play makes jack a dull boy"`
2. Waits for a `ResponseData` reply from the server
3. Prints the response to standard output
4. Closes the connection

### Example Log Output

```
WARN  [main] NettyClient: Connection to localhost:5002 failed (attempt 1/10). Retrying in 2000ms...
WARN  [main] NettyClient: Connection to localhost:5002 failed (attempt 2/10). Retrying in 4000ms...
INFO  [main] NettyClient: Connected to localhost:5002 on attempt 3
ResponseData(intValue=456)
```

---

## ⚙️ Configuration

Key constants are defined in `NettyClient.java`:

| Parameter        | Default     | Description                          |
|------------------|-------------|--------------------------------------|
| `HOST`           | `localhost` | Target server hostname               |
| `PORT`           | `5002`      | Target server port                   |
| `MAX_RETRIES`    | `10`        | Maximum connection attempts          |
| `BASE_DELAY_MS`  | `2000`      | Initial retry delay (× attempt number) |

> 💡 **Tip:** For production use, consider externalizing these values to `application.yml`.

---

## 🛠️ Built With

| Library                                                                 | Version         | Purpose                                   |
|-------------------------------------------------------------------------|-----------------|-------------------------------------------|
| [Spring Boot](https://spring.io/projects/spring-boot)                   | 2.5.2           | Application framework & dependency injection |
| [Netty](https://netty.io/)                                              | 4.1.65.Final    | Non-blocking network I/O framework         |
| [Lombok](https://projectlombok.org/)                                    | 1.18.30         | Boilerplate code reduction                 |
| [Maven](https://maven.apache.org/)                                      | —               | Build & dependency management              |

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. 🍴 Fork the project
2. 🌿 Create your feature branch (`git checkout -b feature/amazing-feature`)
3. 💾 Commit your changes (`git commit -m 'Add some amazing feature'`)
4. 📤 Push to the branch (`git push origin feature/amazing-feature`)
5. 🔀 Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Made with ❤️ by <a href="https://github.com/andriibats">andriibats</a>
</div>
