param(
    [string]$RabbitApi = "http://localhost:15672/api",
    [string]$RabbitUser = "admin",
    [string]$RabbitPassword = "admin123",
    [string]$Exchange = "borrow.exchange",
    [string]$RoutingKey = "borrow.success",
    [int]$Count = 3,
    [int]$UserId = 11,
    [int]$BookId = 999999,
    [string]$BookTitle = "RabbitMQ-Demo-Book",
    [switch]$SkipLogs
)

$ErrorActionPreference = "Stop"

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

function Invoke-RabbitApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $headers = @{
        Authorization = $script:AuthHeader
    }

    $uri = "$RabbitApi$Path"
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    }

    $json = $Body | ConvertTo-Json -Depth 12 -Compress
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body $json
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

function Publish-BorrowEvent {
    param([int]$Index)

    $today = Get-Date -Format "yyyy-MM-dd"
    $dueDate = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
    $recordId = [int64](([DateTimeOffset]::Now.ToUnixTimeMilliseconds() * 100) + $Index)

    $payload = [ordered]@{
        eventType = "BORROW_SUCCESS"
        recordId = $recordId
        userId = $UserId
        bookId = $BookId
        copyId = 0
        bookTitle = "$BookTitle-$Index"
        borrowDate = $today
        dueDate = $dueDate
        returnDate = $null
        timestamp = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
        source = "rabbitmq-demo-script"
    } | ConvertTo-Json -Compress

    $body = @{
        properties = @{
            content_type = "application/json"
            delivery_mode = 2
        }
        routing_key = $RoutingKey
        payload = $payload
        payload_encoding = "string"
    }

    Invoke-RabbitApi -Method Post -Path "/exchanges/%2F/$Exchange/publish" -Body $body
}

Assert-LocalTarget -TargetUrl $RabbitApi

if ($Count -lt 1 -or $Count -gt 20) {
    throw "Count must be between 1 and 20."
}

$script:AuthHeader = New-BasicAuthHeader -User $RabbitUser -Password $RabbitPassword
$queues = @(
    "queue.borrow.success",
    "queue.notify.sms",
    "queue.notify.email",
    "queue.stats.daily"
)

Write-Host "RabbitMQ borrow event demo"
Write-Host "RabbitMQ API: $RabbitApi"
Write-Host "Exchange:     $Exchange"
Write-Host "Routing key:  $RoutingKey"
Write-Host "Count:        $Count"
Write-Host ""

Write-Host "Queue metrics before publish"
$before = @{}
foreach ($queue in $queues) {
    try {
        $metric = Get-QueueMetrics -QueueName $queue
        $before[$queue] = $metric
        $metric | Format-Table -AutoSize
    } catch {
        Write-Host "Queue not available: $queue ($($_.Exception.Message))"
    }
}

$startedAt = Get-Date
Write-Host ""
Write-Host "Publishing messages..."

for ($i = 1; $i -le $Count; $i++) {
    $result = Publish-BorrowEvent -Index $i
    Write-Host ("[{0}/{1}] routed={2}" -f $i, $Count, $result.routed)
}

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "Queue metrics after publish"
$after = @{}
foreach ($queue in $queues) {
    try {
        $metric = Get-QueueMetrics -QueueName $queue
        $after[$queue] = $metric
        $metric | Format-Table -AutoSize
    } catch {
        Write-Host "Queue not available: $queue ($($_.Exception.Message))"
    }
}

Write-Host ""
Write-Host "Metric delta"
foreach ($queue in $queues) {
    if (-not $before.ContainsKey($queue) -or -not $after.ContainsKey($queue)) {
        continue
    }

    $b = $before[$queue]
    $a = $after[$queue]
    [pscustomobject]@{
        Queue = $queue
        PublishDelta = $a.Publish - $b.Publish
        DeliverDelta = $a.Deliver - $b.Deliver
        AckDelta = $a.Ack - $b.Ack
        ReadyNow = $a.Ready
        UnackedNow = $a.Unacked
        Consumers = $a.Consumers
    } | Format-Table -AutoSize
}

if (-not $SkipLogs) {
    Write-Host ""
    Write-Host "Recent consumer logs"
    Write-Host "stats-service:"
    docker logs --since "$($startedAt.ToUniversalTime().ToString("o"))" library-stats-service 2>&1 |
        Select-String -Pattern "RabbitMQ-Demo-Book|BORROW_SUCCESS|stats|message|统计|处理" |
        Select-Object -First 20

    Write-Host ""
    Write-Host "notify-service:"
    docker logs --since "$($startedAt.ToUniversalTime().ToString("o"))" library-notify-service 2>&1 |
        Select-String -Pattern "RabbitMQ-Demo-Book|BORROW_SUCCESS|notify|message|通知|处理" |
        Select-Object -First 20
}

Write-Host ""
Write-Host "RabbitMQ console:"
Write-Host "http://localhost:15672"
Write-Host "Username: $RabbitUser"
Write-Host "Password: $RabbitPassword"
Write-Host "Check: Exchanges -> borrow.exchange, Queues -> queue.stats.daily / queue.notify.sms / queue.notify.email"
