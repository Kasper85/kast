$ErrorActionPreference = "Stop"

function Get-JdkMajorVersion {
    param([Parameter(Mandatory)][string]$JdkHome)

    $java = Join-Path $JdkHome "bin\java.exe"
    $javac = Join-Path $JdkHome "bin\javac.exe"
    if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $javac)) {
        return $null
    }

    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = $java
    $processInfo.Arguments = "-version"
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardError = $true
    $processInfo.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($processInfo)
    $versionLine = $process.StandardError.ReadLine()
    $process.WaitForExit()
    if ($versionLine -notmatch '"(?:1\.)?(?<major>\d+)') {
        return $null
    }

    return [int]$Matches.major
}

function Add-Candidate {
    param(
        [System.Collections.Generic.List[string]]$Candidates,
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    try {
        $resolved = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
        if (-not $Candidates.Contains($resolved)) {
            $Candidates.Add($resolved)
        }
    } catch {
        # Missing optional locations are expected.
    }
}

$candidates = [System.Collections.Generic.List[string]]::new()

Add-Candidate $candidates $env:KASTLG_JAVA_HOME
Add-Candidate $candidates $env:JAVA_HOME

$fixedLocations = @(
    (Join-Path $env:USERPROFILE ".local\jdk17"),
    (Join-Path $env:USERPROFILE ".local\jdk-17"),
    (Join-Path $env:ProgramFiles "Android\Android Studio\jbr")
)
foreach ($location in $fixedLocations) {
    Add-Candidate $candidates $location
}

$searchRoots = @(
    (Join-Path $env:USERPROFILE ".local"),
    (Join-Path $env:USERPROFILE ".jdks"),
    (Join-Path $env:USERPROFILE ".gradle\jdks"),
    (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium"),
    (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
    (Join-Path $env:ProgramFiles "Java")
)
foreach ($root in $searchRoots) {
    if (Test-Path -LiteralPath $root) {
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            ForEach-Object { Add-Candidate $candidates $_.FullName }
    }
}

foreach ($commandName in @("javac.exe", "java.exe")) {
    $command = Get-Command $commandName -ErrorAction SilentlyContinue
    if ($command) {
        Add-Candidate $candidates (Split-Path (Split-Path $command.Source -Parent) -Parent)
    }
}

$validJdks = foreach ($candidate in $candidates) {
    $major = Get-JdkMajorVersion -JdkHome $candidate
    if ($major -ge 17) {
        [pscustomobject]@{
            Home = $candidate
            Major = $major
        }
    }
}

$selected = $validJdks | Select-Object -First 1

if (-not $selected) {
    Write-Error "KastLG requires a local JDK 17 or newer. Install one in a standard user/system location or set KASTLG_JAVA_HOME."
    exit 1
}

Write-Output $selected.Home
