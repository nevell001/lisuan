@echo off
setlocal

echo ========================================
echo   Cashier System Installation
echo ========================================
echo.

set APP_VERSION=2.2.1
set DB_TYPE=none
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=cashier_system
set DB_USERNAME=root
set DB_PASSWORD=RootPassword123!

set JAR_PATH=target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar
if exist %JAR_PATH% goto :ask_recompile
goto :check_java

:ask_recompile
echo [Warning] Detected existing compiled files
set /p REPLY="Recompile? (y/N): "
if /i "%REPLY%"=="y" goto :do_recompile
echo [Skip] Skipping compilation
goto :select_db_option

:do_recompile
echo [Clean] Cleaning old files...
call mvn clean >nul 2>&1

:check_java
echo [1/8] Checking Java environment...
where java >nul 2>&1
if errorlevel 1 goto :no_java

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [Info] Java version: %JAVA_VERSION%

where javac >nul 2>&1
if errorlevel 1 goto :no_javac
echo [Info] JDK compiler found
set HAS_JAVAC=true
goto :javac_done

:no_javac
echo [Warning] JDK compiler not found
echo [Note] Maven will use bundled compiler
set HAS_JAVAC=false

:javac_done
echo [Done] Java environment check passed
echo.
goto :check_maven

:no_java
echo [Error] Java runtime not found!
echo Please install JDK 17 or higher
pause
exit /b 1

:check_maven
echo [2/8] Checking Maven environment...
where mvn >nul 2>&1
if errorlevel 1 goto :no_maven

for /f "tokens=3" %%i in ('mvn -version 2^>^&1 ^| findstr "Apache MySQL"') do set MAVEN_VERSION=%%i
echo [Info] Maven version: %MAVEN_VERSION%
echo [Done] Maven environment check passed
echo.
goto :create_dirs

:no_maven
echo [Error] Maven not found!
echo Please install Maven 3.8 or higher
pause
exit /b 1

:create_dirs
echo [3/8] Creating necessary directories...
if not exist config mkdir config
if not exist data mkdir data
if not exist logs mkdir logs
if not exist docker\mysql-init mkdir docker\mysql-init
if not exist docker\mysql-backup mkdir docker\mysql-backup
echo [Done] Directories created
echo.

echo [4/8] Checking configuration files...
if exist config\database.properties goto :check_jvm_config
echo [Create] Creating database config file
copy config\database.properties.example config\database.properties >nul
echo [Tip] Please edit config\database.properties

:check_jvm_config
if exist config\jvm.config goto :config_done
echo [Create] Creating JVM config file
copy config\jvm.config.example config\jvm.config >nul

:config_done
echo [Done] Configuration files checked
echo.

echo [5/8] Downloading Maven dependencies...
echo [Tip] First installation may take a while...
call mvn dependency:resolve
if errorlevel 1 goto :dep_error
echo [Done] Dependencies downloaded
echo.
goto :compile

:dep_error
echo [Error] Dependency download failed
echo Please check network or Maven configuration
pause
exit /b 1

:compile
echo [6/8] Compiling project...
call mvn clean package -DskipTests
if errorlevel 1 goto :compile_error
echo [Done] Project compiled
echo.
goto :select_db_option

:compile_error
echo [Error] Compilation failed
echo.
echo [Debug Info]
echo   Java version: %JAVA_VERSION%
echo.
pause
exit /b 1

:select_db_option
echo [7/8] Database Installation
echo.
echo Please select database installation option:
echo   1 - Install Docker Desktop (Recommended)
echo   2 - Use existing local MySQL
echo.
set /p DB_CHOICE="Enter option (1/2, default=1): "
if "%DB_CHOICE%"=="" set DB_CHOICE=1

if "%DB_CHOICE%"=="1" goto :select_docker
if "%DB_CHOICE%"=="2" goto :select_local
echo [Warning] Invalid option, defaulting to Docker
goto :select_docker

:select_docker
set DB_TYPE=docker
echo [Info] Selected: Install Docker Desktop
goto :check_db_type

:select_local
set DB_TYPE=local
echo [Info] Selected: Use existing local MySQL
goto :check_db_type

:check_db_type
echo.

if "%DB_TYPE%"=="docker" goto :docker_setup
if "%DB_TYPE%"=="local" goto :local_setup
goto :database_config

:docker_setup
echo [Docker] Checking Docker installation...
where docker >nul 2>&1
if errorlevel 1 goto :no_docker

echo [Docker] Checking Docker Compose...
docker-compose --version >nul 2>&1
if errorlevel 1 goto :no_docker_compose

