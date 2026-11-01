param(
    [string]$Only = "",
    [string]$Artifacts = "artifacts",
    [switch]$List
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$projects = @(
    @{ Name = "Fabric 26.3"; Path = "fabric/26.3"; Java = 25; Suffix = "+26.3" },
    @{ Name = "Fabric 26.4"; Path = "fabric/26.4"; Java = 25 },
    @{ Name = "Fabric 26.2"; Path = "fabric/26.2"; Java = 25 },
    @{ Name = "Fabric 26.1.2"; Path = "fabric/26.1.2"; Java = 25 },
    @{ Name = "Fabric 26.1.1"; Path = "fabric/26.1.1"; Java = 25 },
    @{ Name = "Fabric 26.1"; Path = "fabric/26.1"; Java = 25 },
    @{ Name = "Fabric 1.21.1"; Path = "fabric/1.21.1"; Java = 21 },
    @{ Name = "Fabric 1.21"; Path = "fabric/1.21"; Java = 21 },
    @{ Name = "Fabric 1.20.1"; Path = "fabric/1.20.1"; Java = 17 },
    @{ Name = "Forge 26.3"; Path = "forge/26.3"; Java = 25 },
    @{ Name = "Forge 26.2"; Path = "forge/26.2"; Java = 25 },
    @{ Name = "Forge 26.1.2"; Path = "forge/26.1.2"; Java = 25 },
    @{ Name = "Forge 26.1.1"; Path = "forge/26.1.1"; Java = 25 },
    @{ Name = "Forge 26.1"; Path = "forge/26.1"; Java = 25 },
    @{ Name = "Forge 1.21.1"; Path = "forge/1.21.1"; Java = 21 },
    @{ Name = "Forge 1.21"; Path = "forge/1.21"; Java = 21 },
    @{ Name = "Forge 1.20.1"; Path = "forge/1.20.1"; Java = 17 },
    @{ Name = "NeoForge 26.3"; Path = "neoforge/26.3"; Java = 25 },
    @{ Name = "NeoForge 26.2"; Path = "neoforge/26.2"; Java = 25 },
    @{ Name = "NeoForge 26.1.2"; Path = "neoforge/26.1.2"; Java = 25 },
    @{ Name = "NeoForge 1.21.1"; Path = "neoforge/1.21.1"; Java = 21 },
    @{ Name = "NeoForge 1.21"; Path = "neoforge/1.21"; Java = 21 }
)

if ($List) {
    $projects | ForEach-Object { [pscustomobject]@{ Name = $_.Name; Path = $_.Path; Java = $_.Java } } | Format-Table -AutoSize
    return
}

function Resolve-JavaHome([int]$major) {
    $envName = "JAVA${major}_HOME"
    $javaHome = [Environment]::GetEnvironmentVariable($envName)
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        throw "Set $envName to a JDK $major installation before running this script."
    }
    $java = Join-Path $javaHome "bin\java.exe"
    if (-not (Test-Path $java)) { $java = Join-Path $javaHome "bin/java" }
    if (-not (Test-Path $java)) { throw "$envName does not look like a JDK: $javaHome" }
    return $javaHome
}

$selected = if ($Only) { $projects | Where-Object { $_.Path -like "*$Only*" } } else { $projects }
if (-not $selected) { throw "No project matches -Only '$Only'" }

$artifactRoot = Join-Path $root $Artifacts
New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null
$isWindows = ($env:OS -eq "Windows_NT")

foreach ($p in $selected) {
    $dir = Join-Path $root $p.Path
    if (-not (Test-Path $dir)) { throw "Missing project folder: $($p.Path)" }
    $javaHome = Resolve-JavaHome $p.Java
    $env:JAVA_HOME = $javaHome
    Write-Host "=== Building $($p.Name) with Java $($p.Java) ===" -ForegroundColor Cyan
    Push-Location $dir
    try {
        if ($isWindows -and (Test-Path (Join-Path $dir "gradlew.bat"))) {
            & (Join-Path $dir "gradlew.bat") build --console=plain --no-daemon
        } else {
            & (Join-Path $dir "gradlew") build --console=plain --no-daemon
        }
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $($p.Name)" }
    }
    finally {
        Pop-Location
    }
    $libs = Join-Path $dir "build/libs"
    if (-not (Test-Path $libs)) { throw "No build/libs for $($p.Name)" }
    $jars = @(Get-ChildItem $libs -Filter *.jar -File | Where-Object { $_.Name -notmatch "sources|javadoc" })
    if ($jars.Count -eq 0) { throw "No jar produced for $($p.Name)" }
    $slug = $p.Path -replace "/", "-"
    $outDir = Join-Path $artifactRoot $slug
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    foreach ($jar in $jars) {
        $name = $jar.Name
        if ($p.Suffix -and $name -notmatch [regex]::Escape($p.Suffix)) {
            $name = $name -replace "\.jar$", "$($p.Suffix).jar"
        }
        Copy-Item $jar.FullName (Join-Path $outDir $name) -Force
        Write-Host "  -> $slug/$name" -ForegroundColor Green
    }
}

# NeoForge 1.20.1 uses the Forge 1.20.1 jar (binary-compatible legacy fork).
$forge1201 = Join-Path $artifactRoot "forge-1.20.1"
if (Test-Path $forge1201) {
    $jar = Get-ChildItem $forge1201 -Filter "*-forge.jar" -File | Select-Object -First 1
    if ($jar) {
        $nfDir = Join-Path $artifactRoot "neoforge-1.20.1"
        New-Item -ItemType Directory -Force -Path $nfDir | Out-Null
        $nfName = $jar.Name -replace "-forge\.jar$", "-neoforge.jar"
        Copy-Item $jar.FullName (Join-Path $nfDir $nfName) -Force
        Write-Host "  -> neoforge-1.20.1/$nfName (Forge 1.20.1 compatibility copy)" -ForegroundColor Green
    }
}

$sums = Join-Path $artifactRoot "SHA256SUMS.txt"
$lines = Get-ChildItem $artifactRoot -Recurse -File -Filter *.jar | Sort-Object FullName | ForEach-Object {
    $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower()
    $rel = $_.FullName.Substring($artifactRoot.Length + 1).Replace("\", "/")
    "$hash *$rel"
}
$lines | Set-Content $sums -Encoding ascii
Write-Host ""
Write-Host "Artifacts: $artifactRoot" -ForegroundColor Cyan
Write-Host "Checksums: $sums" -ForegroundColor Cyan
