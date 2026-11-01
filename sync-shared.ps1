param(
    [Parameter(Mandatory = $true)][string]$FromProject,
    [Parameter(Mandatory = $true)][string]$RelativePath,
    [Parameter(Mandatory = $true)][string[]]$ToProjects,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$source = Join-Path (Join-Path $root $FromProject) $RelativePath
if (-not (Test-Path $source)) { throw "Source not found: $source" }

foreach ($target in $ToProjects) {
    $destination = Join-Path (Join-Path $root $target) $RelativePath
    $destinationDir = Split-Path -Parent $destination
    if (-not (Test-Path $destinationDir)) { New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null }
    Copy-Item $source $destination -Recurse -Force -WhatIf:$WhatIf
    if ($WhatIf) { Write-Host "Would copy -> $target/$RelativePath" }
    else { Write-Host "Copied -> $target/$RelativePath" }
}
