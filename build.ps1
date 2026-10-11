#Requires -Version 5.0
# Cross-platform build script for XpoTriad (Paper plugin).
# Works with Windows PowerShell 5.1 and PowerShell Core (pwsh) on any OS.
# Requires a JDK on PATH (provides javac + jar).
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host '==> Cleaning build directory...'
if (Test-Path build) {
    Remove-Item build -Recurse -Force
    Write-Host '    Removed old build/'
}
New-Item -ItemType Directory -Force build/classes | Out-Null

Write-Host '==> Building classpath...'
$jars = Get-ChildItem lib -Filter '*.jar' | ForEach-Object { $_.FullName }
if (-not $jars) {
    Write-Host 'ERROR: No JARs found in lib/' -ForegroundColor Red
    exit 1
}
Write-Host "    Found $($jars.Count) JAR(s) on classpath"

Write-Host '==> Collecting sources...'
$sources = Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$total = @($sources).Count
Write-Host "    Found $total source file(s)"

# Compile in ONE javac call (reliable for interdependent sources), but translate
# javac's own verbose "[parsing started <file>]" lines into a pip-style in-place
# progress bar. Everything else (warnings/errors/notes) is buffered and only shown
# if the build fails. "lib/*" is expanded by the JVM, not the shell, so the
# classpath is identical on Windows/Linux/macOS.
#
# Two Windows-PowerShell quirks are handled here:
#   1. Command-line length: 126 absolute paths can exceed the OS limit, so the
#      source list is passed via a javac "@argfile" instead of on the command line.
#   2. javac writes -verbose progress to STDERR, and Windows PowerShell wraps
#      native stderr lines as mangled ErrorRecords. Routing through "cmd /c 2>&1"
#      merges stderr into stdout as clean text PowerShell can parse (and cmd still
#      propagates javac's exit code). On pwsh (Core) we call javac directly, since
#      it passes native stderr through as clean strings.
Write-Host '==> Compiling...'
$script:compileDone = 0
$messages = New-Object System.Collections.ArrayList

function Show-CompileProgress {
    param([string]$Line)
    if ($Line -match '^\[parsing started') {
        $script:compileDone++
        $pct = if ($total -gt 0) { [Math]::Min(100, [int]($script:compileDone * 100 / $total)) } else { 100 }
        Write-Host -NoNewline ("`r  [{0,3}/{1}] Compiling {2,3}%" -f $script:compileDone, $total, $pct)
    }
    elseif ($Line -notmatch '^\[' -and $Line.Trim().Length -gt 0) {
        [void]$messages.Add($Line)
    }
}

$argFile = Join-Path $PSScriptRoot '.build-sources.tmp'
$sources | Set-Content -Path $argFile -Encoding ASCII
try {
    if ($PSVersionTable.PSEdition -eq 'Desktop' -or $IsWindows) {
        & cmd /c "javac -verbose -cp `"lib/*`" -d build/classes @`"$argFile`" 2>&1" |
            ForEach-Object { Show-CompileProgress "$_" }
    }
    else {
        & javac -verbose -cp 'lib/*' -d build/classes "@$argFile" 2>&1 |
            ForEach-Object { Show-CompileProgress "$_" }
    }
    $exitCode = $LASTEXITCODE
}
finally {
    Remove-Item $argFile -ErrorAction SilentlyContinue
}
Write-Host ''
if ($exitCode -ne 0) {
    Write-Host 'BUILD FAILED during compilation' -ForegroundColor Red
    $messages | ForEach-Object { Write-Host "    $_" }
    exit $exitCode
}
Write-Host "    Compiled $total source file(s)"

Write-Host '==> Copying resources...'
Copy-Item src/main/resources/plugin.yml build/classes/
Copy-Item src/main/resources/config.yml build/classes/
Write-Host '    Copied plugin.yml, config.yml'

Write-Host '==> Packaging JAR...'
& jar -cf build/XpoTriad.jar -C build/classes .
if ($LASTEXITCODE -ne 0) {
    Write-Host 'BUILD FAILED during packaging' -ForegroundColor Red
    exit $LASTEXITCODE
}

$jar = Resolve-Path build/XpoTriad.jar
$kb = [math]::Round((Get-Item $jar).Length / 1KB, 1)
Write-Host "    Created build/XpoTriad.jar ($kb KB)"

Write-Host 'BUILD SUCCESSFUL' -ForegroundColor Green

