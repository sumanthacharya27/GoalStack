# Builds the React app, copies it into the Spring Boot project and creates ONE runnable jar.
# Run from the project root in PowerShell:  .\build-single-jar.ps1
$ErrorActionPreference = "Stop"

Push-Location frontend
npm install
npm run build
Pop-Location

$static = "src\main\resources\static"
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item -ItemType Directory -Path $static | Out-Null
Copy-Item "frontend\dist\*" $static -Recurse

mvn clean package -DskipTests

Write-Host ""
Write-Host "Done. Run it with:"
Write-Host "  `$env:DB_PASSWORD = 'your_mysql_password'"
Write-Host "  java -jar target\goalstack-0.2.0-SNAPSHOT.jar"
Write-Host "Then open http://localhost:8080"
