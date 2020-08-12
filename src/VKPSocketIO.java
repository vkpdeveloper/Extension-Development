package com.vkpdeveloper.VKPSocketIO;

import io.socket.client.*;
import com.google.appinventor.components.runtime.util.*;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import io.socket.emitter.Emitter;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.*;
import android.app.Activity;
import com.google.appinventor.components.runtime.ComponentContainer;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@DesignerComponent(version = 1, description = "", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "https://res.cloudinary.com/dfozqceqg/image/upload/v1597208780/plug.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries="socketio.jar")
public class VKPSocketIO extends AndroidNonvisibleComponent {

    private Socket socket;
    private final ComponentContainer container;
    private final String TAG_LOG = "VKPSocketIO";
    private String socketURL = "";
    private final Activity mActivity;
    private String[] listenedEvents = new String[100];

    public VKPSocketIO(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.mActivity = container.$context();
    }

    @DesignerProperty(defaultValue = "http://example.com", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void SocketURL(String value) {
        String trim = value.trim();
        this.socketURL = trim;
    }

    @SimpleEvent
    public void onConnectionFailed(String error){
        EventDispatcher.dispatchEvent(this, "onConnectionFailed", error);
    }


    @SimpleFunction
    public void connectSocket() {
        if (socketURL != "") {
            try{
                socket = IO.socket(this.socketURL);
                socket.connect();
                if(socket.connected()){
                    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                        public void call(Object... args) {
                            new VKPSocketIO(container).onConnect();
                        }

                    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                        public void call(Object... args) {
                            new VKPSocketIO(container).onSocketDisconnect();
                        }

                    });
                }
            } catch (URISyntaxException e) {
                this.onConnectionFailed(e.toString());
            }
        }else{
            throw new YailRuntimeError("Socket URL is not given", "BlANK SOCKET URL");
        }
    }

    @SimpleEvent
    public void onSocketDisconnect() {
        EventDispatcher.dispatchEvent(this, "onSocketDisconnect");
    }

    @SimpleFunction
    public void emitEvent(String event, YailList keys, YailList values) {
        JSONObject obj = new JSONObject();
        if (socketURL != "") {
            if(socket.connected()){
                int size = keys.size();
                for(int i = 0; i <= size - 1; i++){
                    String key = keys.getObject(i).toString();
                    String value = values.getObject(i).toString();
                    obj.put(key, value);
                }
                socket.emit(event, obj);
            }else{
                this.onSocketDisconnect();
            }
        }else{
            throw new YailRuntimeError("Socket URL is not given", "BlANK SOCKET URL");
        }
    }

    @SimpleFunction
    public void disconnectSocket() {
        socket.disconnect();
        if(!socket.connected()){
            this.onSocketDisconnect();
        }
    }

    @SimpleEvent
    public void onConnect() {
        EventDispatcher.dispatchEvent(this, "onConnect");
    }

    @SimpleEvent
    public void onListenerReceived(String eventName, List keys, List values) {
        EventDispatcher.dispatchEvent(this, "onListenerReceived", eventName, keys, values);
    }

    @SimpleFunction
    public String getKeyByIndex(int index, List keys){
        return keys.get(index).toString();
    }


    @SimpleFunction
    public void subscribeEvent(String event) {
        if (socketURL != "") {
            if(socket.connected()){
                final String eventName = event;
                socket.on(event, new Emitter.Listener() {
                    public void call(final Object... args) {
                        mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                if(socket.connected()){
                                    List<String> allKeys =new ArrayList<String>();
                                    List<String> allValues =new ArrayList<String>();
                                    JSONObject obj = new JSONObject();
                                    obj = (JSONObject) args[0];
                                    Set<String> allKeysSet = obj.keySet();
                                    for(String key : allKeysSet){
                                        allKeys.add(key.toString());
                                        allValues.add(obj.get(key).toString());
                                        Log.e(TAG_LOG, key.toString());
                                        Log.e(TAG_LOG, obj.get(key).toString());
                                    }
                                    onListenerReceived(eventName, allKeys, allValues);
                                }
                            }
                        });
                    }
                }
                );

            }else{
                if(socket != null){
                    this.onSocketDisconnect();
                }
            }
        }else{
            throw new YailRuntimeError("Socket URL is not given", "BlANK SOCKET URL");
        }
    }



}
