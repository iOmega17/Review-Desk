# Android Remote Automation System

A real-time Android remote control system using **SignalR** for bidirectional communication between a .NET backend and an Android app with AccessibilityService.

## 🎯 Features

- **Real-time Communication**: SignalR-based client-server architecture
- **Remote Automation**: Control Android devices from a central server
- **Multiple Actions**: Click, Type, Scroll, Navigate, Open Apps
- **Text & Coordinate Based**: Target elements by text or screen coordinates
- **Automatic Reconnection**: SignalR handles connection drops gracefully

## 📁 Project Structure

```
Review-Desk/
├── src/
│   ├── backend/              # .NET 10 Backend
│   │   └── ReviewDesk.Api/
│   │       ├── Program.cs
│   │       ├── appsettings.json
│   │       └── ReviewDesk.Api.csproj
│   └── android/              # Android App
│       └── ReviewDesk/
│           ├── app/
│           │   ├── src/main/
│           │   │   ├── java/com/reviewdesk/android/
│           │   │   │   ├── MainActivity.java
│           │   │   │   └── MyAccessibilityService.java
│           │   │   ├── res/
│           │   │   └── AndroidManifest.xml
│           │   └── build.gradle
│           ├── build.gradle
│           └── settings.gradle
└── README.md
```

## 🚀 Getting Started

### Prerequisites

**Backend:**
- .NET 8 SDK
- Visual Studio 2022 or VS Code

**Android:**
- Android Studio Hedgehog or later
- Android device or emulator (Min SDK 24 / Android 7.0)
- Java 8+

### Backend Setup

1. **Navigate to the backend directory:**
   ```bash
   cd src/backend/ReviewDesk.Api
   ```

2. **Restore dependencies:**
   ```bash
   dotnet restore
   ```

3. **Run the backend:**
   ```bash
   dotnet run
   ```

4. **The server will start on `http://0.0.0.0:5000`**

5. **Find your PC's IP address:**
   - **Windows:** Open Command Prompt and run `ipconfig` (look for IPv4 Address)
   - **Linux/Mac:** Run `ifconfig` or `ip addr show`

6. **Test the backend:**
   ```bash
   curl http://localhost:5000
   ```

### Android Setup

1. **Open the Android project in Android Studio:**
   - Open `src/android/ReviewDesk` folder

2. **Update the Server URL:**
   - Open `MyAccessibilityService.java`
   - Change the default IP in line:
     ```java
     private String serverUrl = "http://192.168.1.100:5000/androidHub";
     ```
   - Replace `192.168.1.100` with your PC's IP address

3. **Build and install the app:**
   - Connect your Android device via USB (enable USB Debugging)
   - OR use an Android emulator
   - Click **Run** in Android Studio

4. **Enable Accessibility Service:**
   - Open the ReviewDesk app
   - Tap "Enable Accessibility Service" button
   - Find "ReviewDesk" in the list
   - Toggle it **ON**
   - Grant permissions when prompted

5. **Verify Connection:**
   - Return to the app
   - Check that "Service Status: ENABLED" is shown
   - Check that "Connection: CONNECTED" is shown

## 🎮 Usage

### Command API

Send commands to Android devices via HTTP GET:

```
http://<SERVER_IP>:5000/api/command?action=<ACTION>&targetType=<TYPE>&targetValue=<VALUE>&extraData=<DATA>
```

### Available Actions

#### 1. CLICK (by text)
```bash
curl "http://localhost:5000/api/command?action=CLICK&targetType=TEXT&targetValue=Login"
```

#### 2. CLICK (by coordinates)
```bash
curl "http://localhost:5000/api/command?action=CLICK&targetType=COORDS&targetValue=500,1000"
```

#### 3. TYPE (into focused field)
```bash
curl "http://localhost:5000/api/command?action=TYPE&extraData=mypassword123"
```

#### 4. SCROLL
```bash
curl "http://localhost:5000/api/command?action=SCROLL&targetValue=DOWN"
curl "http://localhost:5000/api/command?action=SCROLL&targetValue=UP"
```

#### 5. HOME (press home button)
```bash
curl "http://localhost:5000/api/command?action=HOME"
```

#### 6. BACK (press back button)
```bash
curl "http://localhost:5000/api/command?action=BACK"
```

#### 7. RECENTS (open recent apps)
```bash
curl "http://localhost:5000/api/command?action=RECENTS"
```

#### 8. OPEN_APP (by package name)
```bash
curl "http://localhost:5000/api/command?action=OPEN_APP&targetValue=com.android.chrome"
```

### Command JSON Schema

**Server → Android:**
```json
{
  "action": "CLICK",
  "targetType": "TEXT",
  "targetValue": "Login",
  "extraData": ""
}
```

**Android → Server (Status):**
```json
{
  "status": "SUCCESS",
  "message": "Clicked button 'Login'",
  "deviceTime": "12:00:01"
}
```

## 🔧 Configuration

### Backend Configuration

Edit `appsettings.json` to change the listening port:

```json
{
  "Kestrel": {
    "Endpoints": {
      "Http": {
        "Url": "http://0.0.0.0:5000"
      }
    }
  }
}
```

### Android Configuration

Update server URL in the app:
- Enter the URL in the text field on the main screen
- Format: `http://<YOUR_PC_IP>:5000/androidHub`
- The app will reconnect automatically

## 🛠️ Troubleshooting

### Android App Not Connecting

1. **Check Network:**
   - Ensure Android device and PC are on the same WiFi network
   - Ping the PC from Android to verify connectivity

2. **Check Firewall:**
   - Windows: Allow port 5000 in Windows Firewall
   - Linux: `sudo ufw allow 5000`

3. **Verify Server URL:**
   - Use your PC's **local IP** (192.168.x.x), not 127.0.0.1
   - Include the port: `:5000`

4. **Check Accessibility Service:**
   - Settings → Accessibility → ReviewDesk → **ON**
   - If it keeps turning off, check battery optimization settings

### Commands Not Working

1. **Element Not Found:**
   - Text searches are case-sensitive
   - Make sure the element is visible on screen

2. **Coordinates Not Working:**
   - Use correct format: `500,1000` (no spaces)
   - Coordinates must be within screen bounds

3. **Typing Not Working:**
   - Ensure a text field is focused first
   - Use CLICK to focus on the text field before TYPE

## 📱 Finding App Package Names

To open apps, you need their package names:

```bash
# List all installed packages on Android
adb shell pm list packages

# Find a specific app
adb shell pm list packages | grep chrome
```

Common package names:
- Chrome: `com.android.chrome`
- Gmail: `com.google.android.gm`
- Settings: `com.android.settings`
- WhatsApp: `com.whatsapp`

## 🔒 Security Notes

⚠️ **This is a development/testing configuration:**

- CORS is set to allow all origins
- No authentication is implemented
- Network traffic is unencrypted (HTTP)

**For production use:**
- Add authentication/authorization
- Use HTTPS/WSS
- Restrict CORS to specific origins
- Implement rate limiting
- Add device registration/whitelisting

## 🐛 Debugging

### View Backend Logs
```bash
cd src/backend/ReviewDesk.Api
dotnet run
```

### View Android Logs
```bash
adb logcat -s MyAccessibilityService:I AndroidHub:I
```

### Test SignalR Connection
Use a SignalR client library or browser console to test connection:
```javascript
const connection = new signalR.HubConnectionBuilder()
    .withUrl("http://192.168.1.100:5000/androidHub")
    .build();

connection.start();
```

## 📄 License

This project is provided as-is for educational and development purposes.

## 🤝 Contributing

Feel free to submit issues and enhancement requests!
