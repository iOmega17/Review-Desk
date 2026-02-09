using Microsoft.AspNetCore.SignalR;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);

// Add SignalR
builder.Services.AddSignalR();

// Add CORS for testing (allow any origin)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// Enable CORS
app.UseCors();

// Map SignalR Hub
app.MapHub<AndroidHub>("/androidHub");

// API endpoint to send commands to Android
app.MapGet("/api/command", async (
    IHubContext<AndroidHub> hubContext,
    string? action,
    string? targetType,
    string? targetValue,
    string? extraData) =>
{
    if (string.IsNullOrEmpty(action))
    {
        return Results.BadRequest(new { error = "Action parameter is required" });
    }

    var command = new
    {
        action = action.ToUpper(),
        targetType = targetType ?? "TEXT",
        targetValue = targetValue ?? "",
        extraData = extraData ?? ""
    };

    var jsonCommand = JsonSerializer.Serialize(command);
    
    // Broadcast to all connected Android clients
    await hubContext.Clients.All.SendAsync("ReceiveCommand", jsonCommand);
    
    return Results.Ok(new 
    { 
        success = true, 
        message = "Command sent to all connected clients",
        command = command
    });
});

// Health check endpoint
app.MapGet("/", () => Results.Ok(new 
{ 
    service = "Android Remote Automation System",
    status = "running",
    hubEndpoint = "/androidHub",
    apiEndpoint = "/api/command"
}));

app.Run();

// SignalR Hub for Android clients
public class AndroidHub : Hub
{
    private readonly ILogger<AndroidHub> _logger;

    public AndroidHub(ILogger<AndroidHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        _logger.LogInformation($"Android client connected: {Context.ConnectionId}");
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        _logger.LogInformation($"Android client disconnected: {Context.ConnectionId}");
        await base.OnDisconnectedAsync(exception);
    }

    // Method called by Android to report status
    public async Task SendStatus(string jsonStatus)
    {
        _logger.LogInformation($"Status received from {Context.ConnectionId}: {jsonStatus}");
        
        // You can broadcast status to other clients if needed
        await Clients.Others.SendAsync("StatusUpdate", jsonStatus);
    }
}
