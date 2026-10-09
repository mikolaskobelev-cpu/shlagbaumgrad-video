# Live Camera (Android)

A starter Android app for viewing an RTSP camera using LibVLC. Includes landscape/fullscreen playback, low-latency tuning, and exponential reconnect attempts.

## Important security note

- Do **not** hardcode camera credentials in source code or share them in chat.
- The RTSP address is entered locally on the phone. This starter does not upload it to a server.
- For viewing from anywhere, the safest approach is a VPN into the camera's local network (e.g. WireGuard or a router VPN) or a properly secured RTSP-to-WebRTC relay with authentication.
- Avoid exposing the camera's RTSP port (usually 554) directly to the public internet. RTSP over plain `rtsp://` is not encrypted; use a VPN or a secure tunnel.
- The app does not guarantee connectivity: the camera/router must be reachable through your VPN/relay, and firewall/NAT rules must be configured safely.

## Requirements

- Android Studio (recent version)
- Android SDK 35
- JDK 17

## Build

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the debug APK on your Android phone.
5. Enter the camera's RTSP URL in the app and tap **Connect / reconnect**.

## Notes

- If latency is too high or the stream stutters, increase `--network-caching=150` to 300–500.
- If playback fails on a particular camera, try removing `--rtsp-tcp` or check the camera's supported RTSP transport.
- This is a starter project, not a prebuilt APK. The current environment did not build/sign an Android APK.


## Build an APK in GitHub Actions (without installing Android Studio locally)

1. Create a GitHub repository and upload the contents of this project to the repository (the `.github/workflows/android.yml` file must be included).
2. Open the repository's **Actions** tab.
3. Select **Build Android APK**, then click **Run workflow** (or push to the `main` branch).
4. When the run finishes successfully, open the run and download the **LiveCamera-debug-apk** artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android phone.

The cloud build is not run automatically from this chat. You must upload the project to your own GitHub account and trigger the workflow. A debug APK is for direct installation/testing, not Play Store publication.


## Брендинг

- Название приложения: **Шлагбаумград видео**
- Цвета интерфейса: чёрный, белый, оранжевый
- Логотип: `app/src/main/res/drawable/shlagbaumgrad_logo.png`

Перед выпуском рекомендуется подключить логотип как launcher icon через Android adaptive icon resources. В этом проекте изображение логотипа включено как брендовый ресурс.
