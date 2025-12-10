

# WeatherApp

- Uses **OpenWeather API** to fetch current weather.
- Has **Registration + Login** using **Room DB**.
- Shows **current weather** and **history** in **two tabs**.

---

## Features

1. **Registration & Sign In**
   - Register with **username, email, password, confirm password**.
   - Email must be **unique**.
   - Login with email + password.
   - If already logged in, app opens directly in Weather screen.

2. **Splash Screen**
   - Handles **location permission** and **GPS ON/OFF**.
   - If permission is denied / GPS is off:
     - Shows clear message and buttons to go to **App Settings** / **Location Settings**.
   - Only after permission + GPS are OK:
     - If logged in → go to **Weather screen**
     - Else → go to **Login screen**

3. **Weather Screen (2 Tabs)**
   - **Tab 1 – Current**
     - Current **city & country**
     - **Current temperature in Celsius**  
       e.g. `Current temperature in Celsius : 26 °C`
     - **Sunrise** and **Sunset** time.
     - Weather icon:
       - 🌧️ if raining  
       - 🌙 if after **6 PM**  
       - ☀️ otherwise
     - Shows error message + Retry buttons for:
       - Location error
       - GPS off
       - No internet / API failure
   - **Tab 2 – History**
     - List of all weather fetches (each time current weather is loaded).
     - Shows city, temp, description, and fetched time.

---

## OpenWeather API Key Setup
Please add your own key in `app/build.gradle.kts` inside `defaultConfig`: (config key to "OPEN_WEATHER_API_KEY") 
My Key : fb0c903cf3817263fc991407028800a0
```kotlin
defaultConfig {
    applicationId = "com.example.weatherapp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Put your OpenWeather API key here:
    buildConfigField(
        "String",
        "OPEN_WEATHER_API_KEY",
        "\"YOUR_OPEN_WEATHER_API_KEY_HERE\""
    )
}
