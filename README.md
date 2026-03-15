# Vinyl Store

Интернет-магазин виниловых пластинок: **Java 21**, **Spring Boot 3.4**, **PostgreSQL**, **Thymeleaf**, **JDBC** (без JPA).

## Что нужно установить

1. **JDK 21** — [Adoptium](https://adoptium.net/) или Oracle JDK  
   Проверка: `java -version`
2. **Apache Maven 3.9+** — [maven.apache.org](https://maven.apache.org/download.cgi)  
   Проверка: `mvn -version`
3. **PostgreSQL 14+** — сервер должен быть запущен

## Подготовка базы данных

В psql или pgAdmin выполните:

```sql
CREATE DATABASE vinyl_store;
```

Логин и пароль по умолчанию в `application.yml`: пользователь `postgres`, пароль `0000`.  
Если у вас другие — задайте переменные окружения (см. ниже).

При первом запуске приложение само создаст таблицы (`src/main/resources/db/schema.sql`) и учётную запись администратора.

## Запуск для разработки (Windows)

Если `java` показывает версию **1.8** или `mvn` не найден — сначала настройте окружение (см. ниже).

### Вариант A — CMD (проще всего)

```cmd
cd C:\Users\Петр\Documents\GitHub\Vinyl
setup-env.cmd
run.cmd
```

`setup-env.cmd` настраивает Java 21 и Maven в **текущем** окне.  
`run.cmd` запускает приложение.

### Вариант B — PowerShell (одна строка, без скриптов)

```powershell
cd "C:\Users\Петр\Documents\GitHub\Vinyl"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:MAVEN_HOME = "$env:LOCALAPPDATA\Programs\apache-maven-3.9.6"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;" + $env:Path
java -version
mvn -version
mvn spring-boot:run
```

### Вариант C — PowerShell-скрипт (если политика блокирует `.ps1`)

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-env.ps1
mvn spring-boot:run
```

При необходимости укажите доступ к PostgreSQL:

```powershell
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "ваш_пароль"
$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/vinyl_store"
```

Откройте в браузере: **http://127.0.0.1:8080**

Остановка: `Ctrl+C` в том же окне терминала.

## Сборка и запуск на Tomcat 11

```powershell
mvn clean package -DskipTests
```

Файл `target\vinyl-store.war` скопируйте в каталог `webapps` вашего Tomcat 11.  
Запустите Tomcat и откройте: **http://localhost:8080/vinyl-store/**  
(порт и контекст зависят от настроек Tomcat).

## Учётные записи по умолчанию

| Роль | E-mail | Пароль |
|------|--------|--------|
| Администратор | `admin@vinylstore.local` | `Admin12345` |

После первого запуска подтвердите e-mail администратора по ссылке из письма (или из логов консоли, если SMTP не настроен).

Покупатель и продавец регистрируются через сайт (кнопки на главной).

## Почта (необязательно)

Без настройки SMTP ссылки подтверждения выводятся в **лог консоли** при запуске.

Для реальной отправки писем:

```powershell
$env:SMTP_HOST = "smtp.yandex.ru"
$env:SMTP_USER = "ваш@email.ru"
$env:SMTP_PASSWORD = "пароль_приложения"
$env:BASE_URL = "http://127.0.0.1:8080"
```

## Структура проекта

```
pom.xml
src/main/java/ru/vinyl/     — Java-код (web / service / repository)
src/main/resources/
  application.yml           — настройки
  db/schema.sql             — схема БД
  templates/                — HTML (Thymeleaf)
  static/                   — CSS, JS
```

## Возможные проблемы

- **«mvn не является командой»** — добавьте Maven в PATH или используйте полный путь к `mvn.cmd`.
- **Ошибка подключения к PostgreSQL** — проверьте, что служба PostgreSQL запущена, база `vinyl_store` создана, логин/пароль верны.
- **Порт 8080 занят** — в `application.yml` измените `server.port`.
