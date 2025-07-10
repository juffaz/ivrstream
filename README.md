# IVRStream

IVRStream is a lightweight Clojure middleware that provides a WebSocket-based Realtime API for integrating a voice bot with Cisco Call Center IVR. It proxies audio streams (base64-encoded) and TTS commands from the voice bot to Cisco IVR, and forwards call events (e.g., `call_started`, `call_ended`) from Cisco to the bot. Designed for low-latency speech-to-speech interactions, IVRStream serves as a drop-in replacement for Twilio in voice bot applications. This project includes a mock API for prototyping and an AsyncAPI specification for the WebSocket interface. It runs in a Docker container for easy setup and deployment.

## Features
- **WebSocket Realtime API**: Connects voice bots to Cisco IVR via `ws://localhost:8080/ws`.
- **Low-Latency Proxying**: Handles audio (`{ "event": "audio", "payload": "base64_audio" }`) and TTS commands (`{ "command": "say", "text": "Hello!" }`).
- **Cisco Integration**: Mock API for call events; ready for real Cisco Finesse/UCCX/UCCE integration.
- **AsyncAPI Documentation**: Defines the WebSocket API in `resources/asyncapi.yaml`.
- **Dockerized**: Runs in a container, no local Clojure/Java setup required.
- **Simple Logging**: All events and messages logged to the console.

## Prerequisites
- **Docker**: Installed and running (e.g., [Docker Desktop](https://www.docker.com/products/docker-desktop) for Windows/Mac or `docker.io` for Linux).
- **Node.js** (optional, for testing): To use `wscat` for WebSocket testing.
- **Cisco API** (for production): Access to Cisco Finesse/UCCX/UCCE API endpoint and token.

## Project Structure
```
ivrstream/
├── Dockerfile
├── project.clj
├── README.md
├── resources/
│   └── asyncapi.yaml
└── src/
    └── ivrstream/
        └── core.clj
```

- `core.clj`: Main middleware logic.
- `asyncapi.yaml`: WebSocket API specification.
- `project.clj`: Clojure dependencies.
- `Dockerfile`: Container setup.

## Setup
### 1. Clone or Create the Project
If you have a repository, clone it:
```bash
git clone <repository-url>
cd ivrstream
```

Or create the project manually:
- Create a directory: `mkdir ivrstream && cd ivrstream`.
- Copy the files: `project.clj`, `src/ivrstream/core.clj`, `resources/asyncapi.yaml`, `Dockerfile`, and `README.md`.

### 2. Build the Docker Image
```bash
docker build -t ivrstream .
```

### 3. Run the Container
```bash
docker run -p 8080:8080 ivrstream
```

The WebSocket server starts at `ws://localhost:8080/ws`. Logs appear in the console, including mock Cisco events every 5 seconds (e.g., `Simulated Cisco event: {"event":"call_started","call_id":"call-123","caller":"+9941234567"}`).

## Testing
1. **Install wscat** (WebSocket client):
   ```bash
   npm install -g wscat
   ```

2. **Connect to WebSocket**:
   ```bash
   wscat -c ws://localhost:8080/ws
   ```

3. **Observe Events**:
   - Receive mock Cisco events: `{"event":"call_started","call_id":"call-123","caller":"+9941234567","timestamp":1697051234567}`.

4. **Send Test Messages**:
   - Audio: `{"event":"audio","payload":"base64_audio_data"}`
   - TTS: `{"command":"say","text":"Hello, this is a test!"}`
   - Check Docker logs (`docker logs <container-id>`) for: `Sending to Cisco IVR: {:type "say" :text "Hello, this is a test!"}`.

## Integration with Cisco Call Center
The prototype uses a mock Cisco API. To connect to a real Cisco Finesse/UCCX/UCCE API:

1. **Obtain API Details**:
   - Get the API endpoint (e.g., `https://cisco-uccx/api/events`) and authentication token from your Cisco admin.

2. **Update Event Fetching**:
   Replace `simulate-cisco-events` in `src/ivrstream/core.clj`:
   ```clojure
   (require '[clj
