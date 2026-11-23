<#
.SYNOPSIS
  Lets the planner's port in from the tailnet only. Run in an Administrator PowerShell.

.DESCRIPTION
  The server binds 0.0.0.0 (server.address) and has no login, and the first
  Java start adds an "OpenJDK Platform binary" rule allowing every port on
  Private AND Public networks. Home Wi-Fi is usually Public, so without this
  any device on the Wi-Fi can read the plan.

  Two rules, both in the group "Financial Planner":
    - Block inbound TCP on the port from every address outside Tailscale's
      ranges (100.64.0.0/10 and fd7a:115c:a1e0::/48). A block rule beats any
      allow rule, so the Java rule cannot reopen it.
    - Allow inbound TCP on the port from those ranges, so the tailnet still
      gets in if the Java rule is ever removed.
  Loopback is never filtered, so this machine's own browser is unaffected.

  Re-running replaces the rules. -Remove deletes them.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File ops\firewall.ps1
  powershell -ExecutionPolicy Bypass -File ops\firewall.ps1 -Ports 8080,8081
  powershell -ExecutionPolicy Bypass -File ops\firewall.ps1 -Remove
#>
param(
    [int[]] $Ports = @(8080),
    [switch] $Remove
)

$ErrorActionPreference = 'Stop'
$group = 'Financial Planner'

$admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $admin) {
    Write-Host 'Run this from an Administrator PowerShell.'
    exit 1
}

Get-NetFirewallRule -Group $group -ErrorAction SilentlyContinue | Remove-NetFirewallRule
if ($Remove) {
    Write-Host "Removed the '$group' rules."
    exit 0
}

$tailnet = @('100.64.0.0/10', 'fd7a:115c:a1e0::/48')
# Everything else, as ranges: the firewall has no "not in" for addresses.
$outside = @(
    '0.0.0.0-100.63.255.255',
    '100.128.0.0-255.255.255.255',
    '::-fd7a:115c:a1df:ffff:ffff:ffff:ffff:ffff',
    'fd7a:115c:a1e1::-ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff'
)
$portList = $Ports | ForEach-Object { "$_" }
$portText = $Ports -join ', '

New-NetFirewallRule -Group $group -DisplayName "Financial Planner: block $portText outside the tailnet" `
    -Direction Inbound -Action Block -Protocol TCP -LocalPort $portList -RemoteAddress $outside -Profile Any | Out-Null
New-NetFirewallRule -Group $group -DisplayName "Financial Planner: allow $portText from the tailnet" `
    -Direction Inbound -Action Allow -Protocol TCP -LocalPort $portList -RemoteAddress $tailnet -Profile Any | Out-Null

Write-Host "Port(s) $portText now answer only on the tailnet (and loopback)."
Get-NetFirewallRule -Group $group | Format-Table DisplayName, Action, Enabled -AutoSize
