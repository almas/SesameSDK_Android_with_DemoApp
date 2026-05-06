# Sesame Local HTTP Server

Simple Node.js HTTP server to replace AWS IoT Core MQTT and API Gateway for local device control.

## Installation & Setup

```bash
npm install
npm start
```

Server will run on `http://localhost:3000`

## Quick Start with Express

For a fully-featured server, install Express:

```bash
npm install express body-parser cors
node server.js
```

## Supported Endpoints

All endpoints accept `appidentifyid` header for authentication.

### Device Management
- `POST /device` - Upload device keys
- `GET /device/list` - Get devices list
- `PUT /device` - Update device key
- `DELETE /device` - Delete device key

### Device Control
- `POST /device/v1/iot/sesame2/{device_id}` - Send lock/unlock commands
- `POST /device/v1/sesame5/{device_id}` - Register OS3 device

### Status & Monitoring
- `GET /device/v1/wifi_module/{device_id}/status` - Get device status
- `POST /device/v1/sesame5/{device_id}/battery` - Upload battery data
- `POST /device/v1/sesame5/{device_id}/fwVer` - Upload firmware version

### History
- `POST /device/v1/sesame2/historys` - Upload history logs

### Admin (debugging)
- `GET /admin/devices` - List all devices
- `GET /admin/history/{device_id}` - Get device history
- `DELETE /admin/devices/{device_id}` - Delete device

## Configuration

Set custom port:
```bash
PORT=8080 npm start
```

## Docker

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package.json .
RUN npm install
COPY . .
CMD ["npm", "start"]
EXPOSE 3000
```

Build and run:
```bash
docker build -t sesame-local-server .
docker run -p 3000:3000 sesame-local-server
```

## Android App Configuration

Point your app to this server in BuildConfig or preferences:
```
SERVER_ENDPOINT=http://192.168.1.100:3000
```

