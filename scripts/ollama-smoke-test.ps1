# Checks, WITHOUT starting GoalStack, that your Ollama model can follow the plan format.
# Run in PowerShell from the project root:   .\scripts\ollama-smoke-test.ps1
# Optional: .\scripts\ollama-smoke-test.ps1 -Model "qwen2.5:7b" -BaseUrl "http://localhost:11434"
# The first run can take a minute or two while Ollama loads the model into memory.
param(
    [string]$Model = "llama3.2",
    [string]$BaseUrl = "http://localhost:11434"
)

$body = @"
{
  "model": "$Model",
  "stream": false,
  "keep_alive": "5m",
  "options": { "temperature": 0.2, "num_ctx": 8192, "num_predict": 1500 },
  "messages": [
    { "role": "system", "content": "You are a goal planning assistant. Reply with JSON only. Estimate realistic effort in hours for every subgoal (between 0.25 and 1000, at most 2 decimals). Do not include weights or dates." },
    { "role": "user", "content": "I want to learn Java well enough to apply for entry-level developer jobs. I can spend about 8 hours per week. I already know basic C." }
  ],
  "format": {
    "type": "object",
    "properties": {
      "goalTitle": { "type": "string" },
      "goalDescription": { "type": "string" },
      "subgoals": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "title": { "type": "string" },
            "description": { "type": "string" },
            "estimatedHours": { "type": "number" }
          },
          "required": ["title", "estimatedHours"],
          "additionalProperties": false
        }
      },
      "assumptions": { "type": "array", "items": { "type": "string" } }
    },
    "required": ["goalTitle", "subgoals"],
    "additionalProperties": false
  }
}
"@

Write-Host "Asking $Model at $BaseUrl ... (this may take a while the first time)"
$watch = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -ContentType "application/json" -Body $body -TimeoutSec 600
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Is Ollama running? Is the model installed? Try:  ollama list   and   ollama pull $Model"
    exit 1
}
$watch.Stop()
Write-Host ("Answered in {0:N1} seconds." -f $watch.Elapsed.TotalSeconds)

$content = $response.message.content
Write-Host "`nRaw answer:`n$content`n"

try {
    $plan = $content | ConvertFrom-Json
} catch {
    Write-Host "PROBLEM: the answer is not valid JSON. GoalStack would ask the model to fix it once." -ForegroundColor Yellow
    exit 2
}

$total = 0
foreach ($s in $plan.subgoals) { $total += [double]$s.estimatedHours }
Write-Host ("Subgoals: {0}   Total estimated hours: {1}" -f @($plan.subgoals).Count, $total)
$bad = @($plan.subgoals | Where-Object { $_.estimatedHours -lt 0.25 -or $_.estimatedHours -gt 1000 })
if ($bad.Count -gt 0) {
    Write-Host "PROBLEM: some hours are outside 0.25 to 1000 (GoalStack would reject them)." -ForegroundColor Yellow
} else {
    Write-Host "Looks usable: every subgoal has hours inside the allowed range." -ForegroundColor Green
}
Write-Host "Check by eye: are the hours realistic for the work described?"
