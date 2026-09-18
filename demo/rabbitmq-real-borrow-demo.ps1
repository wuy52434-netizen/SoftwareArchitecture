param(
    [string]$BorrowApi = "http://localhost:8083/api",
    [string]$BookApi = "http://localhost:8082/api",
    [string]$RabbitApi = "http://localhost:15672/api",
    [string]$RabbitUser = "admin",
    [string]$RabbitPassword = "admin123",
    [int]$UserId = 11,
    [int]$BookId = 2,
    [int]$ConsumerWaitSeconds = 8,
    [switch]$NoReturn,
    [switch]$SkipClearSentinel,
    [switch]$SkipLogs
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

function New-BasicAuthHeader {
    param([string]$User, [string]$Password)

    $pair = "$User`:$Password"
    $bytes = [Text.Encoding]::ASCII.GetBytes($pair)
    return "Basic " + [Convert]::ToBase64String($bytes)
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )

    if (-not $Headers.ContainsKey("Accept")) {
        $Headers["Accept"] = "application/json"
    }

    if ([string]::IsNullOrEmpty($Body)) {
        $response = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $Headers -UseBasicParsing -TimeoutSec 15
    } else {
        $response = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $Headers -ContentType "application/json; charset=utf-8" -Body $Body -UseBasicParsing -TimeoutSec 15
    }

    $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Invoke-RabbitApi {
    param(
        [string]$Method,
        [string]$Path
    )

    $headers = @{
        Authorization = $script:AuthHeader
    }
    return Invoke-JsonRequest -Method $Method -Uri "$RabbitApi$Path" -Headers $headers
}

function Get-IntValue {
    param($Value)

    if ($null -eq $Value) {
        return 0
    }
    return [int]$Value
}

function Get-QueueMetrics {
    param([string]$QueueName)

    $encodedQueue = [Uri]::EscapeDataString($QueueName)
    $q = Invoke-RabbitApi -Method Get -Path "/queues/%2F/$encodedQueue"
    $stats = $q.message_stats

    [pscustomobject]@{
        Queue = $QueueName
        Consumers = Get-IntValue $q.consumers
        Ready = Get-IntValue $q.messages_ready
        Unacked = Get-IntValue $q.messages_unacknowledged
        Publish = Get-IntValue $stats.publish
        Deliver = Get-IntValue $stats.deliver_get
        Ack = Get-IntValue $stats.ack
    }
}

function Read-QueueMetrics {
    param([string[]]$Queues)

    $result = @{}
    foreach ($queue in $Queues) {
        try {
            $result[$queue] = Get-QueueMetrics -QueueName $queue
        } catch {
            Write-Host "Warning: queue not available: $queue ($($_.Exception.Message))"
        }
    }
    return $result
}

function New-DeltaRows {
    param(
        [hashtable]$Before,
        [hashtable]$After
    )

    $rows = @()
    foreach ($queue in $After.Keys) {
        if (-not $Before.ContainsKey($queue)) {
            continue
        }

        $b = $Before[$queue]
        $a = $After[$queue]
        $rows += [pscustomobject]@{
            Queue = $queue
            Published = $a.Publish - $b.Publish
            Delivered = $a.Deliver - $b.Deliver
            Acked = $a.Ack - $b.Ack
            ReadyNow = $a.Ready
            Consumers = $a.Consumers
        }
    }
    return $rows
}

function Write-LogLines {
    param(
        [string]$Title,
        [string]$Container,
        [string]$Since,
        [string]$Pattern
    )

    Write-Host ""
    Write-Host $Title
    $lines = docker logs --since "$Since" $Container 2>&1 |
        Select-String -Pattern $Pattern |
        Select-Object -First 8

    if ($null -eq $lines -or $lines.Count -eq 0) {
        Write-Host "  No matching log lines found in the selected time window."
        return
    }

    foreach ($line in $lines) {
        Write-Host "  $line"
    }
}

Assert-LocalTarget -TargetUrl $BorrowApi
Assert-LocalTarget -TargetUrl $BookApi
Assert-LocalTarget -TargetUrl $RabbitApi

$script:AuthHeader = New-BasicAuthHeader -User $RabbitUser -Password $RabbitPassword
$queues = @(
    "queue.borrow.success",
    "queue.stats.daily",
    "queue.notify.sms",
    "queue.notify.email"
)

Write-Section "RabbitMQ Demo: Real Borrow Business Flow"
Write-Host "Purpose:"
Write-Host "  Prove RabbitMQ is used after a real borrow request succeeds."
Write-Host ""
Write-Host "Project role:"
Write-Host "  borrow-service completes the borrow transaction, then publishes BORROW_SUCCESS."
Write-Host "  RabbitMQ routes the event to notification and statistics consumers."
Write-Host ""
Write-Host "Borrow API: $BorrowApi"
Write-Host "Book API:   $BookApi"
Write-Host "Rabbit API: $RabbitApi"
Write-Host "UserId:     $UserId"
Write-Host "BookId:     $BookId"
Write-Host "Wait:       $ConsumerWaitSeconds seconds for async consumers"

if (-not $SkipClearSentinel) {
    Write-Section "Step 1: Prepare Environment"
    try {
        Invoke-JsonRequest -Method Delete -Uri "$BorrowApi/sentinel/borrow-demo/rule" | Out-Null
        Write-Host "Sentinel demo rule cleared, so the real borrow request will not be blocked."
    } catch {
        Write-Host "Sentinel rule clear skipped: $($_.Exception.Message)"
    }
} else {
    Write-Section "Step 1: Prepare Environment"
    Write-Host "Sentinel cleanup skipped by parameter."
}

Write-Host "Checking available copy for bookId=$BookId..."
$copyResponse = Invoke-JsonRequest -Method Get -Uri "$BookApi/books/$BookId/available-copy"
if ($copyResponse.code -ne 200 -or $null -eq $copyResponse.data) {
    throw "No available copy found for bookId=$BookId"
}

$copyId = $copyResponse.data.copyId
Write-Host "Available copy selected: copyId=$copyId, barcode=$($copyResponse.data.barcode)"

Write-Section "Step 2: Queue Metrics Before Borrow"
$before = Read-QueueMetrics -Queues $queues
$before.Values | Sort-Object Queue | Format-Table Queue, Consumers, Ready, Unacked, Publish, Deliver, Ack -AutoSize

$startedAt = Get-Date
$borrowBody = @{
    bookId = $BookId
    copyId = $copyId
    userId = $UserId
    note = "rabbitmq real borrow demo"
} | ConvertTo-Json -Compress

Write-Section "Step 3: Call Real Borrow API"
Write-Host "POST $BorrowApi/borrow"
Write-Host "Body: $borrowBody"

$borrowResponse = Invoke-JsonRequest -Method Post -Uri "$BorrowApi/borrow" -Body $borrowBody

if ($borrowResponse.code -ne 200 -or $null -eq $borrowResponse.data) {
    Write-Host "Borrow result: code=$($borrowResponse.code), message=$($borrowResponse.message)"
    throw "Borrow request did not succeed."
}

$recordId = $borrowResponse.data.recordId
if ($null -eq $recordId) {
    $recordId = $borrowResponse.data.id
}

Write-Host "Borrow result: SUCCESS"
Write-Host "RecordId:      $recordId"
Write-Host "BookTitle:     $($borrowResponse.data.bookTitle)"
Write-Host "CopyBarcode:   $($borrowResponse.data.copyBarcode)"
Write-Host "DueDate:       $($borrowResponse.data.dueDate)"

Write-Host ""
Write-Host "Waiting $ConsumerWaitSeconds seconds for RabbitMQ consumers and management counters..."
Start-Sleep -Seconds $ConsumerWaitSeconds

Write-Section "Step 4: Queue Metrics After Borrow"
$after = Read-QueueMetrics -Queues $queues
$after.Values | Sort-Object Queue | Format-Table Queue, Consumers, Ready, Unacked, Publish, Deliver, Ack -AutoSize

Write-Section "Step 5: RabbitMQ Queue Counter Evidence"
$deltaRows = New-DeltaRows -Before $before -After $after
$deltaRows | Sort-Object Queue | Format-Table Queue, Published, Delivered, Acked, ReadyNow, Consumers -AutoSize

Write-Host ""
Write-Host "How to read this:"
Write-Host "  Published +1 means RabbitMQ received/routed the message to that queue."
Write-Host "  Delivered +1 and Acked +1 mean a service consumed and acknowledged it."
Write-Host "  ReadyNow increasing means the queue received messages but has no active consumer."
Write-Host "  Note: RabbitMQ management counters refresh asynchronously; service logs below are the primary proof."

if (-not $SkipLogs) {
    $since = $startedAt.ToUniversalTime().ToString("o")
    Write-Section "Step 6: Primary Proof From Service Logs"
    Write-LogLines -Title "Producer: borrow-service" -Container "library-borrow-service" -Since $since -Pattern "eventType=success|recordId=$recordId|借阅事件|借阅记录创建"
    Write-LogLines -Title "Consumer: stats-service" -Container "library-stats-service" -Since $since -Pattern "BORROW_SUCCESS|recordId=$recordId|处理统计消息|统计借书事件"
    Write-LogLines -Title "Consumer: notify-service" -Container "library-notify-service" -Since $since -Pattern "BORROW_SUCCESS|recordId=$recordId|处理借书成功事件|借书成功通知|站内消息"
}

if (-not $NoReturn -and $null -ne $recordId) {
    Write-Section "Step 7: Cleanup"
    $returnBody = @{
        borrowId = $recordId
    } | ConvertTo-Json -Compress

    $returnResponse = Invoke-JsonRequest -Method Post -Uri "$BorrowApi/return" -Body $returnBody
    Write-Host "Return result: code=$($returnResponse.code), message=$($returnResponse.message)"
}

Write-Section "Conclusion"
Write-Host "RabbitMQ decouples the borrow transaction from follow-up work."
Write-Host "The borrow API can return after the core transaction, while notifications and statistics are handled asynchronously."
Write-Host ""
Write-Host "RabbitMQ console:"
Write-Host "  http://localhost:15672"
Write-Host "  username: $RabbitUser"
Write-Host "  password: $RabbitPassword"
Write-Host ""
Write-Host "Useful pages:"
Write-Host "  Exchanges -> borrow.exchange"
Write-Host "  Queues -> queue.borrow.success / queue.stats.daily / queue.notify.sms / queue.notify.email"
