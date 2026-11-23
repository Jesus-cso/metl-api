$exe = "C:\Program Files\MariaDB 13.0\bin\mariadbd.exe"
if (-not (Get-Process mariadbd -ErrorAction SilentlyContinue)) {
  Start-Process -FilePath $exe -ArgumentList '--datadir=C:\MariaDBData_nuevo','--console'
  Start-Sleep -Seconds 8
} else { "mariadbd ya está corriendo" }
Set-Location "$PSScriptRoot\metl-api"
npm start
