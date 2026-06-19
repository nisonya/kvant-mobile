[![О проекте Kvant](https://img.shields.io/badge/Kvant-About%20project-2ea44f?style=for-the-badge&logo=github)](https://github.com/nisonya/kvant)
[![Скачать установщик](https://img.shields.io/badge/Releases-Download-blue?style=for-the-badge&logo=github)](https://github.com/nisonya/kvant-mobile/releases)

# Kvantroium

**Kvantroium** — мобильное Android-приложение для сотрудников образовательной организации. Работает с backend **Kvant Server** и даёт доступ к тем же данным, что и десктопный клиент: мероприятия, расписание, группы, посещаемость, документы и личный профиль.

## Назначение

Приложение предназначено для преподавателей и администраторов: просмотр и редактирование рабочих данных с телефона, push-напоминания о мероприятиях, где пользователь указан ответственным, и быстрый переход к карточке мероприятия из уведомления.

Подключение к серверу по HTTPS; адрес сервера и сессия задаются при входе. Поддерживается автообновление APK с сервера.

## Скриншоты

<p align="center">
  <img src="docs/screenshots/home.png" alt="Главный экран" width="260" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/events.png" alt="Мероприятия" width="260" />
</p>
<p align="center">
  <img src="docs/screenshots/notifications.png" alt="Напоминания" width="260" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/profile.png" alt="Профиль" width="260" />
</p>

<p align="center">
  <img src="docs/screenshots/home-dark.png" alt="Главный экран (тёмная тема)" width="260" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/events-detail.png" alt="Карточка мероприятия" width="260" />
</p>

## Основные разделы

- **Мероприятия** — организация и участие: список, фильтры, карточка, создание и редактирование
- **Напоминания** — сегодня/завтра и новые назначения, где вы ответственный
- **Расписание** — занятия по преподавателям и группам
- **Группы, ученики, посещаемость, пиксели** — учебная аналитика и журналы
- **Бронь помещений** — аренда аудиторий
- **Документы** — файлы организации
- **Профиль** — данные сотрудника, смена пароля, проверка обновлений, KPI по ссылке

## Уведомления

- **Утром (10:00–16:59):** push о сегодняшних и завтрашних мероприятиях (организация и участие; для участия — только если не отмечено «Участвовал»)
- **Вечером (17:00+):** push только о завтрашних
- **Постоянно:** новые мероприятия, где пользователь стал ответственным
- Работа в фоне через WorkManager; защита от дублей по ключам дедупликации

## Технологии

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **WorkManager** — фоновые проверки
- **Encrypted SharedPreferences** — сессия и настройки
- REST API Kvant Server (JSON, Bearer/cookie)
- minSdk 24, targetSdk 36

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

Готовый APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

Для release-сборки используйте **Build → Generate Signed Bundle / APK** в Android Studio.

## Требования

- Android Studio (актуальная версия с поддержкой compileSdk 36)
- JDK 11+
- Настроенный `local.properties` с путём к Android SDK

## Связанные репозитории

- [Kvant — основной проект](https://github.com/nisonya/kvant)
- [Releases — скачать APK](https://github.com/nisonya/kvant-mobile/releases)
