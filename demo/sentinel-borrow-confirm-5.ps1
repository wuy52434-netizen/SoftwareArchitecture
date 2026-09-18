param(
    [string]$Url = "http://localhost:8083/api/borrow",
    [int]$UserId = 11,
    [int]$BookId = 999999,
    [int]$Count = 5,
    [double]$QpsLimit = 1,
    [switch]$SetupRule,
    [switch]$ClearRuleAfter
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Title
    Write-Host "============================================================"
}

function Assert-LocalTarget {
    param([string]$TargetUrl)

    $uri = [Uri]$TargetUrl
    $allowedHosts = @("localhost", "127.0.0.1", "::1")

    if ($allowedHosts -notcontains $uri.Host) {
        throw "For safety this demo script only allows localhost targets. Current host: $($uri.Host)"
    }
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body = $null
    )

    $headers = @{
        Accept = "application/json"
    }

    if ($null -eq $Body) {
        $response = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $headers -UseBasicParsing -TimeoutSec 10
    } else {
        $response = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $headers -ContentType "application/json; charset=utf-8" -Body $Body -UseBasicParsing -TimeoutSec 10
    }

    $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Get-ResultType {
    param($Response)

    if ($Response.code -eq 200) {
        return "PASSED"
    }
    if ($Response.code -eq 503 -or $Response.message -match "Sentinel|Blocked|block|busy|limit|tempor|unavailable|繁忙|限流") {
        return "BLOCKED"
    }
    return "BUSINESS"
}

Assert-LocalTarget -TargetUrl $Url

if ($Count -lt 1 -or $Count -gt 20) {
    throw "Count must be between 1 and 20."
}

$uri = [Uri]$Url
$base = "$($uri.Scheme)://$($uri.Host):$($uri.Port)"

Write-Section "Sentinel Demo: 5 Real Borrow Confirm Requests"
Write-Host "Purpose:"
Write-Host "  Show Sentinel flow control on the borrowBook resource."
Write-Host ""
Write-Host "How it works:"
Write-Host "  1. Send real POST requests to /api/borrow."
Write-Host "  2. Use an invalid BookId so the passed requests do not borrow a real book."
Write-Host "  3. When borrowBook QPS=1 is active, extra requests return code 503."
Write-Host ""
Write-Host "Target:   $Url"
Write-Host "UserId:   $UserId"
Write-Host "BookId:   $BookId"
Write-Host "Count:    $Count"

if ($SetupRule) {
    Write-Section "Step 1: Configure Sentinel Rule"
    $ruleUrl = "$base/api/sentinel/borrow-demo/rule?qps=$QpsLimit"
    Invoke-JsonRequest -Method Post -Uri $ruleUrl | Out-Null
    Write-Host "Rule loaded: resource=borrowBook, QPS=$QpsLimit"
} else {
    Write-Section "Step 1: Sentinel Rule"
    Write-Host "Rule setup skipped by script."
    Write-Host "Manual setup:"
    Write-Host "  http://localhost:8858 -> borrow-service -> Cluster Link -> borrowBook -> Flow Control"
    Write-Host "  QPS threshold: 1"
}

$body = @{
    bookId = $BookId
    userId = $UserId
    note = "sentinel confirm borrow demo"
} | ConvertTo-Json -Compress

Write-Section "Step 2: Send Requests"
Write-Host ("{0,-8} {1,-9} {2,-8} {3,-10} {4}" -f "No.", "Result", "Code", "Elapsed", "Message")
Write-Host ("{0,-8} {1,-9} {2,-8} {3,-10} {4}" -f "---", "------", "----", "-------", "-------")

$summary = [ordered]@{
    Total = 0
    Passed = 0
    Blocked = 0
    BusinessError = 0
    Failed = 0
}

for ($i = 1; $i -le $Count; $i++) {
    $summary.Total++
    $start = Get-Date

    try {
        $response = Invoke-JsonRequest -Method Post -Uri $Url -Body $body
        $elapsed = [int]((Get-Date) - $start).TotalMilliseconds
        $type = Get-ResultType -Response $response

        if ($type -eq "PASSED") {
            $summary.Passed++
        } elseif ($type -eq "BLOCKED") {
            $summary.Blocked++
        } else {
            $summary.BusinessError++
        }

        Write-Host ("{0,-8} {1,-9} {2,-8} {3,-10} {4}" -f "$i/$Count", $type, $response.code, "${elapsed}ms", $response.message)
    } catch {
        $summary.Failed++
        Write-Host ("{0,-8} {1,-9} {2,-8} {3,-10} {4}" -f "$i/$Count", "FAILED", "-", "-", $_.Exception.Message)
    }
}

Write-Section "Step 3: Result Summary"
Write-Host "Total requests:        $($summary.Total)"
Write-Host "Passed to business:    $($summary.Passed)"
Write-Host "Blocked by Sentinel:   $($summary.Blocked)"
Write-Host "Business errors:       $($summary.BusinessError)"
Write-Host "Request failures:      $($summary.Failed)"
Write-Host ""
Write-Host "Expected evidence:"
Write-Host "  BLOCKED rows with code=503 prove Sentinel intercepted borrowBook."
Write-Host "  BUSINESS rows with code=2001 mean Sentinel allowed the request, then business returned book-not-found."
Write-Host ""
Write-Host "Dashboard:"
Write-Host "  http://localhost:8858 -> borrow-service -> Realtime Monitor / Cluster Link -> borrowBook"

if ($ClearRuleAfter) {
    Write-Section "Cleanup"
    $clearUrl = "$base/api/sentinel/borrow-demo/rule"
    Invoke-JsonRequest -Method Delete -Uri $clearUrl | Out-Null
    Write-Host "Sentinel rule cleared."
}
