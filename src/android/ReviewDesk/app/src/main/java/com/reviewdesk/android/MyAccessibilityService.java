package com.reviewdesk.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private HubConnection hubConnection;
    private static MyAccessibilityService instance;
    
    // Server URL - should match your backend IP
    private String serverUrl = "http://192.168.29.7:5000/androidHub";

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Accessibility Service Connected");
        
        // Initialize SignalR connection
        initializeSignalR();
    }

    public static MyAccessibilityService getInstance() {
        return instance;
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.stop();
        }
        initializeSignalR();
    }

    private void initializeSignalR() {
        try {
            // Build SignalR hub connection
            hubConnection = HubConnectionBuilder.create(serverUrl)
                    .withAutomaticReconnect()
                    .build();

            // Register handler for receiving commands from server
            hubConnection.on("ReceiveCommand", (jsonCommand) -> {
                Log.i(TAG, "Received command: " + jsonCommand);
                handleCommand(jsonCommand);
            }, String.class);

            // Connection lifecycle handlers
            hubConnection.onClosed((error) -> {
                Log.e(TAG, "SignalR connection closed", error);
                sendStatusToServer("FAILED", "Connection closed", error != null ? error.getMessage() : "");
            });

            // Start the connection
            hubConnection.start().blockingAwait();
            Log.i(TAG, "SignalR connected successfully to: " + serverUrl);
            sendStatusToServer("READY", "Service connected and ready", "");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing SignalR", e);
            sendStatusToServer("FAILED", "Failed to connect to server", e.getMessage());
        }
    }

    private void handleCommand(String jsonCommand) {
        try {
            JSONObject command = new JSONObject(jsonCommand);
            String action = command.getString("action");
            String targetType = command.optString("targetType", "TEXT");
            String targetValue = command.optString("targetValue", "");
            String extraData = command.optString("extraData", "");

            Log.i(TAG, "Handling action: " + action + ", targetType: " + targetType + ", targetValue: " + targetValue);

            switch (action) {
                case "CLICK":
                    handleClick(targetType, targetValue);
                    break;
                case "TYPE":
                    handleType(targetValue, extraData);
                    break;
                case "SCROLL":
                    handleScroll(targetValue);
                    break;
                case "HOME":
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    sendStatusToServer("SUCCESS", "Pressed HOME button", "");
                    break;
                case "BACK":
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    sendStatusToServer("SUCCESS", "Pressed BACK button", "");
                    break;
                case "RECENTS":
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    sendStatusToServer("SUCCESS", "Opened recent apps", "");
                    break;
                case "OPEN_APP":
                    handleOpenApp(targetValue);
                    break;
                default:
                    sendStatusToServer("FAILED", "Unknown action: " + action, "");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling command", e);
            sendStatusToServer("FAILED", "Error processing command", e.getMessage());
        }
    }

    private void handleClick(String targetType, String targetValue) {
        try {
            if ("COORDS".equals(targetType)) {
                // Handle coordinate-based click
                String[] coords = targetValue.split(",");
                if (coords.length == 2) {
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    clickAtCoordinates(x, y);
                } else {
                    sendStatusToServer("FAILED", "Invalid coordinates format", "Expected: x,y");
                }
            } else {
                // Handle text-based click (search for element by text)
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode == null) {
                    sendStatusToServer("FAILED", "Cannot access window content", "");
                    return;
                }

                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(targetValue);
                if (nodes != null && !nodes.isEmpty()) {
                    boolean clicked = false;
                    for (AccessibilityNodeInfo node : nodes) {
                        if (node.isClickable()) {
                            clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (clicked) {
                                sendStatusToServer("SUCCESS", "Clicked element with text: " + targetValue, "");
                                break;
                            }
                        } else {
                            // Try to click parent if node itself is not clickable
                            AccessibilityNodeInfo parent = node.getParent();
                            if (parent != null && parent.isClickable()) {
                                clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                if (clicked) {
                                    sendStatusToServer("SUCCESS", "Clicked parent of element with text: " + targetValue, "");
                                    break;
                                }
                            }
                        }
                    }
                    if (!clicked) {
                        sendStatusToServer("FAILED", "Found element but could not click", targetValue);
                    }
                } else {
                    sendStatusToServer("FAILED", "Element not found with text: " + targetValue, "");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleClick", e);
            sendStatusToServer("FAILED", "Error clicking element", e.getMessage());
        }
    }

    private void clickAtCoordinates(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 100));
        
        boolean dispatched = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                sendStatusToServer("SUCCESS", "Clicked at coordinates: " + x + "," + y, "");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                sendStatusToServer("FAILED", "Gesture cancelled at: " + x + "," + y, "");
            }
        }, null);

        if (!dispatched) {
            sendStatusToServer("FAILED", "Could not dispatch gesture at: " + x + "," + y, "");
        }
    }

    private void handleType(String targetValue, String textToType) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                sendStatusToServer("FAILED", "Cannot access window content", "");
                return;
            }

            // Find the focused edit text or search by target value
            AccessibilityNodeInfo targetNode = null;
            
            if (!targetValue.isEmpty()) {
                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(targetValue);
                if (nodes != null && !nodes.isEmpty()) {
                    targetNode = nodes.get(0);
                }
            } else {
                targetNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            }

            if (targetNode != null && targetNode.isEditable()) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType);
                boolean success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                
                if (success) {
                    sendStatusToServer("SUCCESS", "Typed text: " + textToType, "");
                } else {
                    sendStatusToServer("FAILED", "Could not type text", "");
                }
            } else {
                sendStatusToServer("FAILED", "No editable field found", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleType", e);
            sendStatusToServer("FAILED", "Error typing text", e.getMessage());
        }
    }

    private void handleScroll(String direction) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                sendStatusToServer("FAILED", "Cannot access window content", "");
                return;
            }

            int action = direction.equalsIgnoreCase("UP") || direction.equalsIgnoreCase("FORWARD") 
                    ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD 
                    : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

            boolean scrolled = rootNode.performAction(action);
            
            if (scrolled) {
                sendStatusToServer("SUCCESS", "Scrolled " + direction, "");
            } else {
                sendStatusToServer("FAILED", "Could not scroll " + direction, "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleScroll", e);
            sendStatusToServer("FAILED", "Error scrolling", e.getMessage());
        }
    }

    private void handleOpenApp(String packageName) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                sendStatusToServer("SUCCESS", "Opened app: " + packageName, "");
            } else {
                sendStatusToServer("FAILED", "App not found: " + packageName, "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleOpenApp", e);
            sendStatusToServer("FAILED", "Error opening app", e.getMessage());
        }
    }

    private void sendStatusToServer(String status, String message, String extraInfo) {
        try {
            if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                JSONObject statusObj = new JSONObject();
                statusObj.put("status", status);
                statusObj.put("message", message);
                statusObj.put("deviceTime", getCurrentTime());
                if (!extraInfo.isEmpty()) {
                    statusObj.put("extraInfo", extraInfo);
                }

                String jsonStatus = statusObj.toString();
                hubConnection.send("SendStatus", jsonStatus);
                Log.i(TAG, "Status sent: " + jsonStatus);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending status to server", e);
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This method is called when accessibility events occur
        // We don't need to handle events for this implementation
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Accessibility Service Interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (hubConnection != null) {
            hubConnection.stop();
        }
        instance = null;
        Log.i(TAG, "Accessibility Service Destroyed");
    }
}