echo [Docker] Starting MySQL container...
docker-compose up -d mysql
if errorlevel 1 goto :docker_start_failed

echo [Docker] MySQL container started successfully
echo [Docker] Waiting for MySQL to be ready...
timeout /t 10 /nobreak >nul

echo [Docker] Importing sample data...
docker exec cashier-mysql mysql -uroot -p%DB_PASSWORD% --default-character-set=utf8mb4 %DB_NAME% < docker\mysql-init\03-sample-data.sql 2>nul
echo [Done] Database initialization completed
echo [Note] Tables will be created automatically when you start the application
echo.
goto :write_docker_config

:no_docker
echo [Info] Docker Desktop not installed yet
echo.
echo Please follow these steps:
echo   1. Download Docker Desktop from: https://www.docker.com/products/docker-desktop
echo   2. Install Docker Desktop
echo   3. Restart this script and select option 1
echo   4. Or proceed with local MySQL setup (select option 2)
echo.
set /p DOCKER_RETRY="Retry after installing Docker? (y/N): "
if /i "%DOCKER_RETRY%"=="y" goto :docker_setup
set DB_TYPE=none
goto :database_config

:no_docker_compose
echo [Error] Docker Compose not found!
echo Please ensure Docker Desktop is installed and running
echo.
set DB_TYPE=none
goto :database_config

:docker_start_failed
echo [Warning] Failed to start MySQL container
echo.
set DB_TYPE=none
goto :database_config

:local_setup
echo [Local MySQL] Configuring local MySQL connection...
echo.
echo Please enter your MySQL connection details:
echo.
set /p DB_HOST_INPUT="MySQL Host (default=localhost): "
if not "%DB_HOST_INPUT%"=="" set DB_HOST=%DB_HOST_INPUT%

set /p DB_PORT_INPUT="MySQL Port (default=3306): "
if not "%DB_PORT_INPUT%"=="" set DB_PORT=%DB_PORT_INPUT%

set /p DB_USERNAME_INPUT="MySQL Username (default=root): "
if not "%DB_USERNAME_INPUT%"=="" set DB_USERNAME=%DB_USERNAME_INPUT%

set /p DB_PASSWORD_INPUT="MySQL Password: "
if not "%DB_PASSWORD_INPUT%"=="" set DB_PASSWORD=%DB_PASSWORD_INPUT%

echo.
echo [Local MySQL] Testing connection...
where mysql >nul 2>&1
if errorlevel 1 goto :no_mysql_client

mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USERNAME% -p%DB_PASSWORD% -e "SELECT 1" >nul 2>&1
if errorlevel 1 goto :mysql_conn_failed

echo [Local MySQL] Connection successful
echo.

echo [Local MySQL] Creating database if not exists...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USERNAME% -p%DB_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul

echo [Local MySQL] Importing sample data...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USERNAME% -p%DB_PASSWORD% %DB_NAME% < docker\mysql-init\03-sample-data.sql 2>nul

echo [Done] Database initialization completed
echo [Note] Tables will be created automatically when you start the application
echo.
goto :write_local_config

:mysql_conn_failed
echo [Error] Failed to connect to MySQL
echo Please check your connection details:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Username: %DB_USERNAME%
echo.
set /p RETRY_CONN="Retry with different credentials? (y/N): "
if /i "%RETRY_CONN%"=="y" goto :local_setup
set DB_TYPE=none
goto :database_config

:no_mysql_client
echo [Warning] MySQL client not found in PATH
echo.
echo [Java] Using Java to initialize database...

