param(
    [string]$Url = "http://localhost:8083/api/sentinel/borrow-demo",
    [ValidateSet("GET", "POST")]
    [string]$Method = "GET",
    [int]$Concurrency = 20,
    [int]$DurationSeconds = 20,
    [int]$MaxRequests = 800,
    [int]$UserId = 11,
    [int]$BookId = 999999,
    [double]$QpsLimit = 1,
    [switch]$SkipRuleSetup,
    [switch]$ClearRuleAfter
)

$ErrorActionPreference = "Stop"

function Assert-LocalTarget {
    param([string]$TargetUrl)

    $uri = [Uri]$TargetUrl
    $allowedHosts = @("localhost", "127.0.0.1", "::1")

    if ($allowedHosts -notcontains $uri.Host) {
        throw "For safety this demo script only allows localhost targets. Current host: $($uri.Host)"
    }

    if ($uri.Scheme -notin @("http", "https")) {
        throw "Only http/https URLs are supported."
    }
}

function New-Stats {
    [pscustomobject]@{
        Total = 0
        Success = 0
        BusinessError = 0
        Blocked = 0
        Failed = 0
    }
}

Assert-LocalTarget -TargetUrl $Url

if ($Concurrency -lt 1 -or $Concurrency -gt 100) {
    throw "Concurrency must be between 1 and 100."
}

if ($DurationSeconds -lt 1 -or $DurationSeconds -gt 120) {
    throw "DurationSeconds must be between 1 and 120."
}

if ($MaxRequests -lt 1 -or $MaxRequests -gt 10000) {
    throw "MaxRequests must be between 1 and 10000."
}

$body = @{
    bookId = $BookId
    userId = $UserId
} | ConvertTo-Json -Compress

if (-not $SkipRuleSetup) {
    $ruleUri = "http://$(([Uri]$Url).Host):$(([Uri]$Url).Port)/api/sentinel/borrow-demo/rule?qps=$QpsLimit"
    Write-Host "Loading Sentinel flow rule: borrowBook QPS=$QpsLimit"
    Invoke-RestMethod -Method Post -Uri $ruleUri | Out-Null
}

$startedAt = Get-Date
$deadline = $startedAt.AddSeconds($DurationSeconds)
$jobs = @()

Write-Host "Sentinel borrowBook load demo"
Write-Host "Target:      $Url"
Write-Host "Method:      $Method"
Write-Host "Concurrency: $Concurrency"
Write-Host "Duration:    ${DurationSeconds}s"
Write-Host "MaxRequests: $MaxRequests"
if ($Method -eq "POST") {
    Write-Host "Body:        $body"
}
Write-Host ""
Write-Host "Sentinel rule:"
if ($SkipRuleSetup) {
    Write-Host "Skipped automatic rule setup. Configure borrowBook QPS manually if needed."
} else {
    Write-Host "borrowBook QPS = $QpsLimit"
}
Write-Host ""

for ($i = 1; $i -le $Concurrency; $i++) {
    $workerMaxRequests = [Math]::Ceiling($MaxRequests / $Concurrency)
    $jobs += Start-Job -ArgumentList $Url, $Method, $body, $deadline, $workerMaxRequests -ScriptBlock {
        param($WorkerUrl, $WorkerMethod, $WorkerBody, $WorkerDeadline, $WorkerMaxRequests)

        $localStats = @{
            Total = 0
            Success = 0
            BusinessError = 0
            Blocked = 0
            Failed = 0
        }

        while ((Get-Date) -lt $WorkerDeadline) {
            if ($localStats.Total -ge $WorkerMaxRequests) {
                break
            }

            try {
                if ($WorkerMethod -eq "POST") {
                    $response = Invoke-RestMethod `
                        -Method Post `
                        -Uri $WorkerUrl `
                        -ContentType "application/json" `
                        -Body $WorkerBody `
                        -TimeoutSec 5
                } else {
                    $response = Invoke-RestMethod `
                        -Method Get `
                        -Uri $WorkerUrl `
                        -TimeoutSec 5
                }

                $localStats.Total++

                if ($response.success -eq $true -or $response.code -eq 200) {
                    $localStats.Success++
                } elseif (($response.message -match "busy|繁忙|Blocked|限流|流控") -or $response.code -in @(429, 503, 5001, 1006)) {
                    $localStats.Blocked++
                } else {
                    $localStats.BusinessError++
                }
            } catch {
                $localStats.Total++
                $message = $_.Exception.Message
                if ($message -match "busy|繁忙|Blocked|限流|流控|429") {
                    $localStats.Blocked++
                } else {
                    $localStats.Failed++
                }
            }
        }

        [pscustomobject]$localStats
    }
}

while (($jobs | Where-Object State -eq "Running").Count -gt 0) {
    $completed = ($jobs | Where-Object State -eq "Completed").Count
    $running = ($jobs | Where-Object State -eq "Running").Count
    $elapsed = [int]((Get-Date) - $startedAt).TotalSeconds
    Write-Progress -Activity "Sending borrowBook demo traffic" -Status "Elapsed ${elapsed}s, running workers: $running, completed workers: $completed" -PercentComplete ([Math]::Min(100, ($elapsed / $DurationSeconds) * 100))
    Start-Sleep -Milliseconds 500
}

Write-Progress -Activity "Sending borrowBook demo traffic" -Completed

$summary = New-Stats

foreach ($job in $jobs) {
    $result = Receive-Job $job
    Remove-Job $job

    $summary.Total += $result.Total
    $summary.Success += $result.Success
    $summary.BusinessError += $result.BusinessError
    $summary.Blocked += $result.Blocked
    $summary.Failed += $result.Failed
}

$elapsedTotal = [Math]::Max(1, ((Get-Date) - $startedAt).TotalSeconds)
$qps = [Math]::Round($summary.Total / $elapsedTotal, 2)

Write-Host ""
Write-Host "Result"
Write-Host "Total requests:  $($summary.Total)"
Write-Host "Approx QPS:      $qps"
Write-Host "Success:         $($summary.Success)"
Write-Host "Business errors: $($summary.BusinessError)"
Write-Host "Sentinel blocked:$($summary.Blocked)"
Write-Host "Request failed:  $($summary.Failed)"
Write-Host ""
Write-Host "Now check Sentinel Dashboard:"
Write-Host "http://localhost:8858 -> borrow-service -> Cluster Link / Realtime Monitor -> borrowBook"

if ($ClearRuleAfter) {
    $clearUri = "http://$(([Uri]$Url).Host):$(([Uri]$Url).Port)/api/sentinel/borrow-demo/rule"
    Invoke-RestMethod -Method Delete -Uri $clearUri | Out-Null
    Write-Host "Sentinel rule cleared."
}
