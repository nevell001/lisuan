@echo off

echo ========================================
echo   Cashier System Installation
echo ========================================
echo.

set APP_VERSION=2.4.2
set DB_TYPE=none
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=cashier_system
set DB_USERNAME=root
set DB_PASSWORD=RootPassword123!

setlocal enabledelayedexpansion

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

echo [Docker] Checking for local MySQL instance...
REM Check if port 3306 is in use
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 goto :port_free

REM Port is in use, check if it's MySQL
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3306" ^| findstr "LISTENING"') do set LOCAL_MYSQL_PID=%%a
tasklist /FI "PID eq %LOCAL_MYSQL_PID%" /FI "IMAGENAME eq mysqld.exe" 2>nul | find /i "mysqld.exe" >nul 2>&1
if errorlevel 1 goto :port_not_mysql

REM Local MySQL is running
echo [Warning] Local MySQL is already running on port 3306!
echo.
echo Detected MySQL process:
echo   PID: %LOCAL_MYSQL_PID%
tasklist /FI "PID eq %LOCAL_MYSQL_PID%" /V 2>nul | findstr "mysqld.exe"
echo.
echo This will cause a port conflict with Docker MySQL.
echo.
echo Please select an option:
echo   1 - Stop local MySQL and use Docker MySQL (Recommended)
echo   2 - Use Docker MySQL on different port (3307)
echo   3 - Use existing local MySQL instead
echo.
set /p LOCAL_MYSQL_CHOICE="Enter option (1/2/3, default=1): "
if "%LOCAL_MYSQL_CHOICE%"=="" set LOCAL_MYSQL_CHOICE=1

if "%LOCAL_MYSQL_CHOICE%"=="1" goto :stop_local_mysql
if "%LOCAL_MYSQL_CHOICE%"=="2" goto :use_port_3307
if "%LOCAL_MYSQL_CHOICE%"=="3" goto :use_local_mysql_instead

echo [Warning] Invalid option, continuing with Docker MySQL on port 3306
echo [Warning] This may cause a port conflict
goto :docker_start_mysql

:stop_local_mysql
echo [Docker] Stopping local MySQL...
REM Try to stop MySQL service
net stop MySQL >nul 2>&1
net stop MySQL80 >nul 2>&1
net stop MySQL84 >nul 2>&1
REM If service stop fails, try to kill process
taskkill /F /PID %LOCAL_MYSQL_PID% >nul 2>&1
echo [Docker] Waiting for port 3306 to be released...
timeout /t 3 /nobreak >nul
goto :docker_start_mysql

:use_port_3307
echo [Docker] Using Docker MySQL on port 3307...
REM Temporarily modify port mapping in docker-compose.yml
if exist docker-compose.yml (
    findstr /V "published:" docker-compose.yml > docker-compose.yml.tmp
    echo     - "3307:3306" >> docker-compose.yml.tmp
    move /Y docker-compose.yml.tmp docker-compose.yml >nul
    set DB_PORT=3307
    echo [Note] docker-compose.yml has been modified to use port 3307
) else (
    echo [Error] docker-compose.yml not found
    echo [Fallback] Continuing with default port 3306 (may cause conflict)
)
goto :docker_start_mysql

:use_local_mysql_instead
echo [Info] Switching to local MySQL setup...
set DB_TYPE=local
goto :local_setup

:port_not_mysql
echo [Docker] Port 3306 is in use by another process (not MySQL)
echo [Warning] This may cause issues with Docker MySQL
set /p CONTINUE_ANYWAY="Continue anyway? (y/N): "
if /i not "%CONTINUE_ANYWAY%"=="y" goto :cancel_install
goto :docker_start_mysql

:port_free
echo [Docker] Port 3306 is free

:docker_start_mysql
echo [Docker] Starting MySQL container...
docker-compose up -d mysql
if errorlevel 1 goto :docker_start_failed

echo [Docker] MySQL container started successfully
echo [Docker] Waiting for MySQL to be ready...
timeout /t 10 /nobreak >nul

echo [Docker] Importing initialization script...
docker exec cashier-mysql mysql -uroot -p%DB_PASSWORD% --default-character-set=utf8mb4 %DB_NAME% < docker\mysql-init\00-init-complete.sql 2>nul
echo [Done] Database initialization completed
echo [Note] All tables and sample data have been imported
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

echo [Local MySQL] Importing initialization script...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USERNAME% -p%DB_PASSWORD% %DB_NAME% < docker\mysql-init\00-init-complete.sql 2>nul

echo [Done] Database initialization completed
echo [Note] All tables and sample data have been imported
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

