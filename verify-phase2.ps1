# Phase 2 verification script
# Run from any directory: powershell -File "D:\Payment ledger\verify-phase2.ps1"

$backend = "D:\Payment ledger\backend"
Set-Location $backend

Write-Host "=== STEP 1: mvn clean compile ===" -ForegroundColor Cyan
mvn clean compile 2>&1 | Out-File "D:\Payment ledger\p2-compile.log"
$compileExit = $LASTEXITCODE
Get-Content "D:\Payment ledger\p2-compile.log" | Select-String "BUILD|ERROR|WARNING: A|javac|Compiling" | Select-Object -Last 20
Write-Host "Compile exit code: $compileExit" -ForegroundColor $(if ($compileExit -eq 0) { 'Green' } else { 'Red' })

Write-Host ""
Write-Host "=== STEP 2: mvn test ===" -ForegroundColor Cyan
mvn test 2>&1 | Out-File "D:\Payment ledger\p2-test.log"
$testExit = $LASTEXITCODE
Get-Content "D:\Payment ledger\p2-test.log" | Select-String "BUILD|Tests run|ERROR|FAILED|SUCCESS" | Select-Object -Last 20
Write-Host "Test exit code: $testExit" -ForegroundColor $(if ($testExit -eq 0) { 'Green' } else { 'Red' })

Write-Host ""
Write-Host "=== STEP 3: mvn clean package ===" -ForegroundColor Cyan
mvn clean package 2>&1 | Out-File "D:\Payment ledger\p2-package.log"
$packageExit = $LASTEXITCODE
Get-Content "D:\Payment ledger\p2-package.log" | Select-String "BUILD|ERROR|Tests run|jar" | Select-Object -Last 20
Write-Host "Package exit code: $packageExit" -ForegroundColor $(if ($packageExit -eq 0) { 'Green' } else { 'Red' })

Write-Host ""
Write-Host "=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "Compile : exit $compileExit $(if ($compileExit -eq 0) { '✓ PASS' } else { '✗ FAIL' })"
Write-Host "Test    : exit $testExit $(if ($testExit -eq 0) { '✓ PASS' } else { '✗ FAIL' })"
Write-Host "Package : exit $packageExit $(if ($packageExit -eq 0) { '✓ PASS' } else { '✗ FAIL' })"
Write-Host ""
Write-Host "Full logs saved to:"
Write-Host "  D:\Payment ledger\p2-compile.log"
Write-Host "  D:\Payment ledger\p2-test.log"
Write-Host "  D:\Payment ledger\p2-package.log"