REM ???? Java ???? SQL ???
(
echo import java.io.*;
echo import java.sql.*;
echo import java.nio.file.*;
echo import java.util.*;
echo.
echo public class InitDatabase {
echo     public static void main(String[] args^) throws Exception {
echo         String host = "%DB_HOST%";
echo         int port = Integer.parseInt("%DB_PORT%"^);
echo         String user = "%DB_USERNAME%";
echo         String password = "%DB_PASSWORD%";
echo         String dbName = "%DB_NAME%";
echo         String baseDir = "docker" + File.separator + "mysql-init";
echo.
echo         // 1. ?????
echo         String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=utf8mb4";
echo         try ^(Connection conn = DriverManager.getConnection(url, user, password^)^) {
echo             Statement stmt = conn.createStatement(^);
echo             stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"^);
echo             System.out.println("[OK] Database created"^);
echo             stmt.close(^);
echo         }
echo.
echo         // 2. ????????
echo         url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=utf8mb4";
echo         try ^(Connection conn = DriverManager.getConnection(url, user, password^)^) {
echo             // ????????
echo             File scriptFile = new File(baseDir, "03-sample-data.sql"^);
echo             if ^(!scriptFile.exists(^)^) {
echo                 System.err.println("[Error] Script file not found: " + scriptFile.getAbsolutePath(^)^);
echo                 System.exit^(1^);
echo             }
echo.
echo             String content = new String^(Files.readAllBytes^(scriptFile.toPath^(^)^)^);
echo             // ?? SQL ??????????
echo             String[] statements = content.split^(";\\s*\\n"^);
echo.
echo             Statement stmt = conn.createStatement(^);
echo             int count = 0, errors = 0;
echo             for ^(String sql : statements^) {
echo                 sql = sql.trim(^);
echo                 if ^(!sql.isEmpty(^) ^&^& ^!sql.startsWith^("--"^) ^&^& ^!sql.startsWith^("/*"^)^) {
echo                     try {
echo                         stmt.execute^(sql^);
echo                         count++;
echo                     } catch ^(SQLException e^) {
echo                         errors++;
echo                         // ???????????????????????
echo                         if ^(!e.getMessage^(^).toLowerCase^(^).contains^("doesn't exist"^)^) {
echo                             System.err.println("[Warning] SQL Error: " + e.getMessage^(^)^);
echo                         }
echo                     }
echo                 }
echo             }
echo             System.out.println("[OK] " + count + " SQL statements executed, " + errors + " errors (tables will be created on app start)"^);
echo             stmt.close(^);
echo         }
echo.
echo         System.out.println("[Success] Database initialization completed"^);
echo         System.out.println("[Note] Tables will be created automatically when you start the application"^);
echo     }
echo }
) > InitDatabase.java

REM ??????????
echo [Java] Compiling database initializer...
javac -cp target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar InitDatabase.java 2>nul
if errorlevel 1 (
    echo [Error] Failed to compile database initializer
    pause
    goto :write_local_config
)

echo [Java] Running database initialization...
java -cp ".;target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar" InitDatabase
if errorlevel 1 (
    echo [Error] Database initialization failed
    pause
    goto :write_local_config
)

REM ??????
del InitDatabase.java >nul 2>&1
del InitDatabase.class >nul 2>&1

echo [Done] Database initialization completed using Java
echo.
goto :write_local_config

:write_docker_config
set DB_HOST=localhost
set DB_PORT=3306
set DB_USERNAME=root
set DB_PASSWORD=RootPassword123!
echo [Config] Updating database.properties for Docker MySQL...
goto :write_config

:write_local_config
echo [Config] Updating database.properties for Local MySQL...
goto :write_config

:write_config
(
    echo # Database Configuration
    echo db.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=utf8mb4
    echo db.username=%DB_USERNAME%
    echo db.password=%DB_PASSWORD%
    echo db.pool.size=10
    echo db.connection.timeout=30000
    echo db.idle.timeout=600000
    echo db.max.lifetime=1800000
) > config\database.properties
echo [Done] Database configuration updated
echo.
goto :create_shortcut

:database_config
echo [Skip] Database configuration skipped
echo.
goto :create_shortcut

:create_shortcut
echo [8/8] Creating desktop shortcut...
echo [Tip] Create desktop shortcut?
set /p CREATE_SHORTCUT="Create shortcut? (Y/n): "
if /i "%CREATE_SHORTCUT%"=="n" goto :skip_shortcut
call create-shortcut.bat
if errorlevel 0 goto :shortcut_success
echo [Warning] Desktop shortcut creation failed
goto :shortcut_done

:shortcut_success
echo [Done] Desktop shortcut created

:skip_shortcut
:shortcut_done
echo.

echo ========================================
echo [Success] Installation completed!
echo ========================================
echo.
echo Application Info:
echo   Version: %APP_VERSION%
echo   JAR: target\cashier-system-fx-%APP_VERSION%.jar
echo.
if "%DB_TYPE%"=="docker" goto :show_docker_info
if "%DB_TYPE%"=="local" goto :show_local_info
echo Database: Not configured
goto :show_next_steps

:show_docker_info
echo Database: Docker MySQL (localhost:3306)
echo   Start: docker-compose up -d mysql
echo   Stop: docker-compose stop mysql
goto :show_next_steps

:show_local_info
echo Database: Local MySQL (%DB_HOST%:%DB_PORT%)
goto :show_next_steps

:show_next_steps
echo.
echo Next Steps:
echo   1. Review config\database.properties
echo   2. Run start.bat to start
echo.
echo Default Login: admin / admin123
echo.
pause
exit /b 0