REM Create Java initialization file
echo import java.io.*; > InitDatabase.java
echo import java.sql.*; >> InitDatabase.java
echo import java.nio.file.*; >> InitDatabase.java
echo import java.util.*; >> InitDatabase.java
echo. >> InitDatabase.java
echo public class InitDatabase { >> InitDatabase.java
echo     public static void main(String[] args) throws Exception { >> InitDatabase.java
echo         // Explicitly load MySQL JDBC driver class >> InitDatabase.java
echo         try { >> InitDatabase.java
echo             Class.forName("com.mysql.cj.jdbc.Driver"); >> InitDatabase.java
echo         } catch (ClassNotFoundException e) { >> InitDatabase.java
echo             System.err.println("[Error] MySQL JDBC Driver not found in classpath"); >> InitDatabase.java
echo             System.err.println("Please ensure the JAR with dependencies was built successfully"); >> InitDatabase.java
echo             System.exit(1); >> InitDatabase.java
echo         } >> InitDatabase.java
echo         String host = "%DB_HOST%"; >> InitDatabase.java
echo         int port = Integer.parseInt("%DB_PORT%"); >> InitDatabase.java
echo         String user = "%DB_USERNAME%"; >> InitDatabase.java
echo         String password = "%DB_PASSWORD%"; >> InitDatabase.java
echo         String dbName = "%DB_NAME%"; >> InitDatabase.java
echo         String baseDir = "docker" + File.separator + "mysql-init"; >> InitDatabase.java
echo. >> InitDatabase.java
echo         // 1. Create database >> InitDatabase.java
echo         String url = "jdbc:mysql://" + host + ":" + port + "?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=UTF-8"; >> InitDatabase.java
echo         try (Connection conn = DriverManager.getConnection(url, user, password)) { >> InitDatabase.java
echo             Statement stmt = conn.createStatement(); >> InitDatabase.java
echo             stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); >> InitDatabase.java
echo             System.out.println("[OK] Database created"); >> InitDatabase.java
echo             stmt.close(); >> InitDatabase.java
echo         } >> InitDatabase.java
echo. >> InitDatabase.java
echo         // 2. Import sample data >> InitDatabase.java
echo         url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=UTF-8"; >> InitDatabase.java
echo         try (Connection conn = DriverManager.getConnection(url, user, password)) { >> InitDatabase.java
echo             // Read initialization script >> InitDatabase.java
echo             File scriptFile = new File(baseDir, "00-init-complete.sql"); >> InitDatabase.java
echo             if (!scriptFile.exists()) { >> InitDatabase.java
echo                 System.err.println("[Error] Script file not found: " + scriptFile.getAbsolutePath()); >> InitDatabase.java
echo                 System.exit(1); >> InitDatabase.java
echo             } >> InitDatabase.java
echo. >> InitDatabase.java
echo             String content = new String(Files.readAllBytes(scriptFile.toPath())); >> InitDatabase.java
echo             // Split SQL statements (ignore empty lines and comments) >> InitDatabase.java
echo             String[] statements = content.split(";\\s*\\n"); >> InitDatabase.java
echo. >> InitDatabase.java
echo             Statement stmt = conn.createStatement(); >> InitDatabase.java
echo             int count = 0, errors = 0; >> InitDatabase.java
echo             for (String sql : statements) { >> InitDatabase.java
echo                 sql = sql.trim(); >> InitDatabase.java
echo                 if (!sql.isEmpty() ^&^& !sql.startsWith("--") ^&^& !sql.startsWith("/*")) { >> InitDatabase.java
echo                     try { >> InitDatabase.java
echo                         stmt.execute(sql); >> InitDatabase.java
echo                         count++; >> InitDatabase.java
echo                     } catch (SQLException e) { >> InitDatabase.java
echo                         errors++; >> InitDatabase.java
echo                         // Ignore table not exists errors
echo                         if (!e.getMessage().toLowerCase().contains("doesn't exist")) { >> InitDatabase.java
echo                             System.err.println("[Warning] SQL Error: " + e.getMessage()); >> InitDatabase.java
echo                         } >> InitDatabase.java
echo                     } >> InitDatabase.java
echo                 } >> InitDatabase.java
echo             } >> InitDatabase.java
echo             System.out.println("[OK] " + count + " SQL statements executed, " + errors + " errors"); >> InitDatabase.java
echo             stmt.close(); >> InitDatabase.java
echo         } >> InitDatabase.java
echo. >> InitDatabase.java
echo         System.out.println("[Success] Database initialization completed"); >> InitDatabase.java
echo         System.out.println("[Note] All tables and sample data have been imported"); >> InitDatabase.java
echo     } >> InitDatabase.java
echo } >> InitDatabase.java

REM Compile and run initialization program
echo [Java] Compiling database initializer...
set "JAR_WITH_DEPS=target\cashier-system-fx-!APP_VERSION!-jar-with-dependencies.jar"
if not exist "!JAR_WITH_DEPS!" (
    echo [Error] JAR file not found: !JAR_WITH_DEPS!
    echo [Info] Please run compilation first or select option 1 (Docker) instead
    pause
    goto :write_local_config
)

javac -cp "!JAR_WITH_DEPS!" InitDatabase.java 2>nul
if errorlevel 1 (
    echo [Error] Failed to compile database initializer
    echo [Info] Skipping database initialization
    del InitDatabase.java >nul 2>&1
    goto :write_local_config
)

echo [Java] Running database initialization...
java -cp ".;!JAR_WITH_DEPS!" InitDatabase
if errorlevel 1 (
    echo [Warning] Database initialization failed
    del InitDatabase.java >nul 2>&1
    del InitDatabase.class >nul 2>&1
    goto :write_local_config
)

REM Clean up temporary files
del InitDatabase.java >nul 2>&1
del InitDatabase.class >nul 2>&1

echo [Done] Database initialization completed using Java
echo.
goto :write_local_config

:cancel_install
echo [Info] Installation cancelled
pause
exit /b 0

:write_docker_config
endlocal
setlocal
set DB_HOST=localhost
set DB_PORT=3306
set DB_USERNAME=root
set DB_PASSWORD=RootPassword123!
setlocal enabledelayedexpansion
echo [Config] Updating database.properties for Docker MySQL...
goto :write_config

:write_local_config
echo [Config] Updating database.properties for Local MySQL...
goto :write_config

:write_config
endlocal
setlocal
(
    echo # Database Configuration
    echo db.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=UTF-8
    echo db.username=%DB_USERNAME%
    echo db.password=%DB_PASSWORD%
    echo db.pool.size=10
    echo db.connection.timeout=30000
    echo db.idle.timeout=600000
    echo db.max.lifetime=1800000
) > config\database.properties
endlocal
setlocal enabledelayedexpansion
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
